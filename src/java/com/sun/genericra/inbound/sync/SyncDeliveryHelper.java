/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.sync;

import com.sun.genericra.inbound.*;

import com.sun.genericra.AbstractXAResourceType;
import com.sun.genericra.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.logging.*;

import javax.jms.*;

import javax.resource.*;
import javax.resource.spi.endpoint.*;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;



/**
 * Helper class that delivers a message to MDB.
 * Important assumptions:
 * - There is one delivery helper for each message(delivery).
 * - Redelivery will be carried out by the same SyncDeliveryHelper.
 * @author Binod P.G
 */
public class SyncDeliveryHelper {
    private static Logger _logger;
    private static TxMgr mTxMgr;
    static {
        _logger = LogUtils.getLogger();
        mTxMgr = new TxMgr();
    }
    
    com.sun.genericra.inbound.ActivationSpec spec;
    SyncJmsResource jmsResource;
    XAResource xar;
    Message msg = null;
    Destination dest = null;
    boolean transacted;
    boolean sentToDmd = false;
    boolean redeliveryFailed = false;
    Coordinator coord = null;
    boolean mHoldUntilAck;

    int acktimeout;
   
    
    public SyncDeliveryHelper(SyncJmsResource jmsResource,
            SyncJmsResourcePool pool) {
        this.spec = pool.getConsumer().getSpec();
        this.jmsResource = jmsResource;
        this.transacted = pool.isTransacted();
        
        mHoldUntilAck = this.spec.getHUAMode();
        acktimeout = this.spec.getAckTimeOut();
        AbstractXAResourceType xarObject = null;
        
        if (redeliveryRequired()) {
            if (this.spec.getUseFirstXAForRedelivery()) {
                xarObject = new FirstXAResourceProxy(jmsResource.getXAResource());
            } else {
                xarObject = new InboundXAResourceProxy(jmsResource.getXAResource());
            }
        } else {
            xarObject = new SimpleXAResourceProxy(jmsResource.getXAResource());
            
            //this.xar = jmsResource.getXAResource();
        }
        
        xarObject.setRMPolicy(this.spec.getRMPolicy());
        xarObject.setConnection(pool.getConnection());
        this.xar = xarObject;
    }
    
    public boolean redeliveryRequired() {
        return this.transacted && (this.spec.getRedeliveryAttempts() > 0);
    }
    
    public XAResource getXAResource() {
        return this.xar;
    }
    
    private DeadMessageProducer createProducer(Connection con, Destination dest)
    throws JMSException {
        
        return new DeadMessageProducer(con, this.jmsResource.getPool(), dest);
    }
    
    public void sendMessageToDMD() {
        _logger.log(Level.FINE, "Trying to send message  to DMD :" + dest);
        Session session = null;
        DeadMessageProducer msgProducer = null;
        Exception dmdexception = null;
        boolean dmdSendSuccess = true;
        try {
            if ((this.dest != null) && this.spec.getSendBadMessagesToDMD()) {
                _logger.log(Level.FINE, "Sending the message to DMD :" + dest);
                
                
                if (redeliveryRequired()) {
                    AbstractXAResourceType localXar = (AbstractXAResourceType) this.xar;
                    if (localXar.endCalled() == false) {
                        localXar.end(null, XAResource.TMSUCCESS);
                    }
                    localXar.prepare(null);
                    _logger.log(Level.FINE, "Prepared DMD transaction");
                } else {
                    AbstractXAResourceType localXar = (AbstractXAResourceType) this.xar;
                    localXar.end(null, XAResource.TMSUCCESS);
                    localXar.prepare(null);
                    _logger.log(Level.FINE, "Prepared DMD transaction");
                }
                Connection connection = jmsResource.getPool()
                .getConnectionForDMD();
                msgProducer = createProducer(connection, this.dest);
                msgProducer.send(this.msg);
                _logger.log(Level.FINE, "Sent message to DMD");
                if (redeliveryRequired()) {
                    AbstractXAResourceType localXar = (AbstractXAResourceType) this.xar;
                    localXar.commit(null, false);
                    _logger.log(Level.FINE, "Commited DMD transaction");
                    
                } else {
                    AbstractXAResourceType localXar = (AbstractXAResourceType) this.xar;
                    localXar.commit(null, false);
                    _logger.log(Level.FINE, "Commited DMD transaction");
                }
                /**
                 * We know that if commit/prepare fails we may have
                 * the message in the DMD, the message would be present in
                 * both destinations.
                 * Results in duplicate message in DMD.
                 */
                
            } else {
                dmdSendSuccess = false;
            }
        }catch (Exception e) {
            dmdSendSuccess = false;
            dmdexception = e;
            e.printStackTrace();
        }finally {
            this.msg = null;
            this.dest = null;
            this.sentToDmd = false;
            
            if (msgProducer != null) {
                try {
                    msgProducer.close();
                } catch (Exception me) {
                    me.printStackTrace();
                }
            }
        }
        if (!dmdSendSuccess) {
            if (redeliveryRequired()) {
                _logger.log(Level.SEVERE, "FAILED : sending message to DMD");
            }else {
                _logger.log(Level.SEVERE, "FAILED : sending message to DMD");
                AbstractXAResourceType localXar = (AbstractXAResourceType) this.xar;
                localXar.setToRollback(true);
                try {
                    localXar.rollback(null);
                } catch (Exception e) {
                    _logger.log(Level.SEVERE, "FAILED : to rollback XA" + e.getMessage());
                }
            }
        }
    }
    
    public void deliver(Message message, Destination d){
        this.msg = message;
        this.dest = d;
        deliver();
    }
    
    public void deliver() {
        
        /*
         * For now lets us judt deliver the message and not bother about
         * redelivery . We should just call deliver message here.
         * Before that we need to start the inbound transaction by calling
         * before delivery on the endpoint.
         *
         * TODO : Fix redelivery here.
         */
        try {
            if (this.transacted) {
                runOnceStdXA();
            } else {
                runOnceStdNoXA();
            }           
            _logger.log(Level.FINE,"Completed delivery ");
        } catch (Exception ee) {
            
            ee.printStackTrace();
        }
        
    }
    
    public void markForDMD() {
        this.sentToDmd = true;
    }
    
    public boolean markedForDMD() {
        return this.sentToDmd;
    }
    
    private void deliverMessage(Message message) throws ResourceException {
        MessageEndpoint endPoint = jmsResource.getEndpoint();
        
        try {
            _logger.log(Level.FINEST,
                    "Now it is feeding the message to MDB instance");
            ((javax.jms.MessageListener) endPoint).onMessage(message);
        } catch (Exception e) {
            if (transacted) {
                throw ExceptionUtils.newResourceException(e);
            }
        }
    }
    
    
    
    private void runOnceStdXA() throws Exception {
        // The MDB may move the transaction to a different thread
        int myattempts = 0;
        int attempts = this.spec.getRedeliveryAttempts();
        
        AbstractXAResourceType localXar = null;
        Transaction tx = null;
        
        if (this.msg != null) {
            
            while (true) {
                try {
                    coord = newCoord();
                    msg = mHoldUntilAck ? wrapMsg(msg, coord, -1) : msg;
                    if (this.transacted){
                        tx = getTransaction(true);
                        _logger.log(Level.FINE,"Got the transaction " + tx);
                    }
                    deliverMessage(msg);                    
                    _logger.log(Level.FINE,"Delivered the message");
                    if (redeliveryRequired()) {
                        
                    /*
                     * Make the XA rollback  enable.
                     *  This is because we will start the XA now.
                     */
                        localXar =  (AbstractXAResourceType) xar;
                        localXar.startDelayedXA();
                        localXar.setToRollback(true);
                    }
                    coord.msgDelivered(true);
                    break;
                }catch (ResourceException r) {                    
                    _logger.log(Level.FINE,"Exception during Delivery, running redelivery logic");
                    if (redeliveryRequired()) {
                    /*
                     * Do not allow roll back here, because we know that
                     * the XA is not started, we start the XA only when delivery
                     * is successful, here delivery has failed.
                     */
                        localXar =  (AbstractXAResourceType) xar;
                        localXar.setToRollback(false);
                        try {
                            _logger.log(Level.FINE,
                                    "Setting JMSRedelivered header on message");
                            msg.setJMSRedelivered(true);
                            
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        AbstractXAResourceType simpleXar =  (AbstractXAResourceType) this.xar;
                        simpleXar.setToRollback(false);
                        coord.setRollbackOnly(r);
                    }
                    if (myattempts < attempts) {
                        myattempts++;
                        
                        _logger.log(Level.FINEST,
                                "Releasing the endpoint after an exception");
                        this.jmsResource.releaseEndpoint();
                        
                        try {
                            Thread.sleep(spec.getRedeliveryInterval() * 1000);
                            _logger.log(Level.FINE,
                                    "getting the endpoint after an exception");
                            this.jmsResource.refresh();
                        } catch (Exception ie) {
                            ie.printStackTrace();
                        }
                    } else {
                        
                        this.markForDMD();
                        
                        /**
                         * Start XA now.
                         * And we also know that the resource is our proxy
                         */
                        try {
                            this.msg.setJMSRedelivered(false);
                            _logger.log(Level.FINE, "Resetting JMS redelivered header");
                        } catch (Exception jmse) {
                            _logger.log(Level.FINE, "Cannot reset JMS redelivered header");;
                        }
                        if (redeliveryRequired()) {
                            localXar.startDelayedXA();
                        }
                        /**
                         * Start the transaction now so that the DMD delivery
                         * can go on.
                         */
                        /**
                         * If all the delivery attempts fail we have to throw an
                         * exception so that the message listener throws an exception
                         * and the broker will retain the message.
                         * If we do not propagate the error status from here
                         * the broker loses the message and we also lose it.
                         */
                        
                        return;
                    }
                }
            }
            
        }
        
        if (!this.markedForDMD()){
            coord.waitForAcks();
            
            // If the transaction was moved to a different thread, take it back
            _logger.log(Level.FINE, "Is there a TX associated here " + getTransaction(true));
            if (this.transacted && (getTransaction(true) == null)) {                
                mTxMgr.getTransactionManager().resume(tx);
                _logger.log(Level.FINE, "Resumed the transaction ");
            }
            
            if (this.transacted && coord.isRollbackOnly()) {
                _logger.log(Level.FINE, "Setting to RollBack because coordinator was rollback");
                getTransaction(true).setRollbackOnly();
            }            
            _logger.log(Level.FINE,"Releasing the Endpoint");
            this.jmsResource.releaseEndpoint();
            _logger.log(Level.FINE,"Released the Endpoint");
        }
    }
    
    private boolean mTxFailureLoggedOnce;
    
    private Transaction getTransaction(boolean mustSucceed) {
        if (mTxMgr != null) {
            try {
                return mTxMgr.getTransactionManager().getTransaction();
            } catch (Exception e) {
                if (mustSucceed) {
                    throw new RuntimeException("Failed to obtain handle to transaction: " + e, e);
                }
            }
        }
        return null;
    }
    
    private Message wrapMsg(Message toCopy, AckHandler ack, int iBatch) throws JMSException {
        WMessageIn ret;
        
        // Check for multiple interfaces
        int nItf = 0;
        
        if (toCopy instanceof TextMessage) {
            nItf++;
            ret = new WTextMessageIn((TextMessage) toCopy, ack, iBatch);
        } else if (toCopy instanceof BytesMessage) {
            nItf++;
            ret = new WBytesMessageIn((BytesMessage) toCopy, ack, iBatch);
        } else if (toCopy instanceof MapMessage) {
            nItf++;
            ret = new WMapMessageIn((MapMessage) toCopy, ack, iBatch);
        } else if (toCopy instanceof ObjectMessage) {
            nItf++;
            ret = new WObjectMessageIn((ObjectMessage) toCopy, ack, iBatch);
        } else if (toCopy instanceof StreamMessage) {
            nItf++;
            ret = new WStreamMessageIn((StreamMessage) toCopy, ack, iBatch);
        } else {
            nItf++;
            ret = new WMessageIn(toCopy, ack, iBatch);
        }
        
        if (nItf > 1) {
            throw new JMSException("Cannot determine message type: the message "
                    + "implements multiple interfaces.");
        }
        
        //  ret.setBatchSize(mBatchSize);
        
        return ret;
    }
    
    
    
    private void runOnceStdNoXA() throws Exception {
    	
    	// Issue 41
    	// The functionality for hold until ack mode is incomplete and of unknown quality,
    	// and definitely doesn't work for NoXA mode
    	// comment it all out
    	
        if (msg != null) {
//            // Optionally wrap for ack() call
//            /*
//             * TODO : The message  wrappers need to be optimized
//             * we might not beed one wrapper class for each message
//             * type
//             */
//            coord = newCoord();
//            msg = mHoldUntilAck ? wrapMsg(msg, coord, -1) : msg;
//            try {
                deliverMessage(msg);
//                coord.msgDelivered(true);
//            }catch (ResourceException r) {
//                coord.setRollbackOnly(r);
//            }
//            
//            // Wait for ack() to be called if applicable
//            coord.waitForAcks();
//            
//            // Commit/rollback
//            if (!coord.isRollbackOnly()) {
//                this.jmsResource.getSession().commit();
//            } else {
//                this.jmsResource.getSession().rollback();
//            }
//            this.jmsResource.releaseEndpoint();
        }
    }
    
    private abstract class Coordinator extends AckHandler {
        public abstract void setRollbackOnly();
        
        public abstract void setRollbackOnly(Exception e);
        
        public abstract void ack(boolean isRollbackOnly, Message m) throws JMSException;
        
        public abstract boolean isRollbackOnly();
        
        public abstract void msgDelivered(boolean wasDelivered);
        
        public abstract void waitForAcks() throws InterruptedException;
        
        public abstract boolean needsToDiscardEndpoint();
        
        public abstract void setNeedsToDiscardEndpoint();
        
        
    }
    
    private class NonHUACoordinator extends Coordinator {
        private boolean mIsRollbackOnly;
        private boolean mNeedsToDiscardEndpoint;
        private int mNMsgsDelivered;
        
        public void setRollbackOnly() {
            mIsRollbackOnly = true;
        }
        
        public void setRollbackOnly(Exception e) {
            if (e != null) {
                setRollbackOnly();
                mNeedsToDiscardEndpoint = true;
            }
        }
        
        public void ack(boolean isRollbackOnly, Message m) throws JMSException {
        }
        
        public boolean isRollbackOnly() {
            return mIsRollbackOnly;
        }
        
        public void msgDelivered(Exception e) {
        }
        
        public void msgDelivered(boolean wasDelivered) {
            if (wasDelivered) {
                mNMsgsDelivered++;
            }
        }
        
        public void waitForAcks() throws InterruptedException {
        }
        
        public boolean needsToDiscardEndpoint() {
            return mNeedsToDiscardEndpoint;
        }       
      
        
        public void setNeedsToDiscardEndpoint() {
            mNeedsToDiscardEndpoint = true;
            
        }
    }
    
    private class HUACoordinator extends Coordinator {
        private Semaphore mSemaphore = new Semaphore(0);
        private int mNAcksToExpect;
        private boolean mIsRollbackOnly;
        private boolean mNeedsToDiscardEndpoint;
        private int mNMsgsDelivered;
        
        public synchronized void setRollbackOnly() {
            mIsRollbackOnly = true;
        }
        
        public void setRollbackOnly(Exception e) {
            if (e != null) {
                setRollbackOnly();
                mNeedsToDiscardEndpoint = true;
            }
        }
        
        public void ack(boolean isRollbackOnly, Message m) throws JMSException {
            if (isRollbackOnly) {
                setRollbackOnly();               
            _logger.log(Level.FINE, "Setting rollback only");
            }
            mSemaphore.release();
            _logger.log(Level.FINE, "Released Semaphore here");
        }
        
        public synchronized boolean isRollbackOnly() {
            return mIsRollbackOnly;
        }
        
        public void msgDelivered(boolean wasDelivered) {
            if (wasDelivered) {
                mNAcksToExpect++;
                mNMsgsDelivered++;
            }
        }
        
		public void waitForAcks() throws InterruptedException {

			_logger.log(Level.FINE, "Tying to acquire a semaphore");
			if (!mSemaphore.tryAcquire(acktimeout, TimeUnit.SECONDS)) {
				_logger.log(Level.FINE, "Acquired");
				setRollbackOnly();
			}
			/*
			 * if (jmsResource.getIsWorkStopped()) { setRollbackOnly(); return; }
			 */
		}
        
        public boolean needsToDiscardEndpoint() {
            return mNeedsToDiscardEndpoint;
        }
        
       
        
        public void setNeedsToDiscardEndpoint() {
            mNeedsToDiscardEndpoint = true;
        }
    }
    
    public SyncJmsResource getJmsResource() {
        return jmsResource;
    }
    
    private Coordinator newCoord() {
        if (mHoldUntilAck) {
            return new HUACoordinator();
        } else {
            return new NonHUACoordinator();
        }
    }
}

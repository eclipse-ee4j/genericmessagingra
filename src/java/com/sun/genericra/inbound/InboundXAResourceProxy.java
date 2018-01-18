/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound;

import com.sun.genericra.AbstractXAResourceType;
import com.sun.genericra.XAResourceType;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import com.sun.genericra.util.LogUtils;

import java.util.logging.*;

/**
 * <code>XAResource</code> wrapper for Generic JMS Connector. This class
 * intercepts all calls to the actual XAResource to facilitate redelivery.
 *
 *  Basically each (re)delivery for message will happen in different transactions
 *  from appserver perspective. However they will be intercepted and only
 *  one XID will be actually used with JMS provider.
 *
 *  @author Binod P.G
 */
public class InboundXAResourceProxy extends AbstractXAResourceType {
    private XAResource xar = null;
    private boolean toRollback = true;
    private boolean rolledback = false;
    private boolean suspended = false;
    private boolean endCalled = false;
    private static Logger logger;
    private Xid startXid = null;
      static {
        logger = LogUtils.getLogger();
    }
  
    private int startflags;
    private boolean startedDelayedXA = false;
    
    public InboundXAResourceProxy(XAResource xar) {
        this.xar = xar;
    }

    /**
     * Commit the global transaction specified by xid.
     *
     * @param xid
     *            A global transaction identifier
     * @param onePhase
     *            If true, the resource manager should use a one-phase commit
     *            protocol to commit the work done on behalf of xid.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        
        debugxid("Commiting inbound transaction with ID ", xid);
             
        if (xid != null)
        {
            debugxid("Committed inbound transaction with ID ", xid);
            xar.commit(xid, onePhase);
        }
        else
        {            
            debugxid("Committed inbound transaction with ID " , startXid);
            xar.commit(startXid, onePhase);   
        }
    }

    /**
     * Ends the work performed on behalf of a transaction branch.
     *
     * @param xid
     *            A global transaction identifier that is the same as what was
     *            used previously in the start method.
     * @param flags
     *            One of TMSUCCESS, TMFAIL, or TMSUSPEND
     */
    public void end(Xid xid, int flags) throws XAException {
        debugxid("Ending inbound transaction with ID ", xid);
        
        if (xid == null)
        {
            xid = startXid;
        }
        
        if ((startedDelayedXA == true) && (endCalled == false))
        {
            /**
             * Lets make sure that we are calling end only for the transaction
             * that was actually started.
             *
             */
            debugxid("Ended inbound transaction with ID ", xid);
            endCalled = true; 
            xar.end(xid, flags);
        }
        

        /*
        if (beingRedelivered() == false) {
            debug("XAResourceProxy END , Xid ended is " +
                printXid(savedXid()));

            try {
                Thread.sleep(250);
                xar.end(savedXid(), flags);
            } catch (XAException xae) {
                debug("Got an exception at XAR end, " +
                    savedXid());
                debug("Error Code is " + xae.errorCode);

                if (xae.errorCode == XAException.XA_RBROLLBACK) {
                    debug("Got an RB_ROLLBACK error code");
                }

                xae.printStackTrace();

                // xar.rollback(savedXid());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (flags == XAResource.TMSUSPEND) {
                suspended = true;
            }

            endCalled = true;
        }
         */
    }

    /**
     * When message is being redelivered, i.e, end is called
     * and that too without TMSUSPEND flag, return true.
     *
     * This also assumes that, when the message is being
     * redelivered, the MDB wouldnt be coded to such that
     * transaction would need to be suspended.
     */
    private boolean beingRedelivered() {
        return (endCalled == true) && (suspended == false);
    }

    /**
     * Tell the resource manager to forget about a heuristically completed
     * transaction branch.
     *
     * @param xid
     *            A global transaction identifier
     */
    public void forget(Xid xid) throws XAException {
        xar.forget(xid);
    }

    /**
     * Obtain the current transaction timeout value set for this
     * <code>XAResource</code> instance.
     *
     * @return the transaction timeout value in seconds
     */
    public int getTransactionTimeout() throws XAException {
        return xar.getTransactionTimeout();
    }

    /**
     * This method is called to determine if the resource manager instance
     * represented by the target object is the same as the resouce manager
     * instance represented by the parameter xares.
     *
     * @param xares
     *            An <code>XAResource</code> object whose resource manager
     *            instance is to be compared with the resource
     * @return true if it's the same RM instance; otherwise false.
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        XAResource inxa = xares;

        if (xares instanceof XAResourceType) {
            XAResourceType wrapper = (XAResourceType) xares;
            inxa = (XAResource) wrapper.getWrappedObject();

            if (!compare(wrapper)) {
                return false;
            }
        }

        return xar.isSameRM(inxa);
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the
     * transaction specified in xid.
     *
     * @param xid
     *            A global transaction identifier
     * @return A value indicating the resource manager's vote on the outcome of
     *         the transaction. The possible values are: XA_RDONLY or XA_OK. If
     *         the resource manager wants to roll back the transaction, it
     *         should do so by raising an appropriate <code>XAException</code>
     *         in the prepare method.
     */
    public int prepare(Xid xid) throws XAException {
        debugxid("Preparing inbound transaction with ID ", xid);
        if (xid == null)
        {
            debugxid("Prepared inbound transactionw with ID ", startXid);
            return xar.prepare(startXid);
        }
        else
        {
            debugxid("Prepared inbound transactionw with ID ", xid);
            return xar.prepare(xid);
        }
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager.
     *
     * @param flag
     *            One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be
     *            used when no other flags are set in flags.
     * @return The resource manager returns zero or more XIDs for the
     *         transaction branches that are currently in a prepared or
     *         heuristically completed state. If an error occurs during the
     *         operation, the resource manager should throw the appropriate
     *         <code>XAException</code>.
     */
    public Xid[] recover(int flag) throws XAException {
        return xar.recover(flag);
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a
     * transaction branch
     *
     * @param xid
     *            A global transaction identifier
     */
    public void rollback(Xid xid) throws XAException {
        debugxid("Rolling back inbound transaction with ID ", xid);


        //rolledback = true;

        if (toRollback) {
            xar.rollback(xid);            
            debugxid("Rolled back transaction with ID ", xid);
        }
    }

    /**
     * Set the current transaction timeout value for this
     * <code>XAResource</code> instance.
     *
     * @param seconds
     *            the transaction timeout value in seconds.
     * @return true if transaction timeout value is set successfully; otherwise
     *         false.
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xar.setTransactionTimeout(seconds);
    }

    /**
     * Start work on behalf of a transaction branch specified in xid.
     *
     * @param xid
     *            A global transaction identifier to be associated with the
     *            resource
     * @return flags One of TMNOFLAGS, TMJOIN, or TMRESUME
     */
    public void start(Xid xid, int flags) throws XAException {


        if (startedDelayedXA)
        {
            debugxid("Delayed start of inbound transaction with ID ", startXid);
            xar.start(xid, flags);            
        }
        else
        {
            startXid = xid;
            if (flags != xar.TMRESUME){
                startflags = flags;
            }            
        }
        

        /*
        
        if (beingRedelivered()) {
            debug("redelivering " + printXid(xid));

            return;
        }

        int actualflag = flags;

        if (this.savedxid == null) {
            debug("DBG : Starting XA id" + printXid(xid));
            this.savedxid = xid;
        } else if (flags == XAResource.TMNOFLAGS) {
            if (rolledback) {
                rolledback = false;
                endCalled = false;

                if (suspended) {
                    suspended = false;
                    actualflag = XAResource.TMRESUME;
                } else {
                    actualflag = XAResource.TMJOIN;
                }
            }
        }

        xar.start(savedXid(), actualflag);
         */
    }

    public Object getWrappedObject() {
        return this.xar;
    }

    public void setToRollback(boolean flag) {
        toRollback = flag;
    }

    public boolean endCalled() {
        return endCalled;
    }

    /*
    private Xid savedXid() {
        return this.savedxid;
    }
     */

    
    public void startDelayedXA()
    {
        /**
         * This is done to cover the redelivery feature.
         * We make sure that start on an XA is called only when the message
         * delivery to the endpoint is successful. 
         * If not there will be new transactions started by TM for every
         * delivery attempt, and the same cannot be mnanaged with the broker's RM. 
         */
        try
        {
            debugxid("Delayed start of inboudn transaction (in startDelated) ",startXid);
            xar.start(startXid, startflags);            
        }
        catch (XAException xae)
        {
            // What can we do with this ?
            // Only make sure that end fails.
            xae.printStackTrace();
        }
        catch (Exception  e)
        {
            e.printStackTrace();
        }
        startedDelayedXA = true;
    }
    
    void debug(String s) {
        logger.log(Level.FINEST, "InboundXAResourceProxy : " + s);
    }    
    
    void debugxid(String s, Xid xid) {
        if (logger.getLevel() == Level.FINEST) {
            logger.log(Level.FINEST, s + printXid(xid));
        }
    }

}

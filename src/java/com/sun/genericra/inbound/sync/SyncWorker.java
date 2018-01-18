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
import com.sun.genericra.util.*;

import java.util.logging.*;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import javax.resource.spi.work.*;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

public class SyncWorker implements Work {
    private static Logger _logger;
    static {
        _logger = LogUtils.getLogger();
    }
    
    private volatile boolean mIsStopped = true;
    private Object mIsStoppedLock = new Object();
    private javax.jms.MessageConsumer mReceiver;
    private javax.jms.Session mSess;
    private SyncJmsResource resource = null;
    private int sessionid;
    private static long TIMEOUT = 100;
    
    private static long WAIT_TIMEOUT = 1000;
    boolean requiresrefresh = true;
    /**
     * Constructor
     *
     * @param name threadname
     */
    public SyncWorker(SyncJmsResource res) throws JMSException {
        this.resource = res;
        sessionid = this.resource.getSessionid();
        mReceiver = this.resource.getReceiver();
    }
    
    
    /**
     * Closes the allocated resources. Must be called after the thread has ended.
     */
    private void close() {
        
        /* Close the connection here  ?, no, there might be
         * other sessions using the connection, close only the receiver.
         */
        if (mReceiver != null) {
            try {
                mReceiver.close();
            } catch (JMSException e) {
                _logger.log(Level.WARNING, "Non-critical failure to close a " +
                        "message consumer: " + e);
            }
            mReceiver = null;
        }
        //Thread.dumpStack();
        _logger.log(Level.FINE,"Closed Synchronouse receiver #" +
                sessionid);
        // this.resource.releaseEndpoint();
    }
    
    
    
    /**
     * Called by a separate Thread: polls the receiver until it is time to exit
     * or if an exception occurs.
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        synchronized (mIsStoppedLock) {
            if (!mIsStopped) {
                return;
            }
            mIsStopped = false;
        }
        _logger.log(Level.INFO,"Starting synchronouse receiver #" +
                sessionid);
        for (;;) {
            try {
		//_logger.log(Level.FINEST,"Running Receiver #" + sessionid);
                if (requiresrefresh) {
		_logger.log(Level.FINE,"Refreshing Receiver #" + sessionid);
                    this.resource.refreshListener();
                    this.resource.refresh();
                    requiresrefresh = false;
		_logger.log(Level.FINE,"Refresed Receiver #" + sessionid);
                }
                Message m = mReceiver.receive(TIMEOUT);
                if (m != null) {
                    requiresrefresh = true;
                    SyncDeliveryHelper helper = this.resource.getDeliveryHelper();
                    /* The destination will not be null when the TODO for
                     * dmd is done
                     */
                    
                    /* The helper is supposed to call onMessage on the endpoint
                     * and depending on the the success /failure of it  it has to
                     * do the needful to the inbound message (commit/rollback).
                     */
                    helper.deliver(m, this.resource.getPool().getConsumer().getDmdDestination());
		_logger.log(Level.FINE,"Delivered message Receiver #" + sessionid);
                }else {
                    requiresrefresh = false;
                }
                
                synchronized (mIsStoppedLock) {
                    if (mIsStopped) {
                        _logger.log(Level.INFO, "Stopping synchronous receiver #" +
                                sessionid);
                        mIsStoppedLock.notifyAll();
                        break;
                    }
                }
                
            } catch (Exception ex) {
                _logger.log(Level.SEVERE, "Exception during receive , Receiver #" +
                        sessionid + ex);
                break;
            } catch (Throwable ex) {
                _logger.log(Level.SEVERE, "Exception during receive, Receiver #"
                        + sessionid + ex);
                break;
            } finally {
                if (requiresrefresh) {
                    try {
                        this.resource.releaseEndpoint();
                    } catch (Exception ee) {
                        ;
                    }
                    SyncDeliveryHelper helper = this.resource.getDeliveryHelper();
                    if (helper.markedForDMD()) {
                        helper.sendMessageToDMD();
                    }
                    
                }
                /* Check for DMD sending part, at this point, tha XA will
                 * be in end state, its a bit tricky if we want to include
                 * the DMD sending part in the same inbound transaction.
                 * If DMD send is successful then we have to commit inbound,
                 * else we have to roll back. This would guarantee that there
                 * is no message loss. TODO
                 */
                
            }
            
        }
        /* Shutdown everything and close the thread.
         * We expect this to happen only when the consumer is closed.
         * We dont have a logic to create new receiver threads instead of
         * a closed thread, so this thread should be alive for the duration of
         * the consumer
         */
        this.resource.releaseEndpoint();
         _logger.log(Level.FINE, "Closing the receiver from run #" + sessionid);
        close();
    }
    /**
     * Indicates if this object has been stopped
     *
     * @return true if the state is stopped
     */
    public boolean isStopped() {
        synchronized (mIsStoppedLock) {
            return mIsStopped;
        }
    }
    public void release() {
        synchronized (mIsStoppedLock) {
            if (mIsStopped) {
                return;
            }
            _logger.log(Level.FINE, "Stopping the receiver #" + sessionid);
            mIsStopped = true;
            try {
                mIsStoppedLock.wait(WAIT_TIMEOUT);
            }catch (InterruptedException ie) {
                _logger.log(Level.FINE, "Notification received for the receiver #" + sessionid);
            }
            /* Close it here just to be sure that the session is not closed
             * before the receiver is,
             */
            _logger.log(Level.FINE, "Closing the receiver from release #" + sessionid);
            close();
            
        }
    }
    
}

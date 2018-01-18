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

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.inbound.*;
import com.sun.genericra.inbound.async.DeliveryHelper;
import com.sun.genericra.inbound.async.MessageListener;
import com.sun.genericra.inbound.async.WorkImpl;
import com.sun.genericra.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.*;

import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.resource.spi.work.*;

import javax.transaction.xa.XAResource;


/**
 *
 */
public class SyncJmsResource extends AbstractJmsResource {
    private static Logger _logger;
    
    static {
        _logger = LogUtils.getLogger();
    }
    private boolean free;
    
    private SyncDeliveryHelper helper;
    
    private Work w ;
    
    private boolean stopWork = false;
    
    private int sessionid;
    
    public SyncJmsResource(Session session, SyncJmsResourcePool pool)
    throws JMSException {        
        this(session, pool, null);
    }  
    
    
    public SyncJmsResource(Session session, SyncJmsResourcePool pool,
            XAResource xaresource) throws JMSException {
        super(session, pool, xaresource);
    }
    
    public void setSessionid(int id) {
        sessionid = id;
    }
    public int getSessionid() {
        return sessionid;
    }
    public MessageConsumer getReceiver() throws JMSException {
        return getPool().createMessageConsumer(session);
    }
    public void start() throws JMSException {
        try {
            _logger.log(Level.FINER,
                    "Sync Provider is starting the message consumtion #" + sessionid);
            
            w = new SyncWorker(this);
            WorkManager wm = ra.getWorkManager();
            wm.scheduleWork(w);
        } catch (WorkException we) {
            throw ExceptionUtils.newJMSException(we);
        } 
    }
    
    /**
     * Each time a serversession is checked out from the pool, the listener
     * will be recreated.
     */
    public void refreshListener() throws JMSException {
        
        helper = new SyncDeliveryHelper(this, (SyncJmsResourcePool)pool);
        
      //  return this;
    }
    
    public boolean getIsWorkStopped() {
        return stopWork;
    }
    
    public void destroy() {        
        if (session != null) {
            try {
                stopWork = true;
                w.release();
                _logger.log(Level.FINE, "Released the Worker the session #" + sessionid);
                session.close();
                _logger.log(Level.FINE, "Closed the session #" + sessionid);
            } catch (Exception e) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, e.getMessage(), e);
                }
            }
        }        
       // releaseEndpoint();
    }
    
    public SyncDeliveryHelper getDeliveryHelper() {
        return this.helper;
    }
    
    public XAResource getXAResource() {
        return this.xaresource;
    }
    
    public Session getSession() {
        _logger.log(Level.FINEST, "Message provider got the session :" +
                session);
        
        return session;
    }
    
    public XASession getXASession() {
        return (XASession) session;
    }
    
    /**
     * Creates the MessageEndpoint and start the delivery.
     */
    public void refresh() throws JMSException {
        MessageEndpointFactory mef = pool.getConsumer()
        .getMessageEndpointFactory();       
        try {
            _logger.log(Level.FINER, "Creating message endpoint #" + sessionid +
                    xaresource);
            endPoint = mef.createEndpoint(helper.getXAResource());
            endPoint.beforeDelivery(this.ra.getListeningMethod());
            _logger.log(Level.FINE, "Created endpoint  #" + sessionid);
        } catch (Exception e) {
            e.printStackTrace();
            _logger.log(Level.SEVERE, "Refresh resource failed #" + sessionid);
            // TODO. Should we eat this exception?
            //throw ExceptionUtils.newJMSException(e);
        }
    }
    
    /**
     * Completes the Message delivery and release the MessageEndpoint.
     */
    public void releaseEndpoint() {
        try {
            if (this.endPoint != null) {
                this.endPoint.afterDelivery();
                _logger.log(Level.FINE,"After Delivery success in SyncJmsResource");
            }
        } catch (Exception re) {
            _logger.log(Level.SEVERE, "After delivery failed in resource #" + sessionid
                        + re.getMessage());
        } finally {
            this.release();
            /*
            if (this.endPoint != null) {
                try {
                    this.endPoint.release();
                    _logger.log(Level.FINE, "SyncJmsResource: released endpoint in #" 
                            + sessionid);
                } catch (Exception e) {
                    _logger.log(Level.SEVERE,
                            "SyncJmsResource: release endpoint failed #" + sessionid);                    
                }                
                this.endPoint = null;
            }
             */
        }
    }
    
    public void release() {
            if (this.endPoint != null) {
                try {
                    this.endPoint.release();
                    _logger.log(Level.FINE, "SyncJmsResource: released endpoint in #" 
                            + sessionid);
                } catch (Exception e) {
                    _logger.log(Level.SEVERE,
                            "SyncJmsResource: release endpoint failed #" + sessionid);                    
                }                
                this.endPoint = null;
            }        
    }
}

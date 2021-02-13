/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.async;

import com.sun.genericra.inbound.*;
import com.sun.genericra.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.jms.*;
import jakarta.resource.spi.endpoint.*;
import jakarta.resource.spi.work.*;

import javax.transaction.xa.XAResource;


/**
 * ServerSession implementation as per JMS 1.1 specification.
 * This serves as a placeholder for a MessageEndpoint obtained from
 * application server.
 *
 * @author Binod P.G
 */
public class InboundJmsResource extends AbstractJmsResource implements ServerSession {
    private static Logger _logger;

    static {
        _logger = LogUtils.getLogger();
    }
    private DeliveryHelper helper;

    public InboundJmsResource(Session session, InboundJmsResourcePool pool)
        throws JMSException {
        super(session, pool, null);
    }

    public InboundJmsResource(Session session, InboundJmsResourcePool pool,
        XAResource xaresource) throws JMSException {
        super(session, pool, xaresource);
    }

    public void start() throws JMSException {
        try {
            _logger.log(Level.FINER,
                "Provider is starting the message consumtion");

            Work w = new WorkImpl(this);
            WorkManager wm = ra.getWorkManager();
            wm.scheduleWork(w);
        } catch (WorkException e) {
            throw ExceptionUtils.newJMSException(e);
        }
    }

    /**
     * Each time a serversession is checked out from the pool, the listener
     * will be recreated.
     */
    public InboundJmsResource refreshListener() throws JMSException {
        MessageListener listener = new MessageListener(this,(InboundJmsResourcePool) pool);
        this.session.setMessageListener(listener);
          helper = new DeliveryHelper(this, (InboundJmsResourcePool)pool);

        return this;
    }

    public void destroy() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, e.getMessage(), e);
                }
            }
        }

        releaseEndpoint();
    }

    public boolean isFree() {
        return free;
    }

    public InboundJmsResource markAsBusy() {
        this.free = false;

        return this;
    }

    public InboundJmsResource markAsFree() {
        this.free = true;

        return this;
    }

    public DeliveryHelper getDeliveryHelper() {
        return this.helper;
    }



    public Session getSession() {
        _logger.log(Level.FINEST, "Message provider got the session :" +
            session);

        return session;
    }



    public void release() {
        ((InboundJmsResourcePool)getPool()).put(this);
    }

    /**
     * Creates the MessageEndpoint and start the delivery.
     */
    public void refresh() throws JMSException {
        MessageEndpointFactory mef = pool.getConsumer()
                                         .getMessageEndpointFactory();

        try {
            _logger.log(Level.FINER, "Creating message endpoint : " +
                xaresource);
            endPoint = mef.createEndpoint(helper.getXAResource());
            endPoint.beforeDelivery(this.ra.getListeningMethod());
            _logger.log(Level.FINE, "Created endpoint : ");
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "Refresh resource failed");
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
            }
        } catch (Exception re) {
            _logger.log(Level.SEVERE, "After delivery failed " + re.getMessage(), re);
        } finally {
            if (this.endPoint != null) {
                try {
                    this.endPoint.release ();
                    _logger.log(Level.FINE, "InboundJMSResource: released endpoint : ");
                } catch (Exception e) {
                    _logger.log(Level.SEVERE,
                        "InboundJMSResource: release endpoint failed ");
                    ;
                }

                this.endPoint = null;
            }
        }
    }
}

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
import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.inbound.*;
import com.sun.genericra.inbound.async.DeliveryHelper;
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
 * @author rp138409
 */
public abstract class AbstractJmsResource {
    protected static Logger _logger;
    
    static {
        _logger = LogUtils.getLogger();
    }
    
    protected Session session;
    protected XAResource xaresource;
    protected MessageEndpoint endPoint;
    protected boolean free;
    protected GenericJMSRA ra;
    protected AbstractJmsResourcePool pool;
    protected DeliveryHelper helper;
    
    public AbstractJmsResource(Session session, AbstractJmsResourcePool pool,
            XAResource xaresource) throws JMSException {
        this.session = session;
        this.xaresource = xaresource;
        this.pool = pool;
        this.ra = (GenericJMSRA) pool.getConsumer().getResourceAdapter();
    }
    public AbstractJmsResourcePool getPool() {
        return this.pool;
    }
    
    public XASession getXASession() {
        return (XASession) session;
    }
      public XAResource getXAResource() {
        return this.xaresource;
    }  
    public MessageEndpoint getEndpoint() {
        return this.endPoint;
    }
}

/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.outbound;

import com.sun.genericra.util.Constants;

import javax.jms.*;

import javax.resource.spi.security.PasswordCredential;


/**
 * MCF for javax.jms.QueueConnectionFactory
 * @author Sivakumar Thyagarajan
 */
public class ManagedQueueConnectionFactory
    extends AbstractManagedConnectionFactory {
    public ManagedQueueConnectionFactory() {
        this.destinationMode = Constants.QUEUE_SESSION;
    }

    protected String getActualConnectionFactoryClassName() {
        if (this.getSupportsXA()) {
            return this.getXAQueueConnectionFactoryClassName();
        } else {
            return this.getQueueConnectionFactoryClassName();
        }
    }

    protected XAConnection createXAConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((XAQueueConnectionFactory) cf).createXAQueueConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((XAQueueConnectionFactory) cf).createXAQueueConnection();
        }
    }

    protected Connection createConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((QueueConnectionFactory) cf).createQueueConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((QueueConnectionFactory) cf).createQueueConnection();
        }
    }
}

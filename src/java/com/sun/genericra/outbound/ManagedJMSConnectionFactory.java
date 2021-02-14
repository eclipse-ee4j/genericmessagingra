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

import jakarta.jms.*;

import jakarta.resource.spi.security.PasswordCredential;


/**
 * ManagedConnectionFactory implementation for jakarta.jms.ConnectionFactory.
 * @author Sivakumar Thyagarajan
 */
public class ManagedJMSConnectionFactory
    extends AbstractManagedConnectionFactory {
    public ManagedJMSConnectionFactory() {
    }

    protected String getActualConnectionFactoryClassName() {
        if (this.getSupportsXA()) {
            return this.getXAConnectionFactoryClassName();
        } else {
            return this.getConnectionFactoryClassName();
        }
    }

    protected jakarta.jms.XAConnection createXAConnection(PasswordCredential pc,
        jakarta.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((XAConnectionFactory) cf).createXAConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((XAConnectionFactory) cf).createXAConnection();
        }
    }

    protected jakarta.jms.Connection createConnection(PasswordCredential pc,
        jakarta.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((jakarta.jms.ConnectionFactory) cf).createConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((jakarta.jms.ConnectionFactory) cf).createConnection();
        }
    }
}

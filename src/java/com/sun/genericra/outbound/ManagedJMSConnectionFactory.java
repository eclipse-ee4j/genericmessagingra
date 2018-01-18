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

import javax.jms.*;

import javax.resource.spi.security.PasswordCredential;


/**
 * ManagedConnectionFactory implementation for javax.jms.ConnectionFactory.
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

    protected javax.jms.XAConnection createXAConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((XAConnectionFactory) cf).createXAConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((XAConnectionFactory) cf).createXAConnection();
        }
    }

    protected javax.jms.Connection createConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((javax.jms.ConnectionFactory) cf).createConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((javax.jms.ConnectionFactory) cf).createConnection();
        }
    }
}

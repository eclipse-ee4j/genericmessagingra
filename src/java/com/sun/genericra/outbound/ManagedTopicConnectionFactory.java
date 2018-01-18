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
 * MCF for javax.jms.TopicConnectionFactory
 * @author Sivakumar Thyagarajan
 */
public class ManagedTopicConnectionFactory
    extends AbstractManagedConnectionFactory {
    public ManagedTopicConnectionFactory() {
        this.destinationMode = Constants.TOPIC_SESSION;
    }

    protected String getActualConnectionFactoryClassName() {
        if (this.getSupportsXA()) {
            return this.getXATopicConnectionFactoryClassName();
        } else {
            return this.getTopicConnectionFactoryClassName();
        }
    }

    protected XAConnection createXAConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((XATopicConnectionFactory) cf).createXATopicConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((XATopicConnectionFactory) cf).createXATopicConnection();
        }
    }

    protected Connection createConnection(PasswordCredential pc,
        javax.jms.ConnectionFactory cf) throws JMSException {
        if (pc != null && (!pc.getUserName().equals(""))) {
            return ((TopicConnectionFactory) cf).createTopicConnection(pc.getUserName(),
                new String(pc.getPassword()));
        } else {
            return ((TopicConnectionFactory) cf).createTopicConnection();
        }
    }
}

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

import jakarta.jms.JMSException;


/**
 * Topic Wrapper
 * @author Sivakumar Thyagarajan
 */
public class TopicProxy extends DestinationAdapter implements jakarta.jms.Topic {
    protected String getDestinationClassName() {
        return this.getTopicClassName();
    }

    public String getTopicName() throws JMSException {
        return ((jakarta.jms.Topic) this._getPhysicalDestination()).getTopicName();
    }
}

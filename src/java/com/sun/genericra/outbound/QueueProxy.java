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

import javax.jms.JMSException;


/**
 * Queue wrapper
 * @author Sivakumar Thyagarajan
 */
public class QueueProxy extends DestinationAdapter implements javax.jms.Queue {
    public String getQueueName() throws JMSException {
        return ((javax.jms.Queue) this._getPhysicalDestination()).getQueueName();
    }

    protected String getDestinationClassName() {
        return this.getQueueClassName();
    }
}

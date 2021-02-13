/*
 * Copyright (c) 2003, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.sync;

import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;

import java.io.Serializable;

/**
 * See WMessage
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1 $
 */
public class WObjectMessageIn extends WMessageIn implements ObjectMessage {
    private ObjectMessage mDelegate;
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     * @param ackHandler callback to call when ack() or recover() is called
     * @param ibatch index of this message in a batch; -1 for non-batched
     */
    public WObjectMessageIn(ObjectMessage delegate, AckHandler ackHandler, int ibatch) {
        super(delegate, ackHandler, ibatch);
        mDelegate = delegate;
    }

    /**
     * @see jakarta.jms.ObjectMessage#getObject()
     */
    public Serializable getObject() throws JMSException {
        return mDelegate.getObject();
    }

    /**
     * @see jakarta.jms.ObjectMessage#setObject(java.io.Serializable)
     */
    public void setObject(Serializable arg0) throws JMSException {
        mDelegate.setObject(arg0);
    }

}

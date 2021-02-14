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

import jakarta.jms.JMSException;
import jakarta.jms.Message;

/**
 * A callback that can be installed in a message that should be triggered when 
 * acknowledge() is called on the message. The message must make sure that the 
 * callback is called only once.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public abstract class AckHandler {
    
    /**
     * @param isRollbackOnly true if setRollbackOnly was called first
     * @param m message on which this was called
     * @throws JMSException delegated
     */
    public abstract void ack(boolean isRollbackOnly, Message m) throws JMSException;
}


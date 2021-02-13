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

import jakarta.jms.Message;


/**
 * MessageListener for the server session. This is the basic receiver of
 * the message.
 *
 * @author Binod P.G
 */
public class MessageListener implements jakarta.jms.MessageListener {
    private static Logger _logger;

    static {
        _logger = LogUtils.getLogger();
    }

    private static boolean debug = false;
    private InboundJmsResource jmsResource;
    private InboundJmsResourcePool pool;
    private EndpointConsumer consumer;

    public MessageListener(InboundJmsResource jmsResource,
        InboundJmsResourcePool pool) {
        this.jmsResource = jmsResource;
        this.consumer = (EndpointConsumer)pool.getConsumer();
    }

    public void onMessage(Message message){
        if (debug) {
            _logger.log(Level.FINE, "Consuming the message :" + message);
        }
        consumer.consumeMessage(message, jmsResource);
    }
}

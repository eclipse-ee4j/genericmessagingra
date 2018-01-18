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

import com.sun.genericra.inbound.async.InboundJmsResourcePool;
import com.sun.genericra.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.*;


/**
 * InternalMessageProducer implementation used by
 * Inbound code that send the messages to dead
 * message destination.
 *
 * The objective of this class is to handle
 * any type of JMS destination
 *
 * @author Binod P.G
 */
public class DeadMessageProducer {
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    private Session session; 
    private String destinationType;
    private MessageProducer producer;

    public DeadMessageProducer(Connection con, AbstractJmsResourcePool pool, Destination dest)
                throws JMSException {
        
        destinationType = pool.getConsumer().getSpec()
                              .getDeadMessageDestinationType();
        logger.log(Level.FINE,
            "DeadMessageDestinationType is" +
            " obtained for message sender : " + destinationType);
         
        createSession(con);
        createProducer(dest);
    }

    private void createSession(Connection con) throws JMSException {
        if (destinationType.equals(Constants.QUEUE)) {
            session = ((QueueConnection) con).createQueueSession(false,
                    Session.AUTO_ACKNOWLEDGE);
        } else if (destinationType.equals(Constants.TOPIC)) {
            session = ((TopicConnection) con).createTopicSession(false,
                    Session.AUTO_ACKNOWLEDGE);
        } else {
            session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        }
    }

    private void createProducer(Destination dest) throws JMSException {
        if (destinationType.equals(Constants.QUEUE)) {
            producer = ((QueueSession) session).createSender((Queue) dest);
        } else if (destinationType.equals(Constants.TOPIC)) {
            producer = ((TopicSession) session).createPublisher((Topic) dest);
        } else {
            producer = session.createProducer(dest);
        }
    }

    public void send(Message msg) throws JMSException {
        try {
            if (destinationType.equals(Constants.QUEUE)) {
                ((QueueSender) producer).send(msg);
            } else if (destinationType.equals(Constants.TOPIC)) {
                ((TopicPublisher) producer).publish(msg);
            } else {
                producer.send(msg);
            }
        } catch (Exception e) {
            throw ExceptionUtils.newJMSException(e);
        }
    }

    public void close() throws JMSException {
        try {
            if (producer != null) {
                producer.close();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, e.getMessage(), e);
        }

        try {
            session.close();
        } catch (Exception e) {
            logger.log(Level.FINE, e.getMessage(), e);
        }
    }
}

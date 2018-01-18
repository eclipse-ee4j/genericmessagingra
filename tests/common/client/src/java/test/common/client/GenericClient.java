/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.common.client;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class GenericClient {

	/**
	 * Using the specified connection factory, drain all messages from the specified queue
	 * and return the number that were read.
     *
	 * @param connectionFactoryJNDIName
	 * @param queueJNDIname
	 * @return The number of messages in the queue
	 * @throws NamingException
	 * @throws JMSException
	 */
	protected static int drainQueue(String connectionFactoryJNDIName, String queueJNDIname) throws NamingException, JMSException {
	
		Context jndiContext = new InitialContext();
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) jndiContext.lookup(connectionFactoryJNDIName);
		Queue queue = (Queue) jndiContext.lookup(queueJNDIname);
		QueueConnection queueConnection = queueConnectionFactory.createQueueConnection();
		QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueReceiver queueReceiver = queueSession.createReceiver(queue);
		queueConnection.start();
		int numMessages = 0;
		while (true) {
			Message message = queueReceiver.receive(1000);
			if (message == null)
				break;
			numMessages++;
		}
		if (numMessages > 0) {
			System.out.println("Read " + numMessages + " from queue with JNDI name " + queueJNDIname + "and physical queue name "+queue.getQueueName());
		}
		queueConnection.close();
		
		return numMessages;
	}

}

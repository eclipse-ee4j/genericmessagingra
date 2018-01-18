/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.simple.queue.client;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import test.common.client.GenericClient;

public class SimpleMessageClient extends GenericClient {

	static private long MDB_SLEEP_TIME = 2000;
	static private boolean debug = false;

	public static void main(String[] args) throws Exception {

		boolean pass = true;

		drainQueue("java:comp/env/jms/QCFactory","java:comp/env/jms/inboundQueue");
		drainQueue("java:comp/env/jms/QCFactory","java:comp/env/jms/outboundQueue");

		// send one message to the MDB
		send();

		System.out.println("Now reading the reply queue");
		Context jndiContext = null;
		QueueConnectionFactory queueConnectionFactory = null;
		QueueConnection queueConnection = null;
		QueueSession queueSession = null;
		Queue queue = null;
		QueueReceiver queueReceiver = null;

		try {
			jndiContext = new InitialContext();
			queueConnectionFactory = (QueueConnectionFactory) jndiContext.lookup("java:comp/env/jms/QCFactory");
			queue = (Queue) jndiContext.lookup("java:comp/env/jms/outboundQueue");

			queueConnection = queueConnectionFactory.createQueueConnection();
			queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			queueConnection.start();
			queueReceiver = queueSession.createReceiver(queue);

			TextMessage msg = (TextMessage) queueReceiver.receive(10000);
			if (msg == null) {
				System.out.println("Expected message not received");
				pass = false;
			} else {
				String reply = msg.getText();
				if (!reply.equals("REPLIED:CLIENT")) {
					System.out.println("Reply message contains unexpected payload");
					pass = false;
				}

				// Check no more messages can be received
				TextMessage msg2 = (TextMessage) queueReceiver.receive(1000);
				if (msg2 != null) {
					pass = false;
					System.out.println("Received more messages than expected");
				}
			}

			if (pass) {
				System.out.println("Concurrent message delivery test - Queue Stess Test : PASS");
			} else {
				System.out.println("Concurrent message delivery test - Queue Stress Test : FAIL");
			}
			
			queueConnection.close();
		} catch (Throwable t) {
			t.printStackTrace();
			System.out.println("Concurrent message delivery test - Queue stress test :  FAIL");
		}
		System.exit(0);
	}


	public static void send() {

		Context jndiContext = null;
		QueueConnectionFactory queueConnectionFactory = null;
		QueueConnection queueConnection = null;
		QueueSession queueSession = null;
		Queue queue = null;
		QueueSender queueSender = null;
		TextMessage message = null;

		try {
			jndiContext = new InitialContext();
			queueConnectionFactory = (QueueConnectionFactory) jndiContext.lookup("java:comp/env/jms/QCFactory");
			queue = (Queue) jndiContext.lookup("java:comp/env/jms/inboundQueue");

			try {
				queueConnection = queueConnectionFactory.createQueueConnection();
				queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				queueConnection.start();
				queueSender = queueSession.createSender(queue);
				message = queueSession.createTextMessage();
				message.setText("CLIENT");
				message.setIntProperty("id", 0);
				message.setLongProperty("sleeptime", MDB_SLEEP_TIME);
				queueSender.send(message);
				debug("Send the message :" + message.getIntProperty("id") + ":" + message.getText());
			} catch (Exception e) {
				System.out.println("Exception occurred: " + e.toString());
			} finally {
				if (queueConnection != null) {
					try {
						queueConnection.close();
					} catch (JMSException e) {
					}
				}
			}
		} catch (Throwable e) {
			System.out.println("Exception occurred: " + e.toString());
		}
	}

	static void debug(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
}

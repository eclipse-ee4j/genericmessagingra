/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.performance.queue.client;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import test.common.client.GenericClient;

public class PerformanceClient extends GenericClient {
	
	public static void main(String[] args) throws JMSException, InterruptedException, NamingException{
			
		// check only one arg supplied
		if (args.length!=1) {
			for (int i = 0; i < args.length; i++) {
				System.out.println("args["+i+"]="+args[i]);
			}
			throw new RuntimeException("Args: sender | receiver (you supplied "+args.length+" args)");
		}
		
		// invoke appropriate code dependent on arg
		String arg = args[0];
		if (arg.equalsIgnoreCase("sender")){
			Sender sender = new Sender();
			sender.runSender();
		} else  if (arg.equalsIgnoreCase("receiver")){
			Receiver receiver = new Receiver();
			receiver.runReceiver();
		} else {
			throw new RuntimeException("Invalid args: only sender or receive may be supplied as an argument");
		}		
	}
	
	
	private static class Sender extends GenericPerformanceClient {
		
		//static final String clientID = "performance_sender_clientid";
		static final String clientID = null;

		
		/**
		 * @param args
		 *            the command line arguments
		 * @throws NamingException
		 */
		public void runSender() throws JMSException, InterruptedException, NamingException {

			Sender sender = new Sender();

			// set the delivery mode of the outgoing message
			int deliveryMode = DeliveryMode.PERSISTENT;

			// sleep for sleepTime ms after sending each message
			int sleepTime = 0;
			if (sleepTime > 0) {
				System.out.println("Sleeping between messages: sleepTime=" + sleepTime);
			}
			
			int noOfBatches = 10;
			int batchSize = 10000;
			
			try {
				sender.repeatedlySendMessagesToDestination("queue", INBOUND_QUEUE_JNDI_NAME, batchSize, deliveryMode, noOfBatches,
					sleepTime);
			} finally {
				sender.tidyup();
			}
		}

		private Connection dataDestConnection;
		private Session dataDestSession;
		private MessageProducer dataDestProducer;

		private Connection getDataDestConnection() throws JMSException, NamingException {

			if (dataDestConnection == null) {
				Context context = getInitialContext();
				QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(CONNECTION_FACTORY_JNDI_NAME);
				dataDestConnection = qcf.createQueueConnection();
				if (clientID != null)
					dataDestConnection.setClientID(clientID);
			}
			return dataDestConnection;
		}

		private Session getDataDestSession() throws JMSException, NamingException {

			if (dataDestSession == null) {
				Connection connection = getDataDestConnection();
				dataDestSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
			return dataDestSession;
		}

		private MessageProducer getDataDestProducer(String queueOrTopic, String dataDestName) throws JMSException,
				NamingException {

			if (dataDestProducer == null) {
				Connection connection = getDataDestConnection();
				Session session = getDataDestSession();

				Context context = getInitialContext();
				Destination dest = null;
				if (queueOrTopic.equalsIgnoreCase("queue")) {
					dest = (Queue) context.lookup(dataDestName);
				} else if (queueOrTopic.equalsIgnoreCase("topic")) {
					dest = (Topic) context.lookup(dataDestName);
				} else {
					System.out.println("Unrecognised destination type " + queueOrTopic + " (must be queue or topic)");
				}

				dataDestProducer = session.createProducer(dest);
				connection.start();
			}
			return dataDestProducer;
		}

		/**
		 * 
		 * @param destJNDIName
		 * @param batchSize
		 * @param deliveryMode
		 * @param numberOfTimes
		 * @param sleepTime
		 *            Sleep for sleepTime ms after sending each message
		 * @throws javax.jms.JMSException
		 * @throws NamingException
		 */
		public void repeatedlySendMessagesToDestination(String queueOrTopic, String destJNDIName, int batchSize,
				int deliveryMode, int numberOfTimes, int sleepTime) throws JMSException, NamingException {

			for (int i = 0; i < numberOfTimes; i++) {

				System.out.println("Started sending batch " + (i + 1) + " of " + numberOfTimes);
				sendMessagesToDestination(queueOrTopic, destJNDIName, batchSize, deliveryMode, sleepTime);
				System.out.println("Finished sending batch " + (i + 1) + " of " + numberOfTimes);
				if ((i + 1) < numberOfTimes) {
					waitForResume(RESUME_QUEUE_JNDI_NAME, i + 1);
				}
			}
		}

		/**
		 * Wait until a message with the specified resumeID has been received from
		 * the specified resumeQueue
		 * 
		 * resumeID is stored in the integer message property "resumeID"
		 * 
		 * @param resultQueueName
		 * @param resumeIDRequired
		 * @throws javax.jms.JMSException
		 * @throws NamingException
		 */
		public void waitForResume(String resumeQueueJNDIName, int resumeIDRequired) throws JMSException,
				NamingException {
			
			QueueReceiver queueReceiver = getResumeQueueReceiver();

			int noOfAttempts=0;
			boolean notReceived = true;
			do {
				noOfAttempts++;
				if (noOfAttempts==20){
					throw new RuntimeException("Given up waiting for resume message");
				}
				System.out.println("Waiting for resume message with resumeID=" + resumeIDRequired
						+ " before any more messages will be sent");
				TextMessage message = (TextMessage) queueReceiver.receive(10000);
				if (message != null) {
					Object resumeIDObject = message.getObjectProperty("resumeID");
					if (resumeIDObject==null){
						System.out.println("Resume message received with resumeID=null, ignored");
					} else {
						int resumeIDReceived = message.getIntProperty("resumeID");
						if (resumeIDReceived == resumeIDRequired) {
							System.out.println("Received expected resume message with resumeID=" + resumeIDReceived);
							notReceived = false;
						} else {
							System.out.println("Received unexpected resume message with resumeID=" + resumeIDReceived + ", ignored");
						}
					}
				}
			} while (notReceived);

		}

		/**
		 * @param queueOrTopic
		 * @param destJNDIName
		 * @param noOfMessages
		 * @param deliveryMode
		 * @param sleepTime
		 *            Sleep for sleepTime ms after sending each message
		 * @throws javax.jms.JMSException
		 * @throws NamingException
		 */
		public void sendMessagesToDestination(String queueOrTopic, String destJNDIName, int noOfMessages, int deliveryMode,
				int sleepTime) throws JMSException, NamingException {

			System.out.println("Sending " + noOfMessages + " messages to " + queueOrTopic + " " + destJNDIName);

			MessageProducer producer = getDataDestProducer(queueOrTopic, destJNDIName);
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < noOfMessages; i++) {
				TextMessage tm = getDataDestSession().createTextMessage(
						"The quick brown fox jumped over the lazy dog: Text message " + i);
				int priority = Message.DEFAULT_PRIORITY;
				long ttl = Message.DEFAULT_TIME_TO_LIVE;
				tm.setLongProperty("MessNo", Long.valueOf(i));
				producer.send(tm, deliveryMode, priority, ttl);
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException ex) {
					Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
				}
				if ((i % 1000) == 0 && i > 0) {
					long timeFor1000 = System.currentTimeMillis() - startTime;
					// System.out.println("No of messages sent is "+i+", time = "+timeFor1000+" current rate is "+1000000.0/timeFor1000);
					startTime = System.currentTimeMillis();
				}
			}
		}

		private void tidyup() throws JMSException {

			super.tidyUp();

			if (dataDestConnection != null) {
				dataDestConnection.close();
				dataDestConnection = null;
			}
			dataDestProducer = null;
		}

	}
	
	public static class Receiver extends GenericPerformanceClient{
    
	    // client ID used for receiving messages from the data destination. Set to null if you don't want to set clientID
//	    /static final String clientID = "receiver_clientid";
	    static final String clientID = null;

	    /**
	     * @param args the command line arguments
	     * @throws NamingException 
	     */
	    public void runReceiver() throws JMSException, NamingException {

	        int noOfBatches = 10;
	        int batchSize = 10000;
	             
	        // if the destination is a topic and this is not null, create a durable subscription with the specified name
	        String subname="mysub";
	        
	        String queueOrTopic="queue";
	        
	        
	        drainQueue(CONNECTION_FACTORY_JNDI_NAME, RESUME_QUEUE_JNDI_NAME);
	        try {
	        	repeatedlyReceiveMessagesFromDestination(queueOrTopic, OUTBOUND_QUEUE_JNDI_NAME, subname, batchSize, noOfBatches);
	        } finally {
	        	tidyup();
	        }
	        // if we've got this far, the test has passed
	        System.out.println("Receiver finished successfully: test PASS");
	    }
	    
	    private Connection dataDestConnection;
	    private Session dataDestSession;
	    private MessageConsumer dataDestConsumer;
	    // used to maintain rolling mean of last ten rates
	    private int dollop = 0;
	    private final int numberOfRatesSaved = 10;
	    private float[] rates = new float[numberOfRatesSaved];

	    private void repeatedlyReceiveMessagesFromDestination(String queueOrTopic, String destJNDIName, String subname, int batchSize, int numberOfTimes) throws JMSException, NamingException {

	        for (int i = 0; i < numberOfTimes; i++) {
	            System.out.println("Started receiving "+batchSize+ " messages: this is batch "+(i+1)+" of "+numberOfTimes);
	            // receive a batch of messages from the queue
	            int firstHalf = batchSize / 2;
	            int secondHalf = batchSize - firstHalf;
	            receiveMessagesFromDestination(queueOrTopic, destJNDIName, subname, firstHalf);
	            // we have received half the messages, send the resume message to request the next batch
	            // unless this is the final batch
	            if ((i + 1) < numberOfTimes) {
	                int resumeID = i + 1;
	                sendResume(resumeID);
	            }
	            receiveMessagesFromDestination(queueOrTopic, destJNDIName, subname, secondHalf);
	            System.out.println("Finished receiving "+batchSize+ " messages: that was batch "+(i+1)+" of "+numberOfTimes);
	        }
	    }

	    public void receiveMessagesFromDestination(String queueOrTopic, String dataDestJNDIName, String subname, int noOfMessages) throws JMSException, NamingException {
	    	  	
	        MessageConsumer consumer = getDataDestConsumer(queueOrTopic,dataDestJNDIName,subname);
	        long startTime=0;
	        
	        for (int i = 0; i < noOfMessages; i++) {
	            TextMessage tm = null;
	            while (tm==null) {
	                tm = (TextMessage) consumer.receive(100);
	            }
	            if (i==0) {
	                 // start the clock when first message is received,
	                 // so so calculate rate divide time by noOfmessages-1
	                 startTime = System.currentTimeMillis();
	            }
	        }

	        float timeTakenMillis = System.currentTimeMillis()-startTime;
	        float timeTakenSecs=timeTakenMillis/1000;
	        float rate = (noOfMessages-1)/timeTakenSecs;
	        System.out.println("Time to receive last "+noOfMessages+" messages was "+timeTakenSecs+" sec, rate was "+rate+" msgs/sec");

	        // calculate rolling mean of last "numberOfRatesSaved" speed reports
	        dollop++;
	        int dollopIndex = dollop % numberOfRatesSaved;
	        rates[dollopIndex]=rate;

	        if (dollop>=numberOfRatesSaved){
	            float cumulRate=0;
	            for (int i = 0; i < numberOfRatesSaved; i++) {
	                cumulRate = cumulRate + rates[i];
	            }
	            float rollingMeanRate=cumulRate/numberOfRatesSaved;
	            System.out.println("Rolling mean of last "+numberOfRatesSaved+" rate reports is "+rollingMeanRate+" msgs/sec");
	        }
	    }

	    private  void sendResume(int resumeID) throws JMSException, NamingException {

	        QueueSession queueSession = getResumeQueueSession();
	        QueueSender queueSender = getResumeQueueSender();

	        TextMessage textMessage = queueSession.createTextMessage();
	        textMessage.setIntProperty("resumeID", resumeID);
	        queueSender.send(textMessage);
	        System.out.println("Sent resume message with resumeID=" + resumeID);

	    }

	    private Connection getDataDestConnection() throws JMSException, NamingException {

	        if (dataDestConnection==null){
	            Context context = getInitialContext();
	            QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(CONNECTION_FACTORY_JNDI_NAME);
	        	dataDestConnection = qcf.createQueueConnection();
	           if (clientID!=null) dataDestConnection.setClientID(clientID);
	        }
	        return dataDestConnection;
	    }

	    private Session getDataDestSession() throws JMSException, NamingException{

	        if (dataDestSession==null){
	            Connection connection = getDataDestConnection();
	            dataDestSession = connection.createSession(false, Session.DUPS_OK_ACKNOWLEDGE);
	        }
	        return dataDestSession;
	    }

	    private MessageConsumer getDataDestConsumer(String queueOrTopic, String dataDestName, String subname) throws JMSException, NamingException{
	    	
	        if (dataDestConsumer==null){
	        	Connection connection = getDataDestConnection();
	        	Session session = getDataDestSession();
	        	
	            Context context = getInitialContext();
	        	Destination dest=null;
	            if (queueOrTopic.equalsIgnoreCase("queue")){
	                dest = (Queue) context.lookup(dataDestName);
	            } else if (queueOrTopic.equalsIgnoreCase("topic")) {
	                dest = (Topic) context.lookup(dataDestName);
	            } else {
	                System.out.println("Unrecognised destination type "+queueOrTopic+" (must be queue or topic)");
	            }
	    	
	        	if (queueOrTopic.equalsIgnoreCase("topic") && (subname!=null)){
	        		dataDestConsumer = session.createDurableSubscriber((Topic)dest, subname);
	        	} else {
	        		dataDestConsumer = session.createConsumer(dest);
	        	}
	        	connection.start();
	        }
	        return dataDestConsumer;
	    }

	    private void tidyup() throws JMSException {
	    	
	    	super.tidyUp();

	        if (dataDestConnection!=null){
	        	dataDestConnection.close();
	        	dataDestConnection=null;
	        }
	        dataDestConsumer=null;
	    }	     
	    
	}	
	
	private static class GenericPerformanceClient {

	    static final String CONNECTION_FACTORY_JNDI_NAME = "jms/QCFactory";
	    static final String OUTBOUND_QUEUE_JNDI_NAME = "jms/outboundQueue";
	    static final String INBOUND_QUEUE_JNDI_NAME = "jms/inboundQueue";
	    static final String RESUME_QUEUE_JNDI_NAME = "jms/resumeQueue";
		
		protected Context getInitialContext() throws NamingException {
			return new InitialContext();
		}

		private QueueConnection resumeQueueConnection;
		private QueueSession resumeQueueSession;
		private QueueSender resumeQueueSender;
		private QueueReceiver resumeQueueReceiver;

		private QueueConnection getResumeQueueConnection() throws JMSException, NamingException {

			if (resumeQueueConnection == null) {
				Context context = getInitialContext();
				QueueConnectionFactory qcf = (QueueConnectionFactory) context.lookup(CONNECTION_FACTORY_JNDI_NAME);
				resumeQueueConnection = qcf.createQueueConnection();
				resumeQueueConnection.start();
			}
			return resumeQueueConnection;
		}

		protected QueueSession getResumeQueueSession() throws JMSException, NamingException {

			if (resumeQueueSession == null) {
				QueueConnection queueConnection = getResumeQueueConnection();
				// use DUPS_OK to make receier as fast as possible, since we don't
				// want it to be the rate-determining step
				resumeQueueSession = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
			}
			return resumeQueueSession;
		}

		protected QueueSender getResumeQueueSender() throws JMSException, NamingException {

			if (resumeQueueSender == null) {
				QueueSession queueSession = getResumeQueueSession();

				Context context = getInitialContext();
				Queue queue = (Queue) context.lookup(RESUME_QUEUE_JNDI_NAME);

				resumeQueueSender = queueSession.createSender(queue);
			}
			return resumeQueueSender;
		}

		protected QueueReceiver getResumeQueueReceiver() throws JMSException, NamingException {

			if (resumeQueueReceiver == null) {
				QueueSession queueSession = getResumeQueueSession();

				Context context = getInitialContext();
				Queue queue = (Queue) context.lookup(RESUME_QUEUE_JNDI_NAME);

				resumeQueueReceiver = queueSession.createReceiver(queue);
			}
			return resumeQueueReceiver;
		}

		public void tidyUp() throws JMSException {
	        if (resumeQueueConnection!=null){
	            resumeQueueConnection.close();
	            resumeQueueConnection=null;
	        }
	        resumeQueueSender=null;
	        resumeQueueReceiver=null;
		}

	}

}

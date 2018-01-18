/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.s1peqe.connector.mq.simplestress.client;

import javax.jms.*;
import javax.naming.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple stress test with Topic and appclient and MDB configured  with a unique client id. The test starts NUM_CLIENT threads. Each thread sends
 * NUM_CYCLES messages to a Topic. An MDB consumes these messages concurrently
 * and responds with a reply Message to a ReplyQueue. While the threads are being exercised,
 * an attempt to read NUM_CLIENT*NUM_CYCLES messages is performed. An attempt to
 * read an extra message is also done to ascertain that no more than
 * NUM_CLIENT*NUM_CYCLES messages are available.
 */

public class SimpleMessageClient implements Runnable{
    static int NUM_CLIENTS = 3;
    static int NUM_CYCLES = 3;
    static int TIME_OUT = 60000;
    static long MDB_SLEEP_TIME = 2000;
    static boolean debug = true;
    int id =0;

    public SimpleMessageClient(int i) {
        this.id = i;
    }

    public static void main(String[] args) {
        /**
	 * Start the threads that will send messages to MDB
	 */
        ArrayList al = new ArrayList();
        try {
            for (int i =0; i < NUM_CLIENTS; i++) {
                Thread client = new Thread(new SimpleMessageClient(i));
		al.add(client);
                client.start();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        Context                 jndiContext = null;
        QueueConnectionFactory  queueConnectionFactory = null;
        QueueConnection         queueConnection = null;
        QueueSession            queueSession = null;
        Queue                   queue = null;
        QueueReceiver           queueReceiver = null;
        
        TextMessage             message = null;

        try {
            jndiContext = new InitialContext();
            queueConnectionFactory = (QueueConnectionFactory)
                jndiContext.lookup
                ("java:comp/env/jms/QCFactory");
            
            queue = (Queue) jndiContext.lookup("java:comp/env/jms/clientQueue");
            queueConnection =
                queueConnectionFactory.createQueueConnection();
            queueSession =
                queueConnection.createQueueSession(false,
                    Session.AUTO_ACKNOWLEDGE);
            queueConnection.start();
            queueReceiver = queueSession.createReceiver(queue);
            
	    HashMap map = new HashMap();

            long startTime = System.currentTimeMillis();
            boolean pass = true;
	    //
	    // Receives all the messages and keep in the data structure
	    //
            for (int i =0; i < (NUM_CLIENTS * NUM_CYCLES); i++) {
                TextMessage msg = (TextMessage) queueReceiver.receive(TIME_OUT);
                System.out.print("." + i);
                if (msg == null) {
                    pass = false;
                    System.out.println("Reecived only " + i + " messages");
                    break;
                }
		Integer id = new Integer(msg.getIntProperty("replyid"));
		if (map.containsKey(id)) {
		    pass = false;
		    debug("Duplicate :" + id);
		}
		map.put(id, msg.getText());
            }
            long totalTime = System.currentTimeMillis() - startTime;
	    System.out.println("Received messages in :" + totalTime + " milliseconds");
	    System.out.println("------------------------------------------------------");

            // 
	    // Now examine the received data
	    //
            for (int i =0; i < NUM_CLIENTS * NUM_CYCLES; i++) {
	        String reply = (String) map.get(new Integer(i));
		if (!reply.equals("REPLIED:CLIENT")) {
		   pass = false;
		}
		System.out.println("Receeived :" + i + ":" + reply);
	    }
            
	    // Try to receive one more message than expected.

	    System.out.println("Now attempting to read a message before timeout of " +  TIME_OUT + "ms - negative case check");
            TextMessage msg = (TextMessage) queueReceiver.receive(TIME_OUT);

	    if (msg != null) {
	       pass = false;
	       System.out.println("Received more than expected number of messages :" + msg.getText());
	    }

            if (pass) {
                System.out.println("Concurrent message delivery test - Topic Client ID : PASS " );
            } else {
                System.out.println("Concurrent message delivery test - Topic Client ID : FAIL");
	    }
        }catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Concurrent message delivery test  - Topic Client ID : FAIL");
        }finally {
		    for (int i=0; i <al.size(); i++) {
		       Thread client = (Thread) al.get(i);
		       try {
		          client.join();
		       } catch (Exception e) {
		          System.out.println(e.getMessage());
	       }
	    }
            System.exit(0);
        }
         

    }

    public void run() {

        Context                 jndiContext = null;
        TopicConnectionFactory  topicConnectionFactory = null;
        TopicConnection         topicConnection = null;
        TopicSession            topicSession = null;
        Topic                   topic = null;
        TopicPublisher             topicPublisher = null;
        TextMessage             message = null;

        try {
            jndiContext = new InitialContext();
            topicConnectionFactory = (TopicConnectionFactory)
                jndiContext.lookup
                ("java:comp/env/jms/TCFactory");
            topic = (Topic) jndiContext.lookup("java:comp/env/jms/SampleTopic");

	    int startId = id * NUM_CYCLES;
	    int endId = (id * NUM_CYCLES) + NUM_CYCLES;
            debug("Start ID is " + startId);
            debug("End ID is " + endId);
	    for (int i= startId;i < endId; i ++) {
                try {
                    debug("Getting connection for :" + i);
                    topicConnection =
                    topicConnectionFactory.createTopicConnection();
                    debug("Getting session for :" + i);
                    topicSession =
                    topicConnection.createTopicSession(false,
                    Session.AUTO_ACKNOWLEDGE);
                    debug("starting connection  for :" + i);
                    topicConnection.start();
                    debug("creating publisher  for :" + i);
                    topicPublisher = topicSession.createPublisher(topic);
                    message = topicSession.createTextMessage();
                    message.setText("CLIENT");
	            message.setIntProperty("id",i);
	            message.setLongProperty("sleeptime",MDB_SLEEP_TIME);
                    topicPublisher.publish(message);
		    debug("Send the message :" + message.getIntProperty("id") + ":" + message.getText());
                } catch (Exception e) {
                    System.out.println("Exception occurred: " + e.toString());
                } finally {
                    if (topicConnection != null) {
                        try {
                            topicConnection.close();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    } // if
               } // finally
            }
        } catch (Throwable e) {
            System.out.println("Exception occurred: " + e.toString());
        } // finally
    } // main

    static void debug(String msg) {
        if (debug) {
	   System.out.println(msg);
	}
    }
} // class


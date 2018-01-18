/*
 * Copyright (c) 2003, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/*
 * %W% %E%
 */

package test.stress.ejb;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class SendBean implements SessionBean {
	
    private transient Context context;
    private transient QueueConnectionFactory queueConnectionFactory;
    private transient javax.jms.Queue queue;

    public SendBean() {
    }

    public void ejbCreate() throws CreateException {
 
            try {
				context = new InitialContext();
				queueConnectionFactory = (QueueConnectionFactory)context.lookup("java:comp/env/jms/QCFactory");
				queue = (Queue) context.lookup("java:comp/env/jms/outboundQueue2");
			} catch (NamingException e) {
				e.printStackTrace();
			} 
    }

    /**
     * Send a TextMessage containing the specified String to the configured outbound queue
     * @param text
     */
    public void sendTextMessageToQ(String text)  {
    	QueueConnection queueConnection=null;
    	QueueSession queueSession=null;
        try {
			queueConnection =  queueConnectionFactory.createQueueConnection();
			queueConnection.start();
			queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE); 
			QueueSender queueSender = queueSession.createSender(queue);
			TextMessage testMessage = queueSession.createTextMessage(text);
			queueSender.send(testMessage);
		} catch (JMSException e) {
			e.printStackTrace();
		} finally {
	        if (queueConnection!=null) {
				try {
					queueConnection.close();
				} catch (JMSException e) {
					e.printStackTrace();
				}
	        }
		}
    }
    
 
    public void setSessionContext(SessionContext sc) {
    }

    public void ejbRemove() {
        context = null;
        queueConnectionFactory = null;
        queue = null;
    }

    // not called for stateless session bean
    public void ejbActivate() {
    }

    // not called for stateless session bean
    public void ejbPassivate() {
    }
}


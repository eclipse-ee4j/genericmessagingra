/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.performance.queue.ejb;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class PassThroughBean extends GenericPassThroughBean implements MessageDrivenBean, MessageListener {
    private Destination outboundQueue;
    private ConnectionFactory outboundQueueConnectionFactory;

    public PassThroughBean() {
    }

    @Override
    public ConnectionFactory getOutboundConnectionFactory() {
    	return outboundQueueConnectionFactory;
    }

    @Override
    public Destination getOutboundDestination() {
    	return outboundQueue;
    }

	public void ejbCreate() {
		try {
			InitialContext ic = new InitialContext();
			
            outboundQueueConnectionFactory = (ConnectionFactory) ic.lookup("java:comp/env/jms/QCFactory");

            outboundQueue = (Queue) ic.lookup("java:comp/env/jms/outboundQueue");
		} catch (NamingException e) {
			System.out.println("JNDI lookup failed: " + e.toString());
		}
	}
    
	@Override
	public void ejbRemove() throws EJBException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setMessageDrivenContext(MessageDrivenContext arg0) throws EJBException {
		// TODO Auto-generated method stub
		
	}

}

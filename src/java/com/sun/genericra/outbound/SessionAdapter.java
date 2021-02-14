/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.outbound;

import com.sun.genericra.util.*;

import java.io.Serializable;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.*;

import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.IllegalStateException;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TemporaryTopic;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;


/**
 * SessionWrapper of the resource adapter.
 *
 * @author Sivakumar Thyagarajan
 */
public class SessionAdapter implements Session, TopicSession, QueueSession {
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    private Session physicalSession;
    private Session swappedSession;
    private boolean isClosed;
    private boolean isValid = true;
    private ConnectionHandle ch;
    private boolean isInUse = false;
    private ArrayList messageConsumers = new ArrayList();
    private ArrayList messageProducers = new ArrayList();
    private ArrayList queueBrowsers = new ArrayList();

    public SessionAdapter(Session physicalSession, ConnectionHandle ch) {
        this.physicalSession = physicalSession;
        this.ch = ch;
    }

    public int getAcknowledgeMode() throws JMSException {
        checkIfClosed();

        return physicalSession.getAcknowledgeMode();
    }

    public void setInUse() {
        this.isInUse = true;
    }

    public void setInvalid() throws JMSException {
        debug("setinvalid - closeAllJMSEntities");
        this.isValid = false;
        closeAllJMSEntities();
    }

    public boolean isInUse() {
        return isInUse;
    }

    private void checkIfClosed() throws JMSException {
        if (this.isClosed) {
            throw new IllegalStateException("JMS Session closed");
        }

        if (!this.isValid) {
            throw new IllegalStateException("JMS Session invalid");
        }
    }

    public void close() throws JMSException {
        if (this.isClosed) {
            return;
        }

        this.isClosed = true;
        this.isInUse = false;

        debug(" close() called - txinprogress " +
            this.ch.getManagedConnection().isTransactionInProgress());
        closeAllJMSEntities();

        this.ch.getManagedConnection()._closeSession(this.physicalSession);
    }

    private void closeAllJMSEntities() throws JMSException {
        debug("closeAllJMSEntities");

        //Close all entities created via this session
        debug("closing message consumers " + this.messageConsumers.size());

        for (Iterator iter = this.messageConsumers.iterator(); iter.hasNext();) {
            MessageConsumer mc = (MessageConsumer) iter.next();
            debug("closing message consumer " + mc);
            mc.close();
        }

        debug("closing message producers " + this.messageProducers.size());

        for (Iterator iter = this.messageProducers.iterator(); iter.hasNext();) {
            MessageProducer mp = (MessageProducer) iter.next();
            debug("closing message producer " + mp);
            mp.close();
        }

        debug("closing queue browsers " + this.queueBrowsers.size());

        for (Iterator iter = this.queueBrowsers.iterator(); iter.hasNext();) {
            QueueBrowser qp = (QueueBrowser) iter.next();
            debug("closing Queue browser " + qp);
            qp.close();
        }
	
	try {
           this.physicalSession.setMessageListener(null); //XXX:??
	} catch (JMSException jmse) {
           debug("Failed setting Null messagelistener on session");
	}
        this.messageConsumers.clear();
        this.messageProducers.clear();
        this.queueBrowsers.clear();
        ch.getSessions().remove(this);
    }

    public void commit() throws JMSException {
        checkIfClosed();
        this.physicalSession.commit();
    }

    public void recover() throws JMSException {
        checkIfClosed();
        this.physicalSession.recover();
    }

    public void rollback() throws JMSException {
        checkIfClosed();
        this.physicalSession.rollback();
    }

    public void run() {
        try {
            checkIfClosed();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        this.physicalSession.run();
    }

    public boolean getTransacted() throws JMSException {
        checkIfClosed();

        return this.physicalSession.getTransacted();
    }

    public void unsubscribe(String name) throws JMSException {
        checkIfClosed();
        this.physicalSession.unsubscribe(name);
        
    }

    public BytesMessage createBytesMessage() throws JMSException {
        checkIfClosed();

        return (BytesMessage) createProxyMessage(this.physicalSession.createBytesMessage());
    }

    public MapMessage createMapMessage() throws JMSException {
        checkIfClosed();

        return (MapMessage) createProxyMessage(this.physicalSession.createMapMessage());
    }

    public Message createMessage() throws JMSException {
        checkIfClosed();

        return (Message) createProxyMessage(this.physicalSession.createMessage());
    }

    public MessageListener getMessageListener() throws JMSException {
        checkIfClosed();

        return this.physicalSession.getMessageListener();
    }

    public void setMessageListener(MessageListener ml)
        throws JMSException {
        checkIfClosed();
        this.physicalSession.setMessageListener(ml);
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        checkIfClosed();

        return (ObjectMessage) createProxyMessage(this.physicalSession.createObjectMessage());
    }

    public StreamMessage createStreamMessage() throws JMSException {
        checkIfClosed();

        return (StreamMessage) createProxyMessage(this.physicalSession.createStreamMessage());
    }

    public TemporaryQueue createTemporaryQueue() throws JMSException {
        checkIfClosed();

        TemporaryQueue q = this.physicalSession.createTemporaryQueue();
        ch._addTemporaryDest(q);

        return q;
    }

    public TemporaryTopic createTemporaryTopic() throws JMSException {
        checkIfClosed();

        TemporaryTopic t = this.physicalSession.createTemporaryTopic();
        ch._addTemporaryDest(t);

        return t;
    }

    public TextMessage createTextMessage() throws JMSException {
        checkIfClosed();

        return (TextMessage) createProxyMessage(this.physicalSession.createTextMessage());
    }

    public MessageConsumer createConsumer(Destination dest)
        throws JMSException {
        checkIfClosed();
        MessageConsumer mc = this.physicalSession.createConsumer(getWrappedDestination(
                    dest));
        this.messageConsumers.add(mc);

        return mc;
    }

    public MessageProducer createProducer(Destination dest)
        throws JMSException {
        logger.log(Level.FINEST, "Creating producer with " + dest);
        logger.log(Level.FINEST, "Physical Session " + physicalSession);
        checkIfClosed();

        MessageProducer mp = this.physicalSession.createProducer(getWrappedDestination(
                    dest));
        MessageProducer wrappedMp = new MessageProducerProxy(mp);
        this.messageProducers.add(wrappedMp);

        return wrappedMp;
    }

    public ObjectMessage createObjectMessage(Serializable ser)
        throws JMSException {
        checkIfClosed();

        return (ObjectMessage) createProxyMessage(this.physicalSession.createObjectMessage(
                ser));
    }

    public Queue createQueue(String name) throws JMSException {
        checkIfClosed();

        return this.physicalSession.createQueue(name);
    }

    public QueueBrowser createBrowser(Queue name) throws JMSException {
        checkIfClosed();

        QueueBrowser qb = this.physicalSession.createBrowser(getWrappedQueue(
                    name));
        this.queueBrowsers.add(qb);

        return qb;
    }

    public TextMessage createTextMessage(String msg) throws JMSException {
        checkIfClosed();

        return (TextMessage) createProxyMessage(this.physicalSession.createTextMessage(
                msg));
    }

    public Topic createTopic(String name) throws JMSException {
        checkIfClosed();

        return this.physicalSession.createTopic(name);
    }

    public MessageConsumer createConsumer(Destination dest, String msgSel)
        throws JMSException {
        checkIfClosed();

        MessageConsumer mc = this.physicalSession.createConsumer(getWrappedDestination(
                    dest), msgSel);
        this.messageConsumers.add(mc);

        return mc;
    }

    public MessageConsumer createConsumer(Destination dest, String msgSel,
        boolean noLocal) throws JMSException {
        checkIfClosed();

        MessageConsumer mc = this.physicalSession.createConsumer(getWrappedDestination(
                    dest), msgSel, noLocal);
        this.messageConsumers.add(mc);

        return mc;
    }

    public QueueBrowser createBrowser(Queue queue, String msgSel)
        throws JMSException {
        checkIfClosed();
        debug("PhysicalSession class is " +
            this.physicalSession.getClass().getName());

        QueueBrowser qb = this.physicalSession.createBrowser(getWrappedQueue(
                    queue), msgSel);
        this.queueBrowsers.add(qb);

        return qb;
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name)
        throws JMSException {
        checkIfClosed();

        TopicSubscriber ts = this.physicalSession.createDurableSubscriber(getWrappedTopic(
                    topic), name);
        this.messageConsumers.add(ts);

        return ts;
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name,
        String msgSel, boolean noLocal) throws JMSException {
        checkIfClosed();

        TopicSubscriber ts = this.physicalSession.createDurableSubscriber(getWrappedTopic(
                    topic), name, msgSel, noLocal);
        this.messageConsumers.add(ts);

        return ts;
    }

    public TopicPublisher createPublisher(Topic topic)
        throws JMSException {
        checkIfClosed();

        TopicPublisher tp = ((TopicSession) this.physicalSession).createPublisher(getWrappedTopic(
                    topic));
        TopicPublisher wrappedTp = new MessageProducerProxy(tp);
        this.messageProducers.add(wrappedTp);

        return wrappedTp;
    }

    public TopicSubscriber createSubscriber(Topic topic)
        throws JMSException {
        checkIfClosed();

        TopicSubscriber ts = ((TopicSession) this.physicalSession).createSubscriber(getWrappedTopic(
                    topic));
        this.messageConsumers.add(ts);

        return ts;
    }

    public TopicSubscriber createSubscriber(Topic topic, String msgSel,
        boolean noLocal) throws JMSException {
        checkIfClosed();

        TopicSubscriber ts = ((TopicSession) this.physicalSession).createSubscriber(getWrappedTopic(
                    topic), msgSel, noLocal);
        this.messageConsumers.add(ts);

        return ts;
    }

    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        checkIfClosed();

        QueueReceiver qr = ((QueueSession) this.physicalSession).createReceiver(getWrappedQueue(
                    queue));
        this.messageConsumers.add(qr);

        return qr;
    }

    public QueueSender createSender(Queue queue) throws JMSException {
        checkIfClosed();

        QueueSender qs = ((QueueSession) this.physicalSession).createSender(getWrappedQueue(
                    queue));
        QueueSender wrappedQs = new MessageProducerProxy(qs);
        this.messageProducers.add(wrappedQs);

        return wrappedQs;
    }

    public QueueReceiver createReceiver(Queue queue, String msgSel)
        throws JMSException {
        checkIfClosed();

        QueueReceiver qr = ((QueueSession) this.physicalSession).createReceiver(getWrappedQueue(
                    queue), msgSel);
        this.messageConsumers.add(qr);

        return qr;
    }

    private jakarta.jms.Topic getWrappedTopic(Topic topic)
        throws JMSException {
        if (topic instanceof TopicProxy) {
            return (jakarta.jms.Topic) (((TopicProxy) topic)._getPhysicalDestination());
        } else {
            return topic;
        }
    }

    private jakarta.jms.Queue getWrappedQueue(Queue queue)
        throws JMSException {
        if (queue instanceof QueueProxy) {
            return (jakarta.jms.Queue) (((QueueProxy) queue)._getPhysicalDestination());
        } else {
            return queue;
        }
    }

    private jakarta.jms.Destination getWrappedDestination(Destination dest)
        throws JMSException {
        if (dest instanceof DestinationAdapter) {
            return (jakarta.jms.Destination) (((DestinationAdapter) dest)._getPhysicalDestination());
        } else {
            return dest;
        }
    }

    void _swapPhysicalSession(Session in) {
        if (in != this.physicalSession) {
            this.swappedSession = this.physicalSession;
            this.physicalSession = in;
        }
    }

    void _swapPhysicalSession() {
        if (this.swappedSession != null) {
            this.physicalSession = this.swappedSession;
            this.swappedSession = null;
        }
    }

    private void debug(String s) {
        logger.log(Level.FINEST, "[SessionAdapter] " + s);
    }

    private Object createProxyMessage(Message msg) {
        Object mcf = ch.getManagedConnection().getManagedConnectionFactory();
        boolean wrap = ((AbstractManagedConnectionFactory) mcf).getUseProxyMessages();

        if (wrap) {
            InvocationHandler ih = new ProxyMessage(msg);
            Class[] intfcs = {
                    jakarta.jms.Message.class, jakarta.jms.BytesMessage.class,
                    jakarta.jms.MapMessage.class, jakarta.jms.ObjectMessage.class,
                    jakarta.jms.StreamMessage.class, jakarta.jms.TextMessage.class
                };

            return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                intfcs, ih);
        } else {
            return msg;
        }
    }

    public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
        throw new UnsupportedOperationException();
    }

    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException();
    }
}

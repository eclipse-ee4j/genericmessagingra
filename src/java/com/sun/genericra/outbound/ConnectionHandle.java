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

import com.sun.genericra.util.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;


/**
 * <code>ConnectionHandle<code> implementation for the generic JMS resource adapter
 * Implements the <code>Connection</code>, <code>TopicConnection</code>,
 * <code>QueueConnection</code> interfaces.
 *
 * @author Sivakumar Thyagarajan
 */
public class ConnectionHandle implements javax.jms.Connection, TopicConnection,
    QueueConnection {
    private ManagedConnection mc;
    private javax.jms.Connection physicalJMSCon;
    private boolean isClosed = false;
    private ArrayList sessions = new ArrayList();
    private ArrayList tempDestinations = new ArrayList();
    private boolean valid = true;

    /**
     * @param connection
     */
    public ConnectionHandle(ManagedConnection connection) {
        this.mc = connection;
        this.physicalJMSCon = this.mc.getPhysicalConnection();
    }

    private boolean inACC() {
        return ((AbstractManagedConnectionFactory) mc.getManagedConnectionFactory()).isInAppClientContainer();
    }

    public void close() throws JMSException {
        if (isClosed()) {
            return;
        }

        this.physicalJMSCon.stop();

        synchronized (sessions) {
            Object[] sessionObjects = this.sessions.toArray();

            for (int i = 0; i < sessionObjects.length; i++) {
                SessionAdapter sa = (SessionAdapter) sessionObjects[i];
                sa.close();
            }
        }

        synchronized (tempDestinations) {
            Object[] dests = this.tempDestinations.toArray();

            for (int i = 0; i < dests.length; i++) {
                if (dests[i] instanceof TemporaryQueue) {
                    ((TemporaryQueue) dests[i]).delete();
                }

                if (dests[i] instanceof TemporaryTopic) {
                    ((TemporaryTopic) dests[i]).delete();
                }
            }
        }

        this.isClosed = true;

        this.mc.sendConnectionClosedEvent(this);

        if (!this.mc.isTransactionInProgress()) {
            this.mc.sendConnectionErrorEvent(this);
        }
    }

    public void start() throws JMSException {
        checkIfClosed();
        this.physicalJMSCon.start();
    }

    public void stop() throws JMSException {
        if (!inACC()) {
            throw new JMSException(
                "Conncetion.stop() cannot be called on non-ACC clients");
        }

        checkIfClosed();
        this.physicalJMSCon.stop();
    }

    public String getClientID() throws JMSException {
        checkIfClosed();

        if (!inACC()) {
            throw new JMSException("Client ID cannot be set in non-ACC clients");
        }

        return this.physicalJMSCon.getClientID();
    }

    public void setClientID(String clientID) throws JMSException {
        checkIfClosed();

        if (!inACC()) {
            throw new JMSException("Client ID cannot be set in non-ACC clients");
        }

        //can thsi be set multiple times? - apparently yes.
        this.physicalJMSCon.setClientID(clientID);
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        checkIfClosed();

        return this.physicalJMSCon.getMetaData();
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        if (!this.inACC()) {
            throw new JMSException(
                "Conncetion.getExceptionListener() cannot be called on non-ACC clients");
        }

        checkIfClosed();

        return this.physicalJMSCon.getExceptionListener();
    }

    public void setExceptionListener(ExceptionListener excptLstnr)
        throws JMSException {
        if (!this.inACC()) {
            throw new JMSException(
                "Conncetion.setExceptionListener() cannot be called on non-ACC clients");
        }

        checkIfClosed();
        this.physicalJMSCon.setExceptionListener(excptLstnr);
    }

    public Session createSession(boolean transacted, int acknowledgeMode)
        throws JMSException {
        return (Session) createSession(transacted, acknowledgeMode,
            Constants.UNIFIED_SESSION);
    }

    private boolean checkIfSessionsCanBeCreated() throws JMSException {
        checkIfClosed();

        if (this.mc.isTransactionInProgress()) {
            if (this.mc.canSessionsBeCreated()) {
                return true;
            } else {
                throw new JMSException("More than one sessions cannot be " +
                    "created within a transaction");
            }
        } else {
            return true;
        }
    }

    public ConnectionConsumer createConnectionConsumer(Destination dest,
        String msgSel, ServerSessionPool ssp, int maxMsgs)
        throws JMSException {
        checkIfClosed();

        return this.physicalJMSCon.createConnectionConsumer(dest, msgSel, ssp,
            maxMsgs);
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic topic,
        String subName, String msgSel, ServerSessionPool ssp, int maxMsgs)
        throws JMSException {
        checkIfClosed();

        return this.physicalJMSCon.createDurableConnectionConsumer(topic,
            subName, msgSel, ssp, maxMsgs);
    }

    public TopicSession createTopicSession(boolean transacted,
        int acknowledgeMode) throws JMSException {
        return (TopicSession) createSession(transacted, acknowledgeMode,
            Constants.TOPIC_SESSION);
    }

    private Session createSession(boolean transacted, int acknowledgeMode,
        int type) throws JMSException {
        checkIfSessionsCanBeCreated();

        Session s = this.mc.getPhysicalJMSSession(transacted, acknowledgeMode,
                type);
        SessionAdapter sa = new SessionAdapter(s, this);
        this.sessions.add(sa);

        return sa;
    }

    public ConnectionConsumer createConnectionConsumer(Topic topic,
        String subName, ServerSessionPool ssp, int maxMsgs)
        throws JMSException {
        checkIfClosed();

        return this.physicalJMSCon.createConnectionConsumer(topic, subName,
            ssp, maxMsgs);
    }

    private boolean isXA() {
        return ((AbstractManagedConnectionFactory) this.mc.getManagedConnectionFactory()).getSupportsXA();
    }

    public QueueSession createQueueSession(boolean transacted,
        int acknowledgeMode) throws JMSException {
        return (QueueSession) createSession(transacted, acknowledgeMode,
            Constants.QUEUE_SESSION);
    }

    public ConnectionConsumer createConnectionConsumer(Queue queue,
        String subName, ServerSessionPool ssp, int maxMsgs)
        throws JMSException {
        checkIfClosed();

        return this.physicalJMSCon.createConnectionConsumer(queue, subName,
            ssp, maxMsgs);
    }

    public void cleanup() throws JMSException {
        //Iterate through Sessions and close them.
        Object[] sessionObjects = this.sessions.toArray();

        for (int i = 0; i < sessionObjects.length; i++) {
            SessionAdapter sa = (SessionAdapter) sessionObjects[i];
            sa.setInvalid();
        }
    }

    public ManagedConnection getManagedConnection() {
        return this.mc;
    }

    public void associateConnection(ManagedConnection mc) {
        this.mc = mc;
        this.physicalJMSCon = mc.getPhysicalConnection();
    }

    private void checkIfClosed() throws JMSException {
        if (isClosed) {
            throw new IllegalStateException("Connection is closed");
        }

        if (!valid) {
            throw new IllegalStateException("Invalid connection");
        }
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public void removeSessionAdapter(SessionAdapter sessionToBeRemoved) {
        sessions.remove(sessionToBeRemoved);
    }

    public List getSessions() {
        return this.sessions;
    }

    public void setInvalid() {
        this.valid = false;
    }

    void _addTemporaryDest(Destination dest) {
        tempDestinations.add(dest);
    }
}

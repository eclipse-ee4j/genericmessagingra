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

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.util.Constants;
import com.sun.genericra.util.ExceptionUtils;
import com.sun.genericra.util.LogUtils;
import com.sun.genericra.util.SecurityUtils;
import com.sun.genericra.util.StringManager;

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.*;

import javax.jms.*;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.security.PasswordCredential;

import javax.security.auth.Subject;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;


/**
 * Represents the <code>ManagedConnection</code> of the Generic JMS resource
 * adapter.
 *
 * @author Sivakumar Thyagarajan
 */
public class ManagedConnection implements javax.resource.spi.ManagedConnection {
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    private AbstractManagedConnectionFactory mcf;
    private com.sun.genericra.outbound.ConnectionRequestInfo info;
    private PrintWriter logWriter;
    private javax.jms.Connection physicalJMSCon;
    private Session physicalJMSSession;
    private XASession physicalXASession;
    private ConnectionHandle activeHandle;
    private ArrayList connectionHandles = new ArrayList();
    private boolean isDestroyed = false;
    private PasswordCredential passwordCredential;
    private XAResource xaresource;
    private boolean transactionInProgress;
    private ConnectionEventListenerHandler connectionEventListenerHandler;
    private SessionAdapter activeSA = null;
    private StringManager sm = StringManager.getManager(GenericJMSRA.class);

    public ManagedConnection(AbstractManagedConnectionFactory factory,
        PasswordCredential pc,
        com.sun.genericra.outbound.ConnectionRequestInfo info,
        javax.jms.Connection physicalCon) throws ResourceException {
        this.mcf = factory;
        this.passwordCredential = pc;
        this.info = info;
        this.physicalJMSCon = physicalCon;
        this.connectionEventListenerHandler = new ConnectionEventListenerHandler(this);
        initialize();
    }

    private void initialize() throws ResourceException {
        try {
            //Set exception listener to perform validation of ManagedConnection
            //Validation is disabled by default.
            if (((AbstractManagedConnectionFactory) this.getManagedConnectionFactory()).getConnectionValidationEnabled()) {
                ExceptionListener l = new ExceptionListener() {
                        public void onException(JMSException jmsEx) {
                            ManagedConnection.this.connectionEventListenerHandler.sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED,
                                jmsEx, null);
                        }
                    };

                this.physicalJMSCon.setExceptionListener(l);
            }
        } catch (JMSException e) {
            String msg = sm.getString("error_setting_excp_listener");
            throw new ResourceException(msg);
        }
    }

    public Session getPhysicalJMSSession(boolean transacted, int ack,
        int sessionType) throws JMSException {
        // If transaction is in progress physical session will be created by ManagedConnection itself.
        if (this.isTransactionInProgress()) {
            return this.physicalJMSSession;
        } else {
            return createLocalSession(transacted, ack, sessionType);
        }
    }

    private javax.jms.Session createXaSession(int sessionType)
        throws JMSException {
        Session result = null;

        switch (sessionType) {
        case Constants.UNIFIED_SESSION:

            XASession xas = ((XAConnection) this.physicalJMSCon).createXASession();
            this.xaresource = xas.getXAResource();
            this.physicalXASession = xas;
            result = xas.getSession();

            break;

        case Constants.TOPIC_SESSION:

            XATopicSession xast = ((XATopicConnection) this.physicalJMSCon).createXATopicSession();
            this.xaresource = xast.getXAResource();
            this.physicalXASession = xast;
            result = xast.getTopicSession();

            break;

        case Constants.QUEUE_SESSION:

            XAQueueSession xasq = ((XAQueueConnection) this.physicalJMSCon).createXAQueueSession();
            this.xaresource = xasq.getXAResource();
            this.physicalXASession = xasq;
            result = xasq.getQueueSession();

            break;
        }

        return result;
    }

    public javax.jms.Session createLocalSession(boolean transacted,
        int acknowledgeMode, int sessionType) throws JMSException {
        Session result = null;

        switch (sessionType) {
        case Constants.UNIFIED_SESSION:

            Session s = ((Connection) this.physicalJMSCon).createSession(transacted,
                    acknowledgeMode);
            result = s;

            break;

        case Constants.TOPIC_SESSION:

            TopicSession st = ((TopicConnection) this.physicalJMSCon).createTopicSession(transacted,
                    acknowledgeMode);
            result = st;

            break;

        case Constants.QUEUE_SESSION:

            QueueSession sq = ((QueueConnection) this.physicalJMSCon).createQueueSession(transacted,
                    acknowledgeMode);
            result = sq;

            break;
        }

        return result;
    }

    /**
     * Application server calls this method to force any cleanup on the
     * <code>ManagedConnection</code> instance. This method calls the
     * invalidate method on all logical connection handles associated with this
     * <code>ManagedConnection</code>.
     *
     * @throws ResourceException
     *             if the physical connection is no more valid
     */
    public void cleanup() throws ResourceException {
        debug("MC::cleanup - tx in progress ? " + isTransactionInProgress());

        com.sun.genericra.outbound.ConnectionHandle ch = null;

        if (!(isTransactionInProgress())) {
            Iterator iterator = connectionHandles.iterator();

            while (iterator.hasNext()) {
                ch = (com.sun.genericra.outbound.ConnectionHandle) iterator.next();

                try {
                    debug("MC::cleanup - calling ch.cleanup ");
                    ch.cleanup();
                } catch (JMSException jmse) {
                    debug("Exception while cleaning up connection handle" +
                        jmse);
                }

                iterator.remove();
            }
        }
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException
     *             if there is an error in closing the physical connection
     */
    public void destroy() throws ResourceException {
        debug("In destroy");

        if (isDestroyed()) {
            return;
        }

        cleanup();

        try {
            if (this.physicalJMSSession != null) {
                this.physicalJMSSession.close();
            }

            if (this.physicalXASession != null) {
                this.physicalXASession.close();
            }

            physicalJMSCon.close();
            physicalJMSCon = null;
        } catch (JMSException e) {
            debug("Error occured while closing JMS connection." + e);
            throw ExceptionUtils.newResourceException(e);
        } finally {
            this.isDestroyed = true;
        }
    }

    public PrintWriter getLogWriter() throws ResourceException {
        isValid();

        return logWriter;
    }

    public void setLogWriter(PrintWriter pw) throws ResourceException {
        isValid();
        this.logWriter = pw;
    }

    public void associateConnection(Object connectionHandle)
        throws ResourceException {
        debug("In associateConnection");
        isValid();

        if (connectionHandle == null) {
            throw new ResourceException("Connection handle is null");
        }

        com.sun.genericra.outbound.ConnectionHandle ch = (com.sun.genericra.outbound.ConnectionHandle) connectionHandle;
        ManagedConnection otherMC = (ManagedConnection) ch.getManagedConnection();
        ch.associateConnection(this);
        addConnectionHandle(ch);
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener
     *            <code>ConnectionEventListener</code>
     * @see <code>removeConnectionEventListener</code>
     */
    public void addConnectionEventListener(ConnectionEventListener cel) {
        this.connectionEventListenerHandler.addConnectionEventListener(cel);
    }

    public void removeConnectionEventListener(ConnectionEventListener cel) {
        this.connectionEventListenerHandler.removeConnectionEventListener(cel);
    }

    public void sendConnectionEvent(int eventType, Exception ex) {
        this.connectionEventListenerHandler.sendEvent(eventType, ex, null);
    }

    public void sendConnectionEvent(int eventType, Exception ex,
        Object connectionHandle) {
        this.connectionEventListenerHandler.sendEvent(eventType, ex,
            connectionHandle);
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        isValid();

        return new LocalTransactionImpl(this);
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        isValid();

        return new ManagedConnectionMetaData() {
                public int getMaxConnections() throws ResourceException {
                    return 1;
                }

                public String getEISProductName() throws ResourceException {
                    try {
                        return ManagedConnection.this.physicalJMSCon.getMetaData()
                                                                    .getJMSProviderName();
                    } catch (JMSException e) {
                        throw ExceptionUtils.newResourceException(e);
                    }
                }

                public String getEISProductVersion() throws ResourceException {
                    try {
                        return ManagedConnection.this.physicalJMSCon.getMetaData()
                                                                    .getProviderVersion();
                    } catch (JMSException e) {
                        throw ExceptionUtils.newResourceException(e);
                    }
                }

                public String getUserName() throws ResourceException {
                    return ManagedConnection.this.passwordCredential.getUserName();
                }
            };
    }

    public XAResource getXAResource() throws ResourceException {
        isValid();

        if (this.mcf.getSupportsXA() == false) {
            String msg = sm.getString("adapter_not_xa");
            throw new ResourceException(msg);
        }

        XAResourceProxy proxy = new XAResourceProxy(this);
        proxy.setConnection(this.physicalJMSCon);
        proxy.setRMPolicy(((AbstractManagedConnectionFactory) mcf).getRMPolicy());

        return proxy;
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo cri)
        throws ResourceException {
        debug("On getConnection");

        /*
         * Return a proxy of the JMS actual-connection which
         * intercepts all <code> Connection </code> calls and implements
         * javax.jms.Connection, Topic and QueueConnection
         *
         * Intercepts, and delegates to actual connection, if connection handle
         * is not closed already.
         *
         * Intercepts close and calls mc.connectionClosed, so that MC can
         * invalidate this connection handle and send ConnectionHandle closed
         * event to all registered listeners.
         */
        isValid();

        com.sun.genericra.outbound.ConnectionRequestInfo cxRequestInfo = (com.sun.genericra.outbound.ConnectionRequestInfo) cri;
        PasswordCredential passedInCred = SecurityUtils.getPasswordCredential(this.mcf,
                subject, cxRequestInfo);

        if (SecurityUtils.isPasswordCredentialEqual(this.passwordCredential,
                    passedInCred) == false) {
            throw new javax.resource.spi.SecurityException(
                "Re-authentication not supported");
        }

        com.sun.genericra.outbound.ConnectionHandle connectionHandle = new com.sun.genericra.outbound.ConnectionHandle(this);
        addConnectionHandle(connectionHandle);

        try {
            if ((this.mcf.getClientId() != null) &&
                    (this.physicalJMSCon.getClientID() == null)) {
                this.physicalJMSCon.setClientID(this.mcf.getClientId());
            }
        } catch (JMSException e) {
            throw ExceptionUtils.newResourceException(e);
        }

        return connectionHandle;
    }

    private void transactionCompleted() {
        debug("MC: txCompleted  called");
        this.transactionInProgress = false;
        debug("MC: txCompleted  called - supports XA" +
            this.mcf.getSupportsXA());

        if ((activeHandle != null) && activeHandle.isClosed()) {
            sendConnectionErrorEvent(activeHandle);
        }
    }

    private void transactionStarted() throws ResourceException {
        debug("Transaction Started");

        //Mark MC as txn in progress
        this.transactionInProgress = true;
    }

    public boolean isTransactionInProgress() {
        return this.transactionInProgress;
    }

    public javax.jms.Connection getPhysicalConnection() {
        return this.physicalJMSCon;
    }

    public Object getManagedConnectionFactory() {
        return this.mcf;
    }

    public boolean isDestroyed() {
        return this.isDestroyed;
    }

    public void isValid() throws ResourceException {
        if (this.isDestroyed()) {
            throw new ResourceException("MC destroyed!!");
        }
    }

    public PasswordCredential getPasswordCredential() {
        return passwordCredential;
    }

    private void debug(String s) {
        logger.log(Level.FINEST, "[ManagedConnection] " + s);
    }

    public boolean canSessionsBeCreated() {
        for (Iterator iter = this.connectionHandles.iterator(); iter.hasNext();) {
            ConnectionHandle ch = (ConnectionHandle) iter.next();

            if (ch.getSessions().size() > 1) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param handle
     */
    public void sendConnectionClosedEvent(ConnectionHandle handle) {
        handle.setInvalid();
        this.connectionHandles.remove(handle);
        debug("Sending CONN_CLOSED event");
        sendConnectionEvent(ConnectionEvent.CONNECTION_CLOSED, null, handle);
    }

    /**
     * @param handle
     */
    public void sendConnectionErrorEvent(ConnectionHandle handle) {
        if (this.mcf.getClientId() != null) {
            handle.setInvalid();
            this.connectionHandles.remove(handle);
            debug("Sending CONN_CLOSED event");
            sendConnectionEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED,
                null, handle);
        }
    }

    void _endLocalTx(boolean commit) throws JMSException {
        try {
            debug("The physical session is :" + this.physicalJMSSession);

            if (this.physicalJMSSession != null) {
                if (commit == true) {
                    this.physicalJMSSession.commit();
                } else {
                    this.physicalJMSSession.rollback();
                }
            }

            reSyncSessions();
        } finally {
            transactionCompleted();
        }
    }

    void _startLocalTx() throws Exception {
        transactionStarted();

        if (this.physicalJMSSession == null) {
            int sessionMode = ((AbstractManagedConnectionFactory) this.mcf).getDestinationMode();
            this.physicalJMSSession = createLocalSession(true,
                    Session.AUTO_ACKNOWLEDGE, sessionMode);
        }

        syncSessions();
        debug("The physical session is :" + this.physicalJMSSession);
    }

    void _endXaTx() throws JMSException {
        try {
            debug("The physical session is :" + this.physicalJMSSession);
            reSyncSessions();
        } finally {
            transactionCompleted();
        }
    }

    void _startXaTx() throws Exception {
        transactionStarted();

        if (this.physicalJMSSession == null) {
            int sessionMode = ((AbstractManagedConnectionFactory) this.mcf).getDestinationMode();
            this.physicalJMSSession = createXaSession(sessionMode);
        }

        syncSessions();
    }

    /**
     * If a session is obtained before the start of transaction, the SessionAdapter
     * will contain a non-transactional session with ackmode and transaction mopde
     * specified by app. When transaction is started by appserver after getting session
     * replace that session with a session that can actually take part in transaction.
     */
    private void syncSessions() throws ResourceException {
        for (Iterator iter = connectionHandles.iterator(); iter.hasNext();) {
            ConnectionHandle ch = (ConnectionHandle) iter.next();
            List sessions = ch.getSessions();

            if (sessions.size() > 1) {
                throw new ResourceException(
                    "Cannot start a transaction when one or more open " +
                    "sessions are present");
            } else if (sessions.size() == 1) {
                activeSA = (SessionAdapter) ch.getSessions().get(0);

                break;
            }
        }

        if (activeSA != null) {
            activeSA._swapPhysicalSession(this.physicalJMSSession);
        }
    }

    /**
     * Nullify the session swapping done in syncSessions method.
     * This will be issued at the end of transaction.
     */
    private void reSyncSessions() {
        if (activeSA != null) {
            activeSA._swapPhysicalSession();
        }

        this.activeSA = null;
    }

    private void addConnectionHandle(ConnectionHandle ch) {
        this.activeHandle = ch;
        this.connectionHandles.add(this.activeHandle);
    }

    public String toString() {
        return "Physical Session -> " + physicalJMSSession +
        "Physical Connection " + physicalJMSCon + "Super -> " +
        super.toString();
    }

    XAResource _getXAResource() throws JMSException {
        // Though _StartXaTx creates a physical session, it is possible that
        // this method gets executed via xar.isSameRM method.
        if (this.physicalJMSSession == null) {
            int sessionMode = ((AbstractManagedConnectionFactory) this.mcf).getDestinationMode();
            this.physicalJMSSession = createXaSession(sessionMode);
        }

        return this.xaresource;
    }

    void _closeSession(Session sess) throws JMSException {
        if (sess != this.physicalJMSSession) {
            sess.close();
        }
    }
}

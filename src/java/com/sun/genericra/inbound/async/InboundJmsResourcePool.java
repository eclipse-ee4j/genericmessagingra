/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.async;

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.inbound.*;
import com.sun.genericra.util.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.*;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.*;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;


/**
 * ServerSessionPool implementation as per JMS 1.1 spec.
 * @author Binod P.G
 */
public class InboundJmsResourcePool extends AbstractJmsResourcePool implements ServerSessionPool {
    private static Logger _logger;

    static {
        _logger = LogUtils.getLogger();
    }

    private boolean deploymentCompleted = false;
    private ArrayList resources;
    private int maxSize;
    private int connectionsInUse = 0;
    private long maxWaitTime;
    private SortedSet<WaitQueueEntry> waitQueue = null;
    private long TIME_OUT = 180 * 1000;
    private StringManager sm = StringManager.getManager(GenericJMSRA.class);


    public InboundJmsResourcePool(EndpointConsumer consumer, boolean transacted) {
        super(consumer, transacted);
        this.waitQueue = new TreeSet<WaitQueueEntry>();
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public long getMaxWaitTime() {
        return this.maxWaitTime;
    }

    public int getCurrentResources() {
        int ret = 0;
        if (resources != null) {
            ret = resources.size();
        }
        return ret;
    }

    public int getBusyResources() {
        int busy = 0;
        if (resources != null) {
            Iterator it = resources.iterator();
            while (it.hasNext()) {
                InboundJmsResource resource = (InboundJmsResource) it.next();
                if (!resource.isFree()) {
                    busy++;
                }
            }
        }
        return busy;
    }
    
    public int getFreeResources() {
        int free = 0;
        if (resources != null) {
            Iterator it = resources.iterator();
            while (it.hasNext()) {
                InboundJmsResource resource = (InboundJmsResource) it.next();
                if (resource.isFree()) {
                    free++;
                }
            }
        }
        return free;
    }

    public int getConnectionsInUse() {
        return this.connectionsInUse;
    }

    public int getWaiting() {
        int wait = 0;
        if (this.waitQueue != null) {
            wait = this.waitQueue.size();
        }
        return wait;
    }

    public ConnectionConsumer createConnectionConsumer(Destination dest, String name,
            int maxMessages) throws JMSException {
        ConnectionConsumer conconsumer = null;
        Connection con = getConnection();

        if (isTopic()) {
            String selector = constructSelector(name);
            conconsumer = ((TopicConnection) con).createConnectionConsumer((Topic) dest,
                    selector, this, maxMessages);
        } else if (isQueue()) {
            conconsumer = ((QueueConnection) con).createConnectionConsumer((javax.jms.Queue) dest,
                    name, this, maxMessages);
        } else {
            conconsumer = con.createConnectionConsumer(dest, name, this,
                    maxMessages);
        }

        return conconsumer;
    }

    public ConnectionConsumer createDurableConnectionConsumer(Destination dest, String name,
            String sel, int maxMessages) throws JMSException {
        ConnectionConsumer conconsumer = null;
        Connection con = getConnection();
        String selector = constructSelector(sel);
        conconsumer = ((TopicConnection) con).createDurableConnectionConsumer((Topic) dest,
                name, selector, this, maxMessages);
        return conconsumer;
    }

    public synchronized void initialize() throws ResourceException {
        _logger.log(Level.FINER, "Initializing the ServerSession resource pool...");

        try {
            resources = new ArrayList();
            this.maxSize = consumer.getSpec().getMaxPoolSize();
            this.maxWaitTime = consumer.getSpec().getMaxWaitTime() * 1000;
            if (consumer.getSpec().getSupportsXA()) {
                XAConnectionFactory xacf = (XAConnectionFactory) consumer.getConnectionFactory();
                this.con = createXAConnection(xacf);

                ConnectionFactory cf = (ConnectionFactory) consumer.getDmdConnectionFactory();

                if (consumer.getSpec().getSendBadMessagesToDMD() == true) {
                    this.dmdCon = createDmdConnection(cf);
                }
            } else {
                if (!(consumer.getConnectionFactory() instanceof ConnectionFactory)) {
                    String msg = sm.getString("classtype_not_correct",
                            consumer.getConnectionFactory().getClass().getName());
                    throw new ResourceException(msg);
                }

                cf = (ConnectionFactory) consumer.getConnectionFactory();
                this.con = createConnection(cf);
            }

            stopped = false;
        } catch (JMSException e) {
            throw ExceptionUtils.newResourceException(e);
        }

        _logger.log(Level.FINE, "ServerSession resource pool initialized");
    }

    public InboundJmsResource create() throws JMSException {
        _logger.log(Level.FINER, "Creating the ServerSession");

        Session sess = null;
        XAResource xar = null;

        if (transacted) {
            sess = createXASession((XAConnection) con);
            xar = getXAResource((XASession) sess);
            _logger.log(Level.FINE, "Created new XA ServerSession");
        } else {
            sess = createSession(con);
            _logger.log(Level.FINE, "Created new ServerSession");
        }

        return new InboundJmsResource(sess, this, xar);
    }

    public ServerSession getServerSession() throws JMSException {
        InboundJmsResource result = null;
        WaitQueueEntry waitQueueEntry = null;
        long startTime = System.currentTimeMillis();
        long elapsedWaitTime = 0;
        long remainingWaitTime = maxWaitTime;

        while (true) {
            synchronized (this) {
                validate();
                result = _getServerSession();

                if (result != null) {
                    if (waitQueueEntry != null) {
                        waitQueue.remove(waitQueueEntry);
                    }
                    break;
                }

                elapsedWaitTime = System.currentTimeMillis() - startTime;
                if (maxWaitTime > 0) {
                    if (elapsedWaitTime >= maxWaitTime) {
                        _logger.log(Level.WARNING, "MaxWaitTime exceeded without acquiring a ServerSession");
                        if (waitQueueEntry != null) {
                            waitQueue.remove(waitQueueEntry);
                        }
                        String msg = sm.getString("pool_limit_reached");
                        throw new JMSException(msg);
                    }
                    remainingWaitTime = maxWaitTime - elapsedWaitTime;
                }

                if (waitQueueEntry == null) {
                    waitQueueEntry = new WaitQueueEntry(startTime);
                }
                waitQueue.add(waitQueueEntry);
                waitQueueEntry.setNotified(false);
            }

            synchronized (waitQueueEntry) {
                if (!waitQueueEntry.isNotified()) {
                    try {
                        _logger.log(Level.FINE, "Waiting for :" + remainingWaitTime);
                         waitQueueEntry.wait(remainingWaitTime);
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
            }
        }

        return result.refreshListener();
    }

    public void validate() throws JMSException {
        if (destroyed) {
            String msg = sm.getString("serversession_pool_destroyed");
            throw new JMSException(msg);
        }
        if (deploymentCompleted)
            return;
        int retry = this.getConsumer().getSpec().getMDBDeploymentRetryAttempt();
        long retryInterval = this.getConsumer().getSpec().
                                          getMDBDeploymentRetryInterval();
        boolean failed = false;
        MessageEndpoint endPoint = null;
        while(retry > 0) {
           failed = false;
           retry--;
           try {
               //create dummy end point to check if mdb deployment is
               //completed or some error occurred.
               Thread.sleep(retryInterval);
               MessageEndpointFactory mef = getConsumer().getMessageEndpointFactory();
               endPoint = mef.createEndpoint(null);
               deploymentCompleted = true;
               break;
           } catch (UnavailableException ue) {
                   failed = true;
           } catch (Throwable t) {
                   //throwable may not be thrown but as safeguard I added this
                   //since I am passing null object for createEndpoint.
           }
           if (destroyed) {
                String msg = sm.getString("serversession_pool_destroyed");
                throw new JMSException (msg);
           }
       }
       try {
           if (endPoint != null)
               endPoint.release();

       } catch( Exception e) {
           //ignore
       }
       if (failed) {
           _logger.log(Level.FINER, "Application not yet deployed or" +
             " deployment failed.\n Use properties MDBDeploymentRetryAttempt" +
              " & MDBDeploymentRetryInterval for tuning MDB deployment");
           throw new JMSException(
                       "Application not yet deployed or deployment failed.");
       }
    }

    private InboundJmsResource _getServerSession()
        throws JMSException {
        _logger.log(Level.FINER, "JMS provider is getting the ServerSession");

        if (stopped) {
            return null;
        }

        Iterator it = resources.iterator();

        while (it.hasNext()) {
            InboundJmsResource resource = (InboundJmsResource) it.next();

            if (resource.isFree()) {
                connectionsInUse++;

                return resource.markAsBusy();
            }
        }

        if (resources.size() < this.maxSize) {
            InboundJmsResource res = create();
            resources.add(res);
            connectionsInUse++;

            return res.markAsBusy();
        }

        return null;
    }

    public synchronized void put(InboundJmsResource resource) {
        resource.markAsFree();
        connectionsInUse--;
        _logger.log(Level.FINEST, "Connections remaining in use: " + connectionsInUse);

        if (stopped) {
            if (connectionsInUse <= 0) {
                notify();
            }
        } else {
            resumeWaitingThread();
        }
    }

    /**
     * Stops message delivery. Any message that is currently being delivered
     * will not be affected. It can be resumed later.
     */
    public void stop() throws JMSException {
        _logger.log(Level.FINER, "Stopping the ServerSession resource pool...");
        synchronized (this) {
            this.stopped = true;
            this.maxWaitTime = 0;
            releaseAllWaitingThreads();
            waitForAll();
        }
        releaseAllResources();

        if (dmdCon != null) {
            this.dmdCon.close();
        }
        _logger.log(Level.FINE, "ServerSession resource pool stopped");
    }

    /**
     * Destroys the ServerSessionPool.
     */
    public void destroy() throws JMSException {
        _logger.log(Level.FINER, "Destroying the ServerSession resource pool...");
        this.destroyed = true;
        stop();
        _logger.log(Level.FINE, "ServerSession resource pool destroyed");
    }

    public synchronized void waitForAll() {
        if (connectionsInUse > 0) {
            _logger.log(Level.FINE,
                "Waiting for " + connectionsInUse + " ServerSessions" +
                " to come back to pool");

            try {
                wait(this.consumer.getSpec().getEndpointReleaseTimeout() * 1000);
            } catch (InterruptedException ie) {
            }
        }
    }

    public synchronized void releaseAllWaitingThreads() {
        int count = waitQueue.size();
        for (WaitQueueEntry waitQueueEntry : waitQueue) {
            _logger.log(Level.FINE, "Notifying the thread");
            waitQueueEntry.setNotified(true);
            synchronized (waitQueueEntry) {
                waitQueueEntry.notify();
            }
        }
        waitQueue.clear();

        _logger.log(Level.FINE, "Released a total of " + count + " requests");
    }

    public void releaseAllResources() {
        Iterator it = resources.iterator();

        while (it.hasNext()) {
            InboundJmsResource obj = (InboundJmsResource) it.next();

            try {
                obj.destroy();
            } catch (Exception e) {
                // This is just to make sure that if one resource fails to destroy
                // we still call destroy on others.
                _logger.log(Level.SEVERE,
                    "Cannot destroy resource " + obj.toString());
            }
        }
    }

    private void resumeWaitingThread() {
        if (!waitQueue.isEmpty()) {
            _logger.log(Level.FINE, "Notifying the thread");
            WaitQueueEntry waitQueueEntry = waitQueue.first();
            waitQueue.remove(waitQueueEntry);
            waitQueueEntry.setNotified(true);
            synchronized (waitQueueEntry) {
                waitQueueEntry.notify();
            }
        }
    }

    /**
     * The class that holds the logic of wait queue entries.
     */
    static class WaitQueueEntry implements Comparable<WaitQueueEntry> {
        final long entryTime;
        volatile boolean notified = false;

        WaitQueueEntry(long entryTime) {
            this.entryTime = entryTime;
        }

        @Override
        public int compareTo(WaitQueueEntry obj) {
            return (int)(entryTime - obj.entryTime);
        }

        void setNotified(boolean notified) {
            this.notified = notified;
        }

        boolean isNotified() {
            return notified;
        }
    }
}

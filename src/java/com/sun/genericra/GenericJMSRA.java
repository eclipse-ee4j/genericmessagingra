/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra;

import com.sun.genericra.inbound.EndpointConsumerFactory;
import com.sun.genericra.inbound.AbstractConsumer;
import com.sun.genericra.util.*;

import java.io.Serializable;

import java.lang.reflect.Method;

import java.security.*;

import java.util.*;
import java.util.logging.*;

import javax.jms.*;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;

import javax.transaction.xa.XAResource;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import com.sun.genericra.monitoring.*;
import javax.management.StandardMBean;

/**
 * Resource Adapter javabean implementation for JMS resource adapter.
 *
 * Whenever an application server does a start() on the RA java bean,
 * an instance of the javabean will be saved for singleton usage.
 * This is required since admin objects need to obtain default resource
 * adapter instance.
 *
 * @author Sivakumar Thyagarajan, Binod P.G
 */
public class GenericJMSRA extends GenericJMSRAProperties
    implements ResourceAdapter {
    // Serialization of resource adapter will not happen since it is a static instance.
    
    /**
     * Singleton instance for the Generic JMS RA
     */
    private static GenericJMSRA raInstance = null;
    
    /**
     * Logger object to log messages.
     */
    private static Logger logger;

    /**
     * Gets the default logger
     */    
    static {
        logger = LogUtils.getLogger();
    }

    /** 
     * JCA contract bootstarp context supplied by the container.
     */
    private transient BootstrapContext context = null;
    
    /**
     * Table that stores all the consumer endpoints
     */
    private transient Hashtable consumers;
    
    /**
     * Method to be invoked on the endpoint.
     */
    private transient Method onMessageMethod = null;
    
    /** 
     * Util class to create objects from jndi names , or reflection.
     */
    private transient ObjectBuilderFactory obf = null;
    
    private transient MBeanServer mbeanserver = null;
    
    private transient ObjectName monitoringbean = null;
    
    private transient ResourceMonitor monitor = null;
    private transient ObjectName configbean = null;
    
    /**
     * Default log level for logging in genric JMS RA.
     */
    private String logLevel = Constants.LogLevel.INFO;

    /**
     * Returns the singleton implementation of this RA.     
     *
     * @return GenericJMSRA singleton instance.
     */     
    public static GenericJMSRA getInstance() {
        logger.log(Level.FINEST,
            "GenericJMSRA - getInstance() orig " + raInstance);

        return GenericJMSRA.raInstance;
    }

    /**
     * Stops the resource adaptor and all its endpoints.
     */
    public void stop() {
        obf = null;
        onMessageMethod = null;
        if (getMonitoring()) {
            unregisterMonitoringMBean();
        }
    }
    
    /** 
     * Starts/bootstraps the RA. This method is a lifecycle method
     * that is invoked by the application server to start the RA.
     * 
     * @param context Bootstrap context supplied by the application server.
     */
    public void start(BootstrapContext context)
        throws ResourceAdapterInternalException {
        logger.log(Level.FINEST, "GenericJMSRA.start() ....");
        GenericJMSRA.raInstance = this;
        this.obf = new ObjectBuilderFactory();
        this.consumers = new Hashtable();
        this.context = context;
        if (getMonitoring()) {  
            registerMonitoringMBean();
        }
        
        try {
            Class msgListenerClass = javax.jms.MessageListener.class;
            Class[] paramTypes = { javax.jms.Message.class };
            onMessageMethod = msgListenerClass.getMethod("onMessage", paramTypes);
        } catch (NoSuchMethodException ex) {
            throw ExceptionUtils.newResourceAdapterInternalException(ex);
        }
    }
    
    private void registerMonitoringMBean() {        
        try{                        
                mbeanserver = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
                if (mbeanserver == null)
                {
                    logger.log(Level.SEVERE, "Cannot get MBean server, monitoring is disabled");
                    return;
                }                
                monitor = new ResourceMonitor();                
                StandardMBean mbean = new StandardMBean(monitor, ResourceMonitorMBean.class);
                monitoringbean = new ObjectName("com.sun.genericra:name=Monitoring,category=InboundResources");    
                if (mbeanserver.isRegistered(monitoringbean)) {
                    mbeanserver.unregisterMBean(monitoringbean);
                }
                mbeanserver.registerMBean(mbean, monitoringbean);                    
                logger.log(Level.INFO, "Registered monitoring MBean with name " + monitoringbean);
                ConfigurationMonitor configmonitor = new ConfigurationMonitor();                
                configbean = new ObjectName("com.sun.genericra:name=Monitoring,category=Configuration");                
                StandardMBean configmbean = new StandardMBean(configmonitor, ConfigurationMonitorMBean.class);
                if (mbeanserver.isRegistered(configbean)) {
                    mbeanserver.unregisterMBean(configbean);
                }
                mbeanserver.registerMBean(configmbean, configbean);  
                logger.log(Level.INFO, "Registered monitoring MBean with name " + configbean);                
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            logger.log(Level.SEVERE, "Cannot get MBean server, monitoring is disabled");
        }        
    }
    
    private void unregisterMonitoringMBean() {
        try {
            mbeanserver.unregisterMBean(monitoringbean);
            mbeanserver.unregisterMBean(configbean);
            logger.log(Level.INFO, "Unregistered monitoing MBean " + monitoringbean);
            
        }
        catch (Throwable t) {
            logger.log(Level.SEVERE, "Cannot unregister monitoring mbean");
        }
    }

    /**
     * This method is invoked by the application server when it requires 
     * the RA to activate an endpoint. This method would typically be called
     * when an MDB is deployed in the application server and that uses the RA
     * for inbound communication.
     *
     * @param mef message endpoint factory given by the app server.
     */
    public void endpointActivation(MessageEndpointFactory mef,
        ActivationSpec spec) throws ResourceException {
        AbstractConsumer consumer = EndpointConsumerFactory.createEndpointConsumer(mef, spec);        
        consumer.start();        
        if ((getMonitoring()) && (monitor != null))  {
            if (consumer.getSpec().getApplicationName() != null) {  
                monitor.addPool(consumer.getSpec().getApplicationName(), consumer.getPool());
            }
            else {
                logger.log(Level.WARNING, "Application name is not configured in " +
                        "activation spec config, cannot monitor this endpoint");
            }
        }
        Hashtable consumers = getConsumers();
        EndpointKey key = new EndpointKey(mef, spec);
        consumers.put(key, consumer);
    }

    /**
     * This method is invoked by the application server when it requires 
     * the RA to de-activate an endpoint. This method would typically be called
     * when an MDB is un-deployed in the application server and that uses the RA
     * for inbound communication.
     *
     * @param mef message endpoint factory of the endpoint given by the app server.
     */
    public void endpointDeactivation(MessageEndpointFactory mef,
        ActivationSpec spec) {
        EndpointKey key = new EndpointKey(mef, spec);

        AbstractConsumer consumer = (AbstractConsumer) getConsumers().remove(key);
        if ((getMonitoring()) && (monitor != null))  {
            monitor.removePool(consumer.getSpec().getApplicationName());
        }
        if (consumer != null) {
            consumer.stop();
        }
    }

    /**
     * This method is used by the application server during crash recovery.
     * It returns all the XAResources of the activated endpoints, that are described
     * through the activation spec. The application server uses these objects to 
     * query the RM of in-doubt transactions.
     *
     * @ returns an array of XAResource objects corresponding to the endpoints.
     */
    public XAResource[] getXAResources(ActivationSpec[] specs)
        throws ResourceException {
        ArrayList xars = new ArrayList();

        for (int i = 0; i < specs.length; i++) {
            com.sun.genericra.inbound.ActivationSpec tmpSpec = null;

            if (specs[i] instanceof com.sun.genericra.inbound.ActivationSpec) {
                tmpSpec = (com.sun.genericra.inbound.ActivationSpec) specs[i];
            } else {
                continue;
            }

            if (tmpSpec.getSupportsXA()) {
                XAConnection xacon = null;
                XASession xasess = null;
                AbstractConsumer consumer = null;

                try {
                    consumer = EndpointConsumerFactory.createEndpointConsumer(tmpSpec);
                    consumer.initialize(true);
                    xacon = (XAConnection) consumer.getConnection();
                    xasess = xacon.createXASession();

                    XAResource xaRes = xasess.getXAResource();
                    xars.add(xaRes);
                    logger.log(Level.FINEST, "Added XA Resource : " + xaRes);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        return (XAResource[]) xars.toArray(new XAResource[0]);
    }

    /**
     * WorkManager that is provided by the container.
     *
     * @returns work manager provided by the app server.
     */
    public WorkManager getWorkManager() {
        return getInstance().context.getWorkManager();
    }

    /**
     * Object builder factory object that is used to create JMS 
     * administered objects through jndi lookup or reflection.
     *
     * @return ObjectBuilderFactory util class.
     */
    public ObjectBuilderFactory getObjectBuilderFactory() {
        return getInstance().obf;
    }

    
    /**
     * Sets the log level.
     * 
     * @param level log level.
     */
    public void setLogLevel(String level) {
        logger.log(Level.FINEST, "Setting log level:" + level);
        this.logLevel = level;
        setLevelInLogger(level);
    }

    /**
     * Returns the log level for the RA.
     *
     * @return loglevel.
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Returns the list of inbound endpoints.
     * 
     * @return table of endpoints.
     */
    private Hashtable getConsumers() {
        return getInstance().consumers;
    }

    /**
     * Sets the level for the RA logger.
     *
     * @param level loglevel.
     */
    private void setLevelInLogger(String level) {
        Level l = Level.INFO;

        if (level.equalsIgnoreCase(Constants.LogLevel.FINEST)) {
            logger.log(Level.FINEST, "Setting finest as log levels");
            l = Level.FINEST;
        } else if (level.equalsIgnoreCase(Constants.LogLevel.FINER)) {
            l = Level.FINER;
        } else if (level.equalsIgnoreCase(Constants.LogLevel.FINE)) {
            l = Level.FINE;
        } else if (level.equalsIgnoreCase(Constants.LogLevel.INFO)) {
            l = Level.INFO;
        } else if (level.equalsIgnoreCase(Constants.LogLevel.WARNING)) {
            l = Level.WARNING;
        } else if (level.equalsIgnoreCase(Constants.LogLevel.SEVERE)) {
            l = Level.SEVERE;
        }

        final Level tmp = l;
        AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    logger.setLevel(tmp);

                    return null; // nothing to return
                }
            });
    }

    /**
     * Retrieves the Method that is called in the MessageListener
     *
     * @return listening method.
     */
    public Method getListeningMethod() {
        return getInstance().onMessageMethod;
    }

    /** 
     * Unique key for every endpoint.
     */
    class EndpointKey implements Serializable {
        /**
         * MEssage endpoint Factory
         */
        private MessageEndpointFactory mef;
        /**
         * Activation spec.
         */
        private ActivationSpec spec;

        /** 
         * Constructor.
         */
        public EndpointKey(MessageEndpointFactory mef, ActivationSpec spec) {
            this.mef = mef;
            this.spec = spec;
        }

        /**
         * Tests if 2 keys are the same.
         *
         * @param obj key object.
         */
        public boolean equals(Object obj) {
            EndpointKey other = (EndpointKey) obj;

            return other.mef.equals(this.mef) && other.spec.equals(this.spec);
        }

        /**
         * Hash code for the endpoint key.
         * 
         * @returns unique integer.
         */
        public int hashCode() {
            return mef.hashCode() + spec.hashCode();
        }
    }
}

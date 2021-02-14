/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound;

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.GenericJMSRAProperties;
import com.sun.genericra.util.*;

import java.util.logging.*;

import jakarta.resource.spi.*;


/**
 * ActivationSpec for jakarta.jms.MessageListener.
 *
 * @author Binod P.G
 */
public class ActivationSpec extends GenericJMSRAProperties
    implements jakarta.resource.spi.ActivationSpec {
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    private String cfJndiName;
    private String cfProperties;
    private String destJndiName;
    private String destProperties;
    private String destinationType = Constants.DESTINATION;
    private String dmType = Constants.DESTINATION;
    private String messageSelector;
    private String subscriptionDurability = Constants.NONDURABLE;
    private String subscriptionName;
    private String clientId;
    private int redeliveryAttempts;
    private int redeliveryInterval;
    private int reconnectAttempts;
    private int reconnectInterval;
    private int maxPoolSize = 8;
    private int maxWaitTime = 300;
    private boolean isDmd = false;
    private String dmClassName;
    private String dmJndiName;
    private String dmCfJndiName;
    private String dmProperties;
    private String dmCfProperties;
    private int endpointReleaseTimeout = 180;
    private boolean shareclientid = false;
    /* START of properties for Load balancing topics */
    private int instanceCount = 1;
    private boolean loadBalance = true;    
    private String mCurrentInstance = "0";
    private int mCurrentInstanceNo = 0;
    private String mMessageSelector = "";
    private String mInstanceClientId = null;
    private static String SELECTOR_PROPERTY = "com.sun.genericra.loadbalancing.selector";    
    private static String INSTANCENO_PROPERTY = "com.sun.genericra.loadbalancing.instance.id";    
    private static String INSTANCE_CLIENTID_PROPERTY = "com.sun.genericra.loadbalancing.instance.clientid";
    /* END of properties for load balancing */
    
    /*Sync consumer props*/
    private int batchSize = 1;
    private boolean huaMode = false;
    private int ackTimeOut = Constants.DEFAULT_ACK_TIMEOUT;
    
    private StringManager sm = StringManager.getManager(GenericJMSRA.class);

    public void setMaxWaitTime(int waitTime) {
        this.maxWaitTime = waitTime;
    }

    public int getMaxWaitTime() {
        return this.maxWaitTime;
    }

    public void setRedeliveryInterval(int interval) {
        this.redeliveryInterval = interval;
    }

    public int getRedeliveryInterval() {
        return this.redeliveryInterval;
    }

    public void setRedeliveryAttempts(int attempts) {
        this.redeliveryAttempts = attempts;
    }    
    
    public int getRedeliveryAttempts() {
        return this.redeliveryAttempts;
    }
    
/* Following methods have been added for implementing topic lo
 * balancing.
 * BEGIN
 */
    public void setInstanceCount(int instancecount) {
        this.instanceCount = instancecount;
    }    
    
    public int getInstanceCount() {
        return this.instanceCount;
    }
    
    public void setLoadBalancingRequired(boolean loadbalance) {
        this.loadBalance = loadbalance;
    }    
    
    public boolean getLoadBalancingRequired() {
        return this.loadBalance;
    }
    
    /* Instace Id and load balancing selector cannot be configured through
     * the activation spec, but they are here because these seemes a logical place
     * to put them.
     * These have to be configured as jvm properties and can be unique for
     * different instances in a cluster
     */
    
    public int getInstanceID() {
        try {
            mCurrentInstance = System.getProperty(INSTANCENO_PROPERTY, "0");
            mCurrentInstanceNo = Integer.parseInt(mCurrentInstance.trim());
        } catch (Exception e)
        {
            // e.printStackTrace();
            mCurrentInstanceNo = 0;
        }  
        return this.mCurrentInstanceNo;
    }
    
    public String getInstanceClientId() {
        try {
            mInstanceClientId = System.getProperty(INSTANCE_CLIENTID_PROPERTY);
        }
        catch (Exception e) {
            ;
        }
        return mInstanceClientId;
    }        
    
    public String getLoadBalancingSelector()
    {
        try {
            mMessageSelector = System.getProperty(SELECTOR_PROPERTY, "");        
        } catch (Exception e)
        {
            e.printStackTrace();
            mMessageSelector = "";
        }      
        return this.mMessageSelector;
    }
    
    /* END
     */
    public void setReconnectInterval(int interval) {
        this.reconnectInterval = interval;
    }

    public int getReconnectInterval() {
        return this.reconnectInterval;
    }

    public void setReconnectAttempts(int attempts) {
        this.reconnectAttempts = attempts;
    }

    public int getReconnectAttempts() {
        return this.reconnectAttempts;
    }

    public void setSubscriptionDurability(String durability) {
        this.subscriptionDurability = durability;
    }

    public String getSubscriptionDurability() {
        return this.subscriptionDurability;
    }

    public void setSubscriptionName(String name) {
        this.subscriptionName = name;
    }

    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    public void setMessageSelector(String selector) {
        this.messageSelector = selector;
    }

    public String getMessageSelector() {
        return this.messageSelector;
    }

    public void setClientID(String clientId) {
        this.clientId = clientId;
    }

    public String getClientID() {
        return this.clientId;
    }

    public void setConnectionFactoryJndiName(String name) {
        this.cfJndiName = name;
    }

    public String getConnectionFactoryJndiName() {
        return this.cfJndiName;
    }

    public void setDestinationJndiName(String name) {
        this.destJndiName = name;
    }

    public String getDestinationJndiName() {
        return this.destJndiName;
    }

    public void setDestinationProperties(String properties) {
        this.destProperties = properties;
    }

    public String getDestinationProperties() {
        return this.destProperties;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    public void setSendBadMessagesToDMD(boolean isDmd) {
        this.isDmd = isDmd;
    }

    public boolean getSendBadMessagesToDMD() {
        return this.isDmd;
    }

    public void setDeadMessageJndiName(String jndiName) {
        this.dmJndiName = jndiName;
    }

    public void setDeadMessageConnectionFactoryProperties(String p) {
        this.dmCfProperties = p;
    }

    public String getDeadMessageConnectionFactoryProperties() {
        return this.dmCfProperties;
    }

    public void setDeadMessageConnectionFactoryJndiName(String jndiName) {
        this.dmCfJndiName = jndiName;
    }

    public String getDeadMessageConnectionFactoryJndiName() {
        return this.dmCfJndiName;
    }

    public void setDeadMessageDestinationJndiName(String jndiName) {
        this.dmJndiName = jndiName;
    }

    public String getDeadMessageDestinationJndiName() {
        return this.dmJndiName;
    }

    public void setDeadMessageDestinationClassName(String className) {
        this.dmClassName = className;
    }

    public String getDeadMessageDestinationClassName() {
        return this.dmClassName;
    }

    public void setDeadMessageDestinationProperties(String dmdProps) {
        this.dmProperties = dmdProps;
    }

    public String getDeadMessageDestinationProperties() {
        return this.dmProperties;
    }

    public String getDeadMessageDestinationType() {
        return this.dmType;
    }

    public void setDeadMessageDestinationType(String dmType) {
        this.dmType = dmType;
    }

    public void setEndpointReleaseTimeout(int secs) {
        this.endpointReleaseTimeout = secs;
    }

    public int getEndpointReleaseTimeout() {
        return this.endpointReleaseTimeout;
    }
    
    public boolean getShareClientid() {
        return this.shareclientid;
    }
    
    public void setShareClientid(boolean genclientid){
        this.shareclientid = genclientid;
    }    
    
    public void validate() throws InvalidPropertyException {
        logger.log(Level.FINE, "" + this);

        //XXX: perform CF, XAQCF, XATCF validation!
        if (getMaxPoolSize() <= 0) {
            String msg = sm.getString("maxpoolsize_iszero");
            throw new InvalidPropertyException(msg);
        }

        if (getMaxWaitTime() < 0) {
            String msg = sm.getString("maxwaittime_lessthan_zero");
            throw new InvalidPropertyException(msg);
        }

        if (getRedeliveryAttempts() < 0) {
            String msg = sm.getString("redelivery_attempts_lessthan_zero");
            throw new InvalidPropertyException(msg);
        }

        if (getRedeliveryInterval() < 0) {
            String msg = sm.getString("redelivery_attempts_lessthan_zero");
            throw new InvalidPropertyException(msg);
        }

        if (getEndpointReleaseTimeout() < 0) {
            String msg = sm.getString("endpointreleasetimeout_lessthan_zero");
            throw new InvalidPropertyException(msg);
        }

        if (getInstanceCount() < 1)
        {
         String msg = sm.getString("instancecount_lessthan_zero");
            throw new InvalidPropertyException(msg);           
        }
        
        if ((getInstanceID() < 0) || (getInstanceID() >= getInstanceCount()))
        {            
            String msg = sm.getString("instanceid_should_be_between_0_and_instancecount");
            throw new InvalidPropertyException(msg);              
        }
        
            if (getSendBadMessagesToDMD()) {
            if (getProviderIntegrationMode().equalsIgnoreCase(Constants.JNDI_BASED)) {
                if (StringUtils.isNull(getDeadMessageDestinationJndiName())) {
                    String msg = sm.getString("dmd_jndi_null");
                    throw new InvalidPropertyException(msg);
                }
            } else {
                if (StringUtils.isNull(getDeadMessageDestinationProperties())) {
                    String msg = sm.getString("dmd_props_null");
                    throw new InvalidPropertyException(msg);
                }
            }
        }
    }

    public String toString() {
        String s = super.toString();
        s = s + "{RedeliveryInterval = " + getRedeliveryInterval() + "},";
        s = s + "{RedeliveryAttempts = " + getRedeliveryAttempts() + "},";
        s = s + "{ClientID = " + getClientID() + "},";
        s = s + "{MessageSelector = " + getMessageSelector() + "},";
        s = s + "{SubscriptionDurability = " + getSubscriptionDurability() +
            "},";
        s = s + "{ConnectionFactoryJNDIName = " +
            getConnectionFactoryJndiName() + "},";
        s = s + "{SubscriptionName = " + getSubscriptionName() + "},";
        s = s + "{DestinationJNDIName = " + getDestinationJndiName() + "},";
        s = s + "{DestinationType = " + getDestinationType() + "},";
        s = s + "{DeadMessageDestinationType = " +
            getDeadMessageDestinationType() + "},";
        s = s + "{MaxPoolSize = " + getMaxPoolSize() + "},";
        s = s + "{DestinationProperties = " + getDestinationProperties() +
            "},";
        s = s + "{DeadMessageDestinationJndiName = " +
            getDeadMessageDestinationJndiName() + "},";
        s = s + "{DeadMessageConnectionFactoryJndiName = " +
            getDeadMessageConnectionFactoryJndiName() + "},";
        s = s + "{DeadMessageConnectionFactoryProperties = " +
            getDeadMessageConnectionFactoryProperties() + "},";
        s = s + "{DeadMessageDestinationClassName = " +
            getDeadMessageDestinationClassName() + "},";
        s = s + "{DeadMessageDestinationProperties = " +
            getDeadMessageDestinationProperties() + "},";
        s = s + "{SendBadMessagesToDMD = " + getSendBadMessagesToDMD() + "},";
        s = s + "{EndpointReleaseTimeOut = " + getEndpointReleaseTimeout() +
            "},";
        s = s + "{InstanceCount = " + getInstanceCount() + "},";
        s = s + "{LoadBalancingRequired = " + getLoadBalancingRequired() + "},";
        s = s + "{Instance ID = " + getInstanceID() + "},";
        s = s + "{CustomLoadBalancingMessageSelector = " + getLoadBalancingSelector() + "},";
        s = s + "{ShareClientID = " + getShareClientid() + "}";
        s = s + "{DeliveryType = " + getDeliveryType() + "}";
 
        return s;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    /**
     * Holds value of property applicationName.
     */
    private String applicationName;

    /**
     * Getter for property applicationName.
     * @return Value of property applicationName.
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * Setter for property applicationName.
     * @param applicationName New value of property applicationName.
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int size) {
        batchSize = size;
    }
    
    public void setHUAMode(boolean huamode){
        huaMode = huamode;
    }
    
    public boolean getHUAMode() {
        return huaMode;
    }
    
    public void setAckTimeOut(int timeout) {
        ackTimeOut = timeout;
    }
    
    public int getAckTimeOut() {
        return ackTimeOut;
    }
}

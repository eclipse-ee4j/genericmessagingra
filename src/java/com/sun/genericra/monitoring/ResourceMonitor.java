/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.monitoring;

import java.util.Hashtable;


import com.sun.genericra.inbound.AbstractJmsResourcePool;

import com.sun.genericra.inbound.async.InboundJmsResourcePool;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.genericra.util.LogUtils;


/**
 * MBEan implemenation to monitor the inbound resource pool.
 * 
 */
public class ResourceMonitor implements ResourceMonitorMBean {
    
    public Hashtable poolstatistics;
    
    public static Logger logger;
    
    
    static {
        logger = LogUtils.getLogger();
    }
    /** Creates a new instance of ResourceMonitor */
    public ResourceMonitor() {
        poolstatistics = new Hashtable();
    }
    
    public void addPool(String consumer, AbstractJmsResourcePool ps) {
        
        if ((ps != null) && (consumer != null)) {
            try {
                poolstatistics.put(consumer, ps);
            } catch (Throwable t ){
                
                t.printStackTrace();
                logger.log(Level.SEVERE, "Exception while initializing " +
                        "monitoring for endpoint " + consumer);
            }
            logger.log(Level.FINE, "Including endpoint " + consumer + " for monitoring");
        }
    }
    
    public void removePool(String consumer) {
        try {
            poolstatistics.remove(consumer);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Unable to remove " + consumer +
                    " from monitoring table");
        }
    }
    
    private boolean validate(String appname) {
        if ((appname == null) || (appname.trim().equals(""))) {
            return false;
        }
        return true;
    }
    
    public String getPoolStatistics(String appname) {
        if (!validate(appname)) {
            return "Invalid application name";
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return "Endpoint not found OR could not be monitored";
        }
        PoolStatistics ps = new PoolStatistics((InboundJmsResourcePool)pool);
        return ps.formatStatistics();
    }
    
    private AbstractJmsResourcePool getPool(String name) {
        AbstractJmsResourcePool pool = null;
        try {
            pool = (AbstractJmsResourcePool) poolstatistics.get(name.trim());
        } catch (Exception e) {
            ;
        }
        return pool;
    }
    /**
     * Getter for property currentSize.
     * @return Value of property currentSize.
     */
    public  int getCurrentSize(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getCurrentResources();
    }
    
    /**
     * Getter for property busyResources.
     * @return Value of property busyResources.
     */
    public int getBusyResources(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getBusyResources();
    }
    
    
    /**
     * Getter for property freeResources.
     * @return Value of property freeResources.
     */
    public int getFreeResources(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getFreeResources();
    }
    
    /**
     * Getter for property waiting.
     * @return Value of property waiting.
     */
    public int getWaiting(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getWaiting();
    }
    
    public int getConnections(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getConnectionsInUse();
    }
    
    public int getMaxSize(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return pool.getMaxSize();
    }
    
    public long getMaxWaitTime(String appname) {
        if (!validate(appname)) {
            return -1;
        }
        AbstractJmsResourcePool pool = getPool(appname);
        if (pool == null) {
            return -2;
        }
        return  pool.getMaxWaitTime();
    }
    
}

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

/**
 *
 * @author ramesh
 */
public interface ResourceMonitorMBean {
    
    /**
     * Return the pool parameters for the endpoint
     *
     * @param endpoint name.
     * @return pool parameters. 
     */
    String getPoolStatistics(String name);
    
    /**
     * Getter for property currentSize.
     * 
     * @param endpoint name.
     * @return Value of property currentSize.
     */
    int getCurrentSize(String name);
    
    /**
     * Getter for property busyResources.
     *
     * @param endpoint name.
     * @return Value of property busyResources.
     */
    int getBusyResources(String name);    
    
    /**
     * Getter for property freeResources.
     *
     * @param endpoint name.
     * @return Value of property freeResources.
     */
    int getFreeResources(String name);
    
    /**
     * Getter for property waiting.
     *
     * @param endpoint name.
     * @return Value of property waiting.
     */
    int getWaiting(String name);
    
    /**
     * Returns the connections used by the endpoint
     *
     * @param endpoint name.
     * @return number of connections.
     */
    int getConnections(String name);
    
    /** 
     * Returns the maximum size of pool.
     * 
     * @param endpoint name.
     * @return max size.
     */
    int getMaxSize(String name);
    
    /**
     * Returns the max wait time of the pool.
     * @param endpoint name.
     * @return wait time.
     */
    long getMaxWaitTime(String name) ;    
    
}

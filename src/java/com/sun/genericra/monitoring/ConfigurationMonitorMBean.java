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
 * MBean inteface for Generic jms RA configuration changes.
 * 
 */
public interface ConfigurationMonitorMBean {
    /**
     * Returns the log level for the RA.
     */    
    String getLogLevel();
     /**
     * Returns the listener interface that this RA is capable of invoking.
     */    
    String getListenerMethod();
    /**
     * Sets the log level of generic jms ra.
     */    
    String setLogLevel(String level);   

}

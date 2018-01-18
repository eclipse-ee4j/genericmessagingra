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

import com.sun.genericra.GenericJMSRA;

/**
 * This class is the MBean implementation of generic jms ra configuration
 * monitor. It supports runtime modification of Generic RA through MBean
 * operations.
 */

public class ConfigurationMonitor implements ConfigurationMonitorMBean {
    
    /** Creates a new instance of ConfigurationMonitor */
    public ConfigurationMonitor() {
    }
    
    /**
     * Returns the log level for the RA.
     */
    public  String getLogLevel() {
        return GenericJMSRA.getInstance().getLogLevel();
    }
    
    /**
     * Returns the listener interface that this RA is capable of invoking.
     */    
    public String getListenerMethod(){
        return GenericJMSRA.getInstance().getListeningMethod().toString();
    }
    
    /**
     * Sets the log level of generic jms ra.
     */
    public String setLogLevel(String level){
        String ret = null;
        try {
            GenericJMSRA.getInstance().setLogLevel(level);            
        } catch (Throwable t) {
            ret = "Cannot set log level to " + level + " " + t.getMessage();
        }
        ret = "Log level set to " + GenericJMSRA.getInstance().getLogLevel();;
        return ret;
    }  
    
}

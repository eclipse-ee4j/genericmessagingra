/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.util;


/**
 * Constant values used by Generic Resource Adapter.
 *
 * @author Binod P.G
 */
public final class Constants {
    /**
     * Logger name used by the resource adapter
     */
    public static final String LOGGER_NAME = "com.sun.genericjmsra";

    /**
     * default separator used by resource adapter
     */
    public static final String SEPARATOR = "=";

    /**
     * default delimiter used by resource adapter
     */
    public static final String DELIMITER = ",";

    /**
     * Name for Jndi based provider integration mode
     */
    public static final String JNDI_BASED = "jndi";

    /**
     * Name for Javabean provider integration mode
     */
    public static final String JAVABEAN_BASED = "javabean";

    /**
     * String indicating a queue
     */
    public static final String QUEUE = "jakarta.jms.Queue";

    /**
     * String indicating a topic
     */
    public static final String TOPIC = "jakarta.jms.Topic";

    /**
     * String indicating a destination
     */
    public static final String DESTINATION = "jakarta.jms.Destination";

    /**
     * String indicating a durable
     */
    public static final String DURABLE = "Durable";
    
    public static int DEFAULT_ACK_TIMEOUT = 2;

    /**
     * String indicating a non-durable
     */
    public static final String NONDURABLE = "Non-Durable";
    public static final int UNIFIED_SESSION = 0;
    public static final int TOPIC_SESSION = 1;
    public static final int QUEUE_SESSION = 2;

    public class LogLevel {
        /**
         * String indicating FINEST log level
         */
        public static final String FINEST = "finest";

        /**
         * String indicating FINER log level
         */
        public static final String FINER = "finer";

        /**
         * String indicating FINE log level
         */
        public static final String FINE = "fine";

        /**
         * String indicating INFO log level
         */
        public static final String INFO = "info";

        /**
         * String indicating WARNING log level
         */
        public static final String WARNING = "warning";

        /**
         * String indicating SEVERE log level
         */
        public static final String SEVERE = "severe";
        
    }
}

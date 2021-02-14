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

import jakarta.jms.JMSException;

import jakarta.resource.*;
import jakarta.resource.spi.*;

import javax.transaction.xa.XAException;


/**
 * Utility class that generate Exceptions
 *
 * @author Binod P.G
 */
public class ExceptionUtils {
    public static jakarta.resource.spi.SecurityException newSecurityException(
        Throwable t) {
        jakarta.resource.spi.SecurityException se = new jakarta.resource.spi.SecurityException(t.getMessage());

        return (jakarta.resource.spi.SecurityException) se.initCause(t);
    }

    public static InvalidPropertyException newInvalidPropertyException(
        Throwable t) {
        InvalidPropertyException ipe = new InvalidPropertyException(t.getMessage());

        return (InvalidPropertyException) ipe.initCause(t);
    }

    public static Exception newException(Throwable t) {
        Exception se = new Exception(t.getMessage());

        return (Exception) se.initCause(t);
    }

    public static RuntimeException newRuntimeException(Throwable t) {
        RuntimeException se = new RuntimeException(t.getMessage());

        return (RuntimeException) se.initCause(t);
    }

    public static ResourceAdapterInternalException newResourceAdapterInternalException(
        Throwable t) {
        ResourceAdapterInternalException se = new ResourceAdapterInternalException(t.getMessage());

        return (ResourceAdapterInternalException) se.initCause(t);
    }

    public static JMSException newJMSException(Throwable t) {
        JMSException se = new JMSException(t.getMessage());

        return (JMSException) se.initCause(t);
    }

    public static ResourceException newResourceException(Throwable t) {
        ResourceException se = new ResourceException(t.getMessage());

        return (ResourceException) se.initCause(t);
    }

    public static XAException newXAException(Throwable t) {
        XAException se = new XAException(t.getMessage());

        return (XAException) se.initCause(t);
    }
}

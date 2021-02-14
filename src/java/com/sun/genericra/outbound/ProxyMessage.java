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

import com.sun.genericra.util.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.logging.*;

import jakarta.jms.*;


/**
 * ProxyMessage. This is a Proxy Message that overrides the setJMSReplyTo
 * method.
 */
public final class ProxyMessage implements InvocationHandler {
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    Message msg = null;

    public ProxyMessage(Message msg) {
        this.msg = msg;
    }

    /**
     * Invokes the method
     *
     * @param proxy Object
     * @param method <code>Method</code> to be executed.
     * @param args Arguments
     * @throws Throwable.
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
        String methodName = method.getName();

        if (methodName.equals("setJMSReplyTo") ||
                methodName.equals("setJMSDestination")) {
            if (args[0] instanceof DestinationAdapter) {
                args[0] = ((DestinationAdapter) args[0])._getPhysicalDestination();

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                        methodName +
                        "is being called with unwrapped destination");
                }
            }
        }

        return method.invoke(msg, args);
    }
}

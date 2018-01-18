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

import java.lang.reflect.*;

import java.security.*;

import java.util.Hashtable;
import java.util.logging.*;

import javax.naming.*;

import javax.resource.ResourceException;


/**
 * Build an object based on classname or look up
 * based on JNDI name.
 *
 * @author Binod P.G
 */
public class ObjectBuilderFactory {
    private static boolean debug = false;
    private static Logger logger;

    static {
        logger = LogUtils.getLogger();
    }

    public ObjectBuilder createUsingClassName(String name) {
        return new ClassObjectBuilder(name);
    }

    public ObjectBuilder createUsingJndiName(String jndiName, String jndiProps) {
        return new JndiObjectBuilder(jndiName, jndiProps);
    }

    void debug(String str) {
        logger.log(Level.FINEST, str);
    }

    class ClassObjectBuilder extends ObjectBuilder {
        private String className = null;

        public ClassObjectBuilder(String className) {
            this.className = className;
        }

        public Object createObject() throws ResourceException {
            try {
                return Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();                       
                throw ExceptionUtils.newInvalidPropertyException(e);
            } catch (InstantiationException ie) {
                ie.printStackTrace();
                throw ExceptionUtils.newInvalidPropertyException(ie);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
                throw ExceptionUtils.newSecurityException(iae);
            }
        }
    }

    class JndiObjectBuilder extends ObjectBuilder {
        private String jndiName = null;
        private String jndiProps = null;

        JndiObjectBuilder(String jndiName, String jndiProps) {
            this.jndiName = jndiName;
            this.jndiProps = jndiProps;
        }

        public Object createObject() throws ResourceException {
        	InitialContext ic = null;
            try {
                Hashtable props = parseToProperties(this.jndiProps);
                debug("Properties passed to InitialContext :: " + props);

                ic = new InitialContext(props);
                debug("Looking the JNDI name :" + this.jndiName);

                return ic.lookup(this.jndiName);
            } catch (Exception e) {
                throw ExceptionUtils.newInvalidPropertyException(e);
            } finally {
            	if (ic!=null)
					try {
						ic.close();
					} catch (NamingException e) {
						// ignore errors on closing the InitialContext
					}
            }
        }
    }
}

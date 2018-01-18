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

import javax.jms.Connection;


/**
 * Interface used by all XAResource objecrts that are wrapped.
 *
 * @author Binod P.G
 */
public interface XAResourceType {
    /**
     * Retrieve the XAResource object wrapped.
     */
    public Object getWrappedObject();

    /**
     * Set the Resource Manager policy
     */
    public void setRMPolicy(String policy);

    /**
     * Retrieve the RM policy
     */
    public String getRMPolicy();

    /**
     * Decide whether to override the underlying XAResource's implementation of isSameRM() 
     * so that it returns false.
     * 
     * If the decision can be delegated to the underlying XAResourceImplementations,
     * return true.
     * 
     * If this isSameRM() must return false, return false.
     */
    public boolean compare(XAResourceType other);

    /**
     * Retrieves the physical JMS connection object.
     */
    public Connection getConnection();

    /**
     * Set the physical jms connection object associated with
     * this XAResource wrapper
     */
    public void setConnection(Connection con);
}

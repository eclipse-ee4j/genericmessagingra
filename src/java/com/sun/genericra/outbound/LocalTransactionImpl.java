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

import com.sun.genericra.util.ExceptionUtils;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.LocalTransactionException;


/**
 * <code>LocalTransaction</code> implementation for Generic JMS Resource Adapter.
 *
 * @author Sivakumar Thyagarajan
 */
public class LocalTransactionImpl implements jakarta.resource.spi.LocalTransaction {
    private ManagedConnection mc;

    /**
     * Constructor for <code>LocalTransaction</code>.
     * @param   mc  <code>ManagedConnection</code> that returns
     *          this <code>LocalTransaction</code> object as
     *          a result of <code>getLocalTransaction</code>
     */
    public LocalTransactionImpl(ManagedConnection mc) {
        this.mc = mc;
    }

    /**
     * Begin a local transaction.
     *
     */
    public void begin() throws ResourceException {
        try {
            mc._startLocalTx();
        } catch (Exception e) {
            throw ExceptionUtils.newResourceException(e);
        }
    }

    /**
     * Commit a local transaction.
     */
    public void commit() throws ResourceException {
        try {
            mc._endLocalTx(true);
        } catch (Exception e) {
            throw ExceptionUtils.newResourceException(e);
        }
    }

    /**
     * Rollback a local transaction.
     */
    public void rollback() throws ResourceException {
        try {
            mc._endLocalTx(false);
        } catch (Exception e) {
            throw ExceptionUtils.newResourceException(e);
        }
    }
}

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

import java.io.Serializable;


/**
 * Default simple <code>ConnectionManager</code> implementation of the generic
 * JMS Resource Adapter
 *
 * Creates a new <code>ManagedConnection</code> for every
 * <code>allocateConnection</code>
 *
 * @author Sivakumar Thyagarajan
 */
public class ConnectionManager implements javax.resource.spi.ConnectionManager,
    Serializable {
    public Object allocateConnection(
        javax.resource.spi.ManagedConnectionFactory mcf,
        javax.resource.spi.ConnectionRequestInfo cxRequestInfo)
        throws javax.resource.ResourceException {
        javax.resource.spi.ManagedConnection mc = mcf.createManagedConnection(null,
                cxRequestInfo);

        return mc.getConnection(null, cxRequestInfo);
    }
}

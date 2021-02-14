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

import com.sun.genericra.util.SecurityUtils;
import com.sun.genericra.util.StringUtils;

import jakarta.resource.spi.ManagedConnectionFactory;


/**
 * Generic JMS resource adapter specific request properties.
 * Username, password and clientId;
 * @author Sivakumar Thyagarajan
 */
public class ConnectionRequestInfo
    implements jakarta.resource.spi.ConnectionRequestInfo {
    private String userName;
    private String password;
    private String clientID;
    private ManagedConnectionFactory mcf;

    public ConnectionRequestInfo(ManagedConnectionFactory mcf, String userName,
        String password) {
        this.userName = userName;
        this.password = password;
        this.mcf = mcf;
        this.clientID = ((AbstractManagedConnectionFactory) mcf).getClientId();
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof ConnectionRequestInfo) {
            ConnectionRequestInfo other = (ConnectionRequestInfo) obj;

            return (StringUtils.isEqual(this.userName, other.userName) &&
            StringUtils.isEqual(this.password, other.password));
        } else {
            return false;
        }
    }

    /**
     * Retrieves the hashcode of the object.
     *
     * @return  hashCode.
     */
    public int hashCode() {
        String result = "" + userName + password;

        return result.hashCode();
    }
}

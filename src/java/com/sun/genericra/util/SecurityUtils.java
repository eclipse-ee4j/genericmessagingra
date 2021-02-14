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

import com.sun.genericra.outbound.ConnectionRequestInfo;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Iterator;
import java.util.Set;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.security.PasswordCredential;

import javax.security.auth.Subject;


/**
 * Utility class for security relates operations
 *
 * @author Sivakumar Thyagarajan
 */
public class SecurityUtils {
    /**
     * This method returns the <code>PasswordCredential</code> object, given
     * the <code>ManagedConnectionFactory</code>, subject and the
     * <code>ConnectionRequestInfo</code>. It first checks if the
     * <code>ConnectionRequestInfo</code> is null or not. If it is not null,
     * it constructs a <code>PasswordCredential</code> object with the user
     * and password fields from the <code>ConnectionRequestInfo</code> and
     * returns this <code>PasswordCredential</code> object. If the
     * <code>ConnectionRequestInfo</code> is null, it retrieves the
     * <code>PasswordCredential</code> objects from the <code>Subject</code>
     * parameter and returns the first <code>PasswordCredential</code> object
     * which contains a <code>ManagedConnectionFactory</code>, instance
     * equivalent to the <code>ManagedConnectionFactory</code>, parameter.
     *
     * @param mcf
     *            <code>ManagedConnectionFactory</code>
     * @param subject
     *            <code>Subject</code>
     * @param info
     *            <code>ConnectionRequestInfo</code>
     * @return <code>PasswordCredential</code>
     * @throws <code>ResourceException</code> generic exception if operation
     *             fails
     * @throws <code>SecurityException</code> if access to the
     *             <code>Subject</code> instance is denied
     */
    public static PasswordCredential getPasswordCredential(
        final ManagedConnectionFactory mcf, final Subject subject,
        jakarta.resource.spi.ConnectionRequestInfo info)
        throws ResourceException {
        if (info == null) {
            if (subject == null) {
                return null;
            } else {
                //get valid PC from PrivateCredentialsSet in Subject 
                PasswordCredential pc = (PasswordCredential) AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                Set passwdCredentialSet = subject.getPrivateCredentials(PasswordCredential.class);
                                Iterator iter = passwdCredentialSet.iterator();

                                while (iter.hasNext()) {
                                    PasswordCredential temp = (PasswordCredential) iter.next();

                                    if (temp.getManagedConnectionFactory()
                                                .equals(mcf)) {
                                        return temp;
                                    }
                                }

                                return null;
                            }
                        });

                if (pc == null) {
                    throw new jakarta.resource.spi.SecurityException(
                        "No password credentials");
                } else {
                    return pc;
                }
            }
        } else {
            ConnectionRequestInfo cxReqInfo = (ConnectionRequestInfo) info;
            PasswordCredential pc = new PasswordCredential(cxReqInfo.getUserName(),
                    cxReqInfo.getPassword().toCharArray());
            pc.setManagedConnectionFactory(mcf);

            return pc;
        }
    }

    /**
     * Returns true if two <code>PasswordCredential</code> objects are equal; false otherwise
     *
     * @param        pC1        <code>PasswordCredential</code>
     * @param        pC2        <code>PasswordCredential</code>
     * @return        true        if the two PasswordCredentials are equal
     *                false        otherwise
     */
    public static boolean isPasswordCredentialEqual(PasswordCredential pC1,
        PasswordCredential pC2) {
        if (pC1 == pC2) {
            return true;
        }

        if ((pC1 == null) || (pC2 == null)) {
            return (pC1 == pC2);
        }

        if (!StringUtils.isEqual(pC1.getUserName(), pC2.getUserName())) {
            return false;
        }

        String p1 = null;
        String p2 = null;

        if (pC1.getPassword() != null) {
            p1 = new String(pC1.getPassword());
        }

        if (pC2.getPassword() != null) {
            p2 = new String(pC2.getPassword());
        }

        return (StringUtils.isEqual(p1, p2));
    }
}

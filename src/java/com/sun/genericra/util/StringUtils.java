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
 * Common utility functions for <code>String</code>
 * @author Sivakumar Thyagarajan
 */
public class StringUtils {
    public static boolean isNull(String s) {
        return ((s == null) || s.trim().equals(""));
    }

    /**
     * Returns true if two strings are equal; false otherwise
     *
     * @param   str1    <code>String</code>
     * @param   str2    <code>String</code>
     * @return  true    if the two strings are equal
     *          false   otherwise
     */
    static public boolean isEqual(String str1, String str2) {
        boolean result = false;

        if (str1 == null) {
            result = (str2 == null);
        } else {
            result = str1.equals(str2);
        }

        return result;
    }
}

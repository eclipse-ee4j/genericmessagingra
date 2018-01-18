/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package hello;
/*
encapsulates a javax.jms.ObjectMessage that is sent 
*/

public class OrderMessage implements java.io.Serializable
{
    public String name;
    public String quantity;
    public String date;
};


/*
 * Copyright (c) 2003, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/*
 * %W% %E%
 */

package test.stress.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

public interface Send extends EJBObject {
	
    /**
     * Send a TextMessage containing the specified String to the configured outbound queue
     * @param text
     */
    void sendTextMessageToQ(String Property) throws RemoteException ;
}


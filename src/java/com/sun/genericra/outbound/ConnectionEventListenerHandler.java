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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;


/**
 * Encapsulates the event listener usage of the resource adapter.
 * All the connection event listeners registered will be saved
 * in this object.
 *
 * @author Sivakumar Thyagarajan
 */
public class ConnectionEventListenerHandler {
    private ArrayList l = new ArrayList();
    private ManagedConnection mc;

    public ConnectionEventListenerHandler(ManagedConnection mc) {
        this.mc = mc;
    }

    public void addConnectionEventListener(ConnectionEventListener cel) {
        this.l.add(cel);
    }

    public void removeConnectionEventListener(ConnectionEventListener cel) {
        this.l.remove(cel);
    }

    public void sendEvent(int eventType, Exception ex, Object connectionHandle) {
        List lClone = (List) this.l.clone();
        ConnectionEvent cevent = null;

        if (ex != null) {
            cevent = new ConnectionEvent(this.mc, eventType, ex);
        } else {
            cevent = new ConnectionEvent(mc, eventType);
        }

        if (connectionHandle != null) {
            cevent.setConnectionHandle(connectionHandle);
        }

        for (Iterator iter = lClone.iterator(); iter.hasNext();) {
            ConnectionEventListener lstnr = (ConnectionEventListener) iter.next();

            switch (eventType) {
            case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                lstnr.connectionErrorOccurred(cevent);

                break;

            case ConnectionEvent.CONNECTION_CLOSED:
                lstnr.connectionClosed(cevent);

                break;

            case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                lstnr.localTransactionStarted(cevent);

                break;

            case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                lstnr.localTransactionCommitted(cevent);

                break;

            case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                lstnr.localTransactionRolledback(cevent);

                break;

            default:
                throw new IllegalArgumentException("Unknown Connection " +
                    "Event Type :" + eventType);
            }
        }
    }
}

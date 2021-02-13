/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.async;

import com.sun.genericra.util.*;

import java.util.logging.*;

import jakarta.resource.spi.work.*;


/**
 * Work object as per JCA 1.5 specification.
 * This class makes sure that each message delivery happens in
 * different thread.
 *
 * @author Binod P.G
 */
public class WorkImpl implements Work {
    private static Logger _logger;

    static {
        _logger = LogUtils.getLogger();
    }

    InboundJmsResource jmsResource;

    public WorkImpl(InboundJmsResource jmsResource) {
        this.jmsResource = jmsResource;
    }

    public void run() {
        try {
            _logger.log(Level.FINER, "Now running the message consumption");
            this.jmsResource.refresh();
            this.jmsResource.getSession().run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {                
                this.jmsResource.releaseEndpoint();
                DeliveryHelper helper = this.jmsResource.getDeliveryHelper();
                if (helper.markedForDMD()) {
                    helper.sendMessageToDMD();
                }
            } catch (Exception e) {
                _logger.log(Level.SEVERE,
                        "Exception while releasing the JMS endpoint" + e.getMessage());
            } finally {
                try {
                    this.jmsResource.release();
                } catch (Exception e) {
                    _logger.log(Level.SEVERE, 
                            "Exception while releasing the JMS resource" + e.getMessage());
                }
            }
            _logger.log(Level.FINER, "Freed the resource now");
        }
    }

    public void release() {
        // For now do nothing.
    }
}

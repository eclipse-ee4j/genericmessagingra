/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound;


import com.sun.genericra.inbound.sync.*;

import com.sun.genericra.inbound.async.*;

import com.sun.genericra.util.*;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
/**
 *
 * @author rp138409
 */
public class EndpointConsumerFactory {
    
    /** Creates a new instance of EndpointConsumerFactory */
    public EndpointConsumerFactory() {
    }
    
    public static AbstractConsumer createEndpointConsumer(MessageEndpointFactory mef,
        jakarta.resource.spi.ActivationSpec actspec) throws ResourceException   {        
        AbstractConsumer ret = null;
        String type = ((com.sun.genericra.inbound.ActivationSpec)actspec).getDeliveryType();
        if ((type == null) || (type.trim().equals(""))) {
            ret = new EndpointConsumer(mef, actspec);
        } else if ("Synchronous".equals(type.trim())) {
            ret = new SyncConsumer(mef, actspec);
        } else if ("Asynchronous".equals(type.trim())) {
            ret = new EndpointConsumer(mef, actspec);        
        } else {
            ret = new EndpointConsumer(mef, actspec);
        }    
        return ret;
    }           
    
    public static AbstractConsumer createEndpointConsumer(
            jakarta.resource.spi.ActivationSpec actspec) throws ResourceException   {
        return createEndpointConsumer(null,actspec);
    }
}

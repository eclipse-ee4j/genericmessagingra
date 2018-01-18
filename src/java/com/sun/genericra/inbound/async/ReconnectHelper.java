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

import com.sun.genericra.inbound.*;
import com.sun.genericra.util.*;

import java.util.logging.*;

import javax.jms.*;

import javax.resource.*;
import javax.resource.spi.*;


/**
 * Helper class that enables redelivery in case connection
 * generates an Exception.
 *
 * @author Binod P.G
 */
public class ReconnectHelper implements ExceptionListener {
    private static Logger _logger;

    static {
        _logger = LogUtils.getLogger();
    }

    private InboundJmsResourcePool pool = null;
    private EndpointConsumer consumer = null;
    private int attempts = 0;
    private long interval = 0;
    private boolean reconnectInitiated = false;

    public ReconnectHelper(InboundJmsResourcePool pool,
        EndpointConsumer consumer) {
        this.pool = pool;
        this.consumer = consumer;
        this.attempts = consumer.getSpec().getReconnectAttempts();
        this.interval = consumer.getSpec().getReconnectInterval() * 1000;
    }

    public void onException(JMSException exception) {
        if (reconnectInitiated || this.consumer.isStopped()) {
            _logger.log(Level.INFO, "Reconnect is in progress");

            return;
        } else {
            reconnectInitiated = true;
        }

        _logger.log(Level.INFO, "Reconnecting now");
        _logger.log(Level.INFO, "Reconnecting now");
        _logger.log(Level.INFO, "Reconnecting now");
         if (reestablishPool()) {
                // this.consumer.closeConsumer();
                // this.consumer.restart();
                _logger.log(Level.INFO, "Reconnected!!");

                // TO DO i18n
            } else {
                _logger.log(Level.SEVERE, "Reconnect failed in pool!!");

                // TO DO i18n 
            }
    }
    private void createConsumer() throws ResourceException {
        try {
                this.consumer.closeConsumer();
                this.consumer.restart();
        } catch (ResourceException re) {
                _logger.log(Level.INFO,"Reconnection failed, Cannot create consumer");
                try {
                        this.pool.stop();
                        // this.consumer.stop();
                }catch (Exception e) {
                        _logger.log(Level.SEVERE, "Reconnect failed while stopping pool");
                }
                _logger.log(Level.INFO,"Stopping the consumer");
                throw re;
        }
    }

    /**
     * Pause all waiting threads. recreate the serversession pool.
     */
    private boolean reestablishPool() {
        try {
            this.pool.stop();
        } catch (JMSException je) {
            _logger.log(Level.SEVERE, "Reconnect failed while stopping pool");
            je.printStackTrace();

            // TO DO log a message 
            return false;
        } catch (Exception e) {
            _logger.log(Level.SEVERE,
                "Reconnect failed while stopping pool " + e);

            // TO DO log a message 
        }

        boolean result = false;

        for (int i = 0; i < attempts; i++) {
            _logger.log(Level.INFO, "Reconnect attempt->" + i);

            try {
                this.pool.initialize();
                this.pool.releaseAllWaitingThreads();
                createConsumer();
                _logger.log(Level.INFO, "Reconnect successful with pool->" + i);
                result = true;

                break;
            } catch (ResourceException re) {
                _logger.log(Level.INFO,
                    "Reconnect attempt failed. Now sleeping for" + interval);

                // TODO. log a message.
                try {
                    Thread.sleep(interval);
                } catch (Exception e) {
                    _logger.info("Thread.sleep exception");
                }
            }
        }

        return result;
    }

    public InboundJmsResourcePool getPool() {
        return this.pool;
    }
}

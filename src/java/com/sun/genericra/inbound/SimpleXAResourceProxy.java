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

import com.sun.genericra.AbstractXAResourceType;
import com.sun.genericra.XAResourceType;
import com.sun.genericra.util.ExceptionUtils;
import com.sun.genericra.util.LogUtils;

import java.util.logging.*;

import jakarta.resource.ResourceException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;


/**
 * <code>XAResource</code> wrapper for Generic JMS Connector. This class
 * intercepts all calls to the actual XAResource object of the physical
 * JMS connection and performs corresponding book-keeping tasks in the
 * ManagedConnection representing the physical connection.
 *
 *  @todo: This should be a dynamic proxy as well!!
 */
public class SimpleXAResourceProxy extends AbstractXAResourceType {
    private static Logger logger;

    private Xid startxid = null;
    boolean endCalled = false;
    boolean torollback = true;
    static {
        logger = LogUtils.getLogger();
    }

    private XAResource xar;

    /**
     * Constructor for XAResourceImpl
     *
     * @param xar
     *            <code>XAResource</code>
     * @param mc
     *            <code>ManagedConnection</code>
     */
    public SimpleXAResourceProxy(XAResource xar) {
        this.xar = xar;
    }

    /**
     * Commit the global transaction specified by xid.
     *
     * @param xid
     *            A global transaction identifier
     * @param onePhase
     *            If true, the resource manager should use a one-phase commit
     *            protocol to commit the work done on behalf of xid.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        debugxid("Commiting Simple inbound transaction ", xid);
         if (xid == null) {
            xid = startxid;
         }
        _getXAResource().commit(xid, onePhase);        
        debugxid("Commited Simple inbound transaction ", xid);
    }

    /**
     * Ends the work performed on behalf of a transaction branch.
     *
     * @param xid
     *            A global transaction identifier that is the same as what was
     *            used previously in the start method.
     * @param flags
     *            One of TMSUCCESS, TMFAIL, or TMSUSPEND
     */
    public void end(Xid xid, int flags) throws XAException {
        debug("Ending simple inbound transaction " + convertFlag(flags));
        debugxid("Ending simple inbound transaction ", xid);
        if (endCalled)
        {
            return;
        }
                
         if (xid == null) {
            xid = startxid;
        }
            endCalled = true;
            _getXAResource().end(xid, flags);        
            debugxid("Ended simple inbound transaction ", xid);
    }

    /**
     * Tell the resource manager to forget about a heuristically completed
     * transaction branch.
     *
     * @param xid
     *            A global transaction identifier
     */
    public void forget(Xid xid) throws XAException {
        _getXAResource().forget(xid);
    }

    /**
     * Obtain the current transaction timeout value set for this
     * <code>XAResource</code> instance.
     *
     * @return the transaction timeout value in seconds
     */
    public int getTransactionTimeout() throws XAException {
        return _getXAResource().getTransactionTimeout();
    }

    /**
     * This method is called to determine if the resource manager instance
     * represented by the target object is the same as the resouce manager
     * instance represented by the parameter xares.
     *
     * @param xares
     *            An <code>XAResource</code> object whose resource manager
     *            instance is to be compared with the resource
     * @return true if it's the same RM instance; otherwise false.
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        XAResource inxa = xares;

        if (xares instanceof XAResourceType) {
            XAResourceType wrapper = (XAResourceType) xares;
            inxa = (XAResource) wrapper.getWrappedObject();

            if (!compare(wrapper)) {
                debug("isSameRM retursn /compare :" + false);

                return false;
            }
        }

        boolean result = _getXAResource().isSameRM(inxa);
        debug("isSameRM retursn /compare :" + result);

        return result;
    }

    /**
     * Ask the resource manager to prepare for a transaction commit of the
     * transaction specified in xid.
     *
     * @param xid
     *            A global transaction identifier
     * @return A value indicating the resource manager's vote on the outcome of
     *         the transaction. The possible values are: XA_RDONLY or XA_OK. If
     *         the resource manager wants to roll back the transaction, it
     *         should do so by raising an appropriate <code>XAException</code>
     *         in the prepare method.
     */
    public int prepare(Xid xid) throws XAException {
        debugxid("Preparing simple inbound transaction with ID ", xid);
         if (xid == null) {
            xid = startxid;
        }
        return _getXAResource().prepare(xid);
    }

    /**
     * Obtain a list of prepared transaction branches from a resource manager.
     *
     * @param flag
     *            One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be
     *            used when no other flags are set in flags.
     * @return The resource manager returns zero or more XIDs for the
     *         transaction branches that are currently in a prepared or
     *         heuristically completed state. If an error occurs during the
     *         operation, the resource manager should throw the appropriate
     *         <code>XAException</code>.
     */
    public Xid[] recover(int flag) throws XAException {
        return _getXAResource().recover(flag);
    }

    /**
     * Inform the resource manager to roll back work done on behalf of a
     * transaction branch
     *
     * @param xid
     *            A global transaction identifier
     */
    public void rollback(Xid xid) throws XAException {        
        debugxid("Rolling back simple inbound transaction with ID ", xid);
        if (xid == null) {
            xid = startxid;
        }
        if (torollback)
        {
            _getXAResource().rollback(xid);
        }        
        debugxid("Rolled back simple inbound transaction with ID ", xid);
    }

    /**
     * Set the current transaction timeout value for this
     * <code>XAResource</code> instance.
     *
     * @param seconds
     *            the transaction timeout value in seconds.
     * @return true if transaction timeout value is set successfully; otherwise
     *         false.
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return _getXAResource().setTransactionTimeout(seconds);
    }

    /**
     * Start work on behalf of a transaction branch specified in xid.
     *
     * @param xid
     *            A global transaction identifier to be associated with the
     *            resource
     * @return flags One of TMNOFLAGS, TMJOIN, or TMRESUME
     */
    public void start(Xid xid, int flags) throws XAException {
        debug("Starting tx..." + convertFlag(flags));
        debugxid("Staring simple inbound transaction ", xid);
        startxid = xid;
        _getXAResource().start(xid, flags);        
        debugxid("Started simple inbound transaction ", xid);
    }

    private XAResource _getXAResource() throws XAException {
        return xar;
    }

    public void setToRollback(boolean rb)
    {
        torollback = rb;
    }
    public Object getWrappedObject() {
        return this.xar;
    }

    String convertFlag(int i) {
        if (i == XAResource.TMJOIN) {
            return "TMJOIN";
        }

        if (i == XAResource.TMNOFLAGS) {
            return "TMNOFLAGS";
        }

        if (i == XAResource.TMSUCCESS) {
            return "TMSUCCESS";
        }

        if (i == XAResource.TMSUSPEND) {
            return "TMSUSPEND";
        }

        if (i == XAResource.TMRESUME) {
            return "TMRESUME";
        }

        return "" + i;
    }
    
    public void startDelayedXA(){
          throw new UnsupportedOperationException();
    }
    public boolean endCalled() {
        // nobody will call this method on this class
         throw new UnsupportedOperationException();
    }
    
    void debug(String s) {
        logger.log(Level.FINEST, "SimpleXAResourceProxy :"  + s);
    }
    
    void debugxid(String s, Xid xid) {
        if (logger.getLevel() == Level.FINEST) {
            logger.log(Level.FINEST, s + printXid(xid));
        }
    }

}

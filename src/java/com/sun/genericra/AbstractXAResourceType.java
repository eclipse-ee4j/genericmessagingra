/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra;

import javax.jms.Connection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.sun.genericra.inbound.InboundXAResourceProxy;


/**
 * AbstractXAResource object used by all XAResource
 * objects in generic jms ra. The class contains the
 * logic to compare the XAResource object, when
 * RMPolicy is set to "One for Physical Connection".
 *
 * @author Binod P.G
 */
public abstract class AbstractXAResourceType implements XAResourceType,
    XAResource {
    private Connection con;
    private String rmPolicy;

    /**
     * Abstract method declaration.
     */
    public abstract Object getWrappedObject();

    public abstract int prepare(Xid xid) throws XAException;
    
     public abstract void commit(Xid xid, boolean onePhase) throws XAException;
     
     public abstract void end(Xid xid, int flags) throws XAException;
     
     public abstract void rollback(Xid xid) throws XAException;
     
     public abstract void start(Xid xid, int flags) throws XAException ;
     
     public abstract void setToRollback(boolean flag);
     
     public abstract boolean endCalled();
     
     public abstract void startDelayedXA();
    /**
     * Set the physical jms connection object associated with
     * this XAResource wrapper
     */
    public void setConnection(Connection con) {
        this.con = con;
    }

    /**
     * Retrieves the physical JMS connection object.
     */
    public Connection getConnection() {
        return this.con;
    }

    /**
     * Set the Resource Manager policy
     */
    public void setRMPolicy(String policy) {
        this.rmPolicy = policy;
    }

    /**
     * Retrieve the RM policy
     */
    public String getRMPolicy() {
        return this.rmPolicy;
    }


    /**
     * Decide whether to override the underlying XAResource's implementation of isSameRM() 
     * so that it returns false.
     * 
     * If the decision can be delegated to the underlying XAResourceImplementations,
     * return true.
     * 
     * If this isSameRM() must return false, return false.
     */
    public boolean compare(XAResourceType other) {
    	
    	// Issue 40
    	// If one of the resources is a InboundXAResourceProxy then isSameRM() must always return false here
    	// This is because InboundXAResourceProxy delays the call to start() on the first resource
    	// If isSameRM() returned true, then start(join) would be called on the second resource
    	// before (start,noflags) is called on the first resource, which would cause an error
    	if (this instanceof InboundXAResourceProxy || other instanceof InboundXAResourceProxy){
    		return false;
    	}
    	
        // If any one of the resources are configured with
        // a policy of "OneForPhysicalConnection", then
        // compare physical connection. Otherwise, return true
        // so that the actual XAResource wrapper can delegate it
        // to the underlying XAResource implementation.
        String rmPerMc = GenericJMSRAProperties.ONE_PER_PHYSICALCONNECTION;
        if (rmPerMc.equalsIgnoreCase(rmPolicy) ||
                rmPerMc.equalsIgnoreCase(other.getRMPolicy())) {
            return other.getConnection().equals(getConnection());
        }
        
        return true;
    }
    
   /**
     * The following is a method that has been added to aid printing of XID.
     * This method of printing the XID may differ for different providers.
     */
    public String printXid(Xid xid) {
        if (xid == null) {
            return "null";
        }
        
    /* The following code can be enabled for TM implementations 
     * that do not have a toString implementation for xids
     */
        
        String hextab = "0123456789ABCDEF";
        StringBuffer data = new StringBuffer(256);
        int i;
        int value;
        try {
        
        if (xid.getFormatId() == -1) {
            return "-1";
        }

        // Add branch qualifier. Convert data string to hex
        for (i = 0; i < xid.getBranchQualifier().length; i++) {
            value = xid.getBranchQualifier()[i] & 0xff;
            data.append(hextab.charAt(value / 16));
            data.append(hextab.charAt(value & 15));
        }

        // Add global transaction id
        for (i = 0; i < xid.getGlobalTransactionId().length; i++) {
            value = xid.getGlobalTransactionId()[i] & 0xff;
            data.append(hextab.charAt(value / 16));
            data.append(hextab.charAt(value & 15));
        }
        }
        catch (Exception e)
        {
            //for some providers this may not work.
            ;
        }

        return new String(data);
        
    }
}

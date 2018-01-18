/*
 * Copyright (c) 2004, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.sync;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.Properties;
/**
 * Provides access to selected functionality of the J2EE transaction manager. Each
 * ManagedConnection will have one instance of this class and will reuse it.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.2 $
 */
public class TxMgr {
    private TxMgrAdapter mTxMgrAdapter;
    private static TransactionManager sUnitTestTxMgr;
    /**
     * Initializes this object to avoid a parameterized constructor
     * 
     * @param p Properties
     */
    public void init(Properties p) {
        getTxMgrAdapter();
    }
    /**
     * Closes a context
     * 
     * @param ctx context to close
     */
    public static void safeClose(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignore) {
                // ignore
            }
        }
    }
    /**
     * Finds out if the current thread is currently associated with a transaction
     * 
     * @return false if not in transaction or if not able to find out
     */
    public boolean isInTransaction() {
        try {
            TxMgrAdapter txm = getTxMgrAdapter();
            if (txm != null) {
                return txm.isInTransaction();
            }
        } catch (Exception ignore) {
            // ignore
        }
        return false;
    }
   /**
     * Registers a synchronization
     * 
     * @param sync synchronization
     */
    public void register(Synchronization sync) {
        try {
            TxMgrAdapter txm = getTxMgrAdapter();
            if (txm != null) {
                txm.register(sync);
            }
        } catch (Exception ignore) {
            // ignoe
        }
    }
    /**
     * Finds the transaction manager
     * 
     * @return transaction manager
     * @throws Exception when a transaction manager could not be located
     */
    public TransactionManager getTransactionManager() throws Exception {
        TxMgrAdapter txm = getTxMgrAdapter();
        if (txm != null) {
            return txm.getTransactionManager();
        } else {
            throw new Exception("Could not find transaction manager adapter");
        }
    }
    /**
     * Returns a suitable transaction manager adapter
     * 
     * @return TxMgrAdapter
     */
    private TxMgrAdapter getTxMgrAdapter() {
        if (mTxMgrAdapter != null) {
            return mTxMgrAdapter;
        }

        // Try all known locators
        if (mTxMgrAdapter == null) {
            // Unit testing
            mTxMgrAdapter = new UnitTest().init();
        }
        if (mTxMgrAdapter == null) {
            mTxMgrAdapter = new SJSAS().init();
        }
        if (mTxMgrAdapter == null) {
            // WebLogic
            mTxMgrAdapter = new GlobalJNDI("javax.transaction.TransactionManager").init();
        }
        if (mTxMgrAdapter == null) {
            // WAS6 (note, do this before looking up java:/ as that causes a warning in the WAS log)
            mTxMgrAdapter = new WAS6a().init();
        }
        if (mTxMgrAdapter == null) {
            // GlassFish v3
            mTxMgrAdapter = new LocalJNDI("java:appserver/TransactionManager").init();
        }
        if (mTxMgrAdapter == null) {
            // JBoss
            mTxMgrAdapter = new LocalJNDI("java:/TransactionManager").init();
        }
        if (mTxMgrAdapter == null) {
            // WL9 alternative
            mTxMgrAdapter = new WL9().init();
        }
        if (mTxMgrAdapter == null) {
            // Resin 3.x 
            mTxMgrAdapter = new LocalJNDI("java:comp/TransactionManager").init();
        }
        if (mTxMgrAdapter == null) {
            // Most others (Resin 2.x, Oracle OC4J (Orion), JOnAS (JOTM), BEA WebLogic)
            mTxMgrAdapter = new LocalJNDI("java:comp/UserTransaction").init();
        }
        return mTxMgrAdapter;
    }
    /**
     * Exposes the required functionality from a javax.transaction.TransactionManager
     * or in the case of IBM, something similar
     * 
     * @author fkieviet
     */
    private abstract class TxMgrAdapter {
        /**
         * Initializes this adapter
         * 
         * @return this if the initialization was successful; null if not
         */
        public abstract TxMgrAdapter init();
        /**
         * @return the transaction manager
         * @throws Exception on lookup failure
         */
        public abstract TransactionManager getTransactionManager() throws Exception;
        /**
         * @return transactionmanager
         * @throws Exception propagated
         */
        public Transaction getTransaction() throws Exception {
            return getTransactionManager().getTransaction();
        }
        /**
         * Registers a synchronization object
         * 
         * @param sync Synchronization
         * @throws Exception propagated
         */
        public final void register(Synchronization sync) throws Exception {
            getTransaction().registerSynchronization(sync);
        }
        /**
         * @return true if in a transaction
         * @throws Exception propagated
         */
        public final boolean isInTransaction() throws Exception {
            return getTransaction() != null;
        }
    }
    /**
     * For SJSAS 8, 8.1, 8.2, 9 and RTS
     * 
     * @author fkieviet
     */
    private class SJSAS extends TxMgrAdapter {
        private TransactionManager mTransactionManager;
        public TxMgrAdapter init() {
            try {
                Class c1 = Class.forName("com.sun.enterprise.Switch");
                Method m1 = c1.getMethod("getSwitch", new Class[0]);
                Object theswitch = m1.invoke(null, new Object[0]);
                Method m2 = c1.getMethod("getTransactionManager", new Class[0]);
                Object ret = m2.invoke(theswitch, new Object[0]);
                mTransactionManager = (TransactionManager) ret;
                return this;
            } catch (Exception ignore) {
                // ignore
            }
            return null;
        }
        /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#getTransactionManager()
         */
        public TransactionManager getTransactionManager() {
            return mTransactionManager;
        }
    }
    /**
     * For WAS6, based on an article in OnJava at http://www.onjava.com/lpt/a/6055
     * 
     * @author fkieviet
     */
    private class WAS6a extends TxMgrAdapter {
        private TransactionManager mTransactionManager;
        public TxMgrAdapter init() {
            try {
                Class c1 = Class.forName("com.ibm.ws.Transaction.TransactionManagerFactory");
                Method m1 = c1.getMethod("getTransactionManager", new Class[0]);
                Object ret = m1.invoke(null, new Object[0]);
                mTransactionManager = (TransactionManager) ret;
                return this;
            } catch (Exception ex) {
                // ignore
            }
            return null;
        }
        /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#getTransactionManager()
         */
        public TransactionManager getTransactionManager() {
            return mTransactionManager;
        }
    }
    /**
     * For WL9, based on an article in OnJava at http://www.onjava.com/lpt/a/6055
     * 
     * @author fkieviet
     */
    private class WL9 extends TxMgrAdapter {
        private TransactionManager mTransactionManager;
        public TxMgrAdapter init() {
            try {
               Class c1 = Class.forName("weblogic.transaction.TransactionHelper");
                Method m1 = c1.getMethod("getTransactionManager", new Class[0]);
                Object ret = m1.invoke(null, new Object[0]);
                mTransactionManager = (TransactionManager) ret;
                return this;
            } catch (Exception ex) {
                // ignore
            }
            return null;
        }
       /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#getTransactionManager()
         */
        public TransactionManager getTransactionManager() {
            return mTransactionManager;
        }
    }
    /**
     * For WL9, based on an article in OnJava at http://www.onjava.com/lpt/a/6055
     * 
     * @author fkieviet
     */
    private class UnitTest extends TxMgrAdapter {
        /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#init()
         */
        public TxMgrAdapter init() {
            return sUnitTestTxMgr != null ? this : null;
        }
        /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#getTransactionManager()
         */
        public TransactionManager getTransactionManager() {
            return sUnitTestTxMgr;
        }
    }
    /**
     * see http://publib.boulder.ibm.com/infocenter/wasinfo/v5r1/index.jsp?topic=/com
     * .ibm.wasee.doc/info/ee/javadoc/ee/com/ibm/websphere/jtaextensions/ExtendedJTATransaction.html
     * 
     * NOTE: UNDER DEVELOPMENT; DO NOT USE
     * ACCORDING TO IBM DOCUMENTATION, THE SYNCHRONIZATION IS GLOBAL AND NEEDS TO
     * BE UNREGISTERED; THE SYNCHRONIZATION NEEDS TO CHECK THE XID. USE A STATIC OBJECT
     * SO THAT ONLY ONE SYNCHRONIZATION CAN BE USED? TBD!
     * 
     * Note: see http://www.onjava.com/lpt/a/6055 for a better approach
     * 
     * @author fkieviet
     */
//    private class WAS6 extends TxMgrAdapter {
//        private Method mGetGlobalIdMethod;
//        private Method mRegisterMethod;
//        private Class mSynchronizationCallbackClass;
//        
//        private Object getTxMgr() throws Exception {
//            Context ctx = new InitialContext();
//            Object txmgr = ctx.lookup("java:comp/websphere/ExtendedJTATransaction");
//            return txmgr;
//        }
//        
//        public TxMgrAdapter init() {
//            try {
//                Object txmgr = getTxMgr();
//                mGetGlobalIdMethod = txmgr.getClass().getMethod("getGlobalId", new Class[0]);
//                mSynchronizationCallbackClass = Class.forName(
//                    "com.ibm.websphere.jtaextensions.SynchronizationCallback", false, 
//                    txmgr.getClass().getClassLoader());
//                mRegisterMethod = txmgr.getClass().getMethod("registerSynchronizationCallback",
//                    new Class[] {mSynchronizationCallbackClass});
//            } catch (Exception ignore) {
//                // ignore
//            }
//            
//            return this;
//        }
//
//        public boolean isInTransaction() throws Exception {
//            Object txmgr = getTxMgr();
//            Object id = mGetGlobalIdMethod.invoke(txmgr, new Object[] {});
//            return id != null;
//        }
//        
//        public void register(final Synchronization sync) throws Exception {
//            final InvocationHandler ih = new InvocationHandler() {
//                public Object invoke(Object proxy, Method method, Object[] args)
//                    throws Throwable {
//                    if ("afterCompletion".equals(method.getName())) {
//                        int status = args[2].equals(Boolean.TRUE) ? Status.STATUS_COMMITTED
//                            : Status.STATUS_UNKNOWN;
//                        sync.afterCompletion(status);
//                    } else if ("beforeCompletion".equals(method.getName())) {
//                        sync.beforeCompletion();
//                    } else if ("toString".equals(method.getName())) {
//                        return sync.toString();
//                    }
//                    return null;
//                }
//            };
//
//            final Object synchronizationCallback = Proxy.newProxyInstance(
//                getClass().getClassLoader(), new Class[] {mSynchronizationCallbackClass}, ih);
//
//            Object txmgr = getTxMgr();
//            mRegisterMethod.invoke(txmgr, new Object[] {synchronizationCallback});
//        }
//    }
    /**
     * Looks up once in global JNDI and uses this cached object
     * 
     * @author fkieviet
     */
    private class GlobalJNDI extends TxMgrAdapter {
        private TransactionManager mTransactionManager;
        private String mName;
        public GlobalJNDI(String name) {
            mName = name;
        }
        public TxMgrAdapter init() {
            Context ctx = null;
            try {
                ctx = new InitialContext();
                mTransactionManager = (TransactionManager) ctx.lookup(mName);
                return this;
            } catch (Exception ignore) {
                // ignore
            } finally {
                safeClose(ctx);
            }
            return null;
        }
        /**
         * @see com.stc.jmsjca.core.TxMgr.TxMgrAdapter#getTransactionManager()
         */
        public TransactionManager getTransactionManager() {
            return mTransactionManager;
        }
    }
    /**
     * Looks up every time in JNDI (no caching)
     * 
     * @author fkieviet
     */
    private class LocalJNDI extends TxMgrAdapter {
        private String mName;

        public LocalJNDI(String name) {
            mName = name;
        }
        public TxMgrAdapter init() {
            Context ctx = null;
            try {
                ctx = new InitialContext();
                TransactionManager mgr = (TransactionManager) ctx.lookup(mName);
                return mgr == null ? null : this;
            } catch (Exception ignore) {
                // ignore
            } finally {
                safeClose(ctx);
            }
            return null;
        }
        public TransactionManager getTransactionManager() throws Exception {
            Context ctx = null;
            try {
                ctx = new InitialContext();
                return (TransactionManager) ctx.lookup(mName);
            } catch (Exception e) {
                throw e;
            } finally {
                safeClose(ctx);
            }
        }
    }
    /**
     * For testing only! Sets an arbitrary global transaction manager
     * 
     * @param txmgr TransactionManager
     */
    public static void setUnitTestTxMgr(TransactionManager txmgr) {
        sUnitTestTxMgr = txmgr;
    }
    /**
     * For testing only! Gets the global transaction manager if set
     * 
     * @return TransactionManager
     */
    public static TransactionManager getUnitTestTxMgr() {
        return sUnitTestTxMgr;
    }
}

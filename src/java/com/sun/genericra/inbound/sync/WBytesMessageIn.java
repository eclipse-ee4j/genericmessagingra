/*
 * Copyright (c) 2003, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.genericra.inbound.sync;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;

/**
 * See WMessage
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1 $
 */
public class WBytesMessageIn extends WMessageIn implements BytesMessage {
    private BytesMessage mDelegate;
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     * @param ackHandler callback to call when ack() or recover() is called
     * @param ibatch index of this message in a batch; -1 for non-batched
     */
    public WBytesMessageIn(BytesMessage delegate, AckHandler ackHandler, int ibatch) {
        super(delegate, ackHandler, ibatch);
        mDelegate = delegate;
    }

    /**
     * @see jakarta.jms.BytesMessage#readBoolean()
     */
    public boolean readBoolean() throws JMSException {
        return mDelegate.readBoolean();
    }

    /**
     * @see jakarta.jms.BytesMessage#readByte()
     */
    public byte readByte() throws JMSException {
        return mDelegate.readByte();
    }

    /**
     * @see jakarta.jms.BytesMessage#readBytes(byte[], int)
     */
    public int readBytes(byte[] arg0, int arg1) throws JMSException {
        return mDelegate.readBytes(arg0, arg1);
    }

    /**
     * @see jakarta.jms.BytesMessage#readBytes(byte[])
     */
    public int readBytes(byte[] arg0) throws JMSException {
        return mDelegate.readBytes(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#readChar()
     */
    public char readChar() throws JMSException {
        return mDelegate.readChar();
    }

    /**
     * @see jakarta.jms.BytesMessage#readDouble()
     */
    public double readDouble() throws JMSException {
        return mDelegate.readDouble();
    }

    /**
     * @see jakarta.jms.BytesMessage#readFloat()
     */
    public float readFloat() throws JMSException {
        return mDelegate.readFloat();
    }

    /**
     * @see jakarta.jms.BytesMessage#readInt()
     */
    public int readInt() throws JMSException {
        return mDelegate.readInt();
    }

    /**
     * @see jakarta.jms.BytesMessage#readLong()
     */
    public long readLong() throws JMSException {
        return mDelegate.readLong();
    }

    /**
     * @see jakarta.jms.BytesMessage#readShort()
     */
    public short readShort() throws JMSException {
        return mDelegate.readShort();
    }

    /**
     * @see jakarta.jms.BytesMessage#readUnsignedByte()
     */
    public int readUnsignedByte() throws JMSException {
        return mDelegate.readUnsignedByte();
    }

    /**
     * @see jakarta.jms.BytesMessage#readUnsignedShort()
     */
    public int readUnsignedShort() throws JMSException {
        return mDelegate.readUnsignedShort();
    }

    /**
     * @see jakarta.jms.BytesMessage#readUTF()
     */
    public String readUTF() throws JMSException {
        return mDelegate.readUTF();
    }

    /**
     * @see jakarta.jms.BytesMessage#writeBoolean(boolean)
     */
    public void writeBoolean(boolean arg0) throws JMSException {
        mDelegate.writeBoolean(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeByte(byte)
     */
    public void writeByte(byte arg0) throws JMSException {
        mDelegate.writeByte(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeBytes(byte[], int, int)
     */
    public void writeBytes(byte[] arg0, int arg1, int arg2) throws JMSException {
        mDelegate.writeBytes(arg0, arg1, arg2);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeBytes(byte[])
     */
    public void writeBytes(byte[] arg0) throws JMSException {
        mDelegate.writeBytes(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeChar(char)
     */
    public void writeChar(char arg0) throws JMSException {
        mDelegate.writeChar(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeDouble(double)
     */
    public void writeDouble(double arg0) throws JMSException {
        mDelegate.writeDouble(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeFloat(float)
     */
    public void writeFloat(float arg0) throws JMSException {
        mDelegate.writeFloat(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeInt(int)
     */
    public void writeInt(int arg0) throws JMSException {
        mDelegate.writeInt(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeLong(long)
     */
    public void writeLong(long arg0) throws JMSException {
        mDelegate.writeLong(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeObject(java.lang.Object)
     */
    public void writeObject(Object arg0) throws JMSException {
        mDelegate.writeObject(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeShort(short)
     */
    public void writeShort(short arg0) throws JMSException {
        mDelegate.writeShort(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#writeUTF(java.lang.String)
     */
    public void writeUTF(String arg0) throws JMSException {
        mDelegate.writeUTF(arg0);
    }

    /**
     * @see jakarta.jms.BytesMessage#getBodyLength()
     */
    public long getBodyLength() throws JMSException {
        return mDelegate.getBodyLength();
    }

    /**
     * @see jakarta.jms.BytesMessage#reset()
     */
    public void reset() throws JMSException {
        mDelegate.reset();
    }

}

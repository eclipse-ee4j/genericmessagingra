/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.common.ejb;

public class GenericMDB {
	
    // set this to true if you want to monitor number of MDBs in use at a time
    // typically this will increase if onMessage() performs a sleep
    protected boolean monitorNoOfBeansInUse=false;

    // set this to true to log the time taken to process each each batch of messages (across all instances of this MDB)
    // batchsize is set below
    protected boolean reportThroughput=true;
	
	// every time batchsize messages are processed, the time taken is reported
    private static int batchsize=100;
	
    private static int beansInUseCount=0;
    protected synchronized static void incrementBeansInUseCount(){
        beansInUseCount++;
        System.out.println("No of beans in use now: " + beansInUseCount);
    }

    protected synchronized static void decrementBeansInUseCount(){
        beansInUseCount--;
        System.out.println("No of beans in use now: " + beansInUseCount);
    }

    private static long messageCount=0;
    private static long startTime;
    protected static void updateMessageCount(){

        long thisMessageCount = incrementMessageCount();
        if (thisMessageCount==1){
            startTime =System.currentTimeMillis();
        }
        if ((thisMessageCount % batchsize)==0){
            float timeTakenMillis = System.currentTimeMillis()-startTime;
            float timeTakenSecs=timeTakenMillis/1000;
            float rate = batchsize/timeTakenSecs;
            System.out.println("Total messages is "+thisMessageCount+" messages, time for last "+batchsize+" was "+timeTakenSecs+" sec, rate was "+rate+" msgs/sec");
            startTime =System.currentTimeMillis();
        }
    }

    private synchronized static long incrementMessageCount(){
        messageCount++;
        return messageCount;
    }

}

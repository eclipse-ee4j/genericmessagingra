/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.common.junit;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;

public class GenericTestCase extends TestCase{
	
	/**
	 * Copy test.out to standard output, searching for the words "PASS" or "FAIL". 
	 * If PASS found, the test is considered to have passed and the method returns normally
	 * If PASS not found, or if FAIL found, the test fails.
	 * 
	 * @throws IOException
	 */
	protected void printAndCheckTestOutput() throws FileNotFoundException, IOException {
		String testDirectoryProperty = "testDirectory";
		String testDirectoryName =System.getProperty(testDirectoryProperty);
		if (testDirectoryName==null){
			fail("Test config error: system property "+testDirectoryProperty+" not set");
		}
		
		boolean failed = true;
		boolean failFound=false;
		boolean passFound=false;
		String lineOfInterest=null;

		BufferedReader in = new BufferedReader(new FileReader(testDirectoryName+"/test.out"));
		try {
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				if (line.indexOf("PASS") != -1) {
					System.out.println("The word PASS was detected: the test must have passed");
					passFound=true;
					lineOfInterest=line;
					failed = false;
				}
				if (line.indexOf("FAIL") != -1) {
					System.out.println("The word FAIL was detected: the test must have failed");
					failFound=true;
					lineOfInterest=line;
					failed = true;
				}
			}
		} finally {
			in.close();
		}
		
		if (passFound) {
			System.out.println("The word PASS was found in "+lineOfInterest);
		}
		if (failFound) {
			System.out.println("The word FAIL was found in "+lineOfInterest);
		}
		
		if (failed) {
			if (failFound){
				fail("The word FAIL was found in test.out");
			} else {
				fail("The word PASS was not found in test.out");
			}
		}
	}

}

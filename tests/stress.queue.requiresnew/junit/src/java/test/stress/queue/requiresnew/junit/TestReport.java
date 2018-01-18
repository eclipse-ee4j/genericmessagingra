/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package test.stress.queue.requiresnew.junit;

import java.io.IOException;

import test.common.junit.GenericTestCase;

public class TestReport extends GenericTestCase {


	public void testReport() throws IOException {
		printAndCheckTestOutput(); 
	}

}

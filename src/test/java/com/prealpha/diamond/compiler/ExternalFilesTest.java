/*
 * ExternalFilesTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.BasicMachineTest;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/*
 * TODO: right now, testing the values is implementation-sensitive
 */
public final class ExternalFilesTest extends BasicMachineTest {
    @Test
    public void testHelloHeaplessWorld() throws Exception {
        testFileInPackage("HelloHeaplessWorld.dmd");
        assertEquals(42, getReg(0));
        assertEquals(0, getReg(1));
    }

    @Test
    public void testLoopArithmetic() throws Exception {
        testFileInPackage("LoopArithmetic.dmd");
        assertEquals(100, getMem()[0xfffe]);
    }

    @Test
    public void testEulerProblem1() throws Exception {
        testFileInPackage("EulerProblem1.dmd");
        // the answer is 233168
        assertEquals(0x8ed0, getMem()[0xfffd]);
        assertEquals(0x0003, getMem()[0xfffe]);
    }

    private void testFileInPackage(String fileName) throws Exception {
        File file = new File(ExternalFilesTest.class.getResource(fileName).getFile());
        test(Compiler.compile(file));
    }
}

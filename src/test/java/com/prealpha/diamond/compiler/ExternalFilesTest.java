/*
 * ExternalFilesTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public final class ExternalFilesTest extends PipelineTest {
    @Test
    public void testHelloHeaplessWorld() throws Exception {
        testFileInPackage("HelloHeaplessWorld.dmd");
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testLoopArithmetic() throws Exception {
        testFileInPackage("LoopArithmetic.dmd");
        assertEquals(100, (char) getPipeline().remove());
    }

    @Test
    public void testEulerProblem19() throws Exception {
        testFileInPackage("EulerProblem19.dmd");
        assertEquals(171, (char) getPipeline().remove());
    }

    @Test
    public void testArithmeticObject() throws Exception {
        testFileInPackage("ArithmeticObject.dmd");
        assertEquals(10, (char) getPipeline().remove());
    }

    private void testFileInPackage(String fileName) throws Exception {
        File file = new File(ExternalFilesTest.class.getResource(fileName).getFile());
        testWithPipeline(file);
    }
}

/*
 * LocalDeclarationTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import org.junit.Test;

import static org.junit.Assert.*;

public final class LocalDeclarationTest extends PipelineTest {
    @Test
    public void testUnassignedDeclaration() throws Exception {
        String diamond = "void main() { int testVar; testVar = 5; pipeline(testVar); }";
        testWithPipeline(diamond);
        assertEquals(5, (char) getPipeline().remove());
    }

    @Test
    public void testAssignedDeclaration() throws Exception {
        String diamond = "void main() { int testVar = 5; pipeline(testVar); }";
        testWithPipeline(diamond);
        assertEquals(5, (char) getPipeline().remove());
    }
}

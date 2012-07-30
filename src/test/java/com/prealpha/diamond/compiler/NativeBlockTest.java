/*
 * NativeBlockTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import org.junit.Test;

import static org.junit.Assert.*;

public final class NativeBlockTest extends PipelineTest {
    @Test
    public void testNativeMainMethod() throws Exception {
        String diamond = "native void main() { `SET B 0x5555` }";
        test(Compiler.getStrictCompiler().compile(diamond));
        assertEquals(0x5555, getReg(1));
    }

    @Test
    public void testNativeMethod() throws Exception {
        String diamond = "void main() { pipeline(testMethod()); } native int testMethod() { `SET A 0x5555` }";
        testWithPipeline(diamond);
        assertEquals(0x5555, (char) getPipeline().remove());
    }

    @Test(expected = SemanticException.class)
    public void testNotInNativeFunction() throws Exception {
        String diamond = "void main() { `SET B 0x5555` }";
        test(Compiler.getStrictCompiler().compile(diamond));
    }
}

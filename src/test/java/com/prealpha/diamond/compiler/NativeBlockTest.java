/*
 * NativeBlockTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.BasicMachineTest;
import org.junit.Test;

import static org.junit.Assert.*;

public final class NativeBlockTest extends BasicMachineTest {
    @Test
    public void testNativeMainMethod() throws Exception {
        String diamond = "native void main() { `SET B 0x5555` }";
        test(Compiler.getStrictCompiler().compile(diamond));
        assertEquals(0x5555, getReg(1));
    }

    @Test
    public void testNativeMethod() throws Exception {
        String diamond = "void main() { int foo = testMethod(); } native int testMethod() { `SET A 0x5555` }";
        test(Compiler.getStrictCompiler().compile(diamond));
        assertEquals(0x5555, getReg(0));
        assertEquals(0x5555, getMem()[0xfffe]);
    }

    @Test(expected = SemanticException.class)
    public void testNotInNativeFunction() throws Exception {
        String diamond = "void main() { `SET B 0x5555` }";
        test(Compiler.getStrictCompiler().compile(diamond));
    }
}

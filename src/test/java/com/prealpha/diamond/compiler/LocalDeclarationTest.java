/*
 * LocalDeclarationTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.MachineTest;
import org.junit.Test;

import static org.junit.Assert.*;

public final class LocalDeclarationTest extends MachineTest {
    @Test
    public void testUnassignedDeclaration() throws Exception {
        String diamond = "void main() { int testVar; testVar = 5; }";
        test(Compiler.getStrictCompiler().compile(diamond));
        assertEquals(5, getMem()[0xfffe]);
    }

    @Test
    public void testAssignedDeclaration() throws Exception {
        String diamond = "void main() { int testVar = 5; }";
        test(Compiler.getStrictCompiler().compile(diamond));
        assertEquals(5, getMem()[0xfffe]);
    }
}

/*
 * ScopingTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.BasicMachineTest;
import org.junit.Test;

public final class ScopingTest extends BasicMachineTest {
    @Test(expected = SemanticException.class)
    public void testNoDuplicateLocals() throws Exception {
        String diamond = "void main() { int local1 = 0; { int local1 = 0; } }";
        test(Compiler.getStrictCompiler().compile(diamond));
    }
}

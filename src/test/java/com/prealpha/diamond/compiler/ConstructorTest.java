/*
 * ConstructorTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.MachineTest;
import org.junit.Test;

public final class ConstructorTest extends MachineTest {
    @Test
    public void testConstructorWithParameter() throws Exception {
        String diamond = "class TestClass { static TestClass new(int testVar) { return this; } } void main() { TestClass::new(0); }";
        test(Compiler.getStrictCompiler().compile(diamond));
    }
}

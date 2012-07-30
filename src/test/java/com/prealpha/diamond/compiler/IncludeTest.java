/*
 * IncludeTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.MachineTest;
import org.junit.Test;

public final class IncludeTest extends MachineTest {
    @Test
    public void testDuplicateInclude() throws Exception {
        String diamond = "include Device; include Device; void main() { Device::new(0U); }";
        test(Compiler.getStrictCompiler().compile(diamond));
    }
}

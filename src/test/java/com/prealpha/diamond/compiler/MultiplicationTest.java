/*
 * MultiplicationTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.dcputil.emulator.testing.BasicMachineTest;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public final class MultiplicationTest extends BasicMachineTest {
    private static final long[] SIGNED_SHORT_VALUES = {0, 42, 32767, -32768, -42};

    private static final long[] UNSIGNED_SHORT_VALUES = {0, 42, 32767, 32768, 50000, 65535};

    @Test
    public void testSignedShortSignedShort() throws Exception {
        doShortTest(SIGNED_SHORT_VALUES, SIGNED_SHORT_VALUES);
    }

    @Test
    public void testUnsignedShortSignedShort() throws Exception {
        doShortTest(UNSIGNED_SHORT_VALUES, SIGNED_SHORT_VALUES);
    }

    @Test
    public void testSignedShortUnsignedShort() throws Exception {
        doShortTest(SIGNED_SHORT_VALUES, UNSIGNED_SHORT_VALUES);
    }

    @Test
    public void testUnsignedShortUnsignedShort() throws Exception {
        doShortTest(UNSIGNED_SHORT_VALUES, UNSIGNED_SHORT_VALUES);
    }

    private void doShortTest(long[] leftValues, long[] rightValues) throws Exception {
        for (long leftValue : leftValues) {
            for (long rightValue : rightValues) {
                String expression = String.format("%d * %d", leftValue, rightValue);
                testNumericExpression(expression, (short) (leftValue * rightValue));
            }
        }
    }

    private void testNumericExpression(String expression, long result) throws Exception {
        String message = String.format("%s = %d", expression, result);
        String diamond = String.format("void main() { long result = %s; }", expression);
        test(Compiler.compile(new StringReader(diamond)));
        assertEquals(message, result & 0xffff, getMem()[0xfffb]);
        assertEquals(message, result >>> 16 & 0xffff, getMem()[0xfffc]);
        assertEquals(message, result >>> 32 & 0xffff, getMem()[0xfffd]);
        assertEquals(message, result >>> 48 & 0xffff, getMem()[0xfffe]);
    }
}

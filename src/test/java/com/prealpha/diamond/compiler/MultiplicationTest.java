/*
 * MultiplicationTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.prealpha.dcputil.emulator.testing.BasicMachineTest;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public final class MultiplicationTest extends BasicMachineTest {
    private static final long[] SIGNED_SHORT_VALUES = {0x0000, 0x0064, 0x7fff, (short)0x8000, (short)0xffef,
            (short)0xffff};

    private static final long[] UNSIGNED_SHORT_VALUES = {0x0000, 0x0064, 0x7fff, 0x8000, 0xffef, 0xffff};

    private static final long[] SIGNED_INT_VALUES = {0x00000000, 0x00000064, 0x00007fff, 0x00008000, 0x0000ffef,
            0x0000ffff, 0x00640000, 0x00647fff, 0x00648000, 0x0064ffef, 0x0064ffff, 0x7fff0000, 0x7fff0064, 0x7fff7fff,
            0x7fff8000, 0x7fffffef, 0x7fffffff, 0x80000000, 0x80000064, 0x80007fff, 0x80008000, 0x8000ffef, 0x8000ffff,
            0xffef0000, 0xffef0064, 0xffef7fff, 0xffef8000, 0xffefffef, 0xffefffff, 0xffff0000, 0xffff0064, 0xffff7fff,
            0xffff8000, 0xffffffef, 0xffffffff};

    private static final long[] UNSIGNED_INT_VALUES = {0x00000000, 0x00000064, 0x00007fff, 0x00008000, 0x0000ffef,
            0x0000ffff, 0x00640000, 0x00647fff, 0x00648000, 0x0064ffef, 0x0064ffff, 0x7fff0000, 0x7fff0064, 0x7fff7fff,
            0x7fff8000, 0x7fffffef, 0x7fffffff, 0x80000000L, 0x80000064L, 0x80007fffL, 0x80008000L, 0x8000ffefL,
            0x8000ffffL, 0xffef0000L, 0xffef0064L, 0xffef7fffL, 0xffef8000L, 0xffefffefL, 0xffefffffL, 0xffff0000L,
            0xffff0064L, 0xffff7fffL, 0xffff8000L, 0xffffffefL, 0xffffffffL};
    
    private static final Multimap<IntegralTypeToken, Long> TEST_VALUES = HashMultimap.create();
    
    static {
        for (long value : SIGNED_SHORT_VALUES) {
            TEST_VALUES.put(IntegralTypeToken.SIGNED_SHORT, value);
        }
        for (long value : UNSIGNED_SHORT_VALUES) {
            TEST_VALUES.put(IntegralTypeToken.UNSIGNED_SHORT, value);
        }
        for (long value : SIGNED_INT_VALUES) {
            TEST_VALUES.put(IntegralTypeToken.SIGNED_INT, value);
        }
        for (long value : UNSIGNED_INT_VALUES) {
            TEST_VALUES.put(IntegralTypeToken.UNSIGNED_INT, value);
        }
    }

    @Test
    public void testSignedShortSignedShort() throws Exception {
        doShortTest(IntegralTypeToken.SIGNED_SHORT, IntegralTypeToken.SIGNED_SHORT);
    }

    @Test
    public void testSignedShortUnsignedShort() throws Exception {
        doIntTest(IntegralTypeToken.SIGNED_SHORT, IntegralTypeToken.UNSIGNED_SHORT, 0xffffffff);
    }

    @Test
    public void testSignedShortSignedInt() throws Exception {
        doIntTest(IntegralTypeToken.SIGNED_SHORT, IntegralTypeToken.SIGNED_INT, 0xffffffff);
    }

    @Test
    public void testSignedShortUnsignedInt() throws Exception {
        doLongTest(IntegralTypeToken.SIGNED_SHORT, IntegralTypeToken.UNSIGNED_INT, 0xffffffff);
    }

    @Test
    public void testUnsignedShortSignedShort() throws Exception {
        doIntTest(IntegralTypeToken.UNSIGNED_SHORT, IntegralTypeToken.SIGNED_SHORT, 0xffffffff);
    }

    @Test
    public void testUnsignedShortUnsignedShort() throws Exception {
        doIntTest(IntegralTypeToken.UNSIGNED_SHORT, IntegralTypeToken.UNSIGNED_SHORT, 0x0000ffff);
    }

    @Test
    public void testUnsignedShortSignedInt() throws Exception {
        doIntTest(IntegralTypeToken.UNSIGNED_SHORT, IntegralTypeToken.SIGNED_INT, 0xffffffff);
    }

    @Test
    public void testUnsignedShortUnsignedInt() throws Exception {
        doLongTest(IntegralTypeToken.UNSIGNED_SHORT, IntegralTypeToken.UNSIGNED_INT, 0x00000000ffffffffL);
    }

    @Test
    public void testSignedIntSignedShort() throws Exception {
        doIntTest(IntegralTypeToken.SIGNED_INT, IntegralTypeToken.SIGNED_SHORT, 0xffffffff);
    }

    @Test
    public void testSignedIntUnsignedShort() throws Exception {
        doIntTest(IntegralTypeToken.SIGNED_INT, IntegralTypeToken.UNSIGNED_SHORT, 0xffffffff);
    }

    @Test
    public void testSignedIntSignedInt() throws Exception {
        doIntTest(IntegralTypeToken.SIGNED_INT, IntegralTypeToken.SIGNED_INT, 0xffffffff);
    }

    @Test
    public void testSignedIntUnsignedInt() throws Exception {
        doLongTest(IntegralTypeToken.SIGNED_INT, IntegralTypeToken.UNSIGNED_INT, 0xffffffffffffffffL);
    }

    @Test
    public void testUnsignedIntSignedShort() throws Exception {
        doLongTest(IntegralTypeToken.UNSIGNED_INT, IntegralTypeToken.SIGNED_SHORT, 0xffffffffffffffffL);
    }

    @Test
    public void testUnsignedIntUnsignedShort() throws Exception {
        doLongTest(IntegralTypeToken.UNSIGNED_INT, IntegralTypeToken.UNSIGNED_SHORT, 0x00000000ffffffffL);
    }

    @Test
    public void testUnsignedIntSignedInt() throws Exception {
        doLongTest(IntegralTypeToken.UNSIGNED_INT, IntegralTypeToken.SIGNED_INT, 0xffffffffffffffffL);
    }

    @Test
    public void testUnsignedIntUnsignedInt() throws Exception {
        doLongTest(IntegralTypeToken.UNSIGNED_INT, IntegralTypeToken.UNSIGNED_INT, 0x00000000ffffffffL);
    }

    private void doShortTest(IntegralTypeToken left, IntegralTypeToken right) throws Exception {
        for (long leftValue : TEST_VALUES.get(left)) {
            for (long rightValue : TEST_VALUES.get(right)) {
                String expression = String.format("%s left = %s::cast(%d); %s right = %s::cast(%d); result = left * right;",
                        left, left, leftValue, right, right, rightValue);
                testNumericExpression(expression, (short) (leftValue * rightValue));
            }
        }
    }

    private void doIntTest(IntegralTypeToken left, IntegralTypeToken right, int bitmask) throws Exception {
        for (long leftValue : TEST_VALUES.get(left)) {
            for (long rightValue : TEST_VALUES.get(right)) {
                String expression = String.format("%s left = %s::cast(%d); %s right = %s::cast(%d); result = left * right;",
                        left, left, leftValue, right, right, rightValue);
                testNumericExpression(expression, (int) (leftValue * rightValue) & bitmask);
            }
        }
    }

    private void doLongTest(IntegralTypeToken left, IntegralTypeToken right, long bitmask) throws Exception {
        for (long leftValue : TEST_VALUES.get(left)) {
            for (long rightValue : TEST_VALUES.get(right)) {
                String expression = String.format("%s left = %s::cast(%d); %s right = %s::cast(%d); result = left * right;",
                        left, left, leftValue, right, right, rightValue);
                testNumericExpression(expression, leftValue * rightValue & bitmask);
            }
        }
    }

    private void testNumericExpression(String expression, long result) throws Exception {
        String message = String.format("%s [%d]", expression, result);
        String diamond = String.format("void main() { long result = 562958543486978; %s }", expression);
        test(Compiler.compile(new StringReader(diamond)));
        assertEquals(message, result & 0xffff, getMem()[0xfffb]);
        assertEquals(message, result >>> 16 & 0xffff, getMem()[0xfffc]);
        assertEquals(message, result >>> 32 & 0xffff, getMem()[0xfffd]);
        assertEquals(message, result >>> 48 & 0xffff, getMem()[0xfffe]);
    }
}

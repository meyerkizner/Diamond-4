/*
 * FieldAccessTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import org.junit.Test;

import static org.junit.Assert.*;

public final class FieldAccessTest extends PipelineTest {
    @Test
    public void testIdentifierExpressionFieldRead() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; return this; } } " +
                "void main() { TestClass testClass = TestClass::new(); pipeline(testClass.testVar); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testConstructorExpressionFieldRead() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; return this; } } " +
                "void main() { pipeline(TestClass::new().testVar); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testExternalFieldWrite() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { return this; } } " +
                "void main() { TestClass testClass = TestClass::new(); testClass.testVar = 42; pipeline(testClass.testVar); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testInternalFieldRead() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; pipeline(testVar); return this; } } " +
                "void main() { TestClass::new(); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testInternalFieldWrite() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; pipeline(testVar); return this; } } " +
                "void main() { TestClass::new(); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testIdentifierExpressionFieldGetter() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; return this; } int getVar() { return testVar; } } " +
                "void main() { TestClass testClass = TestClass::new(); pipeline(testClass.getVar()); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }

    @Test
    public void testConstructorExpressionFieldGetter() throws Exception {
        String diamond = "class TestClass { int testVar; static TestClass new() { testVar = 42; return this; } int getVar() { return testVar; } } " +
                "void main() { pipeline(TestClass::new().getVar()); }";
        testWithPipeline(diamond);
        assertEquals(42, (char) getPipeline().remove());
    }
}

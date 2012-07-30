/*
 * PipelineTest.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.io.CharStreams;
import com.prealpha.dcputil.emulator.testing.MachineTest;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

abstract class PipelineTest extends MachineTest {
    private static final String PIPELINE_DECLARATION = "include Device;\n" +
            "void pipeline(int value) {\n" +
            "\tpipeline(uint::cast(value));\n" +
            "}\n" +
            "\n" +
            "void pipeline(uint value) {\n" +
            "\tuint[] manufacturer = uint[]::new(2U);\n" +
            "\tmanufacturer[1U] = 0xcaffU;\n" +
            "\tmanufacturer[0U] = 0x1e7eU;\n" +
            "\tuint[] hardwareId = uint[]::new(2U);\n" +
            "\thardwareId[0U] = 0U;\n" +
            "\thardwareId[1U] = 0U;\n" +
            "\tuint version = 0U;\n" +
            "\tDevice pipeline = Device::locateDevice(manufacturer, hardwareId, version);\n" +
            "\tpipeline.interrupt(0U, value);\n" +
            "}\n";

    protected void testWithPipeline(String sourceCode) throws Exception {
        test(Compiler.getStrictCompiler().compile(sourceCode + PIPELINE_DECLARATION));
    }

    protected void testWithPipeline(File file) throws Exception {
        List<String> lines = CharStreams.readLines(new FileReader(file));
        lines.addAll(Arrays.asList(PIPELINE_DECLARATION.split("\n")));
        String sourceCode = "";
        for (String line : lines) {
            sourceCode += line + '\n';
        }
        test(Compiler.getStandardCompiler().compile(sourceCode));
    }
}

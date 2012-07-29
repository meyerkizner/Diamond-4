/*
 * Compiler.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.Lists;
import com.prealpha.diamond.compiler.lexer.Lexer;
import com.prealpha.diamond.compiler.node.AProgram;
import com.prealpha.diamond.compiler.node.Start;
import com.prealpha.diamond.compiler.parser.Parser;

import java.io.File;
import java.io.FileReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.List;

public final class Compiler {
    public static void main(String[] args) throws Exception {
        for (String instruction : compile(new File(args[0]))) {
            System.out.println(instruction);
        }
    }

    public static List<String> compile(File file) throws Exception {
        return doCompile(new FileReader(file), file);
    }

    public static List<String> compile(Reader reader) throws Exception {
        return doCompile(reader, new File("."));
    }

    private static List<String> doCompile(Reader reader, File mainFile) throws Exception {
        List<Exception> exceptionBuffer = Lists.newArrayList();

        Lexer lexer = new Lexer(new PushbackReader(reader));
        Parser parser = new Parser(lexer);
        Start tree = parser.parse();

        NodeReplacementProcessor nodeReplacementProcessor = new NodeReplacementProcessor(exceptionBuffer, mainFile);
        tree.apply(nodeReplacementProcessor);
        checkBuffer(exceptionBuffer);

        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder((AProgram) tree.getPProgram(), exceptionBuffer);
        tree.apply(symbolTableBuilder);
        checkBuffer(exceptionBuffer);

        TypeEnforcer typeEnforcer = new TypeEnforcer(symbolTableBuilder, exceptionBuffer);
        tree.apply(typeEnforcer);
        checkBuffer(exceptionBuffer);

        CodeGenerator codeGenerator = new CodeGenerator(symbolTableBuilder, exceptionBuffer, typeEnforcer.getTypes());
        tree.apply(codeGenerator);
        checkBuffer(exceptionBuffer);

        return codeGenerator.getInstructions();
    }

    private static void checkBuffer(List<Exception> exceptionBuffer) throws Exception {
        if (!exceptionBuffer.isEmpty()) {
            if (exceptionBuffer.size() > 1) {
                for (Exception exception : exceptionBuffer) {
                    exception.printStackTrace();
                }
                throw new AssertionError(String.format("compilation halted: %d syntax error(s)", exceptionBuffer.size()));
            } else {
                throw exceptionBuffer.get(0);
            }
        }
    }

    private Compiler() {
    }
}

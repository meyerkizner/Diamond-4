/*
 * Compiler.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.Lists;
import com.prealpha.diamond.compiler.lexer.Lexer;
import com.prealpha.diamond.compiler.lexer.LexerException;
import com.prealpha.diamond.compiler.node.Start;
import com.prealpha.diamond.compiler.parser.Parser;
import com.prealpha.diamond.compiler.parser.ParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;

public final class Compiler {
    public static void main(String[] args) throws IOException, LexerException, ParserException, SemanticException {
        for (String instruction : compile(new File(args[0]))) {
            System.out.println(instruction);
        }
    }

    public static List<String> compile(File file) throws IOException, LexerException, ParserException, SemanticException {
        List<Exception> exceptionBuffer = Lists.newArrayList();

        Lexer lexer = new Lexer(new PushbackReader(new FileReader(file)));
        Parser parser = new Parser(lexer);
        Start tree = parser.parse();

        IncludeProcessor includeProcessor = new IncludeProcessor(exceptionBuffer, file);
        tree.apply(includeProcessor);
        checkBuffer(exceptionBuffer);

        SymbolTableBuilder symbolTableBuilder = new SymbolTableBuilder(exceptionBuffer);
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

    private static void checkBuffer(List<Exception> exceptionBuffer) {
        if (!exceptionBuffer.isEmpty()) {
            for (Exception exception : exceptionBuffer) {
                exception.printStackTrace();
            }
            throw new AssertionError(String.format("compilation halted: %d syntax errors", exceptionBuffer.size()));
        }
    }

    private Compiler() {
    }
}
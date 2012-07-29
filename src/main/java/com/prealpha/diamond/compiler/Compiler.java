/*
 * Compiler.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.prealpha.diamond.compiler.lexer.Lexer;
import com.prealpha.diamond.compiler.lexer.LexerException;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.Start;
import com.prealpha.diamond.compiler.parser.Parser;
import com.prealpha.diamond.compiler.parser.ParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

public abstract class Compiler {
    public static void main(String[] args) throws IOException, LexerException, ParserException, SemanticException {
        for (String line : getStandardCompiler().compile(new File(args[0]))) {
            System.out.println(line);
        }
    }

    public static Compiler getStandardCompiler() {
        return new Compiler() {
            private final List<Exception> exceptions = Lists.newArrayList();

            @Override
            void raise(Exception exception) {
                exceptions.add(exception);
            }

            @Override
            void checkRaised() throws IOException, LexerException, ParserException, SemanticException {
                if (exceptions.size() == 1) {
                    throwAllowableType(exceptions.get(0));
                } else if (exceptions.size() > 1) {
                    for (Exception exception : exceptions) {
                        exception.printStackTrace();
                    }
                    throw new AssertionError(String.format("compilation halted: %d syntax error(s)", exceptions.size()));
                }
            }
        };
    }

    public static Compiler getStrictCompiler() {
        return new Compiler() {
            private Exception exception;

            @Override
            void raise(Exception exception) {
                if (this.exception == null) {
                    this.exception = exception;
                }
            }

            @Override
            void checkRaised() throws IOException, LexerException, ParserException, SemanticException {
                if (this.exception != null) {
                    throwAllowableType(exception);
                }
            }
        };
    }

    private static enum Phase {
        NODE_REPLACEMENT {
            @Override
            void execute(Compiler compiler, Start tree) {
                NodeReplacementProcessor processor = new NodeReplacementProcessor(compiler);
                tree.apply(processor);
            }
        },

        SYMBOL_TABLE {
            @Override
            void execute(Compiler compiler, Start tree) {
                compiler.scopeSource = new SymbolTableBuilder(compiler);
                tree.apply(compiler.scopeSource);
            }
        },

        TYPE_ENFORCEMENT {
            @Override
            void execute(Compiler compiler, Start tree) {
                TypeEnforcer typeEnforcer = new TypeEnforcer(compiler);
                tree.apply(typeEnforcer);
                compiler.types = typeEnforcer.getTypes();
            }
        },

        CODE_GENERATION {
            @Override
            void execute(Compiler compiler, Start tree) {
                CodeGenerator codeGenerator = new CodeGenerator(compiler);
                tree.apply(codeGenerator);
                try {
                    compiler.instructions = codeGenerator.getInstructions();
                } catch (IOException|SemanticException ex) {
                    compiler.raise(ex);
                }
            }
        };

        abstract void execute(Compiler compiler, Start tree);
    }

    private File mainFile;

    private ScopeAwareWalker scopeSource;

    private Map<Node, TypeToken> types;

    private List<String> instructions;

    private Compiler() {
    }

    public List<String> compile(String sourceCode) throws IOException, LexerException, ParserException, SemanticException {
        return compile(new StringReader(sourceCode));
    }

    public List<String> compile(Reader reader) throws IOException, LexerException, ParserException, SemanticException {
        if (mainFile == null) {
            mainFile = new File(".");
        }

        Lexer lexer = new Lexer(new PushbackReader(reader));
        Parser parser = new Parser(lexer);
        Start tree = parser.parse();
        for (Phase phase : Phase.values()) {
            phase.execute(this, tree);
            checkRaised();
        }

        return clear();
    }

    public List<String> compile(File file) throws IOException, LexerException, ParserException, SemanticException {
        mainFile = file;
        return compile(new FileReader(file));
    }

    private List<String> clear() {
        List<String> toReturn = instructions;
        mainFile = null;
        scopeSource = null;
        types = null;
        instructions = null;
        return toReturn;
    }

    abstract void raise(Exception exception);

    abstract void checkRaised() throws IOException, LexerException, ParserException, SemanticException;

    private static void throwAllowableType(Exception exception)
            throws IOException, LexerException, ParserException, SemanticException {
        if (exception instanceof IOException) {
            throw (IOException) exception;
        } else if (exception instanceof LexerException) {
            throw (LexerException) exception;
        } else if (exception instanceof ParserException) {
            throw (ParserException) exception;
        } else if (exception instanceof SemanticException) {
            throw (SemanticException) exception;
        } else {
            throw new AssertionError("unknown exception type", exception);
        }
    }

    File getMainFile() {
        checkState(mainFile != null);
        return mainFile;
    }

    ScopeAwareWalker getScopeSource() {
        checkState(scopeSource != null);
        return new ScopeAwareWalker(scopeSource);
    }

    Map<Node, TypeToken> getTypes() {
        checkState(types != null);
        return ImmutableMap.copyOf(types);
    }
}

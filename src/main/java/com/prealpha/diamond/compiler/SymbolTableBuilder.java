/*
 * SymbolTableBuilder.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.AProgram;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;

import java.util.List;

import static com.google.common.base.Preconditions.*;

final class SymbolTableBuilder extends ScopeAwareWalker {
    private final List<Exception> exceptionBuffer;

    private ClassSymbol currentClass;

    public SymbolTableBuilder(AProgram root, List<Exception> exceptionBuffer) {
        super(root);
        checkNotNull(exceptionBuffer);
        this.exceptionBuffer = exceptionBuffer;
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        try {
            ClassSymbol symbol = new ClassSymbol(classDeclaration);
            getScope().register(symbol);
            currentClass = symbol;
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAClassDeclaration(classDeclaration);
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        currentClass = null;
        super.outAClassDeclaration(classDeclaration);
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        try {
            getScope().register(new FunctionSymbol(functionDeclaration, currentClass));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        try {
            getScope().register(new FunctionSymbol(functionDeclaration, currentClass));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAVoidFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        try {
            getScope().register(new ConstructorSymbol(constructorDeclaration, currentClass));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAConstructorDeclaration(constructorDeclaration);
    }

    @Override
    public void inAFieldDeclaration(AFieldDeclaration fieldDeclaration) {
        try {
            getScope().register(new FieldSymbol(fieldDeclaration, currentClass));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void inALocalDeclaration(ALocalDeclaration localDeclaration) {
        try {
            getScope().register(new LocalSymbol(localDeclaration));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }
}

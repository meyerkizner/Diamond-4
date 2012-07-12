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
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;

import java.util.List;

import static com.google.common.base.Preconditions.*;

final class SymbolTableBuilder extends ScopeAwareWalker {
    private final List<Exception> exceptionBuffer;

    public SymbolTableBuilder(List<Exception> exceptionBuffer) {
        super();
        checkNotNull(exceptionBuffer);
        this.exceptionBuffer = exceptionBuffer;
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        try {
            getScope().register(new ClassSymbol(classDeclaration));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAClassDeclaration(classDeclaration);
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        try {
            getScope().register(new FunctionSymbol(functionDeclaration));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        try {
            getScope().register(new FunctionSymbol(functionDeclaration));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAVoidFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        try {
            getScope().register(new ConstructorSymbol(constructorDeclaration));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        super.inAConstructorDeclaration(constructorDeclaration);
    }

    @Override
    public void inAFieldDeclaration(AFieldDeclaration fieldDeclaration) {
        try {
            getScope().register(new FieldSymbol(fieldDeclaration));
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

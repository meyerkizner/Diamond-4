/*
 * SymbolTableBuilder.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.Node;

import java.util.List;
import java.util.Map;

final class SymbolTableBuilder extends DepthFirstAdapter {
    private final Map<Node, SymbolTable> scopes;

    private SymbolTable current;

    private final List<SemanticException> exceptions;

    public SymbolTableBuilder() {
        scopes = Maps.newHashMap();
        current = new SymbolTable(null);
        exceptions = Lists.newArrayList();
    }

    public Map<Node, SymbolTable> getScopes() throws SemanticException {
        if (exceptions.isEmpty()) {
            return scopes;
        } else {
            throw new SemanticException(exceptions);
        }
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        try {
            current.register(new ClassSymbol(classDeclaration));
        } catch (SemanticException sx) {
            exceptions.add(sx);
        }
        current = new SymbolTable(current);
        scopes.put(classDeclaration, current);
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        try {
            current.register(new FunctionSymbol(functionDeclaration));
        } catch (SemanticException sx) {
            exceptions.add(sx);
        }
        current = new SymbolTable(current);
        scopes.put(functionDeclaration, current);
    }

    @Override
    public void outAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        try {
            current.register(new ConstructorSymbol(constructorDeclaration));
        } catch (SemanticException sx) {
            exceptions.add(sx);
        }
        current = new SymbolTable(current);
        scopes.put(constructorDeclaration, current);
    }

    @Override
    public void outAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inABlockStatement(ABlockStatement blockStatement) {
        current = new SymbolTable(current);
        scopes.put(blockStatement, current);
    }

    @Override
    public void outABlockStatement(ABlockStatement blockStatement) {
        current = current.getParent();
    }

    @Override
    public void inAFieldDeclaration(AFieldDeclaration fieldDeclaration) {
        try {
            current.register(new FieldSymbol(fieldDeclaration));
        } catch (SemanticException sx) {
            exceptions.add(sx);
        }
    }

    @Override
    public void inALocalDeclaration(ALocalDeclaration localDeclaration) {
        try {
            current.register(new LocalSymbol(localDeclaration));
        } catch (SemanticException sx) {
            exceptions.add(sx);
        }
    }
}

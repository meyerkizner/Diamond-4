/*
 * ScopeAwareWalker.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.Node;

import java.util.Map;

abstract class ScopeAwareWalker extends DepthFirstAdapter {
    private final Map<Node, SymbolTable> scopes;

    private SymbolTable current;

    protected ScopeAwareWalker() {
        scopes = Maps.newHashMap();
        current = new SymbolTable(null);
        scopes.put(null, current);
    }

    protected ScopeAwareWalker(ScopeAwareWalker scopeSource) {
        scopes = ImmutableMap.copyOf(scopeSource.scopes);
        current = scopes.get(null);
    }

    protected SymbolTable getSymbols() {
        return current;
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        if (!scopes.containsKey(classDeclaration)) {
            current = new SymbolTable(current);
            scopes.put(classDeclaration, current);
        } else {
            current = scopes.get(classDeclaration);
        }
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        if (!scopes.containsKey(functionDeclaration)) {
            current = new SymbolTable(current);
            scopes.put(functionDeclaration, current);
        } else {
            current = scopes.get(functionDeclaration);
        }
    }

    @Override
    public void outAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        if (!scopes.containsKey(functionDeclaration)) {
            current = new SymbolTable(current);
            scopes.put(functionDeclaration, current);
        } else {
            current = scopes.get(functionDeclaration);
        }
    }

    @Override
    public void outAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        if (!scopes.containsKey(constructorDeclaration)) {
            current = new SymbolTable(current);
            scopes.put(constructorDeclaration, current);
        } else {
            current = scopes.get(constructorDeclaration);
        }
    }

    @Override
    public void outAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        current = current.getParent();
    }

    @Override
    public void inABlockStatement(ABlockStatement blockStatement) {
        if (!scopes.containsKey(blockStatement)) {
            current = new SymbolTable(current);
            scopes.put(blockStatement, current);
        } else {
            current = scopes.get(blockStatement);
        }
    }

    @Override
    public void outABlockStatement(ABlockStatement blockStatement) {
        current = current.getParent();
    }
}

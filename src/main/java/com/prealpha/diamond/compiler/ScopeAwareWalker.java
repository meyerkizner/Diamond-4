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
import com.prealpha.diamond.compiler.node.ACastDeclaration;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AProgram;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.Node;

import java.util.Map;

/**
 * <p>
 *     A syntax tree walker which creates and keeps track of {@link Scope} instances corresponding to nodes in the tree,
 *     adding symbols to these instances as they are declared. The current scope is accessible using
 *     {@link #getScope()}, and the scope for an arbitrary (already-visited) node can be located using
 *     {@link #getScope(Node)}. In addition, a new {@code ScopeAwareWalker} may make use of the scope information
 *     collected by a previous walker using the {@link #ScopeAwareWalker(ScopeAwareWalker)} constructor, which makes the
 *     scope information stored in the new walker unmodifiable.
 * </p>
 * <p>
 *     If subclasses override any of the methods of this class, they should be careful to invoke the superclass method
 *     at an appropriate point in their own implementation, to avoid corrupting the scope information stored in this
 *     class.
 * </p>
 *
 * @author Meyer Kizner
 *
 */
abstract class ScopeAwareWalker extends DepthFirstAdapter {
    private final Map<Node, Scope> scopes;

    private final AProgram root;

    private Scope current;

    protected ScopeAwareWalker(AProgram root) {
        this.scopes = Maps.newHashMap();
        this.root = root;
        this.current = new Scope(null);
        scopes.put(this.root, this.current);
    }

    protected ScopeAwareWalker(ScopeAwareWalker scopeSource) {
        scopes = ImmutableMap.copyOf(scopeSource.scopes);
        root = scopeSource.root;
        current = getRootScope();
    }

    protected final Scope getRootScope() {
        return scopes.get(root);
    }

    protected final Scope getScope() {
        return current;
    }

    protected final Scope getScope(Node scopeKey) {
        return scopes.get(scopeKey);
    }

    protected void onEnterScope(Node scopeKey) {
        if (!scopes.containsKey(scopeKey)) {
            current = new Scope(current);
            scopes.put(scopeKey, current);
        } else {
            current = scopes.get(scopeKey);
        }
    }

    protected void onExitScope(Node scopeKey) {
        current = current.getParent();
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        onEnterScope(classDeclaration);
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        onExitScope(classDeclaration);
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        onEnterScope(functionDeclaration);
    }

    @Override
    public void outAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        onExitScope(functionDeclaration);
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        onEnterScope(functionDeclaration);
    }

    @Override
    public void outAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        onExitScope(functionDeclaration);
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        onEnterScope(constructorDeclaration);
    }

    @Override
    public void outAConstructorDeclaration(AConstructorDeclaration constructorDeclaration) {
        onExitScope(constructorDeclaration);
    }

    @Override
    public void inACastDeclaration(ACastDeclaration castDeclaration) {
        onEnterScope(castDeclaration);
    }

    @Override
    public void outACastDeclaration(ACastDeclaration castDeclaration) {
        onExitScope(castDeclaration);
    }

    @Override
    public void inABlockStatement(ABlockStatement blockStatement) {
        onEnterScope(blockStatement);
    }

    @Override
    public void outABlockStatement(ABlockStatement blockStatement) {
        onExitScope(blockStatement);
    }

    @Override
    public void inAForStatement(AForStatement forStatement) {
        onEnterScope(forStatement);
    }

    @Override
    public void outAForStatement(AForStatement forStatement) {
        onExitScope(forStatement);
    }
}

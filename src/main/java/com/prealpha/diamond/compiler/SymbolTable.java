/*
 * SymbolTable.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Map;

final class SymbolTable {
    private final SymbolTable parent;

    private final Map<String, ClassSymbol> classSymbols;

    private final Multimap<String, FunctionSymbol> functionSymbols;

    private final Multimap<String, ConstructorSymbol> constructorSymbols;

    private final Map<String, FieldSymbol> fieldSymbols;

    private final Map<String, LocalSymbol> localSymbols;

    SymbolTable(SymbolTable parent) {
        this.parent = parent;
        classSymbols = Maps.newHashMap();
        functionSymbols = HashMultimap.create();
        constructorSymbols = HashMultimap.create();
        fieldSymbols = Maps.newHashMap();
        localSymbols = Maps.newHashMap();
    }

    public ClassSymbol resolveClass(String name) throws SemanticException {
        if (classSymbols.containsKey(name)) {
            return classSymbols.get(name);
        } else if (parent != null) {
            return parent.resolveClass(name);
        } else {
            throw new SemanticException(String.format("cannot resolve class symbol \"%s\"", name));
        }
    }

    public Collection<FunctionSymbol> resolveFunction(String name) throws SemanticException {
        if (functionSymbols.containsKey(name)) {
            return functionSymbols.get(name);
        } else if (parent != null) {
            return parent.resolveFunction(name);
        } else {
            throw new SemanticException(String.format("cannot resolve function symbol \"%s\"", name));
        }
    }

    public Collection<ConstructorSymbol> resolveConstructor(String name) throws SemanticException {
        if (constructorSymbols.containsKey(name)) {
            return constructorSymbols.get(name);
        } else if (parent != null) {
            return parent.resolveConstructor(name);
        } else {
            throw new SemanticException(String.format("cannot resolve constructor symbol \"%s\"", name));
        }
    }

    public FieldSymbol resolveField(String name) throws SemanticException {
        if (fieldSymbols.containsKey(name)) {
            return fieldSymbols.get(name);
        } else if (parent != null) {
            return parent.resolveField(name);
        } else {
            throw new SemanticException(String.format("cannot resolve field symbol \"%s\"", name));
        }
    }

    public LocalSymbol resolveLocal(String name) throws SemanticException {
        if (localSymbols.containsKey(name)) {
            return localSymbols.get(name);
        } else if (parent != null) {
            return parent.resolveLocal(name);
        } else {
            throw new SemanticException(String.format("cannot resolve local symbol \"%s\"", name));
        }
    }
}

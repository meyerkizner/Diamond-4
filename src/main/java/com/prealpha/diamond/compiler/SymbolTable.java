/*
 * SymbolTable.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.prealpha.diamond.compiler.node.PTypeToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class SymbolTable {
    private static final Function<ParametrizedSymbol, List<PTypeToken>> PARAMETER_TYPES = new Function<ParametrizedSymbol, List<PTypeToken>>() {
        @Override
        public List<PTypeToken> apply(ParametrizedSymbol parametrizedSymbol) {
            return Lists.transform(parametrizedSymbol.getParameters(), new Function<LocalSymbol, PTypeToken>() {
                @Override
                public PTypeToken apply(LocalSymbol input) {
                    return input.getType();
                }
            });
        }
    };

    private final SymbolTable parent;

    private final Map<String, ClassSymbol> classSymbols;

    private final Multimap<String, FunctionSymbol> functionSymbols;

    private final List<ConstructorSymbol> constructorSymbols;

    private final Map<String, FieldSymbol> fieldSymbols;

    private final Map<String, LocalSymbol> localSymbols;

    SymbolTable(SymbolTable parent) {
        this.parent = parent;
        classSymbols = Maps.newHashMap();
        functionSymbols = HashMultimap.create();
        constructorSymbols = Lists.newArrayList();
        fieldSymbols = Maps.newHashMap();
        localSymbols = Maps.newHashMap();
    }

    SymbolTable getParent() {
        return parent;
    }

    void register(ClassSymbol classSymbol) throws SemanticException {
        String name = classSymbol.getName();
        if (!classSymbols.containsKey(name)) {
            classSymbols.put(name, classSymbol);
        } else {
            throw new SemanticException(String.format("duplicate class symbol \"%s\"", name));
        }
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

    void register(FunctionSymbol functionSymbol) throws SemanticException {
        String name = functionSymbol.getName();
        if (functionSymbols.containsKey(name)) {
            Collection<FunctionSymbol> overloaded = functionSymbols.get(name);
            for (FunctionSymbol function : overloaded) {
                if (PARAMETER_TYPES.apply(functionSymbol).equals(PARAMETER_TYPES.apply(function))) {
                    throw new SemanticException(String.format("duplicate function symbol \"%s\"", name));
                }
            }
        }
        functionSymbols.put(name, functionSymbol);
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

    void register(ConstructorSymbol constructorSymbol) throws SemanticException {
        for (ConstructorSymbol constructor : constructorSymbols) {
            if (PARAMETER_TYPES.apply(constructorSymbol).equals(PARAMETER_TYPES.apply(constructor))) {
                throw new SemanticException("duplicate constructor");
            }
        }
        constructorSymbols.add(constructorSymbol);
    }

    public Collection<ConstructorSymbol> resolveConstructor() throws SemanticException {
        if (!constructorSymbols.isEmpty()) {
            return ImmutableList.copyOf(constructorSymbols);
        } else if (parent != null) {
            return parent.resolveConstructor();
        } else {
            throw new SemanticException("cannot resolve constructor");
        }
    }

    void register(FieldSymbol fieldSymbol) throws SemanticException {
        String name = fieldSymbol.getName();
        if (!fieldSymbols.containsKey(name)) {
            fieldSymbols.put(name, fieldSymbol);
        } else {
            throw new SemanticException(String.format("duplicate field symbol \"%s\"", name));
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

    void register(LocalSymbol localSymbol) throws SemanticException {
        String name = localSymbol.getName();
        if (!localSymbols.containsKey(name)) {
            localSymbols.put(name, localSymbol);
        } else {
            throw new SemanticException(String.format("duplicate local symbol \"%s\"", name));
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

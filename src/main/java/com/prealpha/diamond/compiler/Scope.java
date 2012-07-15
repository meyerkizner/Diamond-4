/*
 * Scope.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Scope {
    private final Scope parent;

    private final Map<String, ClassSymbol> classSymbols;

    private final Table<String, List<TypeToken>, FunctionSymbol> functionSymbols;

    private final Map<List<TypeToken>, ConstructorSymbol> constructorSymbols;

    private final Map<String, FieldSymbol> fieldSymbols;

    private final Map<String, LocalSymbol> localSymbols;

    Scope(Scope parent) {
        this.parent = parent;
        classSymbols = Maps.newHashMap();
        functionSymbols = HashBasedTable.create();
        constructorSymbols = Maps.newHashMap();
        fieldSymbols = Maps.newLinkedHashMap();
        localSymbols = Maps.newLinkedHashMap();
    }

    Scope getParent() {
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
        List<TypeToken> parameters = Lists.transform(functionSymbol.getParameters(), TypeTokenUtil.getSymbolFunction());
        if (!functionSymbols.contains(name, parameters)) {
            functionSymbols.put(name, parameters, functionSymbol);
        } else {
            throw new SemanticException(String.format("duplicate function symbol \"%s%s\"", name, parameters));
        }
    }

    public FunctionSymbol resolveFunction(String name, List<? extends TypeToken> parameterTypes) throws SemanticException {
        Set<FunctionSymbol> matches = resolveParametrized(functionSymbols.row(name).values(), parameterTypes);
        if (matches.size() == 1) {
            return matches.iterator().next();
        } else if (matches.size() > 1) {
            throw new SemanticException(String.format("ambiguous function symbol \"%s%s\"", name, parameterTypes));
        } else if (parent != null) {
            return parent.resolveFunction(name, parameterTypes);
        } else {
            throw new SemanticException(String.format("cannot resolve function symbol \"%s%s\"", name, parameterTypes));
        }
    }

    public Map<List<TypeToken>, FunctionSymbol> resolveFunction(String name) throws SemanticException {
        if (functionSymbols.containsRow(name)) {
            return functionSymbols.row(name);
        } else if (parent != null) {
            return parent.resolveFunction(name);
        } else {
            throw new SemanticException(String.format("cannot resolve function symbol \"%s\"", name));
        }
    }

    void register(ConstructorSymbol constructorSymbol) throws SemanticException {
        List<TypeToken> parameters = Lists.transform(constructorSymbol.getParameters(), TypeTokenUtil.getSymbolFunction());
        if (!constructorSymbols.containsKey(parameters)) {
            constructorSymbols.put(parameters, constructorSymbol);
        } else {
            throw new SemanticException(String.format("duplicate constructor \"new%s\"", parameters));
        }
    }

    public ConstructorSymbol resolveConstructor(List<? extends TypeToken> parameterTypes) throws SemanticException {
        Set<ConstructorSymbol> matches = resolveParametrized(constructorSymbols.values(), parameterTypes);
        if (matches.size() == 1) {
            return matches.iterator().next();
        } else if (matches.size() > 1) {
            throw new SemanticException(String.format("ambiguous constructor symbol \"new%s\"", parameterTypes));
        } else if (parent != null) {
            return parent.resolveConstructor(parameterTypes);
        } else {
            throw new SemanticException(String.format("cannot resolve constructor symbol \"new%s\"", parameterTypes));
        }
    }

    public Map<List<TypeToken>, ConstructorSymbol> resolveConstructor() throws SemanticException {
        if (!constructorSymbols.isEmpty()) {
            return ImmutableMap.copyOf(constructorSymbols);
        } else if (parent != null) {
            return parent.resolveConstructor();
        } else {
            throw new SemanticException("cannot resolve constructor symbol \"new\"");
        }
    }

    private static <T extends ParametrizedSymbol> Set<T> resolveParametrized(Collection<T> symbols, List<? extends TypeToken> parameterTypes) {
        Set<T> matches = Sets.newHashSet();
        for (T symbol : symbols) {
            int matchCount = 0;
            for (int i = 0; i < parameterTypes.size(); i++) {
                TypeToken expectedType = symbol.getParameters().get(i).getType();
                TypeToken actualType = parameterTypes.get(i);
                if (actualType.isAssignableTo(expectedType)) {
                    matchCount++;
                }
            }
            if (matchCount == parameterTypes.size()) {
                matches.add(symbol);
            }
        }
        return matches;
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

    /**
     * Returns the fields that are strictly within this scope. Fields inherited from parent scopes are not included.
     *
     * @return the list of fields strictly within this scope, in the order declared
     */
    public List<FieldSymbol> getFields() {
        return ImmutableList.copyOf(fieldSymbols.values());
    }

    /**
     * Returns the locals that are strictly within this scope. Locals inherited from parent scopes are not included.
     *
     * @return the list of locals strictly within this scope, in the order declared
     */
    public List<LocalSymbol> getLocals() {
        return ImmutableList.copyOf(localSymbols.values());
    }
}

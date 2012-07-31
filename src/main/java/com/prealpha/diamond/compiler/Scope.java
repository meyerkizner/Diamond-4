/*
 * Scope.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class Scope {
    private final Scope parent;

    private final Map<String, ClassSymbol> classSymbols;

    private final Map<ParametrizedSignature, ParametrizedSymbol> parametrizedSymbols;

    private final Map<String, FieldSymbol> fieldSymbols;

    private final Map<String, LocalSymbol> localSymbols;

    Scope(Scope parent) {
        this.parent = parent;
        classSymbols = Maps.newHashMap();
        parametrizedSymbols = Maps.newHashMap();
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

    void register(ParametrizedSymbol parametrizedSymbol) throws SemanticException {
        ParametrizedSignature signature = new ParametrizedSignature(parametrizedSymbol);
        if (!parametrizedSymbols.containsKey(signature)) {
            parametrizedSymbols.put(signature, parametrizedSymbol);
        } else {
            throw new SemanticException(String.format("duplicate parametrized symbol %s", signature));
        }
    }

    public FunctionSymbol resolveFunction(String name, List<TypeToken> parameterTypes) throws SemanticException {
        checkArgument(!name.equals("new"));
        checkArgument(!name.equals("cast"));
        return (FunctionSymbol) resolveParametrized(new ParametrizedSignature(name, parameterTypes));
    }

    public ConstructorSymbol resolveConstructor(List<TypeToken> parameterTypes) throws SemanticException {
        return (ConstructorSymbol) resolveParametrized(new ParametrizedSignature("new", parameterTypes));
    }

    public CastSymbol resolveCast(TypeToken valueType) throws SemanticException {
        return (CastSymbol) resolveParametrized(new ParametrizedSignature("cast", ImmutableList.of(valueType)));
    }

    public ParametrizedSymbol resolveParametrized(ParametrizedSignature signature) throws SemanticException {
        Set<ParametrizedSignature> matchingSignatures = Sets.filter(parametrizedSymbols.keySet(), signature);
        if (matchingSignatures.size() == 1) {
            return parametrizedSymbols.get(matchingSignatures.iterator().next());
        } else if (matchingSignatures.size() > 1) {
            String message = String.format("ambiguous parametrized signature: %s; matches: %s", signature, matchingSignatures);
            throw new SemanticException(message);
        } else if (parent != null) {
            return parent.resolveParametrized(signature);
        } else {
            throw new SemanticException(String.format("cannot resolve parametrized symbol %s", signature));
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
        assert (parent != null);
        String name = localSymbol.getName();
        boolean isDuplicate;
        try {
            resolveLocal(localSymbol.getName());
            isDuplicate = true;
        } catch (SemanticException sx) {
            isDuplicate = false;
        }
        if (isDuplicate) {
            throw new SemanticException(String.format("duplicate local symbol \"%s\"", name));
        } else {
            localSymbols.put(name, localSymbol);
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

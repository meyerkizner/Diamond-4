/*
 * ParametrizedSignature.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.*;

final class ParametrizedSignature implements Predicate<ParametrizedSignature> {
    private final String name;

    private final List<TypeToken> parameterTypes;

    public ParametrizedSignature(String name, List<TypeToken> parameterTypes) {
        checkNotNull(name);
        checkNotNull(parameterTypes);
        checkArgument(!name.isEmpty());
        this.name = name;
        this.parameterTypes = ImmutableList.copyOf(parameterTypes);
    }

    public ParametrizedSignature(ParametrizedSymbol symbol) {
        this(symbol.getName(), Lists.transform(symbol.getParameters(), TypeTokenUtil.getSymbolFunction()));
    }

    public boolean matches(String name, List<TypeToken> parameterTypes) {
        if (!this.name.equals(name)) {
            return false;
        } else if (this.parameterTypes.size() != parameterTypes.size()) {
            return false;
        } else {
            for (int i = 0; i < this.parameterTypes.size(); i++) {
                TypeToken formalType = this.parameterTypes.get(i);
                TypeToken actualType = parameterTypes.get(i);
                if (!actualType.isAssignableTo(formalType)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean apply(ParametrizedSignature input) {
        return matches(input.name, input.parameterTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParametrizedSignature that = (ParametrizedSignature) o;

        if (!name.equals(that.name)) return false;
        if (!parameterTypes.equals(that.parameterTypes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + parameterTypes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append('(');
        for (TypeToken parameterType : parameterTypes) {
            sb.append(parameterType);
            sb.append(',');
        }
        sb.replace(sb.length() - 1, sb.length(), ")"); // strip the last comma
        return sb.toString();
    }
}

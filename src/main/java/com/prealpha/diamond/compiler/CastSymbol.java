/*
 * CastSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.prealpha.diamond.compiler.node.ACastDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class CastSymbol implements ParametrizedSymbol {
    private final ACastDeclaration declaration;

    private final ClassSymbol declaringClass;

    private final UserDefinedTypeToken returnType;

    private final List<LocalSymbol> parameters;

    private final Set<Modifier> modifiers;

    CastSymbol(ACastDeclaration declaration, ClassSymbol declaringClass) throws SemanticException {
        checkNotNull(declaration);
        checkNotNull(declaringClass);
        this.declaration = declaration;
        this.declaringClass = declaringClass;
        this.returnType = new UserDefinedTypeToken(this.declaration.getReturnType().getText());
        this.parameters = ImmutableList.of(new LocalSymbol((ALocalDeclaration) declaration.getParameter()));
        this.modifiers = EnumSet.noneOf(Modifier.class);
        for (PModifier modifierNode : this.declaration.getModifiers()) {
            Modifier modifier = Modifier.fromNode(modifierNode);
            if (!modifier.modifiesFunctions()) {
                throw new SemanticException(modifierNode, modifier + " cannot modify " + this.declaration.getClass().getSimpleName());
            } else {
                this.modifiers.add(modifier);
            }
        }
        if (!modifiers.contains(Modifier.STATIC)) {
            throw new SemanticException(declaration, "casts must have the modifier static");
        }
    }

    @Override
    public ACastDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public ClassSymbol getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public UserDefinedTypeToken getReturnType() {
        return returnType;
    }

    @Override
    public List<LocalSymbol> getParameters() {
        return ImmutableList.copyOf(parameters);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return ImmutableSet.copyOf(modifiers);
    }
}

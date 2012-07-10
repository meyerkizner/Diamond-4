/*
 * ConstructorSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.AUserDefinedTypeToken;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;
import com.prealpha.diamond.compiler.node.PTypeToken;
import com.prealpha.diamond.compiler.node.TIdentifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

final class ConstructorSymbol implements ParametrizedSymbol {
    private final AConstructorDeclaration declaration;

    private final TIdentifier returnType;

    private final List<LocalSymbol> parameters;

    private final Set<Modifier> modifiers;

    ConstructorSymbol(AConstructorDeclaration declaration) throws SemanticException {
        checkNotNull(declaration);
        this.declaration = declaration;
        this.returnType = this.declaration.getReturnType();
        this.parameters = Lists.newArrayList();
        for (PLocalDeclaration parameterNode : this.declaration.getParameters()) {
            LocalSymbol parameter = new LocalSymbol((ALocalDeclaration) parameterNode);
            this.parameters.add(parameter);
        }
        this.modifiers = EnumSet.noneOf(Modifier.class);
        for (PModifier modifierNode : this.declaration.getModifiers()) {
            Modifier modifier = Modifier.fromNode(modifierNode);
            if (!modifier.modifiesFunctions()) {
                throw new SemanticException(modifierNode, modifier + " cannot modify " + this.declaration.getClass().getSimpleName());
            } else {
                this.modifiers.add(modifier);
            }
        }
    }

    @Override
    public AConstructorDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public PTypeToken getReturnType() {
        return new AUserDefinedTypeToken(returnType);
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

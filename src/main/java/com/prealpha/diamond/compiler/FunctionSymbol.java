/*
 * FunctionSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;
import com.prealpha.diamond.compiler.node.PTypeToken;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class FunctionSymbol implements HasParameters {
    private final AFunctionDeclaration declaration;

    private final String name;

    private final PTypeToken returnType;

    private final List<LocalSymbol> parameters;

    private final Set<Modifier> modifiers;

    FunctionSymbol(AFunctionDeclaration declaration) throws SemanticException {
        checkNotNull(declaration);
        this.declaration = declaration;
        this.name = this.declaration.getName().getText();
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

    public AFunctionDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return name;
    }

    public PTypeToken getReturnType() {
        return returnType;
    }

    @Override
    public List<LocalSymbol> getParameters() {
        return ImmutableList.copyOf(parameters);
    }

    public Set<Modifier> getModifiers() {
        return ImmutableSet.copyOf(modifiers);
    }
}

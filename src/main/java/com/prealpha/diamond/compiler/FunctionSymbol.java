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
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.PFunctionDeclaration;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;
import com.prealpha.diamond.compiler.node.PTypeToken;
import com.prealpha.diamond.compiler.node.TIdentifier;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class FunctionSymbol implements ParametrizedSymbol {
    private final PFunctionDeclaration declaration;

    private final String name;

    private final PTypeToken returnType;

    private final List<LocalSymbol> parameters;

    private final Set<Modifier> modifiers;

    FunctionSymbol(AFunctionDeclaration declaration) throws SemanticException {
        this(declaration, declaration.getName(), declaration.getReturnType(), declaration.getParameters(), declaration.getModifiers());
    }

    FunctionSymbol(AVoidFunctionDeclaration declaration) throws SemanticException {
        this(declaration, declaration.getName(), null, declaration.getParameters(), declaration.getModifiers());
    }

    private FunctionSymbol(PFunctionDeclaration declaration, TIdentifier name, PTypeToken returnType, List<PLocalDeclaration> parameters, List<PModifier> modifiers) throws SemanticException {
        this.declaration = declaration;
        this.name = name.getText();
        this.returnType = returnType;
        this.parameters = Lists.newArrayList();
        for (PLocalDeclaration parameterNode : parameters) {
            LocalSymbol parameter = new LocalSymbol((ALocalDeclaration) parameterNode);
            this.parameters.add(parameter);
        }
        this.modifiers = EnumSet.noneOf(Modifier.class);
        for (PModifier modifierNode : modifiers) {
            Modifier modifier = Modifier.fromNode(modifierNode);
            if (!modifier.modifiesFunctions()) {
                throw new SemanticException(modifierNode, modifier + " cannot modify " + this.declaration.getClass().getSimpleName());
            } else {
                this.modifiers.add(modifier);
            }
        }
    }

    public PFunctionDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return name;
    }

    @Override
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

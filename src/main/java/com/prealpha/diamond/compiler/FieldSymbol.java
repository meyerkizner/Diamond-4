/*
 * FieldSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableSet;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;

import java.util.EnumSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

final class FieldSymbol implements TypedSymbol {
    private final AFieldDeclaration declaration;

    private final String name;

    private final TypeToken type;

    private final Set<Modifier> modifiers;

    FieldSymbol(AFieldDeclaration declaration) throws SemanticException {
        checkNotNull(declaration);
        this.declaration = declaration;
        this.name = this.declaration.getName().getText();
        this.type = TypeTokenUtil.fromNode(declaration.getType());
        this.modifiers = EnumSet.noneOf(Modifier.class);
        for (PModifier modifierNode : this.declaration.getModifiers()) {
            Modifier modifier = Modifier.fromNode(modifierNode);
            if (!modifier.modifiesFields()) {
                throw new SemanticException(modifierNode, modifier + " cannot modify " + this.declaration.getClass().getSimpleName());
            } else {
                this.modifiers.add(modifier);
            }
        }
    }

    @Override
    public AFieldDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return name;
    }

    @Override
    public TypeToken getType() {
        return type;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return ImmutableSet.copyOf(modifiers);
    }
}

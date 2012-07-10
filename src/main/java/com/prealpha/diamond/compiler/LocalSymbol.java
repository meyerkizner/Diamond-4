/*
 * LocalSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableSet;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.PModifier;
import com.prealpha.diamond.compiler.node.PTypeToken;

import java.util.EnumSet;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class LocalSymbol implements TypedSymbol {
    private final ALocalDeclaration declaration;

    private final String name;

    private final PTypeToken type;

    private final Set<Modifier> modifiers;

    LocalSymbol(ALocalDeclaration declaration) throws SemanticException {
        checkNotNull(declaration);
        this.declaration = declaration;
        this.name = this.declaration.getName().getText();
        this.type = declaration.getType();
        this.modifiers = EnumSet.noneOf(Modifier.class);
        for (PModifier modifierNode : this.declaration.getModifiers()) {
            Modifier modifier = Modifier.fromNode(modifierNode);
            if (!modifier.modifiesLocals()) {
                throw new SemanticException(modifierNode, modifier + " cannot modify " + this.declaration.getClass().getSimpleName());
            } else {
                modifiers.add(modifier);
            }
        }
    }

    @Override
    public ALocalDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return name;
    }

    @Override
    public PTypeToken getType() {
        return type;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return ImmutableSet.copyOf(modifiers);
    }
}

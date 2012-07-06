/*
 * Modifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.APrivateModifier;
import com.prealpha.diamond.compiler.node.AStaticModifier;
import com.prealpha.diamond.compiler.node.PModifier;

enum Modifier {
    PRIVATE(false, true, true, false), STATIC(false, true, true, false);

    private final boolean modifiesClasses;

    private final boolean modifiesFunctions;

    private final boolean modifiesFields;

    private final boolean modifiesLocals;

    private Modifier(boolean modifiesClasses, boolean modifiesFunctions, boolean modifiesFields, boolean modifiesLocals) {
        this.modifiesClasses = modifiesClasses;
        this.modifiesFunctions = modifiesFunctions;
        this.modifiesFields = modifiesFields;
        this.modifiesLocals = modifiesLocals;
    }

    public boolean modifiesClasses() {
        return modifiesClasses;
    }

    public boolean modifiesFunctions() {
        return modifiesFunctions;
    }

    public boolean modifiesFields() {
        return modifiesFields;
    }

    public boolean modifiesLocals() {
        return modifiesLocals;
    }

    public static Modifier fromNode(PModifier modifierNode) {
        if (modifierNode instanceof APrivateModifier) {
            return PRIVATE;
        } else if (modifierNode instanceof AStaticModifier) {
            return STATIC;
        } else {
            throw new UnsupportedOperationException("unknown modifier node type: " + modifierNode.getClass().getSimpleName());
        }
    }
}

/*
 * Symbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;

import java.util.Set;

interface Symbol {
    Node getDeclaration();

    String getName();

    Set<Modifier> getModifiers();
}

/*
 * FlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;

interface FlowModifier {
    boolean onBreak(Node context);

    boolean onContinue(Node context);

    boolean onReturn(Node context);
}

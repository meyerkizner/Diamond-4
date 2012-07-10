/*
 * TypedSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.PTypeToken;

interface TypedSymbol {
    PTypeToken getType();
}

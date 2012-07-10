/*
 * HasParameters.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.PTypeToken;

import java.util.List;

interface ParametrizedSymbol {
    PTypeToken getReturnType();

    List<LocalSymbol> getParameters();
}

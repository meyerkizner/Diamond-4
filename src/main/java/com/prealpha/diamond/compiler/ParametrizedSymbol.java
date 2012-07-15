/*
 * ParametrizedSymbol.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import java.util.List;

interface ParametrizedSymbol extends MemberSymbol {
    TypeToken getReturnType();

    List<LocalSymbol> getParameters();
}

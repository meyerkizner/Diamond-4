/*
 * TypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface TypeToken {
    boolean isReference();

    boolean isIntegral();

    boolean isSigned();

    boolean isAssignableTo(TypeToken targetType);
}

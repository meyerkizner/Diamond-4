/*
 * TypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface TypeToken {
    boolean isIntegral();

    boolean isAssignableTo(TypeToken targetType);
}

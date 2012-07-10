/*
 * TypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface TypeToken {
    boolean isNumeric();

    boolean isAssignableTo(TypeToken targetType);

    TypeToken performBinaryOperation(TypeToken otherType) throws SemanticException;
}

/*
 * TypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface TypeToken {
    boolean isReference();

    boolean isNumeric();

    /**
     * Returns the number of words required to store a variable of this type. For reference types, the result is always
     * {@code 1}, because reference types are implemented as pointers to the object proper.
     *
     * @return the width of this type, in words
     */
    int getWidth();

    boolean isAssignableTo(TypeToken targetType);

    TypeToken performBinaryOperation(TypeToken otherType) throws SemanticException;
}

/*
 * BooleanTypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

enum BooleanTypeToken implements TypeToken {
    INSTANCE;

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isAssignableTo(TypeToken targetType) {
        return (targetType == INSTANCE);
    }

    @Override
    public TypeToken performBinaryOperation(TypeToken otherType) throws SemanticException {
        if (equals(otherType)) {
            return this;
        } else {
            throw new SemanticException(String.format("invalid binary operation for %s and %s", this, otherType));
        }
    }

    @Override
    public String toString() {
        return "boolean";
    }
}

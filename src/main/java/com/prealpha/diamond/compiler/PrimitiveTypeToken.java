/*
 * PrimitiveTypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

enum PrimitiveTypeToken implements TypeToken {
    BOOLEAN, INT, UINT;

    @Override
    public boolean isReference() {
        return false;
    }

    @Override
    public boolean isIntegral() {
        return (this != BOOLEAN);
    }

    @Override
    public boolean isSigned() {
        return (this == INT);
    }

    @Override
    public boolean isAssignableTo(TypeToken targetType) {
        return (this == targetType);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

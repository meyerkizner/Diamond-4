/*
 * UserDefinedTypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import static com.google.common.base.Preconditions.*;

final class UserDefinedTypeToken implements TypeToken {
    private final String typeName;

    UserDefinedTypeToken(String typeName) {
        checkNotNull(typeName);
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public boolean isReference() {
        return true;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public boolean isAssignableTo(TypeToken typeToken) {
        return equals(typeToken);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDefinedTypeToken that = (UserDefinedTypeToken) o;

        if (!typeName.equals(that.typeName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    @Override
    public String toString() {
        return typeName;
    }
}

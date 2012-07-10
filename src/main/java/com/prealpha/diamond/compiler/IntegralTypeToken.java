/*
 * IntegralTypeToken.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ABinaryIntegralLiteral;
import com.prealpha.diamond.compiler.node.ADecimalIntegralLiteral;
import com.prealpha.diamond.compiler.node.AHexIntegralLiteral;
import com.prealpha.diamond.compiler.node.AOctalIntegralLiteral;
import com.prealpha.diamond.compiler.node.PIntegralLiteral;

import java.math.BigInteger;

enum IntegralTypeToken implements TypeToken {
    /*
     * Several methods depend on the order, so don't change it.
     */
    SIGNED_SHORT(15), UNSIGNED_SHORT(16), SIGNED_INT(31), UNSIGNED_INT(32), SIGNED_LONG(63), UNSIGNED_LONG(64);

    private final int width;

    private IntegralTypeToken(int width) {
        this.width = width;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isAssignableTo(TypeToken targetType) {
        if (!(targetType instanceof IntegralTypeToken)) {
            return false;
        } else {
            IntegralTypeToken integralTarget = (IntegralTypeToken) targetType;
            if (width % 2 != 0) {
                // signed types should only widen to other signed types
                return (integralTarget.width >= this.width) && (integralTarget.width % 2 != 0);
            } else {
                // unsigned types can widen to signed ones
                return (integralTarget.width >= this.width);
            }
        }
    }

    @Override
    public TypeToken performBinaryOperation(TypeToken otherType) throws SemanticException {
        // go through each type; if both this and other are assignable to that type, return it
        for (IntegralTypeToken candidate : values()) {
            if (isAssignableTo(candidate) && otherType.isAssignableTo(candidate)) {
                return candidate;
            }
        }
        throw new SemanticException(String.format("no type is a valid promotion for both %s and %s", this, otherType));
    }

    public IntegralTypeToken promoteToSigned() throws SemanticException {
        if (width % 2 != 0) {
            return this;
        } else if ((ordinal() + 1) < values().length) {
            return values()[ordinal() + 1];
        } else {
            throw new SemanticException("unsigned long cannot be promoted to a signed type");
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace('_', ' ');
    }

    public static IntegralTypeToken fromNode(PIntegralLiteral node) throws SemanticException {
        BigInteger value;
        if (node instanceof ADecimalIntegralLiteral) {
            value = new BigInteger(((ADecimalIntegralLiteral) node).getDecimalLiteral().getText());
        } else if (node instanceof AHexIntegralLiteral) {
            value = new BigInteger(((AHexIntegralLiteral) node).getHexLiteral().getText().substring(2), 16);
        } else if (node instanceof AOctalIntegralLiteral) {
            value = new BigInteger(((AOctalIntegralLiteral) node).getOctalLiteral().getText(), 8);
        } else if (node instanceof ABinaryIntegralLiteral) {
            value = new BigInteger(((ABinaryIntegralLiteral) node).getBinaryLiteral().getText().substring(2), 2);
        } else {
            throw new UnsupportedOperationException("unknown integral literal flavor");
        }
        for (IntegralTypeToken type : values()) {
            if (value.getLowestSetBit() <= type.width) {
                return type;
            }
        }
        throw new SemanticException(node, "integral literal too big");
    }
}

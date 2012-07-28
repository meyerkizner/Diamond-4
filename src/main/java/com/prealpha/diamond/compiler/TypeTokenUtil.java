/*
 * TypeTokenUtil.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Function;
import com.prealpha.diamond.compiler.node.AArrayTypeToken;
import com.prealpha.diamond.compiler.node.ABinaryIntegralLiteral;
import com.prealpha.diamond.compiler.node.ABooleanTypeToken;
import com.prealpha.diamond.compiler.node.ADecimalIntegralLiteral;
import com.prealpha.diamond.compiler.node.AHexIntegralLiteral;
import com.prealpha.diamond.compiler.node.AIntTypeToken;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.AOctalIntegralLiteral;
import com.prealpha.diamond.compiler.node.AUintTypeToken;
import com.prealpha.diamond.compiler.node.AUserDefinedTypeToken;
import com.prealpha.diamond.compiler.node.PIntegralLiteral;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PTypeToken;

import java.math.BigInteger;

final class TypeTokenUtil {
    public static TypeToken fromNode(PTypeToken node) {
        if (node instanceof ABooleanTypeToken) {
            return PrimitiveTypeToken.BOOLEAN;
        } else if (node instanceof AIntTypeToken) {
            return PrimitiveTypeToken.INT;
        } else if (node instanceof AUintTypeToken) {
            return PrimitiveTypeToken.UINT;
        } else if (node instanceof AUserDefinedTypeToken) {
            return new UserDefinedTypeToken(((AUserDefinedTypeToken) node).getIdentifier().getText());
        } else if (node instanceof AArrayTypeToken) {
            return new ArrayTypeToken(fromNode(((AArrayTypeToken) node).getElementType()));
        } else if (node == null) {
            return null;
        } else {
            throw new UnsupportedOperationException("unknown flavor of type token");
        }
    }

    public static PrimitiveTypeToken fromIntegralLiteral(PIntegralLiteral literal) {
        if (literal.toString().trim().endsWith("u") || literal.toString().trim().endsWith("U")) {
            return PrimitiveTypeToken.UINT;
        } else {
            return PrimitiveTypeToken.INT;
        }
    }

    public static BigInteger parseIntegralLiteral(PIntegralLiteral literal) throws SemanticException {
        String text = literal.toString().trim().replace("u", "").replace("U", "");
        BigInteger value;
        if (literal instanceof ADecimalIntegralLiteral) {
            value = new BigInteger(text);
        } else if (literal instanceof AHexIntegralLiteral) {
            value = new BigInteger(text.substring(2), 16);
        } else if (literal instanceof AOctalIntegralLiteral) {
            value = new BigInteger(text, 8);
        } else if (literal instanceof ABinaryIntegralLiteral) {
            value = new BigInteger(text.substring(2), 2);
        } else {
            throw new SemanticException(literal, "unknown integral literal flavor");
        }

        PrimitiveTypeToken type = fromIntegralLiteral(literal);
        switch (type) {
            case INT:
                if (value.intValue() < Short.MIN_VALUE || value.intValue() > Short.MAX_VALUE) {
                    throw new SemanticException(literal, "integral literal too big");
                } else {
                    return value;
                }
            case UINT:
                if (value.intValue() < 0 || value.intValue() > 0xffff) {
                    throw new SemanticException(literal, "integral literal too big");
                } else {
                    return value;
                }
            default:
                throw new AssertionError();
        }
    }

    public static Function<TypedSymbol, TypeToken> getSymbolFunction() {
        return new Function<TypedSymbol, TypeToken>() {
            @Override
            public TypeToken apply(TypedSymbol input) {
                return input.getType();
            }
        };
    }

    public static Function<PLocalDeclaration, TypeToken> getDeclarationFunction() {
        return new Function<PLocalDeclaration, TypeToken>() {
            @Override
            public TypeToken apply(PLocalDeclaration input) {
                return fromNode(((ALocalDeclaration) input).getType());
            }
        };
    }

    private TypeTokenUtil() {
    }
}

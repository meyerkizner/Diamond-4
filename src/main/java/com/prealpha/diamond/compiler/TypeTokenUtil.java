/*
 * TypeTokenUtil.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AArrayTypeToken;
import com.prealpha.diamond.compiler.node.ABooleanTypeToken;
import com.prealpha.diamond.compiler.node.ASignedIntTypeToken;
import com.prealpha.diamond.compiler.node.ASignedLongTypeToken;
import com.prealpha.diamond.compiler.node.ASignedShortTypeToken;
import com.prealpha.diamond.compiler.node.AUnsignedIntTypeToken;
import com.prealpha.diamond.compiler.node.AUnsignedLongTypeToken;
import com.prealpha.diamond.compiler.node.AUnsignedShortTypeToken;
import com.prealpha.diamond.compiler.node.AUserDefinedTypeToken;
import com.prealpha.diamond.compiler.node.PTypeToken;

final class TypeTokenUtil {
    public static TypeToken fromNode(PTypeToken node) {
        if (node instanceof ABooleanTypeToken) {
            return BooleanTypeToken.INSTANCE;
        } else if (node instanceof ASignedShortTypeToken) {
            return IntegralTypeToken.SIGNED_SHORT;
        } else if (node instanceof AUnsignedShortTypeToken) {
            return IntegralTypeToken.UNSIGNED_SHORT;
        } else if (node instanceof ASignedIntTypeToken) {
            return IntegralTypeToken.SIGNED_INT;
        } else if (node instanceof AUnsignedIntTypeToken) {
            return IntegralTypeToken.UNSIGNED_INT;
        } else if (node instanceof ASignedLongTypeToken) {
            return IntegralTypeToken.SIGNED_LONG;
        } else if (node instanceof AUnsignedLongTypeToken) {
            return IntegralTypeToken.UNSIGNED_LONG;
        } else if (node instanceof AUserDefinedTypeToken) {
            return new UserDefinedTypeToken(((AUserDefinedTypeToken) node).getIdentifier().getText());
        } else if (node instanceof AArrayTypeToken) {
            return new ArrayTypeToken(fromNode(((AArrayTypeToken) node).getElementType()));
        } else {
            throw new UnsupportedOperationException("unknown flavor of type token");
        }
    }

    private TypeTokenUtil() {
    }
}

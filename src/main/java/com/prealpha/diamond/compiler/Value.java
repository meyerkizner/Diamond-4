/*
 * Value.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

enum Value {
    A, B, C, X, Y, Z, I, J, P_A, P_B, P_C, P_X, P_Y, P_Z, P_I, P_J, PA_A(true), PA_B(true), PA_C(true), PA_X(true),
    PA_Y(true), PA_Z(true), PA_I(true), PA_J(true), PUSH_POP, PEEK, PICK(true), SP, PC, EX, P_LITERAL(true),
    LITERAL(true);

    private final byte value;

    private final boolean needsNextWord;

    private Value() {
        value = (byte) ordinal();
        needsNextWord = false;
    }

    private Value(boolean needsNextWord) {
        value = (byte) ordinal();
        this.needsNextWord = needsNextWord;
    }

    public byte getValue() {
        return value;
    }

    public boolean needsNextWord() {
        return needsNextWord;
    }
}

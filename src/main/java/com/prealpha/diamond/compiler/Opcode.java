/*
 * Opcode.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface Opcode {
    byte getValue();

    boolean isSpecial();
}

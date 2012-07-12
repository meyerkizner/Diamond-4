/*
 * SpecialOpcode.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

enum SpecialOpcode implements Opcode {
    JSR(0x01), INT(0x08), IAG(0x09), IAS(0x0a), RFI(0x0b), IAQ(0x0c), HWN(0x10), HWQ(0x11), HWI(0x12);

    private final byte value;

    private SpecialOpcode(int value) {
        this.value = (byte) value;
    }

    @Override
    public byte getValue() {
        return value;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}

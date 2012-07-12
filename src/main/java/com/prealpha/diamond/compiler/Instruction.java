/*
 * Instruction.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import static com.google.common.base.Preconditions.*;

final class Instruction {
    private final Opcode opcode;

    private final Value a;

    private final String aNextWord;

    private final Value b;

    private final String bNextWord;

    public Instruction(Opcode opcode, Value a) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkArgument(opcode.isSpecial());
        checkArgument(!a.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = null;
        this.b = null;
        this.bNextWord = null;
    }

    public Instruction(Opcode opcode, Value a, String aNextWord) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkArgument(opcode.isSpecial());
        checkArgument(a.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = aNextWord;
        this.b = null;
        this.bNextWord = null;
    }

    public Instruction(Opcode opcode, Value b, Value a) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkNotNull(b);
        checkArgument(!opcode.isSpecial());
        checkArgument(!a.needsNextWord());
        checkArgument(!b.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = null;
        this.b = b;
        this.bNextWord = null;
    }

    public Instruction(Opcode opcode, Value b, String bNextWord, Value a) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkNotNull(b);
        checkNotNull(bNextWord);
        checkArgument(!opcode.isSpecial());
        checkArgument(!a.needsNextWord());
        checkArgument(b.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = null;
        this.b = b;
        this.bNextWord = bNextWord;
    }

    public Instruction(Opcode opcode, Value b, Value a, String aNextWord) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkNotNull(aNextWord);
        checkNotNull(b);
        checkArgument(!opcode.isSpecial());
        checkArgument(a.needsNextWord());
        checkArgument(!b.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = aNextWord;
        this.b = b;
        this.bNextWord = null;
    }

    public Instruction(Opcode opcode, Value b, String bNextWord, Value a, String aNextWord) {
        checkNotNull(opcode);
        checkNotNull(a);
        checkNotNull(aNextWord);
        checkNotNull(b);
        checkNotNull(bNextWord);
        checkArgument(!opcode.isSpecial());
        checkArgument(a.needsNextWord());
        checkArgument(b.needsNextWord());
        this.opcode = opcode;
        this.a = a;
        this.aNextWord = aNextWord;
        this.b = b;
        this.bNextWord = bNextWord;
    }
}

/*
 * FlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

interface FlowModifier {
    boolean onBreak();

    boolean onContinue();

    boolean onReturn();
}

/*
 * FlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

/*
 * TODO: these would really be better off as anonymous classes that extend an adapter class
 */
interface FlowStructure {
    boolean onBreak();

    boolean onContinue();

    boolean onReturn();
}

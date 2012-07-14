/*
 * DoFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ADoStatement;

import static com.google.common.base.Preconditions.*;

final class DoFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final ADoStatement doStatement;

    public DoFlowStructure(CodeGenerator codeGenerator, ADoStatement doStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(doStatement);
        this.codeGenerator = codeGenerator;
        this.doStatement = doStatement;
    }

    @Override
    public boolean onBreak() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(doStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainEndLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onContinue() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(doStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainStartLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

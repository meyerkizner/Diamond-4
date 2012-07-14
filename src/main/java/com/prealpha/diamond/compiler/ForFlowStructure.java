/*
 * ForFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AForStatement;

import static com.google.common.base.Preconditions.*;

final class ForFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final AForStatement forStatement;

    public ForFlowStructure(CodeGenerator codeGenerator, AForStatement forStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(forStatement);
        this.codeGenerator = codeGenerator;
        this.forStatement = forStatement;
    }

    @Override
    public boolean onBreak() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(forStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainEndLabel(forStatement.getBody()));
        return true;
    }

    @Override
    public boolean onContinue() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(forStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainStartLabel(forStatement.getUpdate()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

/*
 * WhileFlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AWhileStatement;

import static com.google.common.base.Preconditions.*;

final class WhileFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final AWhileStatement whileStatement;

    public WhileFlowStructure(CodeGenerator codeGenerator, AWhileStatement whileStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(whileStatement);
        this.codeGenerator = codeGenerator;
        this.whileStatement = whileStatement;
    }

    @Override
    public boolean onBreak() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(whileStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.getEndLabel(whileStatement.getBody()));
        return true;
    }

    @Override
    public boolean onContinue() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(whileStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.getStartLabel(whileStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

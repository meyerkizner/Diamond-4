/*
 * ForFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

final class ForFlowModifier implements FlowModifier {
    private final CodeGenerator codeGenerator;

    private final AForStatement forStatement;

    public ForFlowModifier(CodeGenerator codeGenerator, AForStatement forStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(forStatement);
        this.codeGenerator = codeGenerator;
        this.forStatement = forStatement;
    }

    @Override
    public boolean onBreak(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(forStatement)) {
            codeGenerator.reclaimScope(codeGenerator.getScope());
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainEndLabel(forStatement.getBody()));
        return true;
    }

    @Override
    public boolean onContinue(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(forStatement)) {
            codeGenerator.reclaimScope(codeGenerator.getScope());
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainStartLabel(forStatement.getUpdate()));
        return true;
    }

    @Override
    public boolean onReturn(Node context) {
        return false;
    }
}

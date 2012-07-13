/*
 * DoFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

final class DoFlowModifier implements FlowModifier {
    private final CodeGenerator codeGenerator;

    private final ADoStatement doStatement;

    public DoFlowModifier(CodeGenerator codeGenerator, ADoStatement doStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(doStatement);
        this.codeGenerator = codeGenerator;
        this.doStatement = doStatement;
    }

    @Override
    public boolean onBreak(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(doStatement)) {
            codeGenerator.reclaimScope(context, codeGenerator.getScope());
        }
        codeGenerator.jumpTo(context, codeGenerator.obtainEndLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onContinue(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(doStatement)) {
            codeGenerator.reclaimScope(context, codeGenerator.getScope());
        }
        codeGenerator.jumpTo(context, codeGenerator.obtainStartLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onReturn(Node context) {
        return false;
    }
}

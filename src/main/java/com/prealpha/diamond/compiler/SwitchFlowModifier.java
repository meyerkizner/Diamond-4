/*
 * SwitchFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

final class SwitchFlowModifier implements FlowModifier {
    private final CodeGenerator codeGenerator;

    private final ASwitchStatement switchStatement;

    public SwitchFlowModifier(CodeGenerator codeGenerator, ASwitchStatement switchStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(switchStatement);
        this.codeGenerator = codeGenerator;
        this.switchStatement = switchStatement;
    }

    @Override
    public boolean onBreak(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(switchStatement)) {
            codeGenerator.reclaimScope(context, codeGenerator.getScope());
        }
        codeGenerator.jumpTo(context, codeGenerator.obtainEndLabel(switchStatement.getBody().descendingIterator().next()));
        return true;
    }

    @Override
    public boolean onContinue(Node context) {
        return false;
    }

    @Override
    public boolean onReturn(Node context) {
        return false;
    }
}

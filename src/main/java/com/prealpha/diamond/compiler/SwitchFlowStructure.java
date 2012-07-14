/*
 * SwitchFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ASwitchStatement;

import static com.google.common.base.Preconditions.*;

final class SwitchFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final ASwitchStatement switchStatement;

    public SwitchFlowStructure(CodeGenerator codeGenerator, ASwitchStatement switchStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(switchStatement);
        this.codeGenerator = codeGenerator;
        this.switchStatement = switchStatement;
    }

    @Override
    public boolean onBreak() {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(switchStatement)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.write("SET PC " + codeGenerator.obtainEndLabel(switchStatement.getBody().descendingIterator().next()));
        return true;
    }

    @Override
    public boolean onContinue() {
        return false;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

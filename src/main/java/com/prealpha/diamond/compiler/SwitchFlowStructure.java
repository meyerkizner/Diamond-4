/*
 * SwitchFlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ASwitchStatement;

import static com.google.common.base.Preconditions.*;

final class SwitchFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final ASwitchStatement switchStatement;

    private final Scope enclosingScope;

    public SwitchFlowStructure(CodeGenerator codeGenerator, ASwitchStatement switchStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(switchStatement);
        this.codeGenerator = codeGenerator;
        this.switchStatement = switchStatement;
        this.enclosingScope = this.codeGenerator.getScope();
    }

    @Override
    public boolean onBreak() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.doReclaimScope(scope);
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getEndLabel(switchStatement.getBody().descendingIterator().next()));
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

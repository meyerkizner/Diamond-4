/*
 * DoFlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.ADoStatement;

import static com.google.common.base.Preconditions.*;

final class DoFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final ADoStatement doStatement;

    private final Scope enclosingScope;

    public DoFlowStructure(CodeGenerator codeGenerator, ADoStatement doStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(doStatement);
        this.codeGenerator = codeGenerator;
        this.doStatement = doStatement;
        this.enclosingScope = this.codeGenerator.getScope();
    }

    @Override
    public boolean onBreak() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.doReclaimScope(scope);
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getEndLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onContinue() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.doReclaimScope(scope);
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getStartLabel(doStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

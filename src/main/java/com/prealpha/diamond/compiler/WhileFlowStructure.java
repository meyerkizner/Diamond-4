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

    private final Scope enclosingScope;

    public WhileFlowStructure(CodeGenerator codeGenerator, AWhileStatement whileStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(whileStatement);
        this.codeGenerator = codeGenerator;
        this.whileStatement = whileStatement;
        this.enclosingScope = this.codeGenerator.getScope();
    }

    @Override
    public boolean onBreak() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.doReclaimScope(scope);
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getEndLabel(whileStatement.getBody()));
        return true;
    }

    @Override
    public boolean onContinue() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.doReclaimScope(scope);
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getStartLabel(whileStatement.getCondition()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

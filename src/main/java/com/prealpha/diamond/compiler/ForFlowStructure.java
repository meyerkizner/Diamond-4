/*
 * ForFlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.AForStatement;

import static com.google.common.base.Preconditions.*;

final class ForFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final AForStatement forStatement;

    private final Scope enclosingScope;

    public ForFlowStructure(CodeGenerator codeGenerator, AForStatement forStatement) {
        checkNotNull(codeGenerator);
        checkNotNull(forStatement);
        this.codeGenerator = codeGenerator;
        this.forStatement = forStatement;
        this.enclosingScope = this.codeGenerator.getScope();
    }

    @Override
    public boolean onBreak() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.reclaimScope();
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getEndLabel(forStatement.getBody()));
        return true;
    }

    @Override
    public boolean onContinue() {
        Scope scope = codeGenerator.getScope();
        while (scope != enclosingScope) {
            codeGenerator.reclaimScope();
            scope = scope.getParent();
        }
        codeGenerator.write("SET PC " + codeGenerator.getStartLabel(forStatement.getUpdate()));
        return true;
    }

    @Override
    public boolean onReturn() {
        return false;
    }
}

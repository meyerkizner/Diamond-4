/*
 * FunctionFlowStructure.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

final class ParametrizedFlowStructure implements FlowStructure {
    private final CodeGenerator codeGenerator;

    private final Scope enclosingScope;

    public ParametrizedFlowStructure(CodeGenerator codeGenerator) {
        checkNotNull(codeGenerator);
        this.codeGenerator = codeGenerator;
        this.enclosingScope = this.codeGenerator.getScope();
    }

    @Override
    public boolean onBreak() {
        return false;
    }

    @Override
    public boolean onContinue() {
        return false;
    }

    @Override
    public boolean onReturn() {
        while (codeGenerator.getScope() != enclosingScope) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.reclaimScope();
        codeGenerator.write("SET PC POP");
        return true;
    }
}

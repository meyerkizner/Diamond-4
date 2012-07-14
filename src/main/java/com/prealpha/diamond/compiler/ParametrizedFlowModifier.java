/*
 * FunctionFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

final class ParametrizedFlowModifier implements FlowModifier {
    private final CodeGenerator codeGenerator;

    private final Node parametrizedDeclaration;

    public ParametrizedFlowModifier(CodeGenerator codeGenerator, Node parametrizedDeclaration) {
        checkNotNull(codeGenerator);
        checkNotNull(parametrizedDeclaration);
        this.codeGenerator = codeGenerator;
        this.parametrizedDeclaration = parametrizedDeclaration;
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
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(parametrizedDeclaration)) {
            codeGenerator.reclaimScope();
        }
        codeGenerator.reclaimScope();
        codeGenerator.write("SET PC POP");
        return true;
    }
}

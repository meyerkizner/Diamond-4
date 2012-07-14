/*
 * FunctionFlowModifier.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PFunctionDeclaration;

import static com.google.common.base.Preconditions.*;

final class FunctionFlowModifier implements FlowModifier {
    private final CodeGenerator codeGenerator;

    private final PFunctionDeclaration functionDeclaration;

    public FunctionFlowModifier(CodeGenerator codeGenerator, PFunctionDeclaration functionDeclaration) {
        checkNotNull(codeGenerator);
        checkNotNull(functionDeclaration);
        this.codeGenerator = codeGenerator;
        this.functionDeclaration = functionDeclaration;
    }

    @Override
    public boolean onBreak(Node context) {
        return false;
    }

    @Override
    public boolean onContinue(Node context) {
        return false;
    }

    @Override
    public boolean onReturn(Node context) {
        while (codeGenerator.getScope() != codeGenerator.getEnclosingScope(functionDeclaration)) {
            codeGenerator.reclaimScope(context, codeGenerator.getScope());
        }
        codeGenerator.reclaimScope(context, codeGenerator.getScope());
        codeGenerator.jumpTo(context, "POP");
        return true;
    }
}

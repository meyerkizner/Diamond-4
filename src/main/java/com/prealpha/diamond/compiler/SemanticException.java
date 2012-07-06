/*
 * SemanticException.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ImmutableList;
import com.prealpha.diamond.compiler.node.Node;

import java.util.List;

import static com.google.common.base.Preconditions.*;

public final class SemanticException extends Exception {
    private final Node node;

    private final List<SemanticException> causes;

    SemanticException(Node node, String message) {
        super(message);
        checkNotNull(node);
        this.node = node;
        this.causes = null;
    }

    SemanticException(String message) {
        super(message);
        this.node = null;
        this.causes = null;
    }

    SemanticException(List<SemanticException> causes) {
        super();
        this.node = null;
        this.causes = ImmutableList.copyOf(causes);
    }

    public Node getNode() {
        return node;
    }

    public List<SemanticException> getCauses() {
        return causes;
    }
}

/*
 * SemanticException.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.node.Node;

import static com.google.common.base.Preconditions.*;

public final class SemanticException extends Exception {
    private final Node node;

    SemanticException(Node node, String message) {
        super(getLineNumberPrefix(node) + message);
        checkNotNull(node);
        this.node = node;
    }

    SemanticException(String message) {
        super(message);
        this.node = null;
    }

    public Node getNode() {
        return node;
    }

    private static String getLineNumberPrefix(Node node) {
        LineNumberFinder lineNumberFinder = new LineNumberFinder();
        node.apply(lineNumberFinder);
        if (lineNumberFinder.hasLineNumber()) {
            return String.format("[%d,%d] ", lineNumberFinder.getLineNumber(), lineNumberFinder.getColumnNumber());
        } else {
            return "";
        }
    }
}

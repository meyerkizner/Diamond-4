/*
 * NoHeapException.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

final class NoHeapException extends RuntimeException {
    public NoHeapException() {
        super("reference types and allocation on the heap are not yet implemented");
    }
}

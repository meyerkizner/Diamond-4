/*
 * IncludeProcessor.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.lexer.Lexer;
import com.prealpha.diamond.compiler.lexer.LexerException;
import com.prealpha.diamond.compiler.node.AInclude;
import com.prealpha.diamond.compiler.node.AIncludeTopLevelStatement;
import com.prealpha.diamond.compiler.parser.Parser;
import com.prealpha.diamond.compiler.parser.ParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;

import static com.google.common.base.Preconditions.*;

final class IncludeProcessor extends DepthFirstAdapter {
    private final List<Exception> exceptionBuffer;

    private final File mainFile;

    public IncludeProcessor(List<Exception> exceptionBuffer, File mainFile) {
        checkNotNull(exceptionBuffer);
        checkNotNull(mainFile);
        this.exceptionBuffer = exceptionBuffer;
        this.mainFile = mainFile;
    }

    @Override
    public void outAIncludeTopLevelStatement(AIncludeTopLevelStatement include) {
        try {
            String fileName = ((AInclude) include.getInclude()).getFileName().getText();
            File file = new File(mainFile.getCanonicalPath() + fileName);
            PushbackReader reader = new PushbackReader(new FileReader(file));
            Parser parser = new Parser(new Lexer(reader));
            include.replaceBy(parser.parse().getPProgram());
        } catch (IOException iox) {
            exceptionBuffer.add(iox);
        } catch (LexerException lx) {
            exceptionBuffer.add(lx);
        } catch (ParserException px) {
            exceptionBuffer.add(px);
        }
    }
}

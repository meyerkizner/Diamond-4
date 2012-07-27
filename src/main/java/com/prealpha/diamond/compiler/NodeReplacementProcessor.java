/*
 * IncludeProcessor.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.lexer.Lexer;
import com.prealpha.diamond.compiler.lexer.LexerException;
import com.prealpha.diamond.compiler.node.AAddAssignment;
import com.prealpha.diamond.compiler.node.AAddExpression;
import com.prealpha.diamond.compiler.node.AArrayAccessAssignmentTarget;
import com.prealpha.diamond.compiler.node.AArrayAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.AAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseAndAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseAndExpression;
import com.prealpha.diamond.compiler.node.ABitwiseOrAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseOrExpression;
import com.prealpha.diamond.compiler.node.ABitwiseXorAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseXorExpression;
import com.prealpha.diamond.compiler.node.ADivideAssignment;
import com.prealpha.diamond.compiler.node.ADivideExpression;
import com.prealpha.diamond.compiler.node.AIdentifierAssignmentTarget;
import com.prealpha.diamond.compiler.node.AIdentifierPrimaryExpression;
import com.prealpha.diamond.compiler.node.AInclude;
import com.prealpha.diamond.compiler.node.AIncludeTopLevelStatement;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclarationAssignmentTarget;
import com.prealpha.diamond.compiler.node.AModulusAssignment;
import com.prealpha.diamond.compiler.node.AModulusExpression;
import com.prealpha.diamond.compiler.node.AMultiplyAssignment;
import com.prealpha.diamond.compiler.node.AMultiplyExpression;
import com.prealpha.diamond.compiler.node.APrimaryExpression;
import com.prealpha.diamond.compiler.node.AQualifiedNameAssignmentTarget;
import com.prealpha.diamond.compiler.node.AQualifiedNamePrimaryExpression;
import com.prealpha.diamond.compiler.node.AShiftLeftAssignment;
import com.prealpha.diamond.compiler.node.AShiftLeftExpression;
import com.prealpha.diamond.compiler.node.AShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AShiftRightExpression;
import com.prealpha.diamond.compiler.node.ASubtractAssignment;
import com.prealpha.diamond.compiler.node.ASubtractExpression;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightExpression;
import com.prealpha.diamond.compiler.node.PArrayAccess;
import com.prealpha.diamond.compiler.node.PAssignmentTarget;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PPrimaryExpression;
import com.prealpha.diamond.compiler.node.PQualifiedName;
import com.prealpha.diamond.compiler.node.TIdentifier;
import com.prealpha.diamond.compiler.parser.Parser;
import com.prealpha.diamond.compiler.parser.ParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.List;

import static com.google.common.base.Preconditions.*;

final class NodeReplacementProcessor extends DepthFirstAdapter {
    private final List<Exception> exceptionBuffer;

    private final File mainFile;

    public NodeReplacementProcessor(List<Exception> exceptionBuffer, File mainFile) {
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

    @Override
    public void outAAddAssignment(AAddAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AAddExpression addExpression = new AAddExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), addExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outASubtractAssignment(ASubtractAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            ASubtractExpression subtractExpression = new ASubtractExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), subtractExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAMultiplyAssignment(AMultiplyAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AMultiplyExpression multiplyExpression = new AMultiplyExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), multiplyExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outADivideAssignment(ADivideAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            ADivideExpression divideExpression = new ADivideExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), divideExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAModulusAssignment(AModulusAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AModulusExpression modulusExpression = new AModulusExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), modulusExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outABitwiseAndAssignment(ABitwiseAndAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            ABitwiseAndExpression bitwiseAndExpression = new ABitwiseAndExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), bitwiseAndExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outABitwiseXorAssignment(ABitwiseXorAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            ABitwiseXorExpression bitwiseXorExpression = new ABitwiseXorExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), bitwiseXorExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outABitwiseOrAssignment(ABitwiseOrAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            ABitwiseOrExpression bitwiseOrExpression = new ABitwiseOrExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), bitwiseOrExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAShiftLeftAssignment(AShiftLeftAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AShiftLeftExpression shiftLeftExpression = new AShiftLeftExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), shiftLeftExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAShiftRightAssignment(AShiftRightAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AShiftRightExpression shiftRightExpression = new AShiftRightExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), shiftRightExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAUnsignedShiftRightAssignment(AUnsignedShiftRightAssignment assignment) {
        try {
            PPrimaryExpression primaryExpression = getExpressionFromTarget(assignment.getTarget());
            AUnsignedShiftRightExpression unsignedShiftRightExpression = new AUnsignedShiftRightExpression(new APrimaryExpression(primaryExpression), assignment.getValue());
            assignment.replaceBy(new AAssignment(assignment.getTarget(), unsignedShiftRightExpression));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    private PPrimaryExpression getExpressionFromTarget(PAssignmentTarget target) throws SemanticException {
        if (target instanceof AIdentifierAssignmentTarget) {
            TIdentifier identifier = ((AIdentifierAssignmentTarget) target).getIdentifier();
            return new AIdentifierPrimaryExpression((TIdentifier) identifier.clone());
        } else if (target instanceof AQualifiedNameAssignmentTarget) {
            PQualifiedName qualifiedName = ((AQualifiedNameAssignmentTarget) target).getQualifiedName();
            return new AQualifiedNamePrimaryExpression((PQualifiedName) qualifiedName.clone());
        } else if (target instanceof AArrayAccessAssignmentTarget) {
            PArrayAccess arrayAccess = ((AArrayAccessAssignmentTarget) target).getArrayAccess();
            return new AArrayAccessPrimaryExpression((PArrayAccess) arrayAccess.clone());
        } else if (target instanceof ALocalDeclarationAssignmentTarget) {
            PLocalDeclaration localDeclaration = ((ALocalDeclarationAssignmentTarget) target).getLocalDeclaration();
            TIdentifier identifier = ((ALocalDeclaration) localDeclaration).getName();
            return new AIdentifierPrimaryExpression((TIdentifier) identifier.clone());
        } else {
            throw new SemanticException("unknown assignment target flavor");
        }
    }
}
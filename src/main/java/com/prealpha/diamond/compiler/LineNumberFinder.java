/*
 * LineNumberFinder.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.prealpha.diamond.compiler.analysis.AnalysisAdapter;
import com.prealpha.diamond.compiler.node.AAddAssignment;
import com.prealpha.diamond.compiler.node.AAddExpression;
import com.prealpha.diamond.compiler.node.AArrayAccess;
import com.prealpha.diamond.compiler.node.AArrayAccessAssignmentTarget;
import com.prealpha.diamond.compiler.node.AArrayAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.AArrayTypeToken;
import com.prealpha.diamond.compiler.node.AAssignment;
import com.prealpha.diamond.compiler.node.AAssignmentExpression;
import com.prealpha.diamond.compiler.node.ABinaryIntegralLiteral;
import com.prealpha.diamond.compiler.node.ABitwiseAndAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseAndExpression;
import com.prealpha.diamond.compiler.node.ABitwiseComplementExpression;
import com.prealpha.diamond.compiler.node.ABitwiseOrAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseOrExpression;
import com.prealpha.diamond.compiler.node.ABitwiseXorAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseXorExpression;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.ABooleanTypeToken;
import com.prealpha.diamond.compiler.node.ABreakStatement;
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AClassTopLevelStatement;
import com.prealpha.diamond.compiler.node.AConditionalAndExpression;
import com.prealpha.diamond.compiler.node.AConditionalExpression;
import com.prealpha.diamond.compiler.node.AConditionalNotExpression;
import com.prealpha.diamond.compiler.node.AConditionalOrExpression;
import com.prealpha.diamond.compiler.node.AConstructorClassStatement;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorInvocation;
import com.prealpha.diamond.compiler.node.AConstructorInvocationPrimaryExpression;
import com.prealpha.diamond.compiler.node.AContinueStatement;
import com.prealpha.diamond.compiler.node.ADecimalIntegralLiteral;
import com.prealpha.diamond.compiler.node.ADefaultCaseGroup;
import com.prealpha.diamond.compiler.node.ADeleteStatement;
import com.prealpha.diamond.compiler.node.ADivideAssignment;
import com.prealpha.diamond.compiler.node.ADivideExpression;
import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.AEmptyStatement;
import com.prealpha.diamond.compiler.node.AEqualExpression;
import com.prealpha.diamond.compiler.node.AExpressionFieldAccess;
import com.prealpha.diamond.compiler.node.AExpressionFunctionInvocation;
import com.prealpha.diamond.compiler.node.AExpressionStatement;
import com.prealpha.diamond.compiler.node.AFalseLiteral;
import com.prealpha.diamond.compiler.node.AFieldAccessAssignmentTarget;
import com.prealpha.diamond.compiler.node.AFieldAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.AFieldClassStatement;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AFunctionClassStatement;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AFunctionInvocationPrimaryExpression;
import com.prealpha.diamond.compiler.node.AFunctionTopLevelStatement;
import com.prealpha.diamond.compiler.node.AGreaterOrEqualExpression;
import com.prealpha.diamond.compiler.node.AGreaterThanExpression;
import com.prealpha.diamond.compiler.node.AHexIntegralLiteral;
import com.prealpha.diamond.compiler.node.AIdentifierAssignmentTarget;
import com.prealpha.diamond.compiler.node.AIdentifierPrimaryExpression;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AIncludeTopLevelStatement;
import com.prealpha.diamond.compiler.node.AIntTypeToken;
import com.prealpha.diamond.compiler.node.AIntegralLiteral;
import com.prealpha.diamond.compiler.node.ALessOrEqualExpression;
import com.prealpha.diamond.compiler.node.ALessThanExpression;
import com.prealpha.diamond.compiler.node.ALiteralPrimaryExpression;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclarationAssignmentTarget;
import com.prealpha.diamond.compiler.node.AModulusAssignment;
import com.prealpha.diamond.compiler.node.AModulusExpression;
import com.prealpha.diamond.compiler.node.AMultiplyAssignment;
import com.prealpha.diamond.compiler.node.AMultiplyExpression;
import com.prealpha.diamond.compiler.node.ANotEqualExpression;
import com.prealpha.diamond.compiler.node.ANumericNegationExpression;
import com.prealpha.diamond.compiler.node.AOctalIntegralLiteral;
import com.prealpha.diamond.compiler.node.AParentheticalPrimaryExpression;
import com.prealpha.diamond.compiler.node.APrimaryExpression;
import com.prealpha.diamond.compiler.node.APrivateModifier;
import com.prealpha.diamond.compiler.node.AProgram;
import com.prealpha.diamond.compiler.node.AReturnStatement;
import com.prealpha.diamond.compiler.node.AShiftLeftAssignment;
import com.prealpha.diamond.compiler.node.AShiftLeftExpression;
import com.prealpha.diamond.compiler.node.AShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AShiftRightExpression;
import com.prealpha.diamond.compiler.node.AStandardInclude;
import com.prealpha.diamond.compiler.node.AStaticModifier;
import com.prealpha.diamond.compiler.node.AStringLiteral;
import com.prealpha.diamond.compiler.node.ASubtractAssignment;
import com.prealpha.diamond.compiler.node.ASubtractExpression;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AThisPrimaryExpression;
import com.prealpha.diamond.compiler.node.ATrueLiteral;
import com.prealpha.diamond.compiler.node.ATypeTokenFieldAccess;
import com.prealpha.diamond.compiler.node.ATypeTokenFunctionInvocation;
import com.prealpha.diamond.compiler.node.AUintTypeToken;
import com.prealpha.diamond.compiler.node.AUnqualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightExpression;
import com.prealpha.diamond.compiler.node.AUserDefinedTypeToken;
import com.prealpha.diamond.compiler.node.AUserInclude;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.Start;
import com.prealpha.diamond.compiler.node.Token;

final class LineNumberFinder extends AnalysisAdapter {
    private Token firstToken;

    public LineNumberFinder() {
    }

    public boolean hasLineNumber() {
        return (firstToken != null);
    }

    public int getLineNumber() {
        return firstToken.getLine();
    }

    public int getColumnNumber() {
        return firstToken.getPos();
    }

    public void caseStart(Start node) {
        node.getPProgram().apply(this);
    }

    public void caseAProgram(AProgram node) {
        node.getTopLevelStatement().iterator().next().apply(this);
    }

    public void caseAClassTopLevelStatement(AClassTopLevelStatement node) {
        node.getClassDeclaration().apply(this);
    }

    public void caseAFunctionTopLevelStatement(AFunctionTopLevelStatement node) {
        node.getFunctionDeclaration().apply(this);
    }

    public void caseAIncludeTopLevelStatement(AIncludeTopLevelStatement node) {
        node.getInclude().apply(this);
    }

    public void caseAIfThenStatement(AIfThenStatement node) {
        node.getCondition().apply(this);
    }

    public void caseAIfThenElseStatement(AIfThenElseStatement node) {
        node.getCondition().apply(this);
    }

    public void caseAWhileStatement(AWhileStatement node) {
        node.getCondition().apply(this);
    }

    public void caseAForStatement(AForStatement node) {
        node.getInit().apply(this);
    }

    public void caseADoStatement(ADoStatement node) {
        node.getBody().apply(this);
    }

    public void caseASwitchStatement(ASwitchStatement node) {
        node.getValue().apply(this);
    }

    public void caseADeleteStatement(ADeleteStatement node) {
        node.getObject().apply(this);
    }

    public void caseABreakStatement(ABreakStatement node) {
    }

    public void caseAContinueStatement(AContinueStatement node) {
    }

    public void caseAReturnStatement(AReturnStatement node) {
        node.getReturnValue().apply(this);
    }

    public void caseAExpressionStatement(AExpressionStatement node) {
        node.getExpression().apply(this);
    }

    public void caseABlockStatement(ABlockStatement node) {
        node.getStatement().iterator().next().apply(this);
    }

    public void caseAEmptyStatement(AEmptyStatement node) {
    }

    public void caseACaseGroup(ACaseGroup node) {
        node.getValues().iterator().next().apply(this);
    }

    public void caseADefaultCaseGroup(ADefaultCaseGroup node) {
        if (!node.getValues().isEmpty()) {
            node.getValues().iterator().next().apply(this);
        }
    }

    public void caseAClassDeclaration(AClassDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getName().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseAFieldClassStatement(AFieldClassStatement node) {
        node.getFieldDeclaration().apply(this);
    }

    public void caseAFunctionClassStatement(AFunctionClassStatement node) {
        node.getFunctionDeclaration().apply(this);
    }

    public void caseAConstructorClassStatement(AConstructorClassStatement node) {
        node.getConstructorDeclaration().apply(this);
    }

    public void caseAFieldDeclaration(AFieldDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getType().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseAFunctionDeclaration(AFunctionDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getReturnType().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseAVoidFunctionDeclaration(AVoidFunctionDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getName().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseAConstructorDeclaration(AConstructorDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getReturnType().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseALocalDeclaration(ALocalDeclaration node) {
        if (node.getModifiers().isEmpty()) {
            node.getType().apply(this);
        } else {
            node.getModifiers().iterator().next().apply(this);
        }
    }

    public void caseAUserInclude(AUserInclude node) {
        node.getFileName().apply(this);
    }

    public void caseAStandardInclude(AStandardInclude node) {
        node.getClassName().apply(this);
    }

    public void caseALiteralPrimaryExpression(ALiteralPrimaryExpression node) {
        node.getLiteral().apply(this);
    }

    public void caseAIdentifierPrimaryExpression(AIdentifierPrimaryExpression node) {
        node.getIdentifier().apply(this);
    }

    public void caseAThisPrimaryExpression(AThisPrimaryExpression node) {
    }

    public void caseAParentheticalPrimaryExpression(AParentheticalPrimaryExpression node) {
        node.getExpression().apply(this);
    }

    public void caseAFunctionInvocationPrimaryExpression(AFunctionInvocationPrimaryExpression node) {
        node.getFunctionInvocation().apply(this);
    }

    public void caseAConstructorInvocationPrimaryExpression(AConstructorInvocationPrimaryExpression node) {
        node.getConstructorInvocation().apply(this);
    }

    public void caseAFieldAccessPrimaryExpression(AFieldAccessPrimaryExpression node) {
        node.getFieldAccess().apply(this);
    }
    
    public void caseAArrayAccessPrimaryExpression(AArrayAccessPrimaryExpression node) {
        node.getArrayAccess().apply(this);
    }
    
    public void caseAIntegralLiteral(AIntegralLiteral node) {
        node.getIntegralLiteral().apply(this);
    }
    
    public void caseAStringLiteral(AStringLiteral node) {
        node.getStringLiteral().apply(this);
    }
    
    public void caseATrueLiteral(ATrueLiteral node) {
    }
    
    public void caseAFalseLiteral(AFalseLiteral node) {
    }
    
    public void caseADecimalIntegralLiteral(ADecimalIntegralLiteral node) {
        node.getDecimalLiteral().apply(this);
    }
    
    public void caseAHexIntegralLiteral(AHexIntegralLiteral node) {
        node.getHexLiteral().apply(this);
    }
    
    public void caseAOctalIntegralLiteral(AOctalIntegralLiteral node) {
        node.getOctalLiteral().apply(this);
    }
    
    public void caseABinaryIntegralLiteral(ABinaryIntegralLiteral node) {
        node.getBinaryLiteral().apply(this);
    }
    
    public void caseAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation node) {
        node.getFunctionName().apply(this);
    }
    
    public void caseAExpressionFunctionInvocation(AExpressionFunctionInvocation node) {
        node.getTarget().apply(this);
    }

    public void caseATypeTokenFunctionInvocation(ATypeTokenFunctionInvocation node) {
        node.getTarget().apply(this);
    }
    
    public void caseAConstructorInvocation(AConstructorInvocation node) {
        node.getTarget().apply(this);
    }

    public void caseAExpressionFieldAccess(AExpressionFieldAccess node) {
        node.getTarget().apply(this);
    }

    public void caseATypeTokenFieldAccess(ATypeTokenFieldAccess node) {
        node.getTarget().apply(this);
    }

    public void caseAArrayAccess(AArrayAccess node) {
        node.getArray().apply(this);
    }
    
    public void caseAPrimaryExpression(APrimaryExpression node) {
        node.getPrimaryExpression().apply(this);
    }
    
    public void caseANumericNegationExpression(ANumericNegationExpression node) {
        node.getValue().apply(this);
    }
    
    public void caseAConditionalNotExpression(AConditionalNotExpression node) {
        node.getValue().apply(this);
    }
    
    public void caseABitwiseComplementExpression(ABitwiseComplementExpression node) {
        node.getValue().apply(this);
    }
    
    public void caseAMultiplyExpression(AMultiplyExpression node) {
        node.getLeft().apply(this);
    }
    
    public void caseADivideExpression(ADivideExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAModulusExpression(AModulusExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAAddExpression(AAddExpression node) {
        node.getLeft().apply(this);
    }

    public void caseASubtractExpression(ASubtractExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAShiftLeftExpression(AShiftLeftExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAShiftRightExpression(AShiftRightExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAUnsignedShiftRightExpression(AUnsignedShiftRightExpression node) {
        node.getLeft().apply(this);
    }

    public void caseALessThanExpression(ALessThanExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAGreaterThanExpression(AGreaterThanExpression node) {
        node.getLeft().apply(this);
    }

    public void caseALessOrEqualExpression(ALessOrEqualExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAGreaterOrEqualExpression(AGreaterOrEqualExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAEqualExpression(AEqualExpression node) {
        node.getLeft().apply(this);
    }

    public void caseANotEqualExpression(ANotEqualExpression node) {
        node.getLeft().apply(this);
    }

    public void caseABitwiseAndExpression(ABitwiseAndExpression node) {
        node.getLeft().apply(this);
    }

    public void caseABitwiseXorExpression(ABitwiseXorExpression node) {
        node.getLeft().apply(this);
    }

    public void caseABitwiseOrExpression(ABitwiseOrExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAConditionalAndExpression(AConditionalAndExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAConditionalOrExpression(AConditionalOrExpression node) {
        node.getLeft().apply(this);
    }

    public void caseAConditionalExpression(AConditionalExpression node) {
        node.getCondition().apply(this);
    }
    
    public void caseAAssignmentExpression(AAssignmentExpression node) {
        node.getAssignment().apply(this);
    }
    
    public void caseAAssignment(AAssignment node) {
        node.getTarget().apply(this);
    }
    
    public void caseAAddAssignment(AAddAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseASubtractAssignment(ASubtractAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseAMultiplyAssignment(AMultiplyAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseADivideAssignment(ADivideAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseAModulusAssignment(AModulusAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseABitwiseAndAssignment(ABitwiseAndAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseABitwiseXorAssignment(ABitwiseXorAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseABitwiseOrAssignment(ABitwiseOrAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseAShiftLeftAssignment(AShiftLeftAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseAShiftRightAssignment(AShiftRightAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseAUnsignedShiftRightAssignment(AUnsignedShiftRightAssignment node) {
        node.getTarget().apply(this);
    }

    public void caseALocalDeclarationAssignmentTarget(ALocalDeclarationAssignmentTarget node) {
        node.getLocalDeclaration().apply(this);
    }
    
    public void caseAIdentifierAssignmentTarget(AIdentifierAssignmentTarget node) {
        node.getIdentifier().apply(this);
    }
    
    public void caseAFieldAccessAssignmentTarget(AFieldAccessAssignmentTarget node) {
        node.getFieldAccess().apply(this);
    }
    
    public void caseAArrayAccessAssignmentTarget(AArrayAccessAssignmentTarget node) {
        node.getArrayAccess().apply(this);
    }
    
    public void caseABooleanTypeToken(ABooleanTypeToken node) {
    }
    
    public void caseAIntTypeToken(AIntTypeToken node) {
    }
    
    public void caseAUintTypeToken(AUintTypeToken node) {
    }

    public void caseAUserDefinedTypeToken(AUserDefinedTypeToken node) {
        node.getIdentifier().apply(this);
    }

    public void caseAArrayTypeToken(AArrayTypeToken node) {
        node.getElementType().apply(this);
    }

    public void caseAPrivateModifier(APrivateModifier node) {
    }

    public void caseAStaticModifier(AStaticModifier node) {
    }

    @Override
    public void defaultCase(Node node) {
        if (node instanceof Token && firstToken == null) {
            firstToken = (Token) node;
        }
    }
}

/*
 * TypeEnforcer.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.prealpha.diamond.compiler.node.AAddAssignment;
import com.prealpha.diamond.compiler.node.AAddExpression;
import com.prealpha.diamond.compiler.node.AAssignment;
import com.prealpha.diamond.compiler.node.AAssignmentExpression;
import com.prealpha.diamond.compiler.node.ABitwiseAndAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseAndExpression;
import com.prealpha.diamond.compiler.node.ABitwiseComplementExpression;
import com.prealpha.diamond.compiler.node.ABitwiseOrAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseOrExpression;
import com.prealpha.diamond.compiler.node.ABitwiseXorAssignment;
import com.prealpha.diamond.compiler.node.ABitwiseXorExpression;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConditionalAndExpression;
import com.prealpha.diamond.compiler.node.AConditionalExpression;
import com.prealpha.diamond.compiler.node.AConditionalNotExpression;
import com.prealpha.diamond.compiler.node.AConditionalOrExpression;
import com.prealpha.diamond.compiler.node.AConstructorInvocation;
import com.prealpha.diamond.compiler.node.ADivideAssignment;
import com.prealpha.diamond.compiler.node.ADivideExpression;
import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.AEqualExpression;
import com.prealpha.diamond.compiler.node.AExpressionAssignmentTarget;
import com.prealpha.diamond.compiler.node.AExpressionQualifiedName;
import com.prealpha.diamond.compiler.node.AFalseLiteral;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AGreaterOrEqualExpression;
import com.prealpha.diamond.compiler.node.AGreaterThanExpression;
import com.prealpha.diamond.compiler.node.AIdentifierPrimaryExpression;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AIntegralLiteral;
import com.prealpha.diamond.compiler.node.ALessOrEqualExpression;
import com.prealpha.diamond.compiler.node.ALessThanExpression;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclarationAssignmentTarget;
import com.prealpha.diamond.compiler.node.AModulusAssignment;
import com.prealpha.diamond.compiler.node.AModulusExpression;
import com.prealpha.diamond.compiler.node.AMultiplyAssignment;
import com.prealpha.diamond.compiler.node.AMultiplyExpression;
import com.prealpha.diamond.compiler.node.ANotEqualExpression;
import com.prealpha.diamond.compiler.node.ANotPointerEqualExpression;
import com.prealpha.diamond.compiler.node.ANumericNegationExpression;
import com.prealpha.diamond.compiler.node.AParentheticalPrimaryExpression;
import com.prealpha.diamond.compiler.node.APointerEqualExpression;
import com.prealpha.diamond.compiler.node.APostDecrementExpression;
import com.prealpha.diamond.compiler.node.APostIncrementExpression;
import com.prealpha.diamond.compiler.node.APreDecrementExpression;
import com.prealpha.diamond.compiler.node.APreIncrementExpression;
import com.prealpha.diamond.compiler.node.APrimaryExpression;
import com.prealpha.diamond.compiler.node.AQualifiedArrayAccess;
import com.prealpha.diamond.compiler.node.AQualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AQualifiedNamePrimaryExpression;
import com.prealpha.diamond.compiler.node.AReturnStatement;
import com.prealpha.diamond.compiler.node.AShiftLeftAssignment;
import com.prealpha.diamond.compiler.node.AShiftLeftExpression;
import com.prealpha.diamond.compiler.node.AShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AShiftRightExpression;
import com.prealpha.diamond.compiler.node.AStringLiteral;
import com.prealpha.diamond.compiler.node.ASubtractAssignment;
import com.prealpha.diamond.compiler.node.ASubtractExpression;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AThisPrimaryExpression;
import com.prealpha.diamond.compiler.node.ATrueLiteral;
import com.prealpha.diamond.compiler.node.ATypeTokenQualifiedName;
import com.prealpha.diamond.compiler.node.AUnqualifiedArrayAccess;
import com.prealpha.diamond.compiler.node.AUnqualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightAssignment;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightExpression;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PClassDeclaration;
import com.prealpha.diamond.compiler.node.PExpression;
import com.prealpha.diamond.compiler.node.PFunctionDeclaration;
import com.prealpha.diamond.compiler.node.PPrimaryExpression;
import com.prealpha.diamond.compiler.node.PQualifiedName;
import com.prealpha.diamond.compiler.node.PTypeToken;
import com.prealpha.diamond.compiler.node.TIdentifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

final class TypeEnforcer extends ScopeAwareWalker {
    private final List<Exception> exceptionBuffer;

    private final Map<Node, TypeToken> types;

    private PClassDeclaration currentClass;

    private PFunctionDeclaration currentFunction;

    public TypeEnforcer(ScopeAwareWalker scopeSource, List<Exception> exceptionBuffer) {
        super(scopeSource);
        checkNotNull(exceptionBuffer);
        this.exceptionBuffer = exceptionBuffer;
        this.types = Maps.newHashMap();
    }

    private void assertAssignableTo(Node node, TypeToken type) {
        if (!types.containsKey(node)) {
            if (exceptionBuffer.isEmpty()) {
                throw new AssertionError("cannot type-check node which was not previously encountered");
            }
        } else if (!types.get(node).isAssignableTo(type)) {
            String message = String.format("expected node with type %s; found %s", type, types.get(node));
            exceptionBuffer.add(new SemanticException(node, message));
        }
    }

    private void assertIntegral(Node node) {
        if (!types.containsKey(node)) {
            if (exceptionBuffer.isEmpty()) {
                throw new AssertionError("cannot type-check node which was not previously encountered");
            }
        } else if (!(types.get(node) instanceof IntegralTypeToken)) {
            String message = String.format("expected node with type <integral>; found %s", types.get(node));
            exceptionBuffer.add(new SemanticException(node, message));
        }
    }

    private void assertNumeric(Node node) {
        if (!types.containsKey(node)) {
            if (exceptionBuffer.isEmpty()) {
                throw new AssertionError("cannot type-check node which was not previously encountered");
            }
        } else if (!types.get(node).isNumeric()) {
            String message = String.format("expected node with type <numeric>; found %s", types.get(node));
            exceptionBuffer.add(new SemanticException(node, message));
        }
    }

    private void assertNumericOrBoolean(Node node) {
        if (!types.containsKey(node)) {
            if (exceptionBuffer.isEmpty()) {
                throw new AssertionError("cannot type-check node which was not previously encountered");
            }
        } else if (!types.get(node).isNumeric()) {
            String message = String.format("expected node with type <numeric, boolean>; found %s", types.get(node));
            exceptionBuffer.add(new SemanticException(node, message));
        }
    }

    private TypeToken assertBinaryNumeric(Node left, Node right) {
        assertNumeric(left);
        assertNumeric(right);
        try {
            return types.get(left).performBinaryOperation(types.get(right));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
            return null;
        }
    }

    private TypeToken assertBinaryNumericOrBoolean(Node left, Node right) {
        assertNumericOrBoolean(left);
        assertNumericOrBoolean(right);
        try {
            return types.get(left).performBinaryOperation(types.get(right));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
            return null;
        }
    }

    private FieldSymbol resolveFieldFromType(TypeToken type, String fieldName, final boolean isStatic) throws SemanticException {
        if (type instanceof UserDefinedTypeToken) {
            ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
            Scope classScope = getScope(classSymbol.getDeclaration());
            Scope filteredScope = new Scope(classScope, new Predicate<Symbol>() {
                @Override
                public boolean apply(Symbol input) {
                    return (isStatic == input.getModifiers().contains(Modifier.STATIC));
                }
            });
            return filteredScope.resolveField(fieldName);
        } else {
            throw new SemanticException("built-in types do not currently support any fields");
        }
    }

    private Collection<FunctionSymbol> resolveFunctionFromType(TypeToken type, String functionName, final boolean isStatic) throws SemanticException {
        if (type instanceof UserDefinedTypeToken) {
            ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
            Scope classScope = getScope(classSymbol.getDeclaration());
            Scope filteredScope = new Scope(classScope, new Predicate<Symbol>() {
                @Override
                public boolean apply(Symbol input) {
                    return (isStatic == input.getModifiers().contains(Modifier.STATIC));
                }
            });
            return filteredScope.resolveFunction(functionName);
        } else {
            throw new SemanticException("built-in types do not currently support any functions");
        }
    }

    @Override
    public void outAIfThenStatement(AIfThenStatement ifThenStatement) {
        assertAssignableTo(ifThenStatement.getCondition(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAIfThenElseStatement(AIfThenElseStatement ifThenElseStatement) {
        assertAssignableTo(ifThenElseStatement.getCondition(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAWhileStatement(AWhileStatement whileStatement) {
        assertAssignableTo(whileStatement.getCondition(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAForStatement(AForStatement forStatement) {
        assertAssignableTo(forStatement.getCondition(), BooleanTypeToken.INSTANCE);
        super.outAForStatement(forStatement);
    }

    @Override
    public void outADoStatement(ADoStatement doStatement) {
        assertAssignableTo(doStatement.getCondition(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outASwitchStatement(ASwitchStatement switchStatement) {
        assertIntegral(switchStatement.getValue());
    }

    @Override
    public void outAReturnStatement(AReturnStatement returnStatement) {
        PExpression returnValue = returnStatement.getReturnValue();
        if (currentFunction != null) {
            if (currentFunction instanceof AFunctionDeclaration) {
                TypeToken returnType = TypeTokenUtil.fromNode(((AFunctionDeclaration) currentFunction).getReturnType());
                assertAssignableTo(returnValue, returnType);
            } else if (currentFunction instanceof AVoidFunctionDeclaration) {
                if (returnValue != null) {
                    exceptionBuffer.add(new SemanticException(returnValue, "expected expression of type void; found " + types.get(returnValue)));
                }
            } else {
                exceptionBuffer.add(new SemanticException(currentFunction, "unknown function declaration flavor"));
            }
        } else {
            exceptionBuffer.add(new SemanticException(returnStatement, "unexpected return statement"));
        }
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        if (currentClass != null) {
            exceptionBuffer.add(new SemanticException(classDeclaration, "unexpected class declaration"));
        } else {
            currentClass = classDeclaration;
        }
        super.inAClassDeclaration(classDeclaration);
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        currentClass = null;
        super.outAClassDeclaration(classDeclaration);
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        if (currentFunction != null) {
            exceptionBuffer.add(new SemanticException(functionDeclaration, "unexpected function declaration"));
        } else {
            currentFunction = functionDeclaration;
        }
        super.inAFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void outAFunctionDeclaration(AFunctionDeclaration functionDeclaration) {
        currentFunction = null;
        super.outAFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        if (currentFunction != null) {
            exceptionBuffer.add(new SemanticException(functionDeclaration, "unexpected function declaration"));
        } else {
            currentFunction = functionDeclaration;
        }
        super.inAVoidFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void outAVoidFunctionDeclaration(AVoidFunctionDeclaration functionDeclaration) {
        currentFunction = null;
        super.outAVoidFunctionDeclaration(functionDeclaration);
    }

    @Override
    public void outAIdentifierPrimaryExpression(AIdentifierPrimaryExpression primaryExpression) {
        try {
            try {
                LocalSymbol localSymbol = getScope().resolveLocal(primaryExpression.getIdentifier().getText());
                types.put(primaryExpression, localSymbol.getType());
            } catch (SemanticException sx) {
                FieldSymbol fieldSymbol = getScope().resolveField(primaryExpression.getIdentifier().getText());
                types.put(primaryExpression, fieldSymbol.getType());
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /**
     * Resolve qualified name expressions using the following rules:
     * <ul>
     *     <li>If qualified with an expression, resolve non-static fields within the scope of the expression type.</li>
     *     <li>If qualified with a type token, resolve static fields within the scope of the named type.</li>
     *     <li>If qualified with no target, resolve locals within the global scope.</li>
     * </ul>
     *
     * @param primaryExpression the qualified name expression
     */
    @Override
    public void outAQualifiedNamePrimaryExpression(AQualifiedNamePrimaryExpression primaryExpression) {
        try {
            PQualifiedName qualifiedName = primaryExpression.getQualifiedName();
            TypedSymbol symbol;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                TypeToken expressionType = types.get(expression);
                symbol = resolveFieldFromType(expressionType, expressionName.getName().getText(), false);
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                PTypeToken rawTarget = typeName.getTarget();
                if (rawTarget != null) {
                    TypeToken target = TypeTokenUtil.fromNode(rawTarget);
                    symbol = resolveFieldFromType(target, typeName.getName().getText(), true);
                } else {
                    Scope scope = getScope(null);
                    symbol = scope.resolveLocal(typeName.getName().getText());
                }
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            types.put(primaryExpression, symbol.getType());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAThisPrimaryExpression(AThisPrimaryExpression primaryExpression) {
        TypeToken typeToken = new UserDefinedTypeToken(((AClassDeclaration) currentClass).getName().getText());
        types.put(primaryExpression, typeToken);
    }

    @Override
    public void outAParentheticalPrimaryExpression(AParentheticalPrimaryExpression primaryExpression) {
        types.put(primaryExpression, types.get(primaryExpression.getExpression()));
    }

    @Override
    public void outAIntegralLiteral(AIntegralLiteral literal) {
        try {
            types.put(literal.parent(), IntegralTypeToken.fromNode(literal.getIntegralLiteral()));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAStringLiteral(AStringLiteral literal) {
        types.put(literal.parent(), new UserDefinedTypeToken("String"));
    }

    @Override
    public void outATrueLiteral(ATrueLiteral literal) {
        types.put(literal.parent(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAFalseLiteral(AFalseLiteral literal) {
        types.put(literal.parent(), BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation invocation) {
        try {
            Collection<FunctionSymbol> symbols = getScope().resolveFunction(invocation.getFunctionName().getText());
            enforceParametrizedInvocation(invocation, symbols, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /**
     * Resolve qualified function invocations using the following rules:
     * <ul>
     *     <li>If qualified with an expression, resolve non-static functions within the scope of the expression type.</li>
     *     <li>If qualified with a type token, resolve static functions within the scope of the named type.</li>
     *     <li>If qualified with no target, resolve functions within the global scope.</li>
     * </ul>
     *
     * @param invocation the qualified function invocation
     */
    @Override
    public void outAQualifiedFunctionInvocation(AQualifiedFunctionInvocation invocation) {
        try {
            PQualifiedName qualifiedName = invocation.getFunctionName();
            Collection<FunctionSymbol> symbols;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                TypeToken expressionType = types.get(expression);
                symbols = resolveFunctionFromType(expressionType, expressionName.getName().getText(), false);
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                PTypeToken rawTarget = typeName.getTarget();
                if (rawTarget != null) {
                    TypeToken target = TypeTokenUtil.fromNode(rawTarget);
                    symbols = resolveFunctionFromType(target, typeName.getName().getText(), true);
                } else {
                    Scope scope = getScope(null);
                    symbols = scope.resolveFunction(typeName.getName().getText());
                }
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            enforceParametrizedInvocation(invocation, symbols, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAConstructorInvocation(AConstructorInvocation invocation) {
        try {
            Scope scope;
            if (invocation.getTarget() != null) {
                TypeToken scopeToken = TypeTokenUtil.fromNode(invocation.getTarget());
                if (scopeToken instanceof UserDefinedTypeToken) {
                    ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) scopeToken).getTypeName());
                    scope = getScope(classSymbol.getDeclaration());
                } else {
                    throw new SemanticException(invocation, "built-in types do not currently support any constructors");
                }
            } else {
                scope = getScope();
            }
            Collection<ConstructorSymbol> symbols = scope.resolveConstructor();
            enforceParametrizedInvocation(invocation, symbols, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    private void enforceParametrizedInvocation(Node invocation, Collection<? extends ParametrizedSymbol> symbols, List<PExpression> parameters) throws SemanticException {
        Iterator<? extends ParametrizedSymbol> iterator = symbols.iterator();
        while (iterator.hasNext()) {
            ParametrizedSymbol symbol = iterator.next();
            if (symbol.getParameters().size() == parameters.size()) {
                for (int i = 0; i < symbol.getParameters().size(); i++) {
                    TypeToken expectedType = symbol.getParameters().get(i).getType();
                    TypeToken actualType = types.get(parameters.get(i));
                    if (!actualType.isAssignableTo(expectedType)) {
                        iterator.remove();
                        break;
                    }
                }
            } else {
                iterator.remove();
            }
        }
        if (symbols.size() > 1) {
            throw new SemanticException(invocation, "ambiguous function invocation");
        } else if (symbols.size() == 0) {
            throw new SemanticException(invocation, "cannot resolve function with appropriate parameters");
        } else {
            types.put(invocation.parent(), symbols.iterator().next().getReturnType());
        }
    }

    @Override
    public void outAUnqualifiedArrayAccess(AUnqualifiedArrayAccess arrayAccess) {
        assertAssignableTo(arrayAccess.getIndex(), IntegralTypeToken.UNSIGNED_SHORT);
        try {
            try {
                LocalSymbol localSymbol = getScope().resolveLocal(arrayAccess.getArrayName().getText());
                enforceArrayAccess(arrayAccess, localSymbol);
            } catch (SemanticException sx) {
                FieldSymbol fieldSymbol = getScope().resolveField(arrayAccess.getArrayName().getText());
                enforceArrayAccess(arrayAccess, fieldSymbol);
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAQualifiedArrayAccess(AQualifiedArrayAccess arrayAccess) {
        assertAssignableTo(arrayAccess.getIndex(), IntegralTypeToken.UNSIGNED_SHORT);
        try {
            PQualifiedName qualifiedName = arrayAccess.getArrayName();
            TIdentifier fieldName;
            TypeToken scopeToken;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                fieldName = expressionName.getName();
                scopeToken = types.get(expressionName.getTarget());
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                fieldName = typeName.getName();
                scopeToken = TypeTokenUtil.fromNode(typeName.getTarget());
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            if (scopeToken instanceof UserDefinedTypeToken) {
                ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) scopeToken).getTypeName());
                Scope classScope = getScope(classSymbol.getDeclaration());
                FieldSymbol fieldSymbol = classScope.resolveField(fieldName.getText());
                enforceArrayAccess(arrayAccess, fieldSymbol);
            } else {
                throw new SemanticException(arrayAccess, "built-in types do not currently support any fields");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    private void enforceArrayAccess(Node arrayAccess, TypedSymbol symbol) throws SemanticException {
        TypeToken arrayType = symbol.getType();
        if (arrayType instanceof ArrayTypeToken) {
            types.put(arrayAccess.parent(), ((ArrayTypeToken) arrayType).getElementType());
        } else {
            throw new SemanticException(arrayAccess, "not an array");
        }
    }

    @Override
    public void outAPrimaryExpression(APrimaryExpression primaryExpression) {
        types.put(primaryExpression, types.get(primaryExpression.getPrimaryExpression()));
    }

    @Override
    public void outAPostIncrementExpression(APostIncrementExpression expression) {
        assertNumeric(expression.getValue());
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outAPostDecrementExpression(APostDecrementExpression expression) {
        assertNumeric(expression.getValue());
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outAPreIncrementExpression(APreIncrementExpression expression) {
        assertNumeric(expression.getValue());
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outAPreDecrementExpression(APreDecrementExpression expression) {
        assertNumeric(expression.getValue());
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outANumericNegationExpression(ANumericNegationExpression expression) {
        try {
            assertNumeric(expression.getValue());
            IntegralTypeToken valueType = (IntegralTypeToken) types.get(expression.getValue());
            types.put(expression, valueType.promoteToSigned());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAConditionalNotExpression(AConditionalNotExpression expression) {
        assertAssignableTo(expression.getValue(), BooleanTypeToken.INSTANCE);
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outABitwiseComplementExpression(ABitwiseComplementExpression expression) {
        assertNumeric(expression.getValue());
        types.put(expression, types.get(expression.getValue()));
    }

    @Override
    public void outAMultiplyExpression(AMultiplyExpression expression) {
        types.put(expression, assertBinaryNumeric(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outADivideExpression(ADivideExpression expression) {
        types.put(expression, assertBinaryNumeric(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outAModulusExpression(AModulusExpression expression) {
        types.put(expression, assertBinaryNumeric(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outAAddExpression(AAddExpression expression) {
        types.put(expression, assertBinaryNumeric(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outASubtractExpression(ASubtractExpression expression) {
        types.put(expression, assertBinaryNumeric(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outAShiftLeftExpression(AShiftLeftExpression expression) {
        assertIntegral(expression.getLeft());
        assertAssignableTo(expression.getRight(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(expression, types.get(expression.getLeft()));
    }

    @Override
    public void outAShiftRightExpression(AShiftRightExpression expression) {
        assertIntegral(expression.getLeft());
        assertAssignableTo(expression.getRight(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(expression, types.get(expression.getLeft()));
    }

    @Override
    public void outAUnsignedShiftRightExpression(AUnsignedShiftRightExpression expression) {
        assertIntegral(expression.getLeft());
        assertAssignableTo(expression.getRight(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(expression, types.get(expression.getLeft()));
    }

    @Override
    public void outALessThanExpression(ALessThanExpression expression) {
        assertBinaryNumeric(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAGreaterThanExpression(AGreaterThanExpression expression) {
        assertBinaryNumeric(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outALessOrEqualExpression(ALessOrEqualExpression expression) {
        assertBinaryNumeric(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAGreaterOrEqualExpression(AGreaterOrEqualExpression expression) {
        assertBinaryNumeric(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAEqualExpression(AEqualExpression expression) {
        assertBinaryNumericOrBoolean(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outANotEqualExpression(ANotEqualExpression expression) {
        assertBinaryNumericOrBoolean(expression.getLeft(), expression.getRight());
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAPointerEqualExpression(APointerEqualExpression expression) {
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outANotPointerEqualExpression(ANotPointerEqualExpression expression) {
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outABitwiseAndExpression(ABitwiseAndExpression expression) {
        types.put(expression, assertBinaryNumericOrBoolean(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outABitwiseXorExpression(ABitwiseXorExpression expression) {
        types.put(expression, assertBinaryNumericOrBoolean(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outABitwiseOrExpression(ABitwiseOrExpression expression) {
        types.put(expression, assertBinaryNumericOrBoolean(expression.getLeft(), expression.getRight()));
    }

    @Override
    public void outAConditionalAndExpression(AConditionalAndExpression expression) {
        assertAssignableTo(expression.getLeft(), BooleanTypeToken.INSTANCE);
        assertAssignableTo(expression.getRight(), BooleanTypeToken.INSTANCE);
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAConditionalOrExpression(AConditionalOrExpression expression) {
        assertAssignableTo(expression.getLeft(), BooleanTypeToken.INSTANCE);
        assertAssignableTo(expression.getRight(), BooleanTypeToken.INSTANCE);
        types.put(expression, BooleanTypeToken.INSTANCE);
    }

    @Override
    public void outAConditionalExpression(AConditionalExpression expression) {
        assertAssignableTo(expression.getCondition(), BooleanTypeToken.INSTANCE);
        TypeToken trueType = types.get(expression.getIfTrue());
        TypeToken falseType = types.get(expression.getIfFalse());
        try {
            types.put(expression, trueType.performBinaryOperation(falseType));
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAAssignmentExpression(AAssignmentExpression expression) {
        types.put(expression, types.get(expression.getAssignment()));
    }

    @Override
    public void outAAssignment(AAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getValue(), targetType);
        types.put(assignment, targetType);
    }

    @Override
    public void outAAddAssignment(AAddAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumeric(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outASubtractAssignment(ASubtractAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumeric(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outAMultiplyAssignment(AMultiplyAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumeric(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outADivideAssignment(ADivideAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumeric(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outAModulusAssignment(AModulusAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumeric(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outABitwiseAndAssignment(ABitwiseAndAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumericOrBoolean(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outABitwiseXorAssignment(ABitwiseXorAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumericOrBoolean(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outABitwiseOrAssignment(ABitwiseOrAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertAssignableTo(assignment.getTarget(), assertBinaryNumericOrBoolean(assignment.getTarget(), assignment.getValue()));
        types.put(assignment, targetType);
    }

    @Override
    public void outAShiftLeftAssignment(AShiftLeftAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertIntegral(assignment.getTarget());
        assertAssignableTo(assignment.getValue(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(assignment, targetType);
    }

    @Override
    public void outAShiftRightAssignment(AShiftRightAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertIntegral(assignment.getTarget());
        assertAssignableTo(assignment.getValue(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(assignment, targetType);
    }

    @Override
    public void outAUnsignedShiftRightAssignment(AUnsignedShiftRightAssignment assignment) {
        TypeToken targetType = types.get(assignment.getTarget());
        assertIntegral(assignment.getTarget());
        assertAssignableTo(assignment.getValue(), IntegralTypeToken.UNSIGNED_LONG);
        types.put(assignment, targetType);
    }

    @Override
    public void outAExpressionAssignmentTarget(AExpressionAssignmentTarget assignmentTarget) {
        types.put(assignmentTarget, types.get(assignmentTarget.getPrimaryExpression()));
    }

    @Override
    public void outALocalDeclarationAssignmentTarget(ALocalDeclarationAssignmentTarget assignmentTarget) {
        ALocalDeclaration localDeclaration = (ALocalDeclaration) assignmentTarget.getLocalDeclaration();
        types.put(assignmentTarget, TypeTokenUtil.fromNode(localDeclaration.getType()));
    }
}

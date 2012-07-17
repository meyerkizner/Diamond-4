/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.AArrayAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.ABitwiseComplementExpression;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.ABreakStatement;
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AClassTopLevelStatement;
import com.prealpha.diamond.compiler.node.AConditionalNotExpression;
import com.prealpha.diamond.compiler.node.AConstructorClassStatement;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorInvocation;
import com.prealpha.diamond.compiler.node.AConstructorInvocationPrimaryExpression;
import com.prealpha.diamond.compiler.node.AContinueStatement;
import com.prealpha.diamond.compiler.node.ADefaultCaseGroup;
import com.prealpha.diamond.compiler.node.ADeleteStatement;
import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.AExpressionQualifiedName;
import com.prealpha.diamond.compiler.node.AExpressionStatement;
import com.prealpha.diamond.compiler.node.AFalseLiteral;
import com.prealpha.diamond.compiler.node.AFieldClassStatement;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AFunctionClassStatement;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AFunctionInvocationPrimaryExpression;
import com.prealpha.diamond.compiler.node.AFunctionTopLevelStatement;
import com.prealpha.diamond.compiler.node.AIdentifierPrimaryExpression;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AIntegralLiteral;
import com.prealpha.diamond.compiler.node.ALiteralPrimaryExpression;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.ANumericNegationExpression;
import com.prealpha.diamond.compiler.node.AParentheticalPrimaryExpression;
import com.prealpha.diamond.compiler.node.APrimaryExpression;
import com.prealpha.diamond.compiler.node.AQualifiedArrayAccess;
import com.prealpha.diamond.compiler.node.AQualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AQualifiedNamePrimaryExpression;
import com.prealpha.diamond.compiler.node.AReturnStatement;
import com.prealpha.diamond.compiler.node.AStringLiteral;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AThisPrimaryExpression;
import com.prealpha.diamond.compiler.node.ATrueLiteral;
import com.prealpha.diamond.compiler.node.ATypeTokenQualifiedName;
import com.prealpha.diamond.compiler.node.AUnqualifiedArrayAccess;
import com.prealpha.diamond.compiler.node.AUnqualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PArrayAccess;
import com.prealpha.diamond.compiler.node.PCaseGroup;
import com.prealpha.diamond.compiler.node.PClassStatement;
import com.prealpha.diamond.compiler.node.PExpression;
import com.prealpha.diamond.compiler.node.PFunctionDeclaration;
import com.prealpha.diamond.compiler.node.PIntegralLiteral;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PPrimaryExpression;
import com.prealpha.diamond.compiler.node.PQualifiedName;
import com.prealpha.diamond.compiler.node.PStatement;
import com.prealpha.diamond.compiler.node.PTopLevelStatement;
import com.prealpha.diamond.compiler.node.PTypeToken;
import com.prealpha.diamond.compiler.node.Token;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class CodeGenerator extends ScopeAwareWalker {
    /**
     * A list of exceptions that have occurred during this phase. The inherited method signatures prevent us from
     * throwing exceptions during the walk, so this buffer is used to store them until the end of the phase. As a side
     * effect, the phase may generate multiple errors if needed, instead of stopping after the first phase.
     */
    private final List<Exception> exceptionBuffer;

    /**
     * The expression type information produced by the {@link TypeEnforcer} phase. Each expression (and some other node
     * types) contains an entry in this map which indicates the type of that expression. This type information is
     * needed, for example, to determine the width of an expression result.
     */
    private final Map<Node, TypeToken> types;

    /**
     * <p>
     *     The instructions generated by this phase. Each top level statement has its own list of instructions; to
     *     generate the output for the entire file, we attach instructions to JSR to the main method, followed by a BRK,
     *     and then (if BRK isn't supported) an infinite loop. Then we emit the instructions for each method, labeled,
     *     in an undefined order.
     * </p>
     *
     * <p>
     *     During the code generation, instructions should always be added to the list corresponding to the
     *     {@link #context} key. In general, the {@link #write(String)} method, which uses {@code context}, should be
     *     used for this purpose.
     * </p>
     */
    private final ListMultimap<PTopLevelStatement, String> instructions;

    /**
     * Models the stack and its contents as they change during program execution. Each local variable should be placed
     * on the stack as soon as it is declared, and removed when it falls out of scope. The stack also contains several
     * placeholder values, used to represent values on the stack that do not correspond to local variables:
     * <ul>
     *     <li>{@link #thisSymbol}, a pointer to {@code this} for instance methods</li>
     *     <li>A one-word JSR pointer, to account for the stack offset caused by the JSR instruction used to enter functions</li>
     * </ul>
     */
    private final Deque<TypedSymbol> stack;

    /**
     * A stack containing the {@link FlowStructure} instances enclosing the currently executing code. By flow
     * structures, we mean structures like loops and functions that determine and modify the behavior of these three
     * statements, the flow modifiers:
     * <ul>
     *     <li>{@code break}</li>
     *     <li>{@code continue}</li>
     *     <li>{@code return}</li>
     * </ul>
     */
    private final Deque<FlowStructure> flowStructures;

    /**
     * The top level statement enclosing the currently executing code. Used to determine the active key for the
     * {@link #instructions} map.
     */
    private PTopLevelStatement context;

    /**
     * The result of the last expression to be executed. There are four possible cases:
     * <ul>
     *     <li>If the expression returned a local variable, {@code expressionResult} is that local variable, which must
     *     already be on the stack.</li>
     *     <li>If the expression returned a field, {@code expressionResult} is that field, which is not placed on the
     *     stack. Instead, the expression must copy the pointer to the field's object into register A.</li>
     *     <li>If the expression returned a value, {@code expressionResult} is {@code null}, and registers A, B, C, and
     *     X contain the value; if the value's width is less than four words, registers are employed in that order.</li>
     *     <li>If the expression was a void method invocation, {@code expressionResult} is {@code null}; the only
     *     context in which this can occur is as a standalone primary expression.</li>
     * </ul>
     */
    private TypedSymbol expressionResult;

    /**
     * A pointer to {@code this}, the implicit parameter to all instance methods. {@code thisSymbol} should be pushed
     * to the stack along with the explicit parameters during all method invocations, and popped when those methods
     * return. If the currently executing code is not part of an instance method, {@code thisSymbol} is {@code null}.
     */
    private TypedSymbol thisSymbol;

    public CodeGenerator(ScopeAwareWalker scopeSource, List<Exception> exceptionBuffer, Map<Node, TypeToken> types) {
        super(scopeSource);
        this.exceptionBuffer = exceptionBuffer;
        this.types = ImmutableMap.copyOf(types);
        instructions = ArrayListMultimap.create();
        stack = Lists.newLinkedList();
        flowStructures = Lists.newLinkedList();
    }

    public List<String> getInstructions() throws SemanticException {
        AVoidFunctionDeclaration mainMethod = null;
        for (PTopLevelStatement topLevelStatement : instructions.keySet()) {
            if (topLevelStatement instanceof AFunctionTopLevelStatement) {
                PFunctionDeclaration functionDeclaration = ((AFunctionTopLevelStatement) topLevelStatement).getFunctionDeclaration();
                if (functionDeclaration instanceof AVoidFunctionDeclaration) {
                    AVoidFunctionDeclaration voidFunctionDeclaration = (AVoidFunctionDeclaration) functionDeclaration;
                    if (voidFunctionDeclaration.getName().getText().equals("main") && voidFunctionDeclaration.getParameters().isEmpty()) {
                        mainMethod = voidFunctionDeclaration;
                    }
                }
            }
        }
        if (mainMethod == null) {
            throw new SemanticException("no main method located");
        }

        List<String> toReturn = Lists.newArrayList();
        toReturn.add("JSR " + getStartLabel(mainMethod));
        toReturn.add("BRK");
        toReturn.add("SUB PC 1");
        toReturn.addAll(instructions.values());
        return toReturn;
    }

    @Override
    protected void onExitScope(Node scopeKey) {
        assert (getScope() == getScope(scopeKey));
        reclaimScope(getScope(scopeKey));
        super.onExitScope(scopeKey);
    }

    private void reclaimScope(Scope scope) {
        for (LocalSymbol local : Lists.reverse(scope.getLocals())) {
            reclaimLocal(local);
        }
    }

    void reclaimScope() {
        for (LocalSymbol local : Lists.reverse(getScope().getLocals())) {
            doReclaimLocal(local);
        }
    }

    private void declareLocal(TypedSymbol local) {
        assert !stack.contains(local);
        stack.push(local);
        doDeclareLocal(local);
    }

    private void doDeclareLocal(TypedSymbol local) {
        write(String.format("SUB SP 0x%4x", local.getType().getWidth()));
    }

    private void reclaimLocal(TypedSymbol local) {
        TypedSymbol popped = stack.pop();
        assert (popped == local);
        doReclaimLocal(local);
    }

    private void doReclaimLocal(TypedSymbol local) {
        write(String.format("ADD SP 0x%4x", local.getType().getWidth()));
    }

    String getStartLabel(Node node) {
        return "start_" + getBaseLabel(node);
    }

    String getEndLabel(Node node) {
        return "end_" + getBaseLabel(node);
    }

    private String getBaseLabel(Node node) {
        class LineNumberFinder extends DepthFirstAdapter {
            private Token firstToken = null;
            @Override
            public void defaultIn(Node node) {
                if (node instanceof Token && firstToken == null) {
                    firstToken = (Token) node;
                }
            }
        }
        LineNumberFinder finder = new LineNumberFinder();
        node.apply(finder);
        return String.format("%s_%s_%s", node.getClass().getSimpleName(), finder.firstToken.getLine(), finder.firstToken.getPos());
    }

    private void inline(Node subject) {
        write(":" + getStartLabel(subject));
        subject.apply(this);
        write(":" + getEndLabel(subject));
    }

    private String lookup(TypedSymbol symbol, int wordOffset) {
        assert stack.contains(symbol);
        checkArgument(wordOffset < symbol.getType().getWidth());
        checkArgument(wordOffset >= 0);
        int symbolOffset = 0;
        for (TypedSymbol stackSymbol : stack) {
            if (stackSymbol == symbol) {
                break;
            } else {
                symbolOffset += stackSymbol.getType().getWidth();
            }
        }
        if (symbolOffset == 0 && wordOffset == 0) {
            return "[SP]";
        } else {
            return String.format("[SP+%d]", symbolOffset + wordOffset);
        }
    }

    private String lookupExpression(int wordOffset) {
        if (expressionResult instanceof LocalSymbol || expressionResult instanceof Placeholder) {
            // the expression result is a variable which is on the stack
            assert stack.contains(expressionResult);
            return lookup(expressionResult, wordOffset);
        } else if (expressionResult instanceof FieldSymbol) {
            if (!expressionResult.getModifiers().contains(Modifier.STATIC)) {
                // the object we need for this field is located in register A
                ClassSymbol fieldClass = ((FieldSymbol) expressionResult).getDeclaringClass();
                Scope scope = getScope(fieldClass.getDeclaration());
                List<FieldSymbol> allFields = scope.getFields();
                int fieldOffset = 0;
                for (FieldSymbol field : allFields) {
                    if (field != expressionResult) {
                        fieldOffset += field.getType().getWidth();
                    } else {
                        break;
                    }
                }
                fieldOffset += wordOffset;
                if (fieldOffset == 0) {
                    return "[A]";
                } else {
                    return String.format("[A+%d]", fieldOffset);
                }
            } else {
                // see #caseAFieldDeclaration(AFieldDeclaration)
                return String.format("%d_%s", wordOffset, getBaseLabel(expressionResult.getDeclaration()));
            }
        } else {
            // the expression result is a value, stored in registers A, B, C, and X (as needed)
            assert expressionResult == null;
            switch (wordOffset) {
                case 0:
                    return "A";
                case 1:
                    return "B";
                case 2:
                    return "C";
                case 3:
                    return "X";
                default:
                    throw new AssertionError();
            }
        }
    }

    private void requireValue() {
        if (expressionResult != null) {
            switch (expressionResult.getType().getWidth()) {
                case 4:
                    write("SET X " + lookupExpression(3));
                    write("SET C " + lookupExpression(2));
                case 2:
                    write("SET B " + lookupExpression(1));
                case 1:
                    write("SET A " + lookupExpression(0));
                    break;
                default:
                    assert false;
            }
            expressionResult = null;
        }
    }
    
    void write(String instruction) {
        instructions.put(context, instruction);
    }

    private static final class Placeholder implements TypedSymbol {
        private final TypeToken type;

        public Placeholder(TypeToken type) {
            checkNotNull(type);
            this.type = type;
        }

        @Override
        public Node getDeclaration() {
            return null;
        }

        @Override
        public TypeToken getType() {
            return type;
        }

        @Override
        public Set<Modifier> getModifiers() {
            return ImmutableSet.of();
        }
    }

    @Override
    public void caseAClassTopLevelStatement(AClassTopLevelStatement topLevelStatement) {
        assert stack.isEmpty();
        context = topLevelStatement;
        inline(topLevelStatement.getClassDeclaration());
        context = null;
        assert stack.isEmpty();
    }

    @Override
    public void caseAFunctionTopLevelStatement(AFunctionTopLevelStatement topLevelStatement) {
        assert stack.isEmpty();
        context = topLevelStatement;
        inline(topLevelStatement.getFunctionDeclaration());
        context = null;
        assert stack.isEmpty();
    }

    @Override
    public void caseAIfThenStatement(AIfThenStatement statement) {
        evaluateIfThenElse(statement, statement.getCondition(), statement.getThen(), null);
    }

    @Override
    public void caseAIfThenElseStatement(AIfThenElseStatement statement) {
        evaluateIfThenElse(statement, statement.getCondition(), statement.getThen(), statement.getElse());
    }

    private void evaluateIfThenElse(Node statement, PExpression condition, PStatement thenBody, PStatement elseBody) {
        inline(condition);

        write("IFN " + lookupExpression(0) + " 0x0000");
        write("SET PC " + getStartLabel(thenBody));

        if (elseBody != null) {
            inline(elseBody);
        }
        write("SET PC " + getEndLabel(statement));
        inline(thenBody);
    }

    @Override
    public void caseAWhileStatement(AWhileStatement statement) {
        flowStructures.push(new WhileFlowStructure(this, statement));

        inline(statement.getCondition());

        write("IFE " + lookupExpression(0) + " 0x0000");
        write("SET PC " + getEndLabel(statement.getBody()));

        inline(statement.getBody());
        write("SET PC " + getStartLabel(statement.getCondition()));

        flowStructures.pop();
    }

    @Override
    public void caseAForStatement(AForStatement statement) {
        super.inAForStatement(statement);
        flowStructures.push(new ForFlowStructure(this, statement));

        inline(statement.getInit());

        // we actually want the update on top, so skip to the condition
        write("SET PC " + getStartLabel(statement.getCondition()));

        inline(statement.getUpdate());

        inline(statement.getCondition());

        write("IFE " + lookupExpression(0) + " 0x0000");
        write("SET PC " + getEndLabel(statement.getBody()));

        inline(statement.getBody());
        write("SET PC " + getStartLabel(statement.getUpdate()));

        flowStructures.pop();
        super.outAForStatement(statement);
    }

    @Override
    public void caseADoStatement(ADoStatement statement) {
        flowStructures.push(new DoFlowStructure(this, statement));

        inline(statement.getBody());

        inline(statement.getCondition());
        write("IFN " + lookupExpression(0) + " 0x0000");
        write("SET PC " + getStartLabel(statement.getBody()));

        flowStructures.pop();
    }

    @Override
    public void caseASwitchStatement(ASwitchStatement statement) {
        flowStructures.push(new SwitchFlowStructure(this, statement));

        inline(statement.getValue());

        PCaseGroup defaultCaseGroup = null;
        for (PCaseGroup caseGroup : statement.getBody()) {
            for (PIntegralLiteral literal : getCaseGroupValues(caseGroup)) {
                try {
                    long value = IntegralTypeToken.parseLiteral(literal).longValue();
                    switch (types.get(statement.getValue()).getWidth()) {
                        case 4:
                            write(String.format("IFE %s 0x%4x", lookupExpression(3), (value & 0xffff000000000000L) >>> 48));
                            write(String.format("IFE %s 0x%4x", lookupExpression(2), (value & 0x0000ffff00000000L) >>> 32));
                        case 2:
                            write(String.format("IFE %s 0x%4x", lookupExpression(1), (value & 0x00000000ffff0000L) >>> 16));
                        case 1:
                            write(String.format("IFE %s 0x%4x", lookupExpression(0), (value & 0x000000000000ffffL)));
                            break;
                        default:
                            assert false; // there shouldn't be any other widths
                    }
                    write("SET PC " + getStartLabel(caseGroup));
                } catch (SemanticException sx) {
                    exceptionBuffer.add(sx);
                }
            }
            if (caseGroup instanceof ADefaultCaseGroup) {
                assert (defaultCaseGroup == null);
                defaultCaseGroup = caseGroup;
            }
        }
        if (defaultCaseGroup != null) {
            write("SET PC " + getStartLabel(defaultCaseGroup));
        } else if (!statement.getBody().isEmpty()) {
            write("SET PC " + getEndLabel(statement.getBody().descendingIterator().next()));
        }

        for (PCaseGroup caseGroup : statement.getBody()) {
            inline(caseGroup);
        }

        flowStructures.pop();
    }

    private Iterable<PIntegralLiteral> getCaseGroupValues(PCaseGroup caseGroup) {
        if (caseGroup instanceof ACaseGroup) {
            return ((ACaseGroup) caseGroup).getValues();
        } else if (caseGroup instanceof ADefaultCaseGroup) {
            return ((ADefaultCaseGroup) caseGroup).getValues();
        } else {
            throw new UnsupportedOperationException("unknown case group flavor");
        }
    }

    @Override
    public void caseADeleteStatement(ADeleteStatement statement) {
        throw new NoHeapException();
    }

    @Override
    public void caseABreakStatement(ABreakStatement statement) {
        boolean flag;
        do {
            if (flowStructures.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid break"));
                break;
            }
            flag = flowStructures.pop().onBreak();
        } while (!flag);
    }

    @Override
    public void caseAContinueStatement(AContinueStatement statement) {
        boolean flag;
        do {
            if (flowStructures.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid continue"));
                break;
            }
            flag = flowStructures.pop().onContinue();
        } while (!flag);
    }

    @Override
    public void caseAReturnStatement(AReturnStatement statement) {
        inline(statement.getReturnValue());
        // if the expression didn't return a value, coerce whatever it did return into one
        // this is done in case the result is a local/field that will fall out of scope
        requireValue();
        boolean flag;
        do {
            if (flowStructures.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid return"));
                break;
            }
            flag = flowStructures.pop().onReturn();
        } while (!flag);
    }

    @Override
    public void caseAExpressionStatement(AExpressionStatement statement) {
        inline(statement.getExpression());
    }

    @Override
    public void caseABlockStatement(ABlockStatement statement) {
        super.inABlockStatement(statement);
        for (PStatement enclosedStatement : statement.getStatement()) {
            inline(enclosedStatement);
        }
        super.outABlockStatement(statement);
    }

    @Override
    public void caseACaseGroup(ACaseGroup caseGroup) {
        for (PStatement enclosedStatement : caseGroup.getBody()) {
            inline(enclosedStatement);
        }
    }

    @Override
    public void caseADefaultCaseGroup(ADefaultCaseGroup caseGroup) {
        for (PStatement enclosedStatement : caseGroup.getBody()) {
            inline(enclosedStatement);
        }
    }

    @Override
    public void caseAClassDeclaration(AClassDeclaration classDeclaration) {
        super.inAClassDeclaration(classDeclaration);
        for (PClassStatement enclosedStatement : classDeclaration.getBody()) {
            inline(enclosedStatement);
        }
        super.outAClassDeclaration(classDeclaration);
    }

    @Override
    public void caseAFieldClassStatement(AFieldClassStatement classStatement) {
        inline(classStatement.getFieldDeclaration());
    }

    @Override
    public void caseAFunctionClassStatement(AFunctionClassStatement classStatement) {
        inline(classStatement.getFunctionDeclaration());
    }

    @Override
    public void caseAConstructorClassStatement(AConstructorClassStatement classStatement) {
        inline(classStatement.getConstructorDeclaration());
    }

    @Override
    public void caseAFieldDeclaration(AFieldDeclaration declaration) {
        try {
            FieldSymbol symbol = getScope().resolveField(declaration.getName().getText());
            if (symbol.getModifiers().contains(Modifier.STATIC)) {
                for (int i = 0; i < symbol.getType().getWidth(); i++) {
                    write(String.format(":%d_%s", i, getBaseLabel(declaration)));
                    write("DAT 0x0000");
                }
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAFunctionDeclaration(AFunctionDeclaration declaration) {
        evaluateParametrizedDeclaration(declaration, declaration.getName().getText(), declaration.getParameters(), declaration.getBody());
    }

    @Override
    public void caseAVoidFunctionDeclaration(AVoidFunctionDeclaration declaration) {
        evaluateParametrizedDeclaration(declaration, declaration.getName().getText(), declaration.getParameters(), declaration.getBody());
    }

    @Override
    public void caseAConstructorDeclaration(AConstructorDeclaration declaration) {
        evaluateParametrizedDeclaration(declaration, "new", declaration.getParameters(), declaration.getBody());
    }

    /*
     * This method invokes super.onEnterScope(Node) and super.onExitScope(Node) directly, bypassing in particular the
     * overridden onExitScope(Node) implementation in this class. This is because of the unusual positioning of locals
     * on the stack within a function declaration. Specifically, the JSR pointer sits between the parameters and the
     * other locals declared in the scope. This requires us to reclaim each set of locals separately, reclaiming the
     * JSR pointer in between.
     */
    private void evaluateParametrizedDeclaration(Node declaration, String name, List<PLocalDeclaration> parameters, List<PStatement> body) {
        assert stack.isEmpty();
        try {
            List<TypeToken> parameterTypes = Lists.transform(parameters, TypeTokenUtil.getDeclarationFunction());
            ParametrizedSymbol symbol;
            if (declaration instanceof AConstructorDeclaration) {
                symbol = getScope().resolveConstructor(parameterTypes);
            } else {
                symbol = getScope().resolveFunction(name, parameterTypes);
            }

            flowStructures.push(new ParametrizedFlowStructure(this, declaration));

            super.onEnterScope(declaration);

            if (!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol) {
                thisSymbol = new Placeholder(new UserDefinedTypeToken(symbol.getDeclaringClass().getName()));
            }
            stack.push(thisSymbol);

            for (PLocalDeclaration parameterDeclaration : parameters) {
                inline(parameterDeclaration);
            }

            TypedSymbol jsrPointer = new Placeholder(IntegralTypeToken.UNSIGNED_SHORT);
            stack.push(jsrPointer);

            for (PStatement enclosedStatement : body) {
                inline(enclosedStatement);
            }

            // TRY to put the last expression as the return value if we get to this point
            // we need to do this here because it could be a local that's about to fall out of scope
            // this behavior is UNDEFINED because it's not type-checked
            requireValue();

            // reclaim all locals that are NOT parameters
            for (LocalSymbol local : Lists.reverse(getScope().getLocals())) {
                if (stack.peek() == jsrPointer) {
                    break;
                } else {
                    reclaimLocal(local);
                }
            }
            write("SET PC POP");
            TypedSymbol poppedJsr = stack.pop();
            assert (poppedJsr == jsrPointer);

            for (LocalSymbol parameter : Lists.reverse(symbol.getParameters())) {
                TypedSymbol poppedParameter = stack.pop();
                assert (poppedParameter == parameter);
            }

            if (thisSymbol != null) {
                TypedSymbol poppedThis = stack.pop();
                assert (poppedThis == thisSymbol);
                thisSymbol = null;
            }

            super.onExitScope(declaration);

            flowStructures.pop();
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
        assert stack.isEmpty();
    }

    @Override
    public void caseALocalDeclaration(ALocalDeclaration declaration) {
        try {
            LocalSymbol symbol = getScope().resolveLocal(declaration.getName().getText());
            declareLocal(symbol);
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseALiteralPrimaryExpression(ALiteralPrimaryExpression primaryExpression) {
        inline(primaryExpression.getLiteral());
    }

    /*
     * TODO: duplicates TypeEnforcer.outAIdentifierPrimaryExpression(AIdentifierPrimaryExpression)
     */
    @Override
    public void caseAIdentifierPrimaryExpression(AIdentifierPrimaryExpression primaryExpression) {
        try {
            try {
                expressionResult = getScope().resolveLocal(primaryExpression.getIdentifier().getText());
                assert stack.contains(expressionResult);
            } catch (SemanticException sx) {
                expressionResult = getScope().resolveField(primaryExpression.getIdentifier().getText());
                assert stack.contains(thisSymbol);
                write("SET A " + lookup(thisSymbol, 0));
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * TODO: duplicates TypeEnforcer.outAQualifiedNamePrimaryExpression(AQualifiedNamePrimaryExpression)
     */
    @Override
    public void caseAQualifiedNamePrimaryExpression(AQualifiedNamePrimaryExpression primaryExpression) {
        try {
            PQualifiedName qualifiedName = primaryExpression.getQualifiedName();
            TypeToken type;
            String fieldName;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                fieldName = expressionName.getName().getText();
                inline(expression); // so that the expressionResult is the object
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                PTypeToken rawTarget = typeName.getTarget();
                if (rawTarget != null) {
                    type = TypeTokenUtil.fromNode(rawTarget);
                    fieldName = typeName.getName().getText();
                } else {
                    throw new SemanticException(primaryExpression, "there are no fields in the global scope");
                }
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            if (type instanceof UserDefinedTypeToken) {
                ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
                Scope classScope = getScope(classSymbol.getDeclaration());
                FieldSymbol symbol = classScope.resolveField(fieldName);
                write("SET A " + lookupExpression(0));
                expressionResult = symbol;
            } else {
                throw new SemanticException("built-in types do not currently support any fields");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAThisPrimaryExpression(AThisPrimaryExpression primaryExpression) {
        assert thisSymbol != null;
        expressionResult = thisSymbol;
    }

    @Override
    public void caseAParentheticalPrimaryExpression(AParentheticalPrimaryExpression primaryExpression) {
        inline(primaryExpression.getExpression());
    }

    @Override
    public void caseAFunctionInvocationPrimaryExpression(AFunctionInvocationPrimaryExpression primaryExpression) {
        inline(primaryExpression.getFunctionInvocation());
    }

    @Override
    public void caseAConstructorInvocationPrimaryExpression(AConstructorInvocationPrimaryExpression primaryExpression) {
        inline(primaryExpression.getConstructorInvocation());
    }

    @Override
    public void caseAArrayAccessPrimaryExpression(AArrayAccessPrimaryExpression primaryExpression) {
        inline(primaryExpression.getArrayAccess());
    }

    @Override
    public void caseAIntegralLiteral(AIntegralLiteral literal) {
        try {
            long value = IntegralTypeToken.parseLiteral(literal.getIntegralLiteral()).longValue();
            switch (types.get(literal).getWidth()) {
                case 4:
                    write(String.format("SET X 0x%4x", (value & 0xffff000000000000L) >>> 48));
                    write(String.format("SET C 0x%4x", (value & 0x0000ffff00000000L) >>> 32));
                case 2:
                    write(String.format("SET B 0x%4x", (value & 0x00000000ffff0000L) >>> 16));
                case 1:
                    write(String.format("SET A 0x%4x", (value & 0x000000000000ffffL)));
                    break;
                default:
                    assert false; // there shouldn't be any other widths
            }
            expressionResult = null;
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAStringLiteral(AStringLiteral literal) {
        throw new NoHeapException();
    }

    @Override
    public void caseATrueLiteral(ATrueLiteral literal) {
        write("SET A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseAFalseLiteral(AFalseLiteral literal) {
        write("SET A 0x0000");
        expressionResult = null;
    }

    /*
     * TODO: duplicates TypeEnforcer.outAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation)
     */
    @Override
    public void caseAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation invocation) {
        try {
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            FunctionSymbol symbol = getScope().resolveFunction(invocation.getFunctionName().getText(), parameterTypes);
            if (!symbol.getModifiers().contains(Modifier.STATIC)) {
                inline(new AThisPrimaryExpression());
            }
            evaluateParametrizedInvocation(symbol, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * TODO: duplicates TypeEnforcer.outAQualifiedFunctionInvocation(AQualifiedFunctionInvocation)
     */
    @Override
    public void caseAQualifiedFunctionInvocation(AQualifiedFunctionInvocation invocation) {
        try {
            PQualifiedName qualifiedName = invocation.getFunctionName();
            TypeToken type;
            String functionName;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                functionName = expressionName.getName().getText();
                inline(expression);
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                type = (typeName.getTarget() == null ? null : TypeTokenUtil.fromNode(typeName.getTarget()));
                functionName = typeName.getName().getText();
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            if (type == null || type instanceof UserDefinedTypeToken) {
                Scope scope;
                if (type != null) {
                    ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
                    scope = getScope(classSymbol.getDeclaration());
                } else {
                    scope = getScope(null);
                }
                List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
                FunctionSymbol symbol = scope.resolveFunction(functionName, parameterTypes);
                evaluateParametrizedInvocation(symbol, invocation.getParameters());
            } else {
                throw new SemanticException("built-in types do not currently support any functions");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * TODO: duplicates TypeEnforcer.outAConstructorInvocation(AConstructorInvocation)
     */
    @Override
    public void caseAConstructorInvocation(AConstructorInvocation invocation) {
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
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            ConstructorSymbol symbol = scope.resolveConstructor(parameterTypes);
            evaluateParametrizedInvocation(symbol, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * If this is an instance method, the implicit this parameter should be the expressionResult from the invoking
     * context. If this is a constructor, we have to make up our own placeholder.
     */
    private void evaluateParametrizedInvocation(ParametrizedSymbol symbol, List<PExpression> parameters) {
        Placeholder thisPlaceholder;
        if (!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol) {
            thisPlaceholder = new Placeholder(new UserDefinedTypeToken(symbol.getDeclaringClass().getName()));
            if (!(symbol instanceof ConstructorSymbol)) {
                write("SET PUSH " + lookupExpression(0));
            } else {
                throw new NoHeapException();
            }
            stack.push(thisPlaceholder);
        }

        Map<PExpression, Placeholder> parameterLocals = Maps.newHashMap();
        for (PExpression parameter : parameters) {
            TypeToken type = types.get(parameter);
            Placeholder placeholder = new Placeholder(type);
            parameterLocals.put(parameter, placeholder);
            inline(parameter);
            switch (type.getWidth()) {
                case 4:
                    write("SET PUSH " + lookupExpression(3));
                    write("SET PUSH " + lookupExpression(2));
                case 2:
                    write("SET PUSH " + lookupExpression(1));
                case 1:
                    write("SET PUSH " + lookupExpression(0));
                    break;
                default:
                    assert false; // there shouldn't be any other widths
            }
            stack.push(placeholder);
        }

        write("JSR " + getStartLabel(symbol.getDeclaration()));

        for (PExpression parameter : Lists.reverse(parameters)) {
            reclaimLocal(parameterLocals.get(parameter));
        }

        if (!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol) {
            write("ADD SP 1"); // clear out this
        }

        // the function invocation will have already set the relevant registers
        expressionResult = null;
    }

    /*
     * TODO: duplicates TypeEnforcer.outAUnqualifiedArrayAccess(AUnqualifiedArrayAccess)
     */
    @Override
    public void caseAUnqualifiedArrayAccess(AUnqualifiedArrayAccess arrayAccess) {
        try {
            try {
                expressionResult = getScope().resolveLocal(arrayAccess.getArrayName().getText());
                assert stack.contains(expressionResult);
                evaluateArrayAccess(arrayAccess, arrayAccess.getIndex());
            } catch (SemanticException sx) {
                expressionResult = getScope().resolveField(arrayAccess.getArrayName().getText());
                assert stack.contains(thisSymbol);
                write("SET A " + lookup(thisSymbol, 0));
                evaluateArrayAccess(arrayAccess, arrayAccess.getIndex());
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * TODO: duplicates TypeEnforcer.outAQualifiedArrayAccess(AQualifiedArrayAccess)
     */
    @Override
    public void caseAQualifiedArrayAccess(AQualifiedArrayAccess arrayAccess) {
        try {
            PQualifiedName qualifiedName = arrayAccess.getArrayName();
            TypeToken type;
            String fieldName;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                fieldName = expressionName.getName().getText();
                inline(expression); // so that expressionResult is the object
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                type = TypeTokenUtil.fromNode(typeName.getTarget());
                fieldName = typeName.getName().getText();
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            if (type instanceof UserDefinedTypeToken) {
                ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
                Scope classScope = getScope(classSymbol.getDeclaration());
                FieldSymbol fieldSymbol = classScope.resolveField(fieldName);
                write("SET A " + lookupExpression(0));
                expressionResult = fieldSymbol;
            } else {
                throw new SemanticException(arrayAccess, "built-in types do not currently support any fields");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * The array should be the expressionResult from the invoking context.
     */
    private void evaluateArrayAccess(PArrayAccess arrayAccess, PExpression index) {
        TypeToken elementType = ((ArrayTypeToken) types.get(arrayAccess)).getElementType();

        // since we need to evaluate the index expression, store the array in register Y
        write("SET Y " + lookupExpression(0));

        inline(index);
        if (expressionResult != null) {
            write("SET A " + lookupExpression(0));
        }
        if (elementType.getWidth() > 1) {
            write("MUL A " + elementType.getWidth());
        }
        write("ADD A Y");

        switch (elementType.getWidth()) {
            case 4:
                write("SET X [A+3]");
                write("SET C [A+2]");
            case 2:
                write("SET B [A+1]");
            case 1:
                write("SET A [A]"); // note that this has to be done last!
                break;
            default:
                assert false; // there shouldn't be any other widths
        }
        expressionResult = null;
    }

    @Override
    public void caseAPrimaryExpression(APrimaryExpression expression) {
        inline(expression.getPrimaryExpression());
    }

    @Override
    public void caseANumericNegationExpression(ANumericNegationExpression expression) {
        inline(new ABitwiseComplementExpression(expression.getValue()));
        requireValue(); // though it'll happen anyway, it makes things clearer
        write("ADD A 0x0001");
        if (types.get(expression).getWidth() >= 2) {
            write("ADD B EX");
        }
        if (types.get(expression).getWidth() >= 4) {
            write("ADD C EX");
            write("ADD X EX");
        }
    }

    @Override
    public void caseAConditionalNotExpression(AConditionalNotExpression expression) {
        inline(expression.getValue());
        requireValue();
        write("XOR A 0x0001");
    }

    @Override
    public void caseABitwiseComplementExpression(ABitwiseComplementExpression expression) {
        inline(expression.getValue());
        requireValue();
        switch (types.get(expression).getWidth()) {
            case 4:
                write("XOR X 0xffff");
                write("XOR C 0xffff");
            case 2:
                write("XOR B 0xffff");
            case 1:
                write("XOR A 0xffff");
                break;
            default:
                assert false; // there shouldn't be any other widths
        }
    }
}

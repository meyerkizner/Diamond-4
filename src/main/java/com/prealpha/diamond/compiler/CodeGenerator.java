/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.AArrayAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.ABreakStatement;
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AClassTopLevelStatement;
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
import com.prealpha.diamond.compiler.node.AParentheticalPrimaryExpression;
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
        PTopLevelStatement mainMethod = null;
        for (PTopLevelStatement topLevelStatement : instructions.keySet()) {
            if (topLevelStatement instanceof AFunctionTopLevelStatement) {
                PFunctionDeclaration functionDeclaration = ((AFunctionTopLevelStatement) topLevelStatement).getFunctionDeclaration();
                if (functionDeclaration instanceof AVoidFunctionDeclaration) {
                    AVoidFunctionDeclaration voidFunctionDeclaration = (AVoidFunctionDeclaration) functionDeclaration;
                    if (voidFunctionDeclaration.getName().getText().equals("main") && voidFunctionDeclaration.getParameters().isEmpty()) {
                        mainMethod = topLevelStatement;
                    }
                }
            }
        }
        if (mainMethod == null) {
            throw new SemanticException("no main method located");
        }

        List<String> toReturn = Lists.newArrayList();
        Multimap<PTopLevelStatement, String> instructionsCopy = ImmutableMultimap.copyOf(instructions);
        toReturn.addAll(instructionsCopy.get(mainMethod));
        instructionsCopy.removeAll(mainMethod);
        toReturn.add("BRK");
        toReturn.addAll(instructionsCopy.values());
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

    private void copy(TypedSymbol from, TypedSymbol to) {
        checkArgument(from.getType().equals(to.getType()));
        for (int i = 0; i < from.getType().getWidth(); i++) {
            write("SET " + lookup(to, i) + " " + lookup(from, i));
        }
    }

    private void copyField(TypedSymbol object, FieldSymbol field, TypedSymbol to) {
        checkArgument(field.getModifiers().contains(Modifier.STATIC) || object.getType().isReference());
        checkArgument(field.getType().equals(to.getType()));

        if (field.getModifiers().contains(Modifier.STATIC)) {
            write("SET B " + getStartLabel(field.getDeclaration()));
            doCopyField(to);
        } else {
            write("SET B " + lookup(object, 0));
            int fieldOffset = 0;
            for (FieldSymbol declaredField : getScope(object.getDeclaration()).getFields()) {
                if (field != declaredField && !declaredField.getModifiers().contains(Modifier.STATIC)) {
                    fieldOffset += declaredField.getType().getWidth();
                } else {
                    break;
                }
            }
            write(String.format("ADD B 0x%4x", fieldOffset));
            doCopyField(to);
        }
    }

    private void doCopyField(TypedSymbol to) {
        write("SET " + lookup(to, 0) + " [B]");
        for (int i = 1; i < to.getType().getWidth(); i++) {
            write(String.format("SET %s [B+0x%4x]", lookup(to, i), i));
        }
    }

    private void copyLiteral(long from, TypedSymbol to) {
        checkArgument(to.getType() instanceof IntegralTypeToken);
        switch (to.getType().getWidth()) {
            case 4:
                write(String.format("SET %s 0x%4x", lookup(to, 3), (from & 0xffff000000000000L) >>> 48));
                write(String.format("SET %s 0x%4x", lookup(to, 2), (from & 0x0000ffff00000000L) >>> 32));
            case 2:
                write(String.format("SET %s 0x%4x", lookup(to, 1), (from & 0x00000000ffff0000L) >>> 16));
            case 1:
                write(String.format("SET %s 0x%4x", lookup(to, 0), (from & 0x000000000000ffffL)));
                break;
            default:
                assert false; // there shouldn't be any other widths
        }
    }
    
    void write(String instruction) {
        instructions.put(context, instruction);
    }

    private static final class PseudoLocal implements TypedSymbol {
        private final TypeToken type;

        public PseudoLocal(TypeToken type) {
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
        assert stack.isEmpty();
    }

    @Override
    public void caseAFunctionTopLevelStatement(AFunctionTopLevelStatement topLevelStatement) {
        assert stack.isEmpty();
        context = topLevelStatement;
        inline(topLevelStatement.getFunctionDeclaration());
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
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(expressionResult);

        inline(condition);

        write("IFN POP 0x0000");
        write("SET PC " + getStartLabel(thenBody));
        TypedSymbol popped = stack.pop();
        assert (popped == expressionResult);
        expressionResult = null;

        if (elseBody != null) {
            inline(elseBody);
        }
        write("SET PC " + getEndLabel(statement));
        inline(thenBody);
    }

    @Override
    public void caseAWhileStatement(AWhileStatement statement) {
        flowStructures.push(new WhileFlowStructure(this, statement));
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(expressionResult);

        write("SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement.getCondition());

        write("IFE " + lookup(expressionResult, 0) + " 0x0000");
        write("SET PC " + getEndLabel(statement.getBody()));

        inline(statement.getBody());
        write("SET PC " + getStartLabel(statement.getCondition()));

        reclaimLocal(expressionResult);
        expressionResult = null;
        flowStructures.pop();
    }

    @Override
    public void caseAForStatement(AForStatement statement) {
        super.inAForStatement(statement);
        flowStructures.push(new ForFlowStructure(this, statement));

        inline(statement.getInit());

        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(expressionResult);

        // we actually want the update on top, so skip to the condition
        write("SET PC " + getStartLabel(statement.getCondition()));

        inline(statement.getUpdate());

        write("SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement.getCondition());

        write("IFE " + lookup(expressionResult, 0) + " 0x0000");
        write("SET PC " + getEndLabel(statement.getBody()));

        inline(statement.getBody());
        write("SET PC " + getStartLabel(statement.getUpdate()));

        reclaimLocal(expressionResult);
        expressionResult = null;
        flowStructures.pop();
        super.outAForStatement(statement);
    }

    @Override
    public void caseADoStatement(ADoStatement statement) {
        flowStructures.push(new DoFlowStructure(this, statement));
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(expressionResult);

        inline(statement.getBody());

        write("SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement.getCondition());
        write("IFN " + lookup(expressionResult, 0) + " 0x0000");
        write("SET PC " + getStartLabel(statement.getBody()));

        reclaimLocal(expressionResult);
        expressionResult = null;
        flowStructures.pop();
    }

    @Override
    public void caseASwitchStatement(ASwitchStatement statement) {
        flowStructures.push(new SwitchFlowStructure(this, statement));
        expressionResult = new PseudoLocal(types.get(statement.getValue()));
        declareLocal(expressionResult);

        inline(statement.getValue());

        PCaseGroup defaultCaseGroup = null;
        for (PCaseGroup caseGroup : statement.getBody()) {
            for (PIntegralLiteral literal : getCaseGroupValues(caseGroup)) {
                try {
                    // TODO: this method is a lot like copyLiteral(long, TypedSymbol)
                    long value = IntegralTypeToken.parseLiteral(literal).longValue();
                    switch (types.get(statement.getValue()).getWidth()) {
                        case 4:
                            write(String.format("IFE %s 0x%4x", lookup(expressionResult, 3), (value & 0xffff000000000000L) >>> 48));
                            write(String.format("IFE %s 0x%4x", lookup(expressionResult, 2), (value & 0x0000ffff00000000L) >>> 32));
                        case 2:
                            write(String.format("IFE %s 0x%4x", lookup(expressionResult, 1), (value & 0x00000000ffff0000L) >>> 16));
                        case 1:
                            write(String.format("IFE %s 0x%4x", lookup(expressionResult, 0), (value & 0x000000000000ffffL)));
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

        reclaimLocal(expressionResult);
        expressionResult = null;
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
        // evaluate the return expression, and store it in returnLocation
        expressionResult = new PseudoLocal(types.get(statement.getReturnValue()));
        declareLocal(expressionResult);
        inline(statement.getReturnValue());
        copy(expressionResult, returnLocation);
        reclaimLocal(expressionResult);
        expressionResult = null;

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
        assert expressionResult == null;
        if (types.get(statement) != null) {
            expressionResult = new PseudoLocal(types.get(statement));
            declareLocal(expressionResult);
        }
        inline(statement.getExpression());
        if (types.get(statement) != null) {
            reclaimLocal(expressionResult);
            expressionResult = null;
        }
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
        assert (thisSymbol == null);
        thisSymbol = new PseudoLocal(new UserDefinedTypeToken(classDeclaration.getName().getText()));
        for (PClassStatement enclosedStatement : classDeclaration.getBody()) {
            inline(enclosedStatement);
        }
        thisSymbol = null;
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

            TypeToken returnType = symbol.getReturnType();
            if (returnType != null) {
                returnLocation = new PseudoLocal(symbol.getReturnType());
                stack.push(returnLocation);
            }
            if (thisSymbol != null && !symbol.getModifiers().contains(Modifier.STATIC)) {
                stack.push(thisSymbol);
            }

            super.onEnterScope(declaration);

            // in a constructor, we need to create this on the heap before proceeding further
            if (declaration instanceof AConstructorDeclaration) {
                // however, there is no heap
                throw new NoHeapException();
            }

            for (PLocalDeclaration parameterDeclaration : parameters) {
                inline(parameterDeclaration);
            }

            TypedSymbol jsrPointer = new PseudoLocal(IntegralTypeToken.UNSIGNED_SHORT);
            stack.push(jsrPointer);

            for (PStatement enclosedStatement : body) {
                inline(enclosedStatement);
            }
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

            super.onExitScope(declaration);

            if (thisSymbol != null && !symbol.getModifiers().contains(Modifier.STATIC)
                    || declaration instanceof AConstructorDeclaration) {
                TypedSymbol poppedThis = stack.pop();
                assert (poppedThis == thisSymbol);
            }
            if (returnLocation != null) {
                TypedSymbol poppedReturn = stack.pop();
                assert (poppedReturn == returnLocation);
                returnLocation = null;
            }
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
        assert types.get(primaryExpression).equals(expressionResult.getType());
        try {
            try {
                LocalSymbol localSymbol = getScope().resolveLocal(primaryExpression.getIdentifier().getText());
                copy(localSymbol, expressionResult);
            } catch (SemanticException sx) {
                FieldSymbol fieldSymbol = getScope().resolveField(primaryExpression.getIdentifier().getText());
                copyField(thisSymbol, fieldSymbol, expressionResult);
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
        assert types.get(primaryExpression).equals(expressionResult.getType());
        try {
            PQualifiedName qualifiedName = primaryExpression.getQualifiedName();
            TypeToken type;
            String fieldName;
            TypedSymbol object;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                fieldName = expressionName.getName().getText();
                object = new PseudoLocal(type);
                declareLocal(object);
                TypedSymbol ourExpressionResult = expressionResult;
                expressionResult = object;
                inline(expression);
                expressionResult = ourExpressionResult;
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                PTypeToken rawTarget = typeName.getTarget();
                if (rawTarget != null) {
                    type = TypeTokenUtil.fromNode(rawTarget);
                    fieldName = typeName.getName().getText();
                    object = null;
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
                copyField(object, symbol, expressionResult);
                if (object != null) {
                    reclaimLocal(object);
                }
            } else {
                throw new SemanticException("built-in types do not currently support any fields");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAThisPrimaryExpression(AThisPrimaryExpression primaryExpression) {
        assert types.get(primaryExpression).equals(expressionResult.getType());
        copy(thisSymbol, expressionResult);
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
        assert types.get(literal).equals(expressionResult.getType());
        try {
            long value = IntegralTypeToken.parseLiteral(literal.getIntegralLiteral()).longValue();
            copyLiteral(value, expressionResult);
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
        assert types.get(literal).equals(expressionResult.getType());
        write("SET " + lookup(expressionResult, 0) + " 0x0001");
    }

    @Override
    public void caseAFalseLiteral(AFalseLiteral literal) {
        assert types.get(literal).equals(expressionResult.getType());
        write("SET " + lookup(expressionResult, 0) + " 0x0000");
    }

    /*
     * TODO: duplicates TypeEnforcer.outAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation)
     */
    @Override
    public void caseAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation invocation) {
        try {
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            FunctionSymbol symbol = getScope().resolveFunction(invocation.getFunctionName().getText(), parameterTypes);
            evaluateParametrizedInvocation(symbol, thisSymbol, invocation.getParameters());
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
            TypedSymbol object;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                functionName = expressionName.getName().getText();
                object = new PseudoLocal(type);
                declareLocal(object);
                TypedSymbol ourExpressionResult = expressionResult;
                inline(expression);
                expressionResult = ourExpressionResult;
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                type = (typeName.getTarget() == null ? null : TypeTokenUtil.fromNode(typeName.getTarget()));
                functionName = typeName.getName().getText();
                object = null;
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
                evaluateParametrizedInvocation(symbol, object, invocation.getParameters());
                if (object != null) {
                    reclaimLocal(object);
                }
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
            TypeToken scopeToken;
            Scope scope;
            if (invocation.getTarget() != null) {
                scopeToken = TypeTokenUtil.fromNode(invocation.getTarget());
                if (scopeToken instanceof UserDefinedTypeToken) {
                    ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) scopeToken).getTypeName());
                    scope = getScope(classSymbol.getDeclaration());
                } else {
                    throw new SemanticException(invocation, "built-in types do not currently support any constructors");
                }
            } else {
                scope = getScope();
                scopeToken = thisSymbol.getType();
            }
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            ConstructorSymbol symbol = scope.resolveConstructor(parameterTypes);
            evaluateParametrizedInvocation(symbol, new PseudoLocal(scopeToken), invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    private void evaluateParametrizedInvocation(ParametrizedSymbol symbol, TypedSymbol object, List<PExpression> parameters) {
        if (symbol.getReturnType() != null) {
            assert symbol.getReturnType().equals(expressionResult.getType());
        } else {
            assert expressionResult == null;
        }
        returnLocation = expressionResult;

        if (object != null) {
            declareLocal(object);
        }

        Map<PExpression, PseudoLocal> parameterLocals = Maps.newHashMap();
        for (PExpression parameter : parameters) {
            parameterLocals.put(parameter, new PseudoLocal(types.get(parameter)));
            expressionResult = parameterLocals.get(parameter);
            declareLocal(expressionResult);
            inline(parameter);
        }

        write("JSR " + getStartLabel(symbol.getDeclaration()));

        for (PExpression parameter : Lists.reverse(parameters)) {
            reclaimLocal(parameterLocals.get(parameter));
        }

        if (object != null) {
            reclaimLocal(object);
        }

        expressionResult = returnLocation;
        returnLocation = null;
    }

    /*
     * TODO: duplicates TypeEnforcer.outAUnqualifiedArrayAccess(AUnqualifiedArrayAccess)
     */
    @Override
    public void caseAUnqualifiedArrayAccess(AUnqualifiedArrayAccess arrayAccess) {
        try {
            try {
                LocalSymbol localSymbol = getScope().resolveLocal(arrayAccess.getArrayName().getText());
                evaluateArrayAccess(localSymbol, arrayAccess.getIndex());
            } catch (SemanticException sx) {
                FieldSymbol fieldSymbol = getScope().resolveField(arrayAccess.getArrayName().getText());
                TypedSymbol array = new PseudoLocal(fieldSymbol.getType());
                declareLocal(array);
                copyField(thisSymbol, fieldSymbol, array);
                evaluateArrayAccess(array, arrayAccess.getIndex());
                reclaimLocal(array);
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
            TypedSymbol object;
            if (qualifiedName instanceof AExpressionQualifiedName) {
                AExpressionQualifiedName expressionName = (AExpressionQualifiedName) qualifiedName;
                PPrimaryExpression expression = expressionName.getTarget();
                type = types.get(expression);
                fieldName = expressionName.getName().getText();
                object = new PseudoLocal(type);
                declareLocal(object);
                TypedSymbol ourExpressionResult = expressionResult;
                expressionResult = object;
                inline(expression);
                expressionResult = ourExpressionResult;
            } else if (qualifiedName instanceof ATypeTokenQualifiedName) {
                ATypeTokenQualifiedName typeName = (ATypeTokenQualifiedName) qualifiedName;
                type = TypeTokenUtil.fromNode(typeName.getTarget());
                fieldName = typeName.getName().getText();
                object = null;
            } else {
                throw new SemanticException(qualifiedName, "unknown qualified name flavor");
            }
            if (type instanceof UserDefinedTypeToken) {
                ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) type).getTypeName());
                Scope classScope = getScope(classSymbol.getDeclaration());
                FieldSymbol fieldSymbol = classScope.resolveField(fieldName);
                TypedSymbol array = new PseudoLocal(fieldSymbol.getType());
                declareLocal(array);
                copyField(object, fieldSymbol, array);
                evaluateArrayAccess(array, arrayAccess.getIndex());
                reclaimLocal(array);
                if (object != null) {
                    reclaimLocal(object);
                }
            } else {
                throw new SemanticException(arrayAccess, "built-in types do not currently support any fields");
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    private void evaluateArrayAccess(TypedSymbol array, PExpression index) {
        assert array.getType() instanceof ArrayTypeToken;
        TypeToken elementType = ((ArrayTypeToken) array.getType()).getElementType();
        assert elementType.equals(expressionResult.getType());

        TypedSymbol ourExpressionResult = expressionResult;
        expressionResult = new PseudoLocal(types.get(index));
        declareLocal(expressionResult);
        inline(index);

        // pointer arithmetic and such
        write(String.format("MUL %s 0x%4x", lookup(expressionResult, 0), elementType.getWidth()));
        write("SET B " + lookup(array, 0));
        write("ADD B POP");
        TypedSymbol popped = stack.pop();
        assert (popped == expressionResult);
        expressionResult = ourExpressionResult;
        doCopyField(expressionResult);
    }
}

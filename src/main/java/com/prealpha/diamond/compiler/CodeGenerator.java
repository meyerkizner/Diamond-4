/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.io.CharStreams;
import com.prealpha.diamond.compiler.node.AAddExpression;
import com.prealpha.diamond.compiler.node.AArrayAccess;
import com.prealpha.diamond.compiler.node.AArrayAccessAssignmentTarget;
import com.prealpha.diamond.compiler.node.AArrayAccessPrimaryExpression;
import com.prealpha.diamond.compiler.node.AAssignment;
import com.prealpha.diamond.compiler.node.AAssignmentExpression;
import com.prealpha.diamond.compiler.node.ABitwiseAndExpression;
import com.prealpha.diamond.compiler.node.ABitwiseComplementExpression;
import com.prealpha.diamond.compiler.node.ABitwiseOrExpression;
import com.prealpha.diamond.compiler.node.ABitwiseXorExpression;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.ABreakStatement;
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.ACastClassStatement;
import com.prealpha.diamond.compiler.node.ACastDeclaration;
import com.prealpha.diamond.compiler.node.ACastInvocation;
import com.prealpha.diamond.compiler.node.ACastInvocationPrimaryExpression;
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
import com.prealpha.diamond.compiler.node.ADefaultCaseGroup;
import com.prealpha.diamond.compiler.node.ADeleteStatement;
import com.prealpha.diamond.compiler.node.ADivideExpression;
import com.prealpha.diamond.compiler.node.ADoStatement;
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
import com.prealpha.diamond.compiler.node.AIdentifierAssignmentTarget;
import com.prealpha.diamond.compiler.node.AIdentifierPrimaryExpression;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AIntegralLiteral;
import com.prealpha.diamond.compiler.node.ALessOrEqualExpression;
import com.prealpha.diamond.compiler.node.ALessThanExpression;
import com.prealpha.diamond.compiler.node.ALiteralPrimaryExpression;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.ALocalDeclarationAssignmentTarget;
import com.prealpha.diamond.compiler.node.AModulusExpression;
import com.prealpha.diamond.compiler.node.AMultiplyExpression;
import com.prealpha.diamond.compiler.node.ANotEqualExpression;
import com.prealpha.diamond.compiler.node.ANumericNegationExpression;
import com.prealpha.diamond.compiler.node.AParentheticalPrimaryExpression;
import com.prealpha.diamond.compiler.node.APrimaryExpression;
import com.prealpha.diamond.compiler.node.AReturnStatement;
import com.prealpha.diamond.compiler.node.AShiftLeftExpression;
import com.prealpha.diamond.compiler.node.AShiftRightExpression;
import com.prealpha.diamond.compiler.node.AStringLiteral;
import com.prealpha.diamond.compiler.node.ASubtractExpression;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AThisPrimaryExpression;
import com.prealpha.diamond.compiler.node.ATrueLiteral;
import com.prealpha.diamond.compiler.node.ATypeTokenFieldAccess;
import com.prealpha.diamond.compiler.node.ATypeTokenFunctionInvocation;
import com.prealpha.diamond.compiler.node.AUnqualifiedFunctionInvocation;
import com.prealpha.diamond.compiler.node.AUnsignedShiftRightExpression;
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
import com.prealpha.diamond.compiler.node.PStatement;
import com.prealpha.diamond.compiler.node.PTopLevelStatement;
import com.prealpha.diamond.compiler.node.TIdentifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/*
 * TODO: there's lots of code duplication with TypeEnforcer, not sure how to fix this
 */
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
     *     <li>{@link #thisSymbol}, a pointer to {@code this} for instance methods.</li>
     *     <li>A one-word JSR pointer, to account for the stack offset caused by the JSR instruction used to enter
     *     functions. (This and the preceding case are collectively called {@link FunctionPlaceholder}.)</li>
     *     <li>{@link TransientPlaceholder} values, which are used in binary expressions to store the left operand while
     *     the right operand is being evaluated. These POP from the stack as soon as they are accessed.</li>
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
     * The result of the last expression to be executed. There are five possible cases:
     * <ul>
     *     <li>If the expression returned a local variable, {@code expressionResult} is that local variable, which must
     *     already be on the stack.</li>
     *     <li>If the expression returned a field, {@code expressionResult} is that field, which is not placed on the
     *     stack. Instead, the expression must copy the pointer to the field's object into register A.</li>
     *     <li>If the expression (fragment) was an {@link PArrayAccess}, {@code expressionResult} is an
     *     {@link ArrayElementPlaceholder}, and register A contains a pointer to the first word of the element accessed.
     *     The placeholders are never placed on the stack and should always be immediately consumed by the enclosing
     *     primary expression or assignment expression.</li>
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
        stack = Queues.newArrayDeque();
        flowStructures = Lists.newLinkedList();
    }

    public List<String> getInstructions() throws IOException, SemanticException {
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

        toReturn.add("SET X 0x8000");
        toReturn.add("JSR heapsetup");
        toReturn.add("JSR " + getStartLabel(mainMethod));
        toReturn.add("BRK");
        toReturn.add("SUB PC 0x0001 ; for emulators that don't support BRK");
        toReturn.addAll(instructions.values());

        InputStream stream = getClass().getResourceAsStream("malloc.dasm16");
        if (stream == null) {
            throw new FileNotFoundException("could not locate malloc.dasm16");
        }
        List<String> malloc = CharStreams.readLines(new InputStreamReader(stream));
        toReturn.addAll(malloc);
        stream.close();

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
            TypedSymbol popped = stack.pop();
            assert (local.equals(popped));
        }
        doReclaimScope(scope);
    }

    void doReclaimScope(Scope scope) {
        if (scope.getLocals().size() == 1) {
            write("SET EX POP");
        } else if (scope.getLocals().size() > 0) {
            write(String.format("ADD SP 0x%04x", scope.getLocals().size()));
        }
    }

    private void reclaimLocal(TypedSymbol local) {
        TypedSymbol popped = stack.pop();
        assert (local.equals(popped));
        write("SET EX POP");
    }

    String getStartLabel(Node node) {
        return "start_" + getBaseLabel(node);
    }

    String getEndLabel(Node node) {
        return "end_" + getBaseLabel(node);
    }

    private final Map<Node, Integer> labels = Maps.newHashMap();

    private int nextLabel = 0;

    private String getBaseLabel(Node node) {
        if (!labels.containsKey(node)) {
            labels.put(node, nextLabel++);
        }
        return String.format("%s_%s", node.getClass().getSimpleName(), labels.get(node));
    }

    private void inline(Node subject) {
        write(":" + getStartLabel(subject));
        subject.apply(this);
        write(":" + getEndLabel(subject));
    }

    private String lookup(TypedSymbol symbol) {
        if (symbol instanceof TransientPlaceholder && stack.peek() == symbol) {
            return ((TransientPlaceholder) symbol).lookup();
        } else if (symbol instanceof LocalSymbol || symbol instanceof FunctionPlaceholder || symbol instanceof TransientPlaceholder) {
            assert stack.contains(symbol);
            int symbolOffset = 0;
            for (TypedSymbol stackSymbol : stack) {
                if (symbol.equals(stackSymbol)) {
                    break;
                } else {
                    symbolOffset += 1;
                }
            }
            if (symbolOffset == 0) {
                return "[SP]";
            } else {
                return String.format("[SP+%d]", symbolOffset);
            }
        } else if (symbol instanceof ArrayElementPlaceholder) {
            return "[A]";
        } else if (symbol instanceof FieldSymbol) {
            if (!symbol.getModifiers().contains(Modifier.STATIC)) {
                // the object we need for this field is located in register A
                ClassSymbol fieldClass = ((FieldSymbol) symbol).getDeclaringClass();
                Scope scope = getScope(fieldClass.getDeclaration());
                List<FieldSymbol> allFields = scope.getFields();
                int fieldOffset = 0;
                for (FieldSymbol field : allFields) {
                    if (field != symbol) {
                        fieldOffset += 1;
                    } else {
                        break;
                    }
                }
                if (fieldOffset == 0) {
                    return "[A]";
                } else {
                    return String.format("[A+%d]", fieldOffset);
                }
            } else {
                return "[" + getStartLabel(symbol.getDeclaration()) + "]";
            }
        } else {
            // the expression result is a value, stored in register A
            assert symbol == null;
            return "A";
        }
    }

    private void requireValue(TypeToken type) {
        if (expressionResult != null) {
            checkArgument(!(expressionResult instanceof TransientPlaceholder));
            write("SET A " + lookup(expressionResult));
            expressionResult = null;
        }
    }

    private void requireStack(TypeToken type) {
        if (expressionResult instanceof LocalSymbol || expressionResult instanceof Placeholder) {
            assert stack.contains(expressionResult);
        } else {
            write("SET PUSH " + lookup(expressionResult));
            expressionResult = new TransientPlaceholder(type);
            stack.push(expressionResult);
        }
    }

    void write(String instruction) {
        instructions.put(context, instruction);
    }

    private static abstract class Placeholder implements TypedSymbol {
        private final TypeToken type;

        protected Placeholder(TypeToken type) {
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

    private static final class FunctionPlaceholder extends Placeholder {
        public FunctionPlaceholder(TypeToken type) {
            super(type);
        }
    }

    private static final class ArrayElementPlaceholder extends Placeholder {
        public ArrayElementPlaceholder(TypeToken type) {
            super(type);
        }
    }

    private final class TransientPlaceholder extends Placeholder {
        public TransientPlaceholder(TypeToken type) {
            super(type);
        }

        public String lookup() {
            checkArgument(stack.contains(this));
            TypedSymbol popped = stack.pop();
            assert (this == popped);
            return "POP";
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

        write("IFN " + lookup(expressionResult) + " 0x0000");
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

        write("IFE " + lookup(expressionResult) + " 0x0000");
        write("SET PC " + getEndLabel(statement));

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

        write("IFE " + lookup(expressionResult) + " 0x0000");
        write("SET PC reclaim_" + getBaseLabel(statement));

        inline(statement.getBody());
        write("SET PC " + getStartLabel(statement.getUpdate()));

        flowStructures.pop();
        write(":reclaim_" + getBaseLabel(statement));
        super.outAForStatement(statement);
    }

    @Override
    public void caseADoStatement(ADoStatement statement) {
        flowStructures.push(new DoFlowStructure(this, statement));

        inline(statement.getBody());

        inline(statement.getCondition());
        write("IFN " + lookup(expressionResult) + " 0x0000");
        write("SET PC " + getStartLabel(statement));

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
                    short value = TypeTokenUtil.parseIntegralLiteral(literal).shortValue();
                    write(String.format("IFE %s 0x%04x", lookup(expressionResult), value));
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
        inline(statement.getObject());
        requireValue(types.get(statement.getObject()));
        write("JSR heapfree");
    }

    @Override
    public void caseABreakStatement(ABreakStatement statement) {
        Iterator<FlowStructure> iterator = flowStructures.iterator();
        boolean flag;
        do {
            if (iterator.hasNext()) {
                flag = iterator.next().onBreak();
            } else {
                exceptionBuffer.add(new SemanticException(statement, "invalid break"));
                break;
            }
        } while (!flag);
    }

    @Override
    public void caseAContinueStatement(AContinueStatement statement) {
        Iterator<FlowStructure> iterator = flowStructures.iterator();
        boolean flag;
        do {
            if (iterator.hasNext()) {
                flag = iterator.next().onContinue();
            } else {
                exceptionBuffer.add(new SemanticException(statement, "invalid continue"));
                break;
            }
        } while (!flag);
    }

    @Override
    public void caseAReturnStatement(AReturnStatement statement) {
        inline(statement.getReturnValue());
        // if the expression didn't return a value, coerce whatever it did return into one
        // this is done in case the result is a local/field that will fall out of scope
        requireValue(types.get(statement.getReturnValue()));

        Iterator<FlowStructure> iterator = flowStructures.iterator();
        boolean flag;
        do {
            if (iterator.hasNext()) {
                flag = iterator.next().onReturn();
            } else {
                exceptionBuffer.add(new SemanticException(statement, "invalid return"));
                break;
            }
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
    public void caseACastClassStatement(ACastClassStatement classStatement) {
        inline(classStatement.getCastDeclaration());
    }

    @Override
    public void caseAFieldDeclaration(AFieldDeclaration declaration) {
        try {
            FieldSymbol symbol = getScope().resolveField(declaration.getName().getText());
            if (symbol.getModifiers().contains(Modifier.STATIC)) {
                write("DAT 0x0000");
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

    @Override
    public void caseACastDeclaration(ACastDeclaration declaration) {
        evaluateParametrizedDeclaration(declaration, "cast", ImmutableList.of(declaration.getParameter()), declaration.getBody());
    }

    /*
     * This method invokes super.onEnterScope(Node) and super.onExitScope(Node) directly, bypassing in particular the
     * overridden onExitScope(Node) implementation in this class. This is because of the unusual positioning of locals
     * on the stack within a function declaration. Specifically, the JSR pointer sits between the parameters and the
     * other locals declared in the scope. This requires us to reclaim each set of locals separately, reclaiming the
     * JSR pointer in between.
     */
    private void evaluateParametrizedDeclaration(Node declaration, String name, List<PLocalDeclaration> parameters, PStatement body) {
        assert stack.isEmpty();
        try {
            List<TypeToken> parameterTypes = Lists.transform(parameters, TypeTokenUtil.getDeclarationFunction());
            ParametrizedSymbol symbol;
            if (declaration instanceof AConstructorDeclaration) {
                symbol = getScope().resolveConstructor(parameterTypes);
            } else if (declaration instanceof ACastDeclaration) {
                assert parameterTypes.size() == 1;
                symbol = getScope().resolveCast(parameterTypes.get(0));
            } else {
                symbol = getScope().resolveFunction(name, parameterTypes);
            }

            super.onEnterScope(declaration);

            flowStructures.push(new ParametrizedFlowStructure(this));

            if ((!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol)
                    && symbol.getDeclaringClass() != null) {
                thisSymbol = new FunctionPlaceholder(new UserDefinedTypeToken(symbol.getDeclaringClass().getName()));
                stack.push(thisSymbol);
            }

            for (PLocalDeclaration parameterDeclaration : parameters) {
                inline(parameterDeclaration);
            }

            FunctionPlaceholder jsrPointer = new FunctionPlaceholder(PrimitiveTypeToken.UINT);
            stack.push(jsrPointer);

            inline(body);

            // TRY to put the last expression as the return value if we get to this point
            // we need to do this here because it could be a local that's about to fall out of scope
            // this behavior is UNDEFINED because it's not type-checked
            if (symbol.getReturnType() != null) {
                requireValue(symbol.getReturnType());
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
                assert (parameter.equals(poppedParameter));
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
            expressionResult = getScope().resolveLocal(declaration.getName().getText());
            stack.push(expressionResult);
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseALiteralPrimaryExpression(ALiteralPrimaryExpression primaryExpression) {
        inline(primaryExpression.getLiteral());
    }

    @Override
    public void caseAIdentifierPrimaryExpression(AIdentifierPrimaryExpression primaryExpression) {
        enforceIdentifier(primaryExpression.getIdentifier());
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
    public void caseACastInvocationPrimaryExpression(ACastInvocationPrimaryExpression primaryExpression) {
        inline(primaryExpression.getCastInvocation());
    }

    @Override
    public void caseAFieldAccessPrimaryExpression(AFieldAccessPrimaryExpression primaryExpression) {
        inline(primaryExpression.getFieldAccess());
    }

    @Override
    public void caseAArrayAccessPrimaryExpression(AArrayAccessPrimaryExpression primaryExpression) {
        inline(primaryExpression.getArrayAccess());
        // see the comment above evaluateArrayAccess(PArrayAccess, PExpression)
        write("SET A [A]");
        expressionResult = null;
    }

    @Override
    public void caseAIntegralLiteral(AIntegralLiteral literal) {
        try {
            short value = TypeTokenUtil.parseIntegralLiteral(literal.getIntegralLiteral()).shortValue();
            write(String.format("SET A 0x%04x", value));
            expressionResult = null;
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAStringLiteral(AStringLiteral literal) {
        throw new StringLiteralException();
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

    @Override
    public void caseAUnqualifiedFunctionInvocation(AUnqualifiedFunctionInvocation invocation) {
        try {
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            FunctionSymbol symbol = getScope().resolveFunction(invocation.getFunctionName().getText(), parameterTypes);
            if (!symbol.getModifiers().contains(Modifier.STATIC) && symbol.getDeclaringClass() != null) {
                inline(new AThisPrimaryExpression());
            }
            evaluateParametrizedInvocation(symbol, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAExpressionFunctionInvocation(AExpressionFunctionInvocation invocation) {
        try {
            TypeToken enclosingType = types.get(invocation.getTarget());
            String enclosingClassName = ((UserDefinedTypeToken) enclosingType).getTypeName();
            Scope scope = getScope(getScope().resolveClass(enclosingClassName).getDeclaration());
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            FunctionSymbol symbol = scope.resolveFunction(invocation.getFunctionName().getText(), parameterTypes);
            evaluateParametrizedInvocation(symbol, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseATypeTokenFunctionInvocation(ATypeTokenFunctionInvocation invocation) {
        try {
            Scope scope;
            if (invocation.getTarget() != null) {
                TypeToken enclosingType = types.get(invocation.getTarget());
                String enclosingClassName = ((UserDefinedTypeToken) enclosingType).getTypeName();
                scope = getScope(getScope().resolveClass(enclosingClassName).getDeclaration());
            } else {
                scope = getScope();
            }
            List<TypeToken> parameterTypes = Lists.transform(invocation.getParameters(), Functions.forMap(types));
            FunctionSymbol symbol = scope.resolveFunction(invocation.getFunctionName().getText(), parameterTypes);
            evaluateParametrizedInvocation(symbol, invocation.getParameters());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

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

    @Override
    public void caseACastInvocation(ACastInvocation invocation) {
        try {
            Scope scope;
            if (invocation.getTarget() != null) {
                TypeToken scopeToken = TypeTokenUtil.fromNode(invocation.getTarget());
                if (scopeToken instanceof UserDefinedTypeToken) {
                    ClassSymbol classSymbol = getScope().resolveClass(((UserDefinedTypeToken) scopeToken).getTypeName());
                    scope = getScope(classSymbol.getDeclaration());
                } else {
                    inline(invocation.getValue());
                    requireValue(types.get(invocation.getValue()));
                    return;
                }
            } else {
                scope = getScope();
            }
            try {
                CastSymbol symbol = scope.resolveCast(types.get(invocation.getValue()));
                evaluateParametrizedInvocation(symbol, ImmutableList.of(invocation.getValue()));
            } catch (SemanticException sx) {
                inline(invocation.getValue());
                requireValue(types.get(invocation.getValue()));
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    /*
     * If this is an instance method, the implicit this parameter should be the expressionResult from the invoking
     * context. If this is a constructor, we have to make up our own placeholder.
     */
    private void evaluateParametrizedInvocation(ParametrizedSymbol symbol, List<PExpression> parameters) {
        FunctionPlaceholder thisPlaceholder;
        if ((!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol)
                && symbol.getDeclaringClass() != null) {
            thisPlaceholder = new FunctionPlaceholder(new UserDefinedTypeToken(symbol.getDeclaringClass().getName()));
            if (!(symbol instanceof ConstructorSymbol)) {
                write("SET PUSH " + lookup(expressionResult));
            } else {
                Scope classScope = getScope(symbol.getDeclaringClass().getDeclaration());
                int objectWidth = classScope.getFields().size();
                write(String.format("SET X 0x%04x", objectWidth));
                write("JSR heapalloc");
                write("SET PUSH A");
            }
            stack.push(thisPlaceholder);
        } else {
            thisPlaceholder = null;
        }

        assert parameters.size() == symbol.getParameters().size();
        Map<PExpression, FunctionPlaceholder> parameterLocals = Maps.newHashMap();
        for (int i = 0; i < parameters.size(); i++) {
            PExpression parameter = parameters.get(i);
            TypeToken type = symbol.getParameters().get(i).getType();
            FunctionPlaceholder placeholder = new FunctionPlaceholder(type);
            parameterLocals.put(parameter, placeholder);
            inline(parameter);
            write("SET PUSH " + lookup(expressionResult));
            stack.push(placeholder);
        }

        write("JSR " + getStartLabel(symbol.getDeclaration()));

        for (PExpression parameter : Lists.reverse(parameters)) {
            reclaimLocal(parameterLocals.get(parameter));
        }

        if (thisPlaceholder != null) {
            reclaimLocal(thisPlaceholder);
        }

        // the function invocation will have already set the relevant registers
        expressionResult = null;
    }

    @Override
    public void caseAExpressionFieldAccess(AExpressionFieldAccess fieldAccess) {
        try {
            TypeToken enclosingType = types.get(fieldAccess.getTarget());
            String enclosingClassName = ((UserDefinedTypeToken) enclosingType).getTypeName();
            Scope scope = getScope(getScope().resolveClass(enclosingClassName).getDeclaration());
            FieldSymbol symbol = scope.resolveField(fieldAccess.getFieldName().getText());

            inline(fieldAccess.getTarget());
            requireValue(enclosingType);
            expressionResult = symbol;
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseATypeTokenFieldAccess(ATypeTokenFieldAccess fieldAccess) {
        try {
            TypeToken enclosingType = TypeTokenUtil.fromNode(fieldAccess.getTarget());
            String enclosingClassName = ((UserDefinedTypeToken) enclosingType).getTypeName();
            Scope scope = getScope(getScope().resolveClass(enclosingClassName).getDeclaration());
            expressionResult = scope.resolveField(fieldAccess.getFieldName().getText());
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void caseAArrayAccess(AArrayAccess arrayAccess) {
        inline(arrayAccess.getArray());
        requireStack(types.get(arrayAccess.getArray()));
        TypedSymbol array = expressionResult;

        inline(arrayAccess.getIndex());
        requireValue(types.get(arrayAccess.getIndex()));

        write("ADD A " + lookup(array));
        write("SET A [A]");
        expressionResult = null;
    }

    @Override
    public void caseAPrimaryExpression(APrimaryExpression expression) {
        inline(expression.getPrimaryExpression());
    }

    @Override
    public void caseANumericNegationExpression(ANumericNegationExpression expression) {
        inline(expression.getValue());
        requireValue(types.get(expression.getValue()));
        write("XOR A 0xffff");
        write("ADD A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseAConditionalNotExpression(AConditionalNotExpression expression) {
        inline(expression.getValue());
        requireValue(types.get(expression));
        write("XOR A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseABitwiseComplementExpression(ABitwiseComplementExpression expression) {
        inline(expression.getValue());
        requireValue(types.get(expression));
        write("XOR A 0xffff");
        expressionResult = null;
    }

    @Override
    public void caseAMultiplyExpression(AMultiplyExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        if (types.get(expression).isSigned()) {
            write("MLI A " + lookup(left));
        } else {
            write("MUL A " + lookup(left));
        }
        expressionResult = null;
    }

    @Override
    public void caseADivideExpression(ADivideExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        if (types.get(expression).isSigned()) {
            write("DVI B A");
        } else {
            write("DIV B A");
        }
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAModulusExpression(AModulusExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        if (types.get(expression).isSigned()) {
            write("MDI B A");
        } else {
            write("MOD B A");
        }
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAAddExpression(AAddExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("ADD A " + lookup(left));
        expressionResult = null;
    }

    @Override
    public void caseASubtractExpression(ASubtractExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        write("SUB B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAShiftLeftExpression(AShiftLeftExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        write("SHL B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAShiftRightExpression(AShiftRightExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        write("ASR B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAUnsignedShiftRightExpression(AUnsignedShiftRightExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("SET B " + lookup(left));
        write("SHR B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseALessThanExpression(ALessThanExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFU %s A", lookup(left)));
        } else {
            write(String.format("IFL %s A", lookup(left)));
        }
        write("SET PC true_" + getBaseLabel(expression));
        write("SET A 0x0000");
        write("SET PC reclaim_" + getBaseLabel(expression));
        write(":true_" + getBaseLabel(expression));
        write("SET A 0x0001");
        write(":reclaim_" + getBaseLabel(expression));
        expressionResult = null;
    }

    @Override
    public void caseAGreaterThanExpression(AGreaterThanExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFA %s A", lookup(left)));
        } else {
            write(String.format("IFG %s A", lookup(left)));
        }
        write("SET PC true_" + getBaseLabel(expression));
        write("SET A 0x0000");
        write("SET PC reclaim_" + getBaseLabel(expression));
        write(":true_" + getBaseLabel(expression));
        write("SET A 0x0001");
        write(":reclaim_" + getBaseLabel(expression));
        expressionResult = null;
    }

    @Override
    public void caseALessOrEqualExpression(ALessOrEqualExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFA %s A", lookup(left)));
        } else {
            write(String.format("IFG %s A", lookup(left)));
        }
        write("SET PC false_" + getBaseLabel(expression));
        write("SET A 0x0001");
        write("SET PC reclaim_" + getBaseLabel(expression));
        write(":false_" + getBaseLabel(expression));
        write("SET A 0x0000");
        write(":reclaim_" + getBaseLabel(expression));
        expressionResult = null;
    }

    @Override
    public void caseAGreaterOrEqualExpression(AGreaterOrEqualExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFU %s A", lookup(left)));
        } else {
            write(String.format("IFL %s A", lookup(left)));
        }
        write("SET PC false_" + getBaseLabel(expression));
        write("SET A 0x0001");
        write("SET PC reclaim_" + getBaseLabel(expression));
        write(":false_" + getBaseLabel(expression));
        write("SET A 0x0000");
        write(":reclaim_" + getBaseLabel(expression));
        expressionResult = null;
    }

    @Override
    public void caseAEqualExpression(AEqualExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("IFE A " + lookup(left));
        write("SET PC true_" + getBaseLabel(expression));
        write("SET A 0x0000");
        write("SET PC " + getEndLabel(expression));
        write(":true_" + getBaseLabel(expression));
        write("SET A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseANotEqualExpression(ANotEqualExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("IFE A " + lookup(left));
        write("SET PC false_" + getBaseLabel(expression));
        write("SET A 0x0001");
        write("SET PC " + getEndLabel(expression));
        write(":false_" + getBaseLabel(expression));
        write("SET A 0x0000");
        expressionResult = null;
    }

    @Override
    public void caseABitwiseAndExpression(ABitwiseAndExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("AND A " + lookup(left));
        expressionResult = null;
    }

    @Override
    public void caseABitwiseXorExpression(ABitwiseXorExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("XOR A " + lookup(left));
        expressionResult = null;
    }

    @Override
    public void caseABitwiseOrExpression(ABitwiseOrExpression expression) {
        inline(expression.getLeft());
        requireStack(types.get(expression.getLeft()));
        TypedSymbol left = expressionResult;

        inline(expression.getRight());
        requireValue(types.get(expression.getRight()));

        write("BOR A " + lookup(left));
        expressionResult = null;
    }

    @Override
    public void caseAConditionalAndExpression(AConditionalAndExpression expression) {
        inline(expression.getLeft());
        write(String.format("IFE %s 0x0000", lookup(expressionResult)));
        write("SET PC false_" + getBaseLabel(expression));

        inline(expression.getRight());
        write(String.format("IFE %s 0x0000", lookup(expressionResult)));
        write("SET PC false_" + getBaseLabel(expression));

        write("SET A 0x0001");
        write("SET PC " + getEndLabel(expression));
        write(":false_" + getBaseLabel(expression));
        write("SET A 0x0000");
        expressionResult = null;
    }

    @Override
    public void caseAConditionalOrExpression(AConditionalOrExpression expression) {
        inline(expression.getLeft());
        write(String.format("IFE %s 0x0001", lookup(expressionResult)));
        write("SET PC true_" + getBaseLabel(expression));

        inline(expression.getRight());
        write(String.format("IFE %s 0x0001", lookup(expressionResult)));
        write("SET PC true_" + getBaseLabel(expression));

        write("SET A 0x0000");
        write("SET PC " + getEndLabel(expression));
        write(":true_" + getBaseLabel(expression));
        write("SET A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseAConditionalExpression(AConditionalExpression expression) {
        inline(expression.getCondition());
        write(String.format("IFE %s 0x0001", lookup(expressionResult)));
        write("SET PC true_" + getBaseLabel(expression));

        inline(expression.getIfFalse());
        write("SET PC " + getEndLabel(expression));

        write(":true_" + getBaseLabel(expression));
        inline(expression.getIfTrue());
    }

    @Override
    public void caseAAssignmentExpression(AAssignmentExpression expression) {
        inline(expression.getAssignment());
    }

    @Override
    public void caseAAssignment(AAssignment assignment) {
        inline(assignment.getTarget());
        assert expressionResult != null;
        assert !(expressionResult instanceof Placeholder) || expressionResult instanceof ArrayElementPlaceholder;
        TypedSymbol targetSymbol = expressionResult;

        inline(assignment.getValue());
        write(String.format("SET %s %s", lookup(targetSymbol), lookup(expressionResult)));
        expressionResult = targetSymbol;
    }

    @Override
    public void caseALocalDeclarationAssignmentTarget(ALocalDeclarationAssignmentTarget assignmentTarget) {
        inline(assignmentTarget.getLocalDeclaration());
        write("SET PUSH EX");
    }

    @Override
    public void caseAIdentifierAssignmentTarget(AIdentifierAssignmentTarget assignmentTarget) {
        enforceIdentifier(assignmentTarget.getIdentifier());
    }

    @Override
    public void caseAFieldAccessAssignmentTarget(AFieldAccessAssignmentTarget assignmentTarget) {
        inline(assignmentTarget.getFieldAccess());
    }

    @Override
    public void caseAArrayAccessAssignmentTarget(AArrayAccessAssignmentTarget assignmentTarget) {
        inline(assignmentTarget.getArrayAccess());
    }

    private void enforceIdentifier(TIdentifier identifier) {
        try {
            try {
                expressionResult = getScope().resolveLocal(identifier.getText());
                assert stack.contains(expressionResult);
            } catch (SemanticException sx) {
                expressionResult = getScope().resolveField(identifier.getText());
                assert stack.contains(thisSymbol);
                write("SET A " + lookup(thisSymbol));
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }
}

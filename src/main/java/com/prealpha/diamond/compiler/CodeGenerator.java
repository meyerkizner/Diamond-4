/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
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
     * The {@code Compiler} we use to {@linkplain Compiler#raise(Exception) raise exceptions} and for information from
     * previous compilation phases.
     */
    private final Compiler compiler;

    /**
     * The expression type information produced by the {@link TypeEnforcer} phase. Each expression (and some other node
     * types) contains an entry in this map which indicates the type of that expression. This type information is
     * needed, for example, to determine the width of an expression result. {@code TypeEnforcer} guarantees that all
     * keys and values in this map are non-{@code null}.
     */
    private final Map<Node, TypeToken> types;

    /**
     * <p>
     *     The instructions generated by this phase. Each top level statement has its own list of instructions. In
     *     addition, a number of instructions are added by {@link #getInstructions()} before the instructions are
     *     returned.
     * </p>
     *
     * <p>
     *     During the code generation, instructions should always be added to the list corresponding to the
     *     {@link #context} key. In general, the {@link #write(String)} method, which uses {@code context}, should be
     *     used for this purpose.
     * </p>
     *
     * @see #context
     * @see #write(String)
     * @see #getInstructions()
     */
    private final ListMultimap<PTopLevelStatement, String> instructions;

    /**
     * Models the stack and its contents as they change during program execution. Each local variable should be placed
     * on the stack as soon as it is declared, and removed when it falls out of scope. The stack also contains several
     * placeholder values, used to represent values on the stack that do not correspond to local variables:
     * <ul>
     *     <li>{@link #thisSymbol}, of type {@link FunctionPlaceholder}, which represents {@code this}, the implicit
     *     parameter to instance functions.</li>
     *     <li>A number of additional {@code FunctionPlaceholder} instances for explicit parameters, which will only be
     *     on the stack within function (or constructor, or cast) invocation code generation. During the function code
     *     itself, the explicit parameters are ordinary local variables, so no placeholders are needed.</li>
     *     <li>A one-word JSR pointer, to account for the stack offset caused by the {@code JSR} instruction used to
     *     enter functions. The JSR pointer is also a {@code FunctionPointer}.</li>
     *     <li>{@link TransientPlaceholder} values, which are used in binary expressions to store the left operand while
     *     the right operand is being evaluated. These {@code POP} from the stack as soon as they are accessed.</li>
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
     * The result of the last expression to be executed. There are three possible cases:
     * <ul>
     *     <li>If the expression returned a local variable or field, {@code expressionResult} is the symbol for that
     *     variable, and register A contains a pointer to that variable.</li>
     *     <li>If the expression returned a value, {@code expressionResult} is {@code null}, and register A contains the
     *     value.</li>
     *     <li>If the expression was a void method invocation, {@code expressionResult} is {@code null}, and the
     *     contents of register A are undefined. The only context in which this can occur is as a standalone expression
     *     statement.</li>
     * </ul>
     */
    private TypedSymbol expressionResult;

    /**
     * A placeholder for {@code this}, the implicit parameter to all instance methods. {@code thisSymbol} should be
     * pushed to the stack along with the explicit parameters during all method invocations, and popped when those
     * methods return. If the currently executing code is not part of an instance method, {@code thisSymbol} is
     * {@code null}.
     */
    private FunctionPlaceholder thisSymbol;

    /**
     * Constructs a new {@code CodeGenerator} as a phase for the given {@link Compiler}. The code generator will use
     * information from previous phases as exposed by the {@code Compiler}, and it will invoke its
     * {@link Compiler#raise(Exception)} method when errors are encountered. The code generator should be used by
     * invoking {@link Node#apply(com.prealpha.diamond.compiler.node.Switch)} on the root of the syntax tree. The syntax
     * tree <i>must</i> be the same tree which was used by the compiler's previous phases, otherwise the result of the
     * code generation is undefined.
     *
     * @param compiler the compiler to use to obtain information from previous phases, and to raise exceptions
     */
    public CodeGenerator(Compiler compiler) {
        super(compiler.getScopeSource());
        this.compiler = compiler;
        types = compiler.getTypes();
        instructions = ArrayListMultimap.create();
        stack = Queues.newArrayDeque();
        flowStructures = Lists.newLinkedList();
    }

    /**
     * Returns the complete list of instructions needed to execute the syntax tree. Each entry in the list corresponds
     * to a line of DCPU-16 assembly (no line breaks are inserted). It contains all the instructions which were emitted
     * during the tree walk and stored in {@link #instructions}, with a number of additions, as follows:
     * <ol>
     *     <li>A {@code JSR} instruction to {@code heapsetup}, defined in the heap library below. A heap size of
     *     {@code 0x8000} is used.</li>
     *     <li>A {@code JSR} instruction to the {@code main} method for this syntax tree, which must be located in the
     *     global scope, have a return type of {@code void}, and accept zero arguments. (If no valid {@code main} method
     *     can be located, a {@code SemanticException} is thrown.)</li>
     *     <li>A {@code BRK} instruction.</li>
     *     <li>{@code SUB SP 0x0001}, to end the program in emulators that do not recognize {@code BRK}.</li>
     *     <li>All instructions stored in {@link #instructions}. The order of the top-level statements (keys in the
     *     {@code instructions} map) is undefined.</li>
     *     <li>The heap library, a copy of which can be found on
     *     <a href="https://github.com/Niriel/dcpu16/blob/master/malloc.dasm16">Github</a>. The example code is removed.
     *     The code itself is loaded from a copy of the {@code malloc.dasm16} file which is exported as a resource; the
     *     attempt to read this file may throw {@code IOException}.</li>
     * </ol>
     *
     * @return the compiled instructions corresponding to the syntax tree
     * @throws IOException if {@code malloc.dasm16}, the heap library, cannot be read
     * @throws SemanticException if no {@code main} method can be located
     */
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
        toReturn.add("SUB PC 0x0001");
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

    /**
     * {@inheritDoc}
     *
     * <p>
     *     In addition to updating the current scope, {@code CodeGenerator} also emits code and updates {@link #stack}
     *     to reclaim all local variables in the scope we are exiting.
     * </p>
     *
     * @param scopeKey the scope key
     */
    @Override
    protected void onExitScope(Node scopeKey) {
        assert (getScope() == getScope(scopeKey));
        reclaimScope(getScope(scopeKey));
        super.onExitScope(scopeKey);
    }

    /**
     * Reclaims all local variables in the specified scope, both by updating {@link #stack} and by emitting instructions
     * to free those variables from the DCPU stack.
     *
     * @param scope the scope to reclaim
     */
    private void reclaimScope(Scope scope) {
        for (LocalSymbol local : Lists.reverse(scope.getLocals())) {
            TypedSymbol popped = stack.pop();
            assert (local.equals(popped));
        }
        doReclaimScope(scope);
    }

    /**
     * Reclaims all local variables in the specified scope by emitting instructions to free those variables from the
     * DCPU stack. The model {@link #stack} is <i>not</i> modified in any way. This method is used to implement the flow
     * modifiers, {@code break}, {@code continue}, and {@code return}.
     *
     * @param scope the scope to reclaim
     */
    void doReclaimScope(Scope scope) {
        if (scope.getLocals().size() == 1) {
            write("SET EX POP");
        } else if (scope.getLocals().size() > 0) {
            write(String.format("ADD SP 0x%04x", scope.getLocals().size()));
        }
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

    /**
     * Pushes {@link #expressionResult} to the stack so that another expression may be evaluated without losing the last
     * result. A {@link TransientPlaceholder} is returned, which may be used to access the current expression result at
     * a later time, without making use of the {@code expressionResult} instance variable.
     *
     * @return a transient placeholder allowing reference to the current expression result at a later time
     */
    private TransientPlaceholder pushExpression() {
        TransientPlaceholder placeholder;
        if (expressionResult != null) {
            placeholder = new TransientVariablePlaceholder(expressionResult.getType());
        } else {
            placeholder = new TransientValuePlaceholder();
        }
        write("SET PUSH A");
        stack.push(placeholder);
        return placeholder;
    }

    /**
     * Returns an assembly value which contains the <i>value</i> of the result of the last expression
     * ({@link #expressionResult}). The assembly value will refer to the expression's value even if the expression
     * itself returned a variable; in that case, the pointer stored in register A is dereferenced.
     *
     * @return an assembly value for the value of the last expression
     */
    private String lookupExpression() {
        if (expressionResult != null) {
            return "[A]";
        } else {
            return "A";
        }
    }

    /**
     * If the result of the last expression was a variable, dereference it, and store the variable's value as the
     * expression result instead. In other words, if {@link #expressionResult} is non-{@code null}, write the assembly
     * {@code SET A [A]} and set {@code expressionResult} to {@code null}. When this method returns, the expression
     * result is guaranteed to be a value rather than a variable.
     */
    private void requireValue() {
        if (expressionResult != null) {
            write("SET A [A]");
            expressionResult = null;
        }
    }

    /**
     * When an expression is a variable located on the stack, we need to set register A to a pointer to that variable.
     * This method performs that task for the specified symbol.
     *
     * @param symbol the symbol that register A should point to
     * @throws AssertionError if {@code symbol} is not on the stack
     */
    private void evaluateStackExpression(TypedSymbol symbol) {
        assert stack.contains(symbol);
        int stackOffset = 0;
        for (TypedSymbol stackSymbol : stack) {
            if (stackSymbol == symbol) {
                break;
            } else {
                stackOffset += 1;
            }
        }
        write("SET A SP");
        if (stackOffset > 0) {
            write(String.format("ADD A 0x%04x", stackOffset));
        }
    }

    void write(String instruction) {
        instructions.put(context, instruction);
    }

    private static abstract class Placeholder implements TypedSymbol {
        private final TypeToken type;

        protected Placeholder(TypeToken type) {
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

    private abstract class TransientPlaceholder extends Placeholder {
        protected TransientPlaceholder(TypeToken type) {
            super(type);
        }

        public abstract String lookup();
    }

    private final class TransientVariablePlaceholder extends TransientPlaceholder {
        public TransientVariablePlaceholder(TypeToken type) {
            super(type);
        }

        @Override
        public String lookup() {
            checkArgument(stack.contains(this));
            TypedSymbol popped = stack.pop();
            assert (this == popped);
            write("SET Y POP");
            return "[Y]";
        }
    }

    private final class TransientValuePlaceholder extends TransientPlaceholder {
        public TransientValuePlaceholder() {
            super(null);
        }

        @Override
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

        write("IFN " + lookupExpression() + " 0x0000");
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

        write("IFE " + lookupExpression() + " 0x0000");
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

        write("IFE " + lookupExpression() + " 0x0000");
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
        write("IFN " + lookupExpression() + " 0x0000");
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
                    write(String.format("IFE %s 0x%04x", lookupExpression(), value));
                    write("SET PC " + getStartLabel(caseGroup));
                } catch (SemanticException sx) {
                    compiler.raise(sx);
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
        requireValue();
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
                compiler.raise(new SemanticException(statement, "invalid break"));
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
                compiler.raise(new SemanticException(statement, "invalid continue"));
                break;
            }
        } while (!flag);
    }

    @Override
    public void caseAReturnStatement(AReturnStatement statement) {
        inline(statement.getReturnValue());
        // if the expression didn't return a value, coerce whatever it did return into one
        // this is done in case the result is a local/field that will fall out of scope
        requireValue();

        Iterator<FlowStructure> iterator = flowStructures.iterator();
        boolean flag;
        do {
            if (iterator.hasNext()) {
                flag = iterator.next().onReturn();
            } else {
                compiler.raise(new SemanticException(statement, "invalid return"));
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
            compiler.raise(sx);
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

            onEnterScope(declaration);
            flowStructures.push(new ParametrizedFlowStructure(this));

            if ((!symbol.getModifiers().contains(Modifier.STATIC) || symbol instanceof ConstructorSymbol)
                    && symbol.getDeclaringClass() != null) {
                thisSymbol = new FunctionPlaceholder(new UserDefinedTypeToken(symbol.getDeclaringClass().getName()));
                stack.push(thisSymbol);
            }

            for (PLocalDeclaration parameterDeclaration : parameters) {
                inline(parameterDeclaration);
            }

            // push the JSR pointer
            TypedSymbol jsrPointer = new FunctionPlaceholder(PrimitiveTypeToken.UINT);
            stack.push(jsrPointer);

            inline(body);

            // if we get to this point, TRY to set the last expression as the return value
            // that means that we need to return a value if the expression result was a variable
            // this behavior is UNDEFINED because it's not type-checked, and should not be relied upon
            if (symbol.getReturnType() != null) {
                requireValue();
            }

            write("SET PC POP");
            TypedSymbol poppedJsr = stack.pop();
            assert (poppedJsr == jsrPointer);

            onExitScope(declaration);

            if (thisSymbol != null) {
                TypedSymbol poppedThis = stack.pop();
                assert (poppedThis == thisSymbol);
                write("SET EX POP");
                thisSymbol = null;
            }

            flowStructures.pop();
        } catch (SemanticException sx) {
            compiler.raise(sx);
        }
        assert stack.isEmpty();
    }

    @Override
    public void caseALocalDeclaration(ALocalDeclaration declaration) {
        try {
            expressionResult = getScope().resolveLocal(declaration.getName().getText());
            stack.push(expressionResult);
        } catch (SemanticException sx) {
            compiler.raise(sx);
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
        assert (thisSymbol != null);
        evaluateStackExpression(thisSymbol);
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
        // an array access primary expression returns a value, but an array access proper returns a variable
        // so dereference the pointer before returning from here
        requireValue();
    }

    @Override
    public void caseAIntegralLiteral(AIntegralLiteral literal) {
        try {
            short value = TypeTokenUtil.parseIntegralLiteral(literal.getIntegralLiteral()).shortValue();
            write(String.format("SET A 0x%04x", value));
            expressionResult = null;
        } catch (SemanticException sx) {
            compiler.raise(sx);
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
            compiler.raise(sx);
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
            compiler.raise(sx);
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
            compiler.raise(sx);
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
                } else if (scopeToken instanceof ArrayTypeToken) {
                    assert (invocation.getParameters().size() == 1);
                    inline(invocation.getParameters().get(0));
                    write("SET X " + lookupExpression());
                    write("JSR heapalloc");
                    expressionResult = null;
                    return;
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
            compiler.raise(sx);
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
                    requireValue();
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
                requireValue();
            }
        } catch (SemanticException sx) {
            compiler.raise(sx);
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
                write("SET PUSH " + lookupExpression());
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
        List<LocalSymbol> formalParameters = getScope(symbol.getDeclaration()).getLocals();
        for (int i = 0; i < parameters.size(); i++) {
            PExpression parameter = parameters.get(i);
            assert (types.get(parameter) == formalParameters.get(i).getType());
            inline(parameter);
            write("SET PUSH " + lookupExpression());
            stack.push(formalParameters.get(i));
        }

        write("JSR " + getStartLabel(symbol.getDeclaration()));

        reclaimScope(getScope(symbol.getDeclaration()));

        if (thisPlaceholder != null) {
            TypedSymbol popped = stack.pop();
            assert (popped == thisPlaceholder);
            write("SET EX POP");
        }

        // the function invocation will have already set the register
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
            int fieldOffset = 0;
            for (FieldSymbol declaredField : scope.getFields()) {
                if (declaredField == symbol) {
                    break;
                } else {
                    fieldOffset += 1;
                }
            }
            if (fieldOffset > 0) {
                write(String.format("ADD A 0x%04x", fieldOffset));
            }
            expressionResult = symbol;
        } catch (SemanticException sx) {
            compiler.raise(sx);
        }
    }

    @Override
    public void caseATypeTokenFieldAccess(ATypeTokenFieldAccess fieldAccess) {
        try {
            TypeToken enclosingType = TypeTokenUtil.fromNode(fieldAccess.getTarget());
            String enclosingClassName = ((UserDefinedTypeToken) enclosingType).getTypeName();
            Scope scope = getScope(getScope().resolveClass(enclosingClassName).getDeclaration());
            expressionResult = scope.resolveField(fieldAccess.getFieldName().getText());
            write("SET A " + getStartLabel(expressionResult.getDeclaration()));
        } catch (SemanticException sx) {
            compiler.raise(sx);
        }
    }

    @Override
    public void caseAArrayAccess(AArrayAccess arrayAccess) {
        inline(arrayAccess.getArray());
        TransientPlaceholder array = pushExpression();

        inline(arrayAccess.getIndex());
        requireValue();

        write("ADD A " + array.lookup());
        expressionResult = new ArrayElementPlaceholder(types.get(arrayAccess));
    }

    @Override
    public void caseAPrimaryExpression(APrimaryExpression expression) {
        inline(expression.getPrimaryExpression());
    }

    @Override
    public void caseANumericNegationExpression(ANumericNegationExpression expression) {
        inline(expression.getValue());
        requireValue();
        write("XOR A 0xffff");
        write("ADD A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseAConditionalNotExpression(AConditionalNotExpression expression) {
        inline(expression.getValue());
        requireValue();
        write("XOR A 0x0001");
        expressionResult = null;
    }

    @Override
    public void caseABitwiseComplementExpression(ABitwiseComplementExpression expression) {
        inline(expression.getValue());
        requireValue();
        write("XOR A 0xffff");
        expressionResult = null;
    }

    @Override
    public void caseAMultiplyExpression(AMultiplyExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        if (types.get(expression).isSigned()) {
            write("MLI A " + left.lookup());
        } else {
            write("MUL A " + left.lookup());
        }
        expressionResult = null;
    }

    @Override
    public void caseADivideExpression(ADivideExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("ADD A " + left.lookup());
        expressionResult = null;
    }

    @Override
    public void caseASubtractExpression(ASubtractExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
        write("SUB B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAShiftLeftExpression(AShiftLeftExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
        write("SHL B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAShiftRightExpression(AShiftRightExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
        write("ASR B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseAUnsignedShiftRightExpression(AUnsignedShiftRightExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("SET B " + left.lookup());
        write("SHR B A");
        write("SET A B");
        expressionResult = null;
    }

    @Override
    public void caseALessThanExpression(ALessThanExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFU %s A", left.lookup()));
        } else {
            write(String.format("IFL %s A", left.lookup()));
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFA %s A", left.lookup()));
        } else {
            write(String.format("IFG %s A", left.lookup()));
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFA %s A", left.lookup()));
        } else {
            write(String.format("IFG %s A", left.lookup()));
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        if (types.get(expression.getLeft()).isSigned()) {
            write(String.format("IFU %s A", left.lookup()));
        } else {
            write(String.format("IFL %s A", left.lookup()));
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("IFE A " + left.lookup());
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("IFE A " + left.lookup());
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
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("AND A " + left.lookup());
        expressionResult = null;
    }

    @Override
    public void caseABitwiseXorExpression(ABitwiseXorExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("XOR A " + left.lookup());
        expressionResult = null;
    }

    @Override
    public void caseABitwiseOrExpression(ABitwiseOrExpression expression) {
        inline(expression.getLeft());
        TransientPlaceholder left = pushExpression();

        inline(expression.getRight());
        requireValue();

        write("BOR A " + left.lookup());
        expressionResult = null;
    }

    @Override
    public void caseAConditionalAndExpression(AConditionalAndExpression expression) {
        inline(expression.getLeft());
        write(String.format("IFE %s 0x0000", lookupExpression()));
        write("SET PC false_" + getBaseLabel(expression));

        inline(expression.getRight());
        write(String.format("IFE %s 0x0000", lookupExpression()));
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
        write(String.format("IFE %s 0x0001", lookupExpression()));
        write("SET PC true_" + getBaseLabel(expression));

        inline(expression.getRight());
        write(String.format("IFE %s 0x0001", lookupExpression()));
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
        write(String.format("IFE %s 0x0001", lookupExpression()));
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
        assert (expressionResult != null && !(expressionResult instanceof TransientPlaceholder));
        TransientPlaceholder target = pushExpression();

        inline(assignment.getValue());
        requireValue();
        write(String.format("SET %s %s", target.lookup(), lookupExpression()));
        expressionResult = null;
    }

    @Override
    public void caseALocalDeclarationAssignmentTarget(ALocalDeclarationAssignmentTarget assignmentTarget) {
        inline(assignmentTarget.getLocalDeclaration());
        write("SET PUSH EX");
        evaluateStackExpression(expressionResult);
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
                evaluateStackExpression(expressionResult);
            } catch (SemanticException sx) {
                expressionResult = getScope().resolveField(identifier.getText());
                assert (thisSymbol != null);
                evaluateStackExpression(thisSymbol);

                int fieldOffset = 0;
                for (TypedSymbol declaredField : getScope().getFields()) {
                    if (declaredField == expressionResult) {
                        break;
                    } else {
                        fieldOffset += 1;
                    }
                }
                if (fieldOffset > 0) {
                    write(String.format("ADD A 0x%04x", fieldOffset));
                }
            }
        } catch (SemanticException sx) {
            compiler.raise(sx);
        }
    }
}

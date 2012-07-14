/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.ABlockStatement;
import com.prealpha.diamond.compiler.node.ABreakStatement;
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.AClassDeclaration;
import com.prealpha.diamond.compiler.node.AConstructorClassStatement;
import com.prealpha.diamond.compiler.node.AConstructorDeclaration;
import com.prealpha.diamond.compiler.node.AContinueStatement;
import com.prealpha.diamond.compiler.node.ADefaultCaseGroup;
import com.prealpha.diamond.compiler.node.ADeleteStatement;
import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.AExpressionStatement;
import com.prealpha.diamond.compiler.node.AFieldClassStatement;
import com.prealpha.diamond.compiler.node.AFieldDeclaration;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AFunctionClassStatement;
import com.prealpha.diamond.compiler.node.AFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.ALocalDeclaration;
import com.prealpha.diamond.compiler.node.AReturnStatement;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AVoidFunctionDeclaration;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PCaseGroup;
import com.prealpha.diamond.compiler.node.PClassStatement;
import com.prealpha.diamond.compiler.node.PExpression;
import com.prealpha.diamond.compiler.node.PIntegralLiteral;
import com.prealpha.diamond.compiler.node.PLocalDeclaration;
import com.prealpha.diamond.compiler.node.PStatement;
import com.prealpha.diamond.compiler.node.Token;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class CodeGenerator extends ScopeAwareWalker {
    private final List<Exception> exceptionBuffer;

    private final Map<Node, TypeToken> types;

    private final ListMultimap<Node, String> instructions;

    private final Map<Node, String> labels;

    private final Set<Node> detachedNodes;

    private final Deque<TypedSymbol> stack;

    private final Deque<FlowModifier> flowModifiers;

    private TypedSymbol expressionResult;

    private TypedSymbol returnLocation;

    private TypedSymbol thisSymbol;

    /*
     * TODO: this variable is symptomatic of some serious problems in the overall design, I think
     */
    private boolean inFlowModifier;

    public CodeGenerator(ScopeAwareWalker scopeSource, List<Exception> exceptionBuffer, Map<Node, TypeToken> types) {
        super(scopeSource);
        this.exceptionBuffer = exceptionBuffer;
        this.types = ImmutableMap.copyOf(types);
        instructions = ArrayListMultimap.create();
        labels = Maps.newHashMap();
        detachedNodes = Sets.newHashSet();
        stack = Lists.newLinkedList();
        flowModifiers = Lists.newLinkedList();
    }

    @Override
    protected void onExitScope(Node scopeKey) {
        assert (getScope() == getScope(scopeKey));
        reclaimScope(scopeKey, getScope(scopeKey));
        super.onExitScope(scopeKey);
    }

    void reclaimScope(Node context, Scope scope) {
        for (LocalSymbol local : Lists.reverse(scope.getLocals())) {
            reclaimLocal(context, local);
        }
    }

    private void declareLocal(Node context, TypedSymbol local) {
        if (local.getType().isReference()) {
            throw new NoHeapException();
        }
        stack.push(local);
        for (int i = 0; i < local.getType().getWidth(); i++) {
            instructions.put(context, "SET PUSH 0x0000");
        }
    }

    private void reclaimLocal(Node context, TypedSymbol local) {
        if (!inFlowModifier) {
            TypedSymbol popped = stack.pop();
            assert (popped == local);
        }
        String widthString = String.format("0x%4x", local.getType().getWidth());
        instructions.put(context, "ADD SP " + widthString);
    }

    private void generateLabel(Node node) {
        if (!labels.containsKey(node)) {
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
            labels.put(node, String.format("%s_%s_%s", node.getClass().getSimpleName(), finder.firstToken.getLine(), finder.firstToken.getPos()));
        }
    }

    String obtainStartLabel(Node node) {
        generateLabel(node);
        return "start_" + labels.get(node);
    }

    String obtainEndLabel(Node node) {
        generateLabel(node);
        return "end_" + labels.get(node);
    }

    /*
     * Only use this on things that will exit with the stack in the same state as it entered; because none of this
     * updates the stack as necessary. Generates labels if they do not already exist for subject.
     */
    private void inline(Node context, Node subject) {
        instructions.put(context, ":" + obtainStartLabel(subject));
        instructions.putAll(context, instructions.get(subject));
        instructions.put(context, ":" + obtainEndLabel(subject));
    }

    private String lookup(TypedSymbol symbol, int wordOffset) {
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

    void jumpTo(Node context, String label) {
        instructions.put(context, "SET PC " + label);
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
    public void outAIfThenStatement(AIfThenStatement statement) {
        evaluateIfThenElse(statement, statement.getCondition(), statement.getThen(), null);
    }

    @Override
    public void outAIfThenElseStatement(AIfThenElseStatement statement) {
        evaluateIfThenElse(statement, statement.getCondition(), statement.getThen(), statement.getElse());
    }

    private void evaluateIfThenElse(Node statement, PExpression condition, PStatement thenBody, PStatement elseBody) {
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, expressionResult);

        inline(statement, condition);

        instructions.put(statement, "IFN POP 0x0000");
        instructions.put(statement, "SET PC " + obtainStartLabel(thenBody));
        TypedSymbol popped = stack.pop();
        assert (popped == expressionResult);
        expressionResult = null;

        if (elseBody != null) {
            instructions.put(statement, "SET PC " + obtainStartLabel(elseBody));
            instructions.put(thenBody, "SET PC " + obtainEndLabel(statement));
            inline(statement, thenBody);
            inline(statement, elseBody);
        } else {
            instructions.put(statement, "SET PC " + obtainEndLabel(statement));
            inline(statement, thenBody);
        }
    }

    @Override
    public void inAWhileStatement(AWhileStatement statement) {
        flowModifiers.push(new WhileFlowModifier(this, statement));
    }

    @Override
    public void outAWhileStatement(AWhileStatement statement) {
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, expressionResult);

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement, statement.getCondition());

        instructions.put(statement, "IFE " + lookup(expressionResult, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody()));

        instructions.put(statement.getBody(), "SET PC " + obtainStartLabel(statement.getCondition()));
        inline(statement, statement.getBody());

        reclaimLocal(statement, expressionResult);
        expressionResult = null;
        flowModifiers.pop();
    }

    @Override
    public void inAForStatement(AForStatement statement) {
        super.inAForStatement(statement);
        flowModifiers.push(new ForFlowModifier(this, statement));
    }

    @Override
    public void outAForStatement(AForStatement statement) {
        inline(statement, statement.getInit());

        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, expressionResult);

        // we actually want the update on top, so skip to the condition
        instructions.put(statement, "SET PC " + obtainStartLabel(statement.getCondition()));

        inline(statement, statement.getUpdate());

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement, statement.getCondition());

        instructions.put(statement, "IFE " + lookup(expressionResult, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody()));

        instructions.put(statement.getBody(), "SET PC " + obtainStartLabel(statement.getUpdate()));
        inline(statement, statement.getBody());

        reclaimLocal(statement, expressionResult);
        expressionResult = null;
        flowModifiers.pop();
        super.outAForStatement(statement);
    }

    @Override
    public void inADoStatement(ADoStatement statement) {
        flowModifiers.push(new DoFlowModifier(this, statement));
    }

    @Override
    public void outADoStatement(ADoStatement statement) {
        expressionResult = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, expressionResult);

        inline(statement, statement.getBody());

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(expressionResult, 0) + " 0x0000");
        inline(statement, statement.getCondition());
        instructions.put(statement, "IFN " + lookup(expressionResult, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainStartLabel(statement.getBody()));

        reclaimLocal(statement, expressionResult);
        expressionResult = null;
        flowModifiers.pop();
    }

    @Override
    public void inASwitchStatement(ASwitchStatement statement) {
        flowModifiers.push(new SwitchFlowModifier(this, statement));
    }

    @Override
    public void outASwitchStatement(ASwitchStatement statement) {
        expressionResult = new PseudoLocal(types.get(statement.getValue()));
        declareLocal(statement, expressionResult);

        inline(statement, statement.getValue());

        PCaseGroup defaultCaseGroup = null;
        for (PCaseGroup caseGroup : statement.getBody()) {
            for (PIntegralLiteral literal : getCaseGroupValues(caseGroup)) {
                try {
                    long value = IntegralTypeToken.parseLiteral(literal).longValue();
                    switch (types.get(statement.getValue()).getWidth()) {
                        case 4:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(expressionResult, 3), (value & 0xffff000000000000L) >>> 48));
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(expressionResult, 2), (value & 0x0000ffff00000000L) >>> 32));
                        case 2:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(expressionResult, 1), (value & 0x00000000ffff0000L) >>> 16));
                        case 1:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(expressionResult, 0), value & 0x000000000000ffffL));
                            break;
                        default:
                            assert false; // there shouldn't be any other widths
                    }
                    instructions.put(statement, "SET PC " + obtainStartLabel(caseGroup));
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
            instructions.put(statement, "SET PC " + obtainStartLabel(defaultCaseGroup));
        } else if (!statement.getBody().isEmpty()) {
            instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody().descendingIterator().next()));
        }

        for (PCaseGroup caseGroup : statement.getBody()) {
            inline(statement, caseGroup);
        }

        reclaimLocal(statement, expressionResult);
        expressionResult = null;
        flowModifiers.pop();
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
    public void outADeleteStatement(ADeleteStatement statement) {
        throw new NoHeapException();
    }

    @Override
    public void outABreakStatement(ABreakStatement statement) {
        inFlowModifier = true;
        boolean flag;
        do {
            if (flowModifiers.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid break"));
                break;
            }
            flag = flowModifiers.pop().onBreak(statement);
        } while (!flag);
        inFlowModifier = false;
    }

    @Override
    public void outAContinueStatement(AContinueStatement statement) {
        inFlowModifier = true;
        boolean flag;
        do {
            if (flowModifiers.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid continue"));
                break;
            }
            flag = flowModifiers.pop().onContinue(statement);
        } while (!flag);
        inFlowModifier = true;
    }

    @Override
    public void outAReturnStatement(AReturnStatement statement) {
        // evaluate the return expression
        expressionResult = new PseudoLocal(types.get(statement.getReturnValue()));
        declareLocal(statement, expressionResult);

        inline(statement, statement.getReturnValue());

        // copy the pseudo-local into the return location
        switch (types.get(statement.getReturnValue()).getWidth()) {
            case 4:
                instructions.put(statement, "SET " + lookup(returnLocation, 3) + " " + lookup(expressionResult, 3));
                instructions.put(statement, "SET " + lookup(returnLocation, 2) + " " + lookup(expressionResult, 2));
            case 2:
                instructions.put(statement, "SET " + lookup(returnLocation, 1) + " " + lookup(expressionResult, 1));
            case 1:
                instructions.put(statement, "SET " + lookup(returnLocation, 0) + " " + lookup(expressionResult, 0));
                break;
            default:
                assert false; // there shouldn't be any other widths
        }

        reclaimLocal(statement, expressionResult);
        expressionResult = null;

        inFlowModifier = true;
        boolean flag;
        do {
            if (flowModifiers.isEmpty()) {
                exceptionBuffer.add(new SemanticException(statement, "invalid return"));
                break;
            }
            flag = flowModifiers.pop().onReturn(statement);
        } while (!flag);
        inFlowModifier = false;
    }

    @Override
    public void outAExpressionStatement(AExpressionStatement statement) {
        inline(statement, statement.getExpression());
    }

    @Override
    public void outABlockStatement(ABlockStatement statement) {
        for (PStatement enclosedStatement : statement.getStatement()) {
            inline(statement, enclosedStatement);
        }
        super.outABlockStatement(statement);
    }

    @Override
    public void outACaseGroup(ACaseGroup caseGroup) {
        for (PStatement enclosedStatement : caseGroup.getBody()) {
            inline(caseGroup, enclosedStatement);
        }
    }

    @Override
    public void outADefaultCaseGroup(ADefaultCaseGroup caseGroup) {
        for (PStatement enclosedStatement : caseGroup.getBody()) {
            inline(caseGroup, enclosedStatement);
        }
    }

    @Override
    public void inAClassDeclaration(AClassDeclaration classDeclaration) {
        super.inAClassDeclaration(classDeclaration);
        assert (thisSymbol == null);
        thisSymbol = new PseudoLocal(new UserDefinedTypeToken(classDeclaration.getName().getText()));
    }

    @Override
    public void outAClassDeclaration(AClassDeclaration classDeclaration) {
        for (PClassStatement enclosedStatement : classDeclaration.getBody()) {
            inline(classDeclaration, enclosedStatement);
        }
        thisSymbol = null;
        super.outAClassDeclaration(classDeclaration);
    }

    @Override
    public void outAFieldClassStatement(AFieldClassStatement classStatement) {
        inline(classStatement, classStatement.getFieldDeclaration());
    }

    @Override
    public void outAFunctionClassStatement(AFunctionClassStatement classStatement) {
        inline(classStatement, classStatement.getFunctionDeclaration());
    }

    @Override
    public void outAConstructorClassStatement(AConstructorClassStatement classStatement) {
        inline(classStatement, classStatement.getConstructorDeclaration());
    }

    @Override
    public void outAFieldDeclaration(AFieldDeclaration declaration) {
        try {
            FieldSymbol symbol = getScope().resolveField(declaration.getName().getText());
            if (symbol.getModifiers().contains(Modifier.STATIC)) {
                generateLabel(declaration);
                for (int i = 0; i < symbol.getType().getWidth(); i++) {
                    instructions.put(declaration, String.format(":%d_%s", i, labels.get(declaration)));
                    instructions.put(declaration, "DAT 0x0000");
                }
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void inAFunctionDeclaration(AFunctionDeclaration declaration) {
        super.inAFunctionDeclaration(declaration);
        inParametrizedDeclaration(declaration, declaration.getName().getText(), declaration.getParameters());
    }

    @Override
    public void inAVoidFunctionDeclaration(AVoidFunctionDeclaration declaration) {
        super.inAVoidFunctionDeclaration(declaration);
        inParametrizedDeclaration(declaration, declaration.getName().getText(), declaration.getParameters());
    }

    @Override
    public void inAConstructorDeclaration(AConstructorDeclaration declaration) {
        super.inAConstructorDeclaration(declaration);
        inParametrizedDeclaration(declaration, "new", declaration.getParameters());
    }

    private void inParametrizedDeclaration(Node declaration, String name, List<PLocalDeclaration> parameters) {
        assert stack.isEmpty();
        try {
            List<TypeToken> parameterTypes = Lists.transform(parameters, TypeTokenUtil.getDeclarationFunction());
            ParametrizedSymbol symbol;
            if (declaration instanceof AConstructorDeclaration) {
                symbol = getScope().resolveConstructor(parameterTypes);
            } else {
                symbol = getScope().resolveFunction(name, parameterTypes);
            }

            flowModifiers.push(new FunctionFlowModifier(this, declaration));
            TypeToken returnType = symbol.getReturnType();
            if (returnType != null) {
                returnLocation = new PseudoLocal(symbol.getReturnType());
                stack.push(returnLocation);
            }
            if (!symbol.getModifiers().contains(Modifier.STATIC) && thisSymbol != null) {
                stack.push(thisSymbol);
            }
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }

    @Override
    public void outAFunctionDeclaration(AFunctionDeclaration declaration) {
        outParametrizedDeclaration(declaration, declaration.getParameters(), declaration.getBody());
        super.outAFunctionDeclaration(declaration);
    }

    @Override
    public void outAVoidFunctionDeclaration(AVoidFunctionDeclaration declaration) {
        outParametrizedDeclaration(declaration, declaration.getParameters(), declaration.getBody());
        super.outAVoidFunctionDeclaration(declaration);
    }

    @Override
    public void outAConstructorDeclaration(AConstructorDeclaration declaration) {
        outParametrizedDeclaration(declaration, declaration.getParameters(), declaration.getBody());
        super.outAConstructorDeclaration(declaration);
    }

    private void outParametrizedDeclaration(Node declaration, List<PLocalDeclaration> parameters, List<PStatement> body) {
        for (PLocalDeclaration parameterDeclaration : parameters) {
            inline(declaration, parameterDeclaration);
        }

        TypedSymbol jsrPointer = new PseudoLocal(IntegralTypeToken.UNSIGNED_SHORT);
        stack.push(jsrPointer);

        // in a constructor, we need to create this on the heap before proceeding further
        if (declaration instanceof AConstructorDeclaration) {
            // however, there is no heap
            throw new NoHeapException();
        }

        for (PStatement enclosedStatement : body) {
            inline(declaration, enclosedStatement);
        }
        instructions.put(declaration, "SET PC POP");
        TypedSymbol poppedJsr = stack.pop();
        assert (poppedJsr == jsrPointer);

        for (PLocalDeclaration parameterDeclaration : Lists.reverse(parameters)) {
            try {
                String parameterName = ((ALocalDeclaration) parameterDeclaration).getName().getText();
                TypedSymbol poppedParameter = stack.pop();
                assert (poppedParameter == getScope().resolveLocal(parameterName));
            } catch (SemanticException sx) {
                exceptionBuffer.add(sx);
            }
        }
        if (stack.contains(thisSymbol)) {
            TypedSymbol poppedThis = stack.pop();
            assert (poppedThis == thisSymbol);
        }
        if (returnLocation != null) {
            TypedSymbol poppedReturn = stack.pop();
            assert (poppedReturn == returnLocation);
            returnLocation = null;
        }
        flowModifiers.pop();

        assert stack.isEmpty();
    }

    @Override
    public void outALocalDeclaration(ALocalDeclaration declaration) {
        try {
            LocalSymbol symbol = getScope().resolveLocal(declaration.getName().getText());
            declareLocal(declaration, symbol);
        } catch (SemanticException sx) {
            exceptionBuffer.add(sx);
        }
    }
}

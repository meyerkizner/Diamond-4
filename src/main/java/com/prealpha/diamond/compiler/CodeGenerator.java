/*
 * CodeGenerator.java
 * Copyright (C) 2012 Pre-Alpha Software
 * All rights reserved.
 */

package com.prealpha.diamond.compiler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.prealpha.diamond.compiler.analysis.DepthFirstAdapter;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AStatementTopLevelStatement;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PExpression;
import com.prealpha.diamond.compiler.node.PStatement;
import com.prealpha.diamond.compiler.node.Token;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class CodeGenerator extends ScopeAwareWalker {
    private final ListMultimap<Node, String> instructions;

    private final Map<Node, String> labels;

    private final List<Node> topLevelNodes;

    private final Set<Node> detachedNodes;

    private final Deque<TypedSymbol> stack;

    public CodeGenerator(ScopeAwareWalker scopeSource) {
        super(scopeSource);
        instructions = ArrayListMultimap.create();
        labels = Maps.newHashMap();
        topLevelNodes = Lists.newArrayList();
        detachedNodes = Sets.newHashSet();
        stack = Lists.newLinkedList();
    }

    @Override
    public void caseAStatementTopLevelStatement(AStatementTopLevelStatement topLevelStatement) {
        PStatement statement = topLevelStatement.getStatement();
        topLevelNodes.add(statement);
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
        // we need to create a pseudo-local to evaluate the (potentially arbitrary) condition
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        // evaluate the condition - this should store the result in the local we just created
        instructions.putAll(statement, instructions.get(condition));

        // OK, now JSR to the then or else blocks as appropriate
        instructions.put(statement, "IFE [SP] 0x0001");
        instructions.put(statement, "JSR " + obtainStartLabel(thenBody));
        detachedNodes.add(thenBody);
        if (elseBody != null) {
            instructions.put(statement, "IFE [SP] 0x0001");
            instructions.put(statement, "JSR " + obtainStartLabel(elseBody));
            detachedNodes.add(elseBody);
        }

        // get our pseudo-local off the stack
        reclaimLocal(statement, pseudoLocal);
    }

    @Override
    public void outAWhileStatement(AWhileStatement statement) {
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        instructions.put(statement, "JSR " + obtainStartLabel(statement.getCondition()));
        instructions.put(statement, "IFE [SP] 0x0000");
        instructions.put(statement, "ADD PC 0x0002");
        instructions.put(statement, "JSR " + obtainStartLabel(statement.getBody()));
        instructions.put(statement, "SUB PC 0x0005");
        detachedNodes.add(statement.getCondition());
        detachedNodes.add(statement.getBody());

        reclaimLocal(statement, pseudoLocal);
    }

    private void declareLocal(Node context, TypedSymbol local) {
        stack.push(local);
        for (int i = 0; i < local.getType().getWidth(); i++) {
            instructions.put(context, "SET PUSH 0x0000");
        }
    }

    private void reclaimLocal(Node context, TypedSymbol local) {
        TypedSymbol popped = stack.pop();
        assert (popped == local);
        String widthString = String.format("0x%x", local.getType().getWidth());
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

    private String obtainStartLabel(Node node) {
        generateLabel(node);
        return "start_" + labels.get(node);
    }

    private String obtainEndLabel(Node node) {
        generateLabel(node);
        return "end_" + labels.get(node);
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
}

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
import com.prealpha.diamond.compiler.node.ACaseGroup;
import com.prealpha.diamond.compiler.node.ADefaultCaseGroup;
import com.prealpha.diamond.compiler.node.ADeleteStatement;
import com.prealpha.diamond.compiler.node.ADoStatement;
import com.prealpha.diamond.compiler.node.AForStatement;
import com.prealpha.diamond.compiler.node.AIfThenElseStatement;
import com.prealpha.diamond.compiler.node.AIfThenStatement;
import com.prealpha.diamond.compiler.node.AStatementTopLevelStatement;
import com.prealpha.diamond.compiler.node.ASwitchStatement;
import com.prealpha.diamond.compiler.node.AWhileStatement;
import com.prealpha.diamond.compiler.node.Node;
import com.prealpha.diamond.compiler.node.PCaseGroup;
import com.prealpha.diamond.compiler.node.PExpression;
import com.prealpha.diamond.compiler.node.PIntegralLiteral;
import com.prealpha.diamond.compiler.node.PStatement;
import com.prealpha.diamond.compiler.node.Token;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

final class CodeGenerator extends ScopeAwareWalker {
    private final Map<Node, TypeToken> types;

    private final ListMultimap<Node, String> instructions;

    private final Map<Node, String> labels;

    private final List<Node> topLevelNodes;

    private final Set<Node> detachedNodes;

    private final Deque<TypedSymbol> stack;

    public CodeGenerator(ScopeAwareWalker scopeSource, Map<Node, TypeToken> types) {
        super(scopeSource);
        this.types = ImmutableMap.copyOf(types);
        instructions = ArrayListMultimap.create();
        labels = Maps.newHashMap();
        topLevelNodes = Lists.newArrayList();
        detachedNodes = Sets.newHashSet();
        stack = Lists.newLinkedList();
    }

    @Override
    protected void onExitScope(Node scopeKey) {
        for (LocalSymbol local : Lists.reverse(getScope().getLocals())) {
            reclaimLocal(scopeKey, local);
        }
        super.onExitScope(scopeKey);
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
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        inline(statement, condition);

        instructions.put(statement, "IFN POP 0x0000");
        instructions.put(statement, "SET PC " + obtainStartLabel(thenBody));
        if (elseBody != null) {
            instructions.put(statement, "SET PC " + obtainStartLabel(elseBody));
            instructions.put(thenBody, "SET PC " + obtainEndLabel(statement));
            inline(statement, thenBody);
            inline(statement, elseBody);
        } else {
            instructions.put(statement, "SET PC " + obtainEndLabel(statement));
            inline(statement, thenBody);
        }

        // note that we already reclaimed our pseudo-local in the IFN POP 0x0000
        TypedSymbol popped = stack.pop();
        assert (popped == pseudoLocal);
    }

    @Override
    public void outAWhileStatement(AWhileStatement statement) {
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(pseudoLocal, 0) + " 0x0000");
        inline(statement, statement.getCondition());

        instructions.put(statement, "IFE " + lookup(pseudoLocal, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody()));

        instructions.put(statement.getBody(), "SET PC " + obtainStartLabel(statement.getCondition()));
        inline(statement, statement.getBody());

        reclaimLocal(statement, pseudoLocal);
    }

    @Override
    public void outAForStatement(AForStatement statement) {
        inline(statement, statement.getInit());

        // use a pseudo-local to evaluate the condition
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        // we actually want the update on top, so skip to the condition
        instructions.put(statement, "SET PC " + obtainStartLabel(statement.getCondition()));

        inline(statement, statement.getUpdate());

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(pseudoLocal, 0) + " 0x0000");
        inline(statement, statement.getCondition());

        instructions.put(statement, "IFE " + lookup(pseudoLocal, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody()));

        instructions.put(statement.getBody(), "SET PC " + obtainStartLabel(statement.getUpdate()));
        inline(statement, statement.getBody());

        reclaimLocal(statement, pseudoLocal);
        super.outAForStatement(statement);
    }

    @Override
    public void outADoStatement(ADoStatement statement) {
        // we'll be smart and make the pseudo-local now, since we're almost certainly going to need it
        TypedSymbol pseudoLocal = new PseudoLocal(BooleanTypeToken.INSTANCE);
        declareLocal(statement, pseudoLocal);

        // to make break and continue work, we actually have to have the condition first, and then jump around a bit
        instructions.put(statement, "SET PC " + obtainStartLabel(statement.getBody()));

        instructions.get(statement.getCondition()).add(0, "SET " + lookup(pseudoLocal, 0) + " 0x0000");
        inline(statement, statement.getCondition());
        instructions.put(statement, "IFE " + lookup(pseudoLocal, 0) + " 0x0000");
        instructions.put(statement, "SET PC " + obtainEndLabel(statement.getBody()));

        instructions.put(statement.getBody(), "SET PC " + obtainStartLabel(statement.getCondition()));
        inline(statement, statement.getBody());

        reclaimLocal(statement, pseudoLocal);
    }

    @Override
    public void outASwitchStatement(ASwitchStatement statement) {
        try {
            TypedSymbol pseudoLocal = new PseudoLocal(types.get(statement.getValue()));
            declareLocal(statement, pseudoLocal);

            inline(statement, statement.getValue());

            PCaseGroup defaultCaseGroup = null;
            for (PCaseGroup caseGroup : statement.getBody()) {
                for (PIntegralLiteral literal : getCaseGroupValues(caseGroup)) {
                    long value = IntegralTypeToken.parseLiteral(literal).longValue();
                    switch (types.get(statement.getValue()).getWidth()) {
                        case 4:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(pseudoLocal, 3), (value & 0xffff000000000000L) >>> 48));
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(pseudoLocal, 2), (value & 0x0000ffff00000000L) >>> 32));
                        case 2:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(pseudoLocal, 1), (value & 0x00000000ffff0000L) >>> 16));
                        case 1:
                            instructions.put(statement, String.format("IFE %s 0x%4x", lookup(pseudoLocal, 0), value & 0x000000000000ffffL));
                            break;
                        default:
                            assert false; // there shouldn't be any other widths
                    }
                    instructions.put(statement, "SET PC " + obtainStartLabel(caseGroup));
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

            reclaimLocal(statement, pseudoLocal);
        } catch (SemanticException sx) {
            // there should not be new semantic problems at this stage
            throw new AssertionError(sx);
        }
    }

    @Override
    public void outADeleteStatement(ADeleteStatement statement) {
        throw new NoHeapException();
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
        TypedSymbol popped = stack.pop();
        assert (popped == local);
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

    private String obtainStartLabel(Node node) {
        generateLabel(node);
        return "start_" + labels.get(node);
    }

    private String obtainEndLabel(Node node) {
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

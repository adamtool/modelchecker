package uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.abta.TreeDirectionxState;
import uniolunisaar.adam.ds.abta.TreeEdge;
import uniolunisaar.adam.ds.abta.TreeState;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.ParameterizedPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanConstants;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaBinary;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators.Binary;
import uniolunisaar.adam.ds.aba.ABAHyperEdge;
import uniolunisaar.adam.ds.aba.ABAIntermediateEndEdge;
import uniolunisaar.adam.ds.aba.ABAOperatorState;
import uniolunisaar.adam.ds.aba.ABAState;
import uniolunisaar.adam.ds.aba.ABATrueFalseEdge;
import uniolunisaar.adam.ds.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.aba.IABALabeledHyperEdge;
import uniolunisaar.adam.ds.aba.IABANode;
import uniolunisaar.adam.ds.kripkestructure.KripkeState;
import uniolunisaar.adam.ds.kripkestructure.LabeledKripkeEdge;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.kripkestructure.TransitionLabel;
import uniolunisaar.adam.exceptions.modelchecking.NotTransformableException;

/**
 * This class belongs to the ATVA20 paper.
 *
 * @author Manuel Gieseking
 */
public class ABTAxKripke2ABATransformer {

    private static class Key {

        private final TreeState treeState;
        private final KripkeState<NodeLabel> kripkeState;

        public Key(TreeState treeState, KripkeState<NodeLabel> kripkeState) {
            this.treeState = treeState;
            this.kripkeState = kripkeState;
        }

        public TreeState getTreeState() {
            return treeState;
        }

        public KripkeState<NodeLabel> getKripkeState() {
            return kripkeState;
        }

        @Override
        public String toString() {
            return treeState.getId() + "," + kripkeState.getId();
        }

    }

    /**
     * Attention: this method can only transform trees which had been created
     * from a CTL formula (cp. An Automata-Theoretic Approach to Branching-Time
     * Model Checking - Kupferman, Vardi, Wolper)
     *
     * @param tree
     * @param k
     * @return
     * @throws
     * uniolunisaar.adam.exceptions.modelchecking.NotTransformableException
     */
    public static GeneralAlternatingBuchiAutomaton transform(AlternatingBuchiTreeAutomaton<Set<NodeLabel>> tree, PnwtKripkeStructure k) throws NotTransformableException {
        // extract the atomic propositions (todo: could make it smarter)
        Set<NodeLabel> AP = new HashSet<>();
        for (Set<NodeLabel> set : tree.getAlphabet()) {
            AP.addAll(set);
        }

        GeneralAlternatingBuchiAutomaton aba = new GeneralAlternatingBuchiAutomaton("T_" + tree.getName() + "xK_" + k.getName());

        LinkedList<Pair<Key, ABAState>> todo = new LinkedList<>();

        // create the initial states
        TreeState treeInit = tree.getInitialState();
        for (KripkeState<NodeLabel> initialState : k.getInitialStates()) {
            Key key = new Key(treeInit, initialState);
            ABAState init = aba.createAndAddState(treeInit.getId() + "," + initialState.getId());
            if (treeInit.isBuchi()) { // if the tree automaton state is a buchi state, then also the created one
                aba.setBuchi(true, init);
            }
            aba.addInitialStates(init);
            todo.add(new Pair<>(key, init));
        }

        while (!todo.isEmpty()) {
            Pair<Key, ABAState> pop = todo.pop();
            TreeState treeState = pop.getFirst().getTreeState();
            KripkeState<NodeLabel> kripkeState = pop.getFirst().getKripkeState();
            Set<NodeLabel> labels = kripkeState.getLabels();
            labels.retainAll(AP); // restrict the labels to the atomic propositions (todo: should be faster doing it just once for the kripke structure)
            // the edge of the tree
            TreeEdge<Set<NodeLabel>> treeEdge = tree.getEdge(treeState.getId(), labels);
            // the edges of the kripke structure
            Map<TransitionLabel, Set<LabeledKripkeEdge<NodeLabel, TransitionLabel>>> postEdgesWithSameLabel = k.getPostEdgesWithSameLabel(kripkeState);
            // for each equally labelled transition of the kripke strukture add the corresponding arc (with respect to the tree automaton) to the aba
            for (TransitionLabel transitionLabel : postEdgesWithSameLabel.keySet()) {
                addEdge(aba, pop.getSecond(), treeEdge, postEdgesWithSameLabel.get(transitionLabel), transitionLabel, todo);
            }
        }
        return aba;
    }

    private static void addEdge(GeneralAlternatingBuchiAutomaton aba, ABAState pre,
            TreeEdge<Set<NodeLabel>> treeEdge, Set<LabeledKripkeEdge<NodeLabel, TransitionLabel>> kripkeEdges,
            TransitionLabel transitionLabel, LinkedList<Pair<Key, ABAState>> todo) throws NotTransformableException {
        IPositiveBooleanFormula formula = treeEdge.getSuccessor();
        if (formula instanceof PositiveBooleanConstants.True) {
            aba.createAndAddSpecialEdge(pre.getId(), ABATrueFalseEdge.Type.TRUE, transitionLabel.getId(), false);
        } else if (formula instanceof PositiveBooleanConstants.False) {
            aba.createAndAddSpecialEdge(pre.getId(), ABATrueFalseEdge.Type.FALSE, transitionLabel.getId(), false);
        } else {
            addEdgesRecursively(aba, pre, treeEdge.getSuccessor(), kripkeEdges, transitionLabel, todo, null);
        }
    }

    /**
     * Goes through the positive Boolean formula and adds one hyper edge for the
     * formula.
     *
     * @param pre
     * @param formula
     * @param kripkeEdges
     * @param transitionLabel
     * @param todo
     * @param edge
     * @return
     * @throws NotTransformableException
     */
    private static IABALabeledHyperEdge addEdgesRecursively(GeneralAlternatingBuchiAutomaton aba, IABANode pre,
            IPositiveBooleanFormula formula, Set<LabeledKripkeEdge<NodeLabel, TransitionLabel>> kripkeEdges,
            TransitionLabel transitionLabel, LinkedList<Pair<Key, ABAState>> todo,
            ABAHyperEdge edge) throws NotTransformableException {

        if (formula instanceof ParameterizedPositiveBooleanFormula) { // These are disjunctions or conjunctions ranging of the degree of the Kripke structure node
            ParameterizedPositiveBooleanFormula f = (ParameterizedPositiveBooleanFormula) formula;
            ABAOperatorState.TYPE type = f.getOp() == Binary.AND ? ABAOperatorState.TYPE.ALL : ABAOperatorState.TYPE.EXISTS;
            int k = kripkeEdges.size();
            TreeDirectionxState post = (TreeDirectionxState) f.getElement(); // when created from CTL this is the only possibility
            TreeState treePostState = post.getState();
            // create all post states
            String[] postStateIds = new String[k];
            int i = 0;
            for (LabeledKripkeEdge<NodeLabel, TransitionLabel> kripkeEdge : kripkeEdges) {
                KripkeState<NodeLabel> kripkePostState = kripkeEdge.getPost();
                Key key = new Key(treePostState, kripkePostState);
                ABAState postState;
                if (aba.containsState(key.toString())) {
                    postState = aba.getState(key.toString());
                } else {
                    postState = aba.createAndAddState(key.toString());
                    if (treePostState.isBuchi()) { // if the tree automaton state is a buchi state, then also the created one
                        aba.setBuchi(true, postState);
                    }
                    todo.add(new Pair<>(key, postState)); // add it to later add the successors
                }
                postStateIds[i++] = postState.getId();
//                System.out.println(postState.getId());
            }
            if (edge == null) { // the inital call
                return aba.createAndAddDirectEdge(pre.getId(), type, transitionLabel.getId(), false, postStateIds);
            } else {
                ABAOperatorState op = (ABAOperatorState) pre;// since edge!=null this can only be an ABAOperatorState
                if (k == 1) { // when there is only one successor we don't want to have the operator
                    edge.addABAEdge(op, aba.createEndEdge(op, aba.getState(postStateIds[0])));
                } else {
//                System.out.println("drin");
                    ABAIntermediateEndEdge intEdge = aba.createIntermediateEdge(op, type);
                    edge.addABAEdge(op, intEdge);
                    ABAOperatorState postOp = (ABAOperatorState) intEdge.getPost(); // we just created this operator
                    for (String postStateId : postStateIds) {
                        edge.addABAEdge(postOp, aba.createEndEdge(postOp, aba.getState(postStateId)));
                    }
                }
                return edge;
            }
        } else if (formula instanceof PositiveBooleanFormulaBinary) {
            PositiveBooleanFormulaBinary f = (PositiveBooleanFormulaBinary) formula;
            ABAOperatorState.TYPE type = f.getOp() == Binary.AND ? ABAOperatorState.TYPE.ALL : ABAOperatorState.TYPE.EXISTS;
            if (edge == null) { // the initial call
//                System.out.println("edge =null " + pre.getId() + "  " + transitionLabel.getId());
                edge = aba.createAndAddStartEdge(pre.getId(), type, transitionLabel.getId());
//                System.out.println(edge.toDot());
                ABAOperatorState opState = edge.getStart().getPost();
                addEdgesRecursively(aba, opState, f.getPhi1(), kripkeEdges, transitionLabel, todo, edge);
                addEdgesRecursively(aba, opState, f.getPhi2(), kripkeEdges, transitionLabel, todo, edge);
            } else {
                ABAOperatorState op = (ABAOperatorState) pre;// since edge!=null this can only be an ABAOperatorState
                ABAIntermediateEndEdge intEdge = aba.createIntermediateEdge(op, type);
                edge.addABAEdge(op, intEdge);
                addEdgesRecursively(aba, intEdge.getPost(), f.getPhi1(), kripkeEdges, transitionLabel, todo, edge);
                addEdgesRecursively(aba, intEdge.getPost(), f.getPhi2(), kripkeEdges, transitionLabel, todo, edge);
            }
            return edge;
        } else {
            throw new NotTransformableException("The given tree and Kripke structure cannot be transformed into the alternating Buchi automaton."
                    + " Is the tree created from a CTL formula?"
                    + " At this part the formula should either be a 'ParameterizedPositiveBooleanFormula'"
                    + " or a 'PositiveBooleanFormulaBinary'.");
        }
    }
}

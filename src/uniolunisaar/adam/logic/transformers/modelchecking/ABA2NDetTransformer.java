package uniolunisaar.adam.logic.transformers.modelchecking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormulaAtom;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanConstants;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators.Binary;
import uniolunisaar.adam.ds.automata.BuchiAutomaton;
import uniolunisaar.adam.ds.automata.BuchiState;
import uniolunisaar.adam.ds.automata.StringLabel;
import uniolunisaar.adam.ds.aba.ABAState;
import uniolunisaar.adam.ds.aba.AlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.aba.IABALabeledEdge;
import uniolunisaar.adam.logic.solver.PositiveBooleanFormulaSolverBDD;

/**
 *
 * @author Manuel Gieseking
 */
public class ABA2NDetTransformer {

    private static class TransformerState {

        private final BuchiState newState;
        private final List<ABAState> X;
        private final List<ABAState> W;

        public TransformerState(BuchiState newState, List<ABAState> X, List<ABAState> W) {
            this.newState = newState;
            this.X = X;
            this.W = W;
        }

        public BuchiState getNewState() {
            return newState;
        }

        public List<ABAState> getX() {
            return X;
        }

        public List<ABAState> getW() {
            return W;
        }

        @Override
        public String toString() {
            return newState.getId();
        }

    }

    public static BuchiAutomaton transform(AlternatingBuchiAutomaton<? extends IABALabeledEdge> aba, boolean withMaxSatSolver) {
        LinkedList<TransformerState> todo = new LinkedList<>();
        List<ABAState> buchiStates = aba.getBuchiStates();
        Set<String> alphabet = aba.getAlphabet();

        BuchiAutomaton out = new BuchiAutomaton(aba.getName() + "ndet");
        // add initial states
        for (ABAState initialState : aba.getInitialStates()) {
            BuchiState newInit = out.createAndAddState("[" + initialState.getId() + "],[]", false);
            out.addInitialState(newInit);
            List<ABAState> list = new ArrayList<>();
            list.add(initialState);
            todo.add(new TransformerState(newInit, list, new ArrayList<>()));
        }

        while (!todo.isEmpty()) {
            TransformerState state = todo.pop();
//            System.out.println("pop "+state.toString());
            // loop for each element in the alphabet in the special case of the ([],[]) state
            if (state.getW().isEmpty() && state.getX().isEmpty()) {
                for (String label : alphabet) {
                    out.createAndAddEdge(state.getNewState().getId(), new StringLabel(label), state.getNewState().getId(), false);
                }
                continue;
            }
            // get for all labels the corresponding list successors states (a state is a list of states)
            Map<String, List<List<ABAState>>> allSuccs = (withMaxSatSolver) ? getSuccessorsMaxSatSolver(aba, state.getX()) : getSuccessorsByPaths(aba, state.getX());
            for (Map.Entry<String, List<List<ABAState>>> entry : allSuccs.entrySet()) { // for each sigma
                String label = entry.getKey();
                List<List<ABAState>> all_x_ = entry.getValue();
                if (all_x_.isEmpty()) { // there is no successor (especially the false case)
                    continue;
                }
                for (List<ABAState> x_ : all_x_) { // for each x_ which satisfies the relation                    
                    if (state.getW().isEmpty()) { // the successor for the empty w set
                        // create w_
                        List<ABAState> w_ = new ArrayList<>(x_);
                        w_.removeAll(buchiStates);
                        String key = x_.toString() + "," + w_.toString();
                        BuchiState newState;
                        if (!out.containsState(key)) {
                            newState = out.createAndAddState(key, false);
                            todo.add(new TransformerState(newState, x_, w_));
                        } else {
                            newState = out.getState(key);
                        }
                        out.createAndAddEdge(state.getNewState().getId(), new StringLabel(label), key, false);
                        // mark as buchi
                        if (w_.isEmpty()) {
                            out.setBuchi(true, newState);
                        }
                    } else { // the successors if w is not empty
                        Map<String, List<List<ABAState>>> allWSuccs = (withMaxSatSolver) ? getSuccessorsMaxSatSolver(aba, state.getW()) : getSuccessorsByPaths(aba, state.getW());
//                        System.out.println("%%");
//                        System.out.println("for x " + x_.toString());
//                        System.out.println("all w succs " + allWSuccs.toString());
                        // only those with the same label
//                        System.out.println("label "+label);
                        List<List<ABAState>> all_w_ = allWSuccs.get(label);                        
                        if(all_w_== null) {
                            continue;
                        }
                        for (List<ABAState> w_ : all_w_) { // for all the x_ and w_ combinations add a new state
                            w_.removeAll(buchiStates); // \F
                            w_.retainAll(x_);  // subseteq X_
                            String key = x_.toString() + "," + w_.toString();
                            BuchiState newState;
                            if (!out.containsState(key)) { // if not already existing
                                newState = out.createAndAddState(key, false);
                                todo.add(new TransformerState(newState, x_, w_));
                            } else {
                                newState = out.getState(key);
                            }
                            out.createAndAddEdge(state.getNewState().getId(), new StringLabel(label), key, false);
                            // mark as buchi
                            if (w_.isEmpty()) {
                                out.setBuchi(true, newState);
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    private static Map<String, List<IABALabeledEdge>> groupSuccessorEdges(AlternatingBuchiAutomaton<? extends IABALabeledEdge> aba, List<ABAState> states) {
        // collect the edges with the same label
        Map<String, List<IABALabeledEdge>> edgesPerLabel = new HashMap<>();
        for (ABAState xState : states) {// get all postset edges
            List<? extends IABALabeledEdge> postset = aba.getPostset(xState);
            if (postset == null) { // no successors
                continue;
            }
            for (IABALabeledEdge edge : postset) {
                List<IABALabeledEdge> succs = edgesPerLabel.get(edge.getLabel());
                if (succs == null) {
                    succs = new ArrayList<>();
                    edgesPerLabel.put(edge.getLabel(), succs);
                }
                succs.add(edge);
            }
        }
        return edgesPerLabel;
    }

    private static Map<String, List<List<ABAState>>> getSuccessorsMaxSatSolver(AlternatingBuchiAutomaton<? extends IABALabeledEdge> aba, List<ABAState> states) {
        // collect the edges with the same label
        Map<String, List<IABALabeledEdge>> edgesPerLabel = groupSuccessorEdges(aba, states);

        // for all this packages combine the elements with an AND
        Map<String, List<List<ABAState>>> map = new HashMap<>();
        for (Map.Entry<String, List<IABALabeledEdge>> entry : edgesPerLabel.entrySet()) {
            String label = entry.getKey();
            List<IABALabeledEdge> edgesWithSameLabel = entry.getValue();
            IPositiveBooleanFormula[] formulas = new IPositiveBooleanFormula[edgesWithSameLabel.size()];
            for (int i = 0; i < formulas.length; i++) {
                formulas[i] = edgesWithSameLabel.get(i).getPositiveBooleanFormula();
            }
            IPositiveBooleanFormula phi = PositiveBooleanFormulaFactory.createBinaryFormula(Binary.AND, formulas);
            if (phi instanceof PositiveBooleanConstants.True) {           // special case true -> add the empty set
                List<List<ABAState>> succs = new ArrayList<>();
                succs.add(new ArrayList<>());
                map.put(label, succs);
                continue;
            } else if (phi instanceof PositiveBooleanConstants.False) {             // special case false -> have no successor
                map.put(label, new ArrayList<>());
                continue;
            }
            // now solve this formula (all solutions), each soluation creates one successor (is a list of states)
//            throw new RuntimeException("The All-Sat problem is not yet implemented.");
            List<List<IPositiveBooleanFormulaAtom>> solutions = PositiveBooleanFormulaSolverBDD.solve(phi);
            // cast todo: think of s.th. better
            List<List<ABAState>> casted = new ArrayList<>();
            for (List<IPositiveBooleanFormulaAtom> solution : solutions) {
                List<ABAState> cast = new ArrayList<>();
                for (IPositiveBooleanFormulaAtom sol : solution) {
                    cast.add((ABAState) sol); // the formula was created like that
                }
                casted.add(cast);
            }
            map.put(label, casted);
        }

        return map;
    }

    private static Map<String, List<List<ABAState>>> getSuccessorsByPaths(AlternatingBuchiAutomaton<? extends IABALabeledEdge> aba, List<ABAState> states) {
        // collect the edges with the same label
        Map<String, List<IABALabeledEdge>> edgesPerLabel = groupSuccessorEdges(aba, states);

        // For each of these groups get the list of successor states
        Map<String, List<List<ABAState>>> map = new HashMap<>();
        for (Map.Entry<String, List<IABALabeledEdge>> entry : edgesPerLabel.entrySet()) {
            String label = entry.getKey();
            List<IABALabeledEdge> edgesWithSameLabel = entry.getValue();
        }
        throw new RuntimeException("The approach getting the successors by path is not yet implemented.");
//        return map;
    }
}

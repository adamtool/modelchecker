package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;

/**
 *
 * @author Manuel Gieseking
 */
public class GeneralAlternatingBuchiAutomaton extends AlternatingBuchiAutomaton<IABAHyperEdge> {

    public GeneralAlternatingBuchiAutomaton(String name) {
        super(name);
    }
//
//    public ABAHyperEdge createAndAddDirectEdge(String preStateID, String label, boolean check, IPositiveBooleanFormula formula) {
//    }

    public ABAIntermediateEndEdge createAndAddIntermediateEdge(ABAOperatorState pre, ABAOperatorState.TYPE postType) {
        return new ABAIntermediateEndEdge(pre, new ABAOperatorState(postType));
    }

    public ABAIntermediateEndEdge createAndAddEndEdge(ABAOperatorState pre, ABAState post) {
        return new ABAIntermediateEndEdge(pre, post);
    }

    public ABAHyperEdge createAndAddStartEdge(String preStateID, ABAOperatorState.TYPE type, String label) {
        Map<ABAState, Set<IABAHyperEdge>> edges = getEdges();
        ABAState pre = getStates().get(preStateID);
        Set<IABAHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new HashSet<>();
            edges.put(pre, postEdges);
        }
        return new ABAHyperEdge(new ABAStartEdge(pre, label, new ABAOperatorState(type)));
    }

    public ABAHyperEdge createAndAddDirectEdge(String preStateID, ABAOperatorState.TYPE type, String label, boolean check, String... postStateIDs) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, Set<IABAHyperEdge>> edges = getEdges();
        ABAState pre = states.get(preStateID);

        // check
        if (check) {
            if (states.containsKey(preStateID)) {
                // check the existenz of all given successors
                for (String postStateID : postStateIDs) {
                    if (!states.containsKey(postStateID)) {
                        throw new StructureException("There is no state with ID '" + postStateID + "'");
                    }
                }
            } else {
                throw new StructureException("There is no state with ID '" + preStateID + "'");
            }
        }

        // create and add
        Set<IABAHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new HashSet<>();
            edges.put(pre, postEdges);
        }
        ABAOperatorState op = new ABAOperatorState(type);
        ABAStartEdge start = new ABAStartEdge(pre, label, op);
        ABAHyperEdge edge = new ABAHyperEdge(start);
        for (String post : postStateIDs) {
            if (check) {
                edge.addABAEdgeAndCheckConnectivity(new ABAIntermediateEndEdge(op, states.get(post)));
            } else {
                edge.addABAEdge(new ABAIntermediateEndEdge(op, states.get(post)));
            }
        }
        postEdges.add(edge);
        return edge;
    }

    public ABATrueFalseEdge createAndAddSpecialEdge(String preStateID, ABATrueFalseEdge.Type type, boolean check) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, Set<IABAHyperEdge>> edges = getEdges();
        if (check && !states.containsKey(preStateID)) {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
        ABAState pre = states.get(preStateID);
        Set<IABAHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new HashSet<>();
            edges.put(pre, postEdges);
        }
        ABATrueFalseEdge edge = new ABATrueFalseEdge(pre, type);
        postEdges.add(edge);
        return edge;
    }

   
}

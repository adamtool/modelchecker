package uniolunisaar.adam.ds.aba;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uniol.apt.adt.exception.StructureException;

/**
 *
 * @author Manuel Gieseking
 */
public class GeneralAlternatingBuchiAutomaton extends AlternatingBuchiAutomaton<IABALabeledHyperEdge> {

    public GeneralAlternatingBuchiAutomaton(String name) {
        super(name);
    }
//
//    public ABAHyperEdge createAndAddDirectEdge(String preStateID, String label, boolean check, IPositiveBooleanFormula formula) {
//    }

    public ABAIntermediateEndEdge createIntermediateEdge(ABAOperatorState pre, ABAOperatorState.TYPE postType) {
        return new ABAIntermediateEndEdge(pre, new ABAOperatorState(postType));
    }

    public ABAIntermediateEndEdge createEndEdge(ABAOperatorState pre, ABAState post) {
        return new ABAIntermediateEndEdge(pre, post);
    }

    public ABAHyperEdge createAndAddStartEdge(String preStateID, ABAOperatorState.TYPE type, String label) {
        Map<ABAState, List<IABALabeledHyperEdge>> edges = getEdges();
        ABAState pre = getStates().get(preStateID);
        List<IABALabeledHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new ArrayList<>();
            edges.put(pre, postEdges);
        }
        ABAHyperEdge edge = new ABAHyperEdge(new ABAStartEdge(pre, label, new ABAOperatorState(type)));
        postEdges.add(edge);
        return edge;
    }

    public IABALabeledHyperEdge createAndAddDirectEdge(String preStateID, ABAOperatorState.TYPE type, String label, boolean check, String... postStateIDs) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, List<IABALabeledHyperEdge>> edges = getEdges();
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
        List<IABALabeledHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new ArrayList<>();
            edges.put(pre, postEdges);
        }

        // when there is only one successor make it a standard edge
        if (postStateIDs.length == 1) {
            IABALabeledHyperEdge edge = new ABADirectEdge(pre, label, states.get(postStateIDs[0]));
            postEdges.add(edge);
            return edge;
        } else {
            ABAOperatorState op = new ABAOperatorState(type);
            ABAStartEdge start = new ABAStartEdge(pre, label, op);
            ABAHyperEdge edge = new ABAHyperEdge(start);
            for (String post : postStateIDs) {
                if (check) {
                    edge.addABAEdgeAndCheckConnectivity(op, new ABAIntermediateEndEdge(op, states.get(post)));
                } else {
                    edge.addABAEdge(op, new ABAIntermediateEndEdge(op, states.get(post)));
                }
            }
            postEdges.add(edge);
            return edge;
        }
    }

    public ABATrueFalseEdge createAndAddSpecialEdge(String preStateID, ABATrueFalseEdge.Type type, String label, boolean check) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, List<IABALabeledHyperEdge>> edges = getEdges();
        if (check && !states.containsKey(preStateID)) {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
        ABAState pre = states.get(preStateID);
        List<IABALabeledHyperEdge> postEdges = edges.get(pre);
        if (postEdges == null) {
            postEdges = new ArrayList<>();
            edges.put(pre, postEdges);
        }
        ABATrueFalseEdge edge = new ABATrueFalseEdge(pre, type, label);
        postEdges.add(edge);
        return edge;
    }

}

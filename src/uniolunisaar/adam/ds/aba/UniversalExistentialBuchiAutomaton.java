package uniolunisaar.adam.ds.aba;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uniol.apt.adt.exception.StructureException;

/**
 * A deterministic alternating Buchi automaton
 *
 * @author Manuel Gieseking
 */
public class UniversalExistentialBuchiAutomaton extends AlternatingBuchiAutomaton<UniversalExistentialEdge> {

    public UniversalExistentialBuchiAutomaton(String name) {
        super(name);
    }

    public UniversalExistentialEdge createAndAddEdge(String preStateID, UniversalExistentialEdge.TYPE type, String label, String... postStateIDs) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, List<UniversalExistentialEdge>> edges = getEdges();
        if (states.containsKey(preStateID)) {
            // check the existenz of all given successors
            List<ABAState> post = new ArrayList<>();
            for (String postStateID : postStateIDs) {
                if (!states.containsKey(postStateID)) {
                    throw new StructureException("There is no state with ID '" + postStateID + "'");
                }
                post.add(states.get(postStateID));
            }
            ABAState pre = states.get(preStateID);
            List<UniversalExistentialEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new ArrayList<>();
                edges.put(pre, postEdges);
            }
            UniversalExistentialEdge edge = new UniversalExistentialEdge(pre, type, label, post);
            postEdges.add(edge);
            return edge;

        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

    public UniversalExistentialEdge createSpecialEdge(String preStateID, UniversalExistentialEdge.Special special, String label) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, List<UniversalExistentialEdge>> edges = getEdges();
        if (states.containsKey(preStateID)) {
            ABAState pre = states.get(preStateID);
            List<UniversalExistentialEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new ArrayList<>();
                edges.put(pre, postEdges);
            }
            UniversalExistentialEdge edge = new UniversalExistentialEdge(pre, special, label);
            postEdges.add(edge);
            return edge;
        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

}

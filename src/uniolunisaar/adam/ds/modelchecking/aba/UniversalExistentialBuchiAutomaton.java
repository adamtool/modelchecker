package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        Map<ABAState, Set<UniversalExistentialEdge>> edges = getEdges();
        if (states.containsKey(preStateID)) {
            // check the existenz of all given successors
            Set<ABAState> post = new HashSet<>();
            for (String postStateID : postStateIDs) {
                if (!states.containsKey(postStateID)) {
                    throw new StructureException("There is no state with ID '" + postStateID + "'");
                }
                post.add(states.get(postStateID));
            }
            ABAState pre = states.get(preStateID);
            Set<UniversalExistentialEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new HashSet<>();
                edges.put(pre, postEdges);
            }
            UniversalExistentialEdge edge = new UniversalExistentialEdge(pre, type, label, post);
            postEdges.add(edge);
            return edge;

        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

    public UniversalExistentialEdge createSpecialEdge(String preStateID, UniversalExistentialEdge.Special special) {
        Map<String, ABAState> states = getStates();
        Map<ABAState, Set<UniversalExistentialEdge>> edges = getEdges();
        if (states.containsKey(preStateID)) {
            ABAState pre = states.get(preStateID);
            Set<UniversalExistentialEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new HashSet<>();
                edges.put(pre, postEdges);
            }
            UniversalExistentialEdge edge = new UniversalExistentialEdge(pre, special);
            postEdges.add(edge);
            return edge;
        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

}

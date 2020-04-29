package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.tools.Logger;

/**
 * A deterministic alternating Buchi automaton
 *
 * @author Manuel Gieseking
 */
public class AlternatingBuchiAutomaton {

    private final Map<String, ABAState> states;
    private final Map<ABAState, Set<ABAEdge>> edges;
    private final String name;
    private ABAState init;

    public AlternatingBuchiAutomaton(String name) {
        states = new HashMap<>();
        edges = new HashMap<>();
        this.name = name;
    }

    public ABAState getInit() {
        return init;
    }

    public void setInit(ABAState init) {
        if (!states.containsKey(init.getId())) {
            throw new StructureException("The state '" + init.getId() + "' does not belong to the alternating automaton '" + name + "'.");
        }
        this.init = init;
    }

    public ABAState createAndAddState(String id) {
        if (states.containsKey(id)) {
            Logger.getInstance().addWarning("A state with the id '" + id + "' already exists. This one is returned.");
            return states.get(id);
        } else {
            ABAState state = new ABAState(id);
            states.put(id, state);
            return state;
        }
    }

    public ABAEdge createAndAddEdge(String preStateID, ABAEdge.TYPE type, String label, String... postStateIDs) {
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
            Set<ABAEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new HashSet<>();
                edges.put(pre, postEdges);
            }
            ABAEdge edge = new ABAEdge(pre, type, label, post);
            postEdges.add(edge);
            return edge;

        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

    public ABAEdge createSpecialEdge(String preStateID, ABAEdge.Special special) {
        if (states.containsKey(preStateID)) {
            ABAState pre = states.get(preStateID);
            Set<ABAEdge> postEdges = edges.get(pre);
            if (postEdges == null) {
                postEdges = new HashSet<>();
                edges.put(pre, postEdges);
            }
            ABAEdge edge = new ABAEdge(special);
            postEdges.add(edge);
            return edge;
        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }
}

package uniolunisaar.adam.ds.kripkestructure;

import uniolunisaar.adam.ds.automata.NodeLabel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtKripkeStructure extends LabeledKripkeStructure<NodeLabel, TransitionLabel> {

    public PnwtKripkeStructure(String name) {
        super(name);
    }

    public KripkeState<NodeLabel> createAndAddState(String id, NodeLabel... labels) {
        Map<String, KripkeState<NodeLabel>> states = getStates();
        if (states.containsKey(id)) {
            Logger.getInstance().addWarning("A state with the id '" + id + "' already exists. This one is returned.");
            return states.get(id);
        } else {
            KripkeState<NodeLabel> state = new KripkeState<>(id, labels);
            states.put(id, state);
            return state;
        }
    }

    public LabeledKripkeEdge<NodeLabel, TransitionLabel> createAndAddEdge(String preStateID, TransitionLabel label, String postStateID) {
        Map<String, KripkeState<NodeLabel>> states = getStates();
        if (states.containsKey(preStateID)) {
            if (states.containsKey(postStateID)) {
                KripkeState<NodeLabel> pre = states.get(preStateID);
                KripkeState<NodeLabel> post = states.get(postStateID);
                Set<LabeledKripkeEdge<NodeLabel, TransitionLabel>> edges = getEdges().get(pre);
                // check if the edge already exists
                if (edges != null) {
                    for (LabeledKripkeEdge<NodeLabel, TransitionLabel> edge : edges) {
                        if (edge.getPost().equals(post) && edge.getLabel().getId().equals(label.getId())) {
                            return edge; // found it, give it back
                        }
                    }
                }
                LabeledKripkeEdge<NodeLabel, TransitionLabel> edge = new LabeledKripkeEdge<>(pre, label, post);
                if (edges == null) {
                    edges = new HashSet<>();
                    getEdges().put(pre, edges);
                }
                edges.add(edge);
                return edge;
            } else {
                throw new StructureException("There is no state with ID '" + postStateID + "'");
            }
        } else {
            throw new StructureException("There is no state with ID '" + preStateID + "'");
        }
    }

    public boolean stateExists(String id) {
        return getStates().containsKey(id);
    }

    public KripkeState<NodeLabel> getState(String id) {
        return getStates().get(id);
    }

}

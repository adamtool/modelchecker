package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Manuel Gieseking
 * @param <SL>
 * @param <EL>
 */
public class LabeledKripkeStructure<SL extends ILabel, EL extends ILabel> extends KripkeStructure<SL, LabeledKripkeEdge<SL, EL>> {

    public LabeledKripkeStructure(String name) {
        super(name);
    }

    public Map<EL, Set<LabeledKripkeEdge<SL, EL>>> getPostEdgesWithSameLabel(KripkeState<SL> pre) {
        Map<EL, Set<LabeledKripkeEdge<SL, EL>>> out = new HashMap<>();
        Set<LabeledKripkeEdge<SL, EL>> edges = getEdges().get(pre);
        if (edges == null) { // no successors
            return out;
        }
        for (LabeledKripkeEdge<SL, EL> edge : edges) {
            Set<LabeledKripkeEdge<SL, EL>> mappedEdges = out.get(edge.getLabel());
            if (mappedEdges == null) {
                mappedEdges = new HashSet<>();
                out.put(edge.getLabel(), mappedEdges);
            }
            mappedEdges.add(edge);
        }
        return out;
    }

}

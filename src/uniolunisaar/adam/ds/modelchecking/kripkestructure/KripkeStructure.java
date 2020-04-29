package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Manuel Gieseking
 * @param <SL> state label
 * @param <E> edge
 */
public class KripkeStructure<SL extends ILabel, E extends KripkeEdge<SL>> {

    private final Set<KripkeState<SL>> states;
    private final Map<KripkeState<SL>, E> edges;

    public KripkeStructure() {
        this.states = new HashSet<>();
        this.edges = new HashMap<>();
    }

}

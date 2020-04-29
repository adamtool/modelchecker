package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.HashMap;
import java.util.Map;
import uniol.apt.adt.exception.StructureException;

/**
 *
 * @author Manuel Gieseking
 * @param <SL> state label
 * @param <E> edge
 */
public class KripkeStructure<SL extends ILabel, E extends KripkeEdge<SL>> {

    private final Map<String, KripkeState<SL>> states;
    private final Map<KripkeState<SL>, E> edges;
    private KripkeState<SL> init;

    public KripkeStructure() {
        this.states = new HashMap<>();
        this.edges = new HashMap<>();
    }

    public KripkeState<SL> getInit() {
        return init;
    }

    public void setInit(KripkeState<SL> init) {
        if (!states.containsKey(init.getId())) {
            throw new StructureException("The state '" + init.getId() + "' does not belong to the Kripke structure.");
        }
        this.init = init;
    }

    public Map<String, KripkeState<SL>> getStates() {
        return states;
    }

    public Map<KripkeState<SL>, E> getEdges() {
        return edges;
    }

}

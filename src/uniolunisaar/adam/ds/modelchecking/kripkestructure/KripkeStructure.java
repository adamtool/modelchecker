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

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph GraphGame {\n");

        // States
        sb.append("#states\n");
        for (String id : states.keySet()) {
            KripkeState<SL> state = states.get(id);
            sb.append(state.toDot());
        }
        sb.append("overlap=false\n");
        sb.append("label=\"").append("states").append("\"\n");
        sb.append("fontsize=12\n\n");
        sb.append("\n#flows\n");

        // Edges
        for (E edge : edges.values()) {
            sb.append(edge.toDot());
        }
        sb.append("overlap=false\n");
        sb.append("label=\"").append("Kripke structure").append("\"\n");
        sb.append("fontsize=12\n\n");
        sb.append("}");
        return sb.toString();
    }

}

package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;

/**
 *
 * @author Manuel Gieseking
 * @param <SL> state label
 * @param <E> edge
 */
public class KripkeStructure<SL extends ILabel, E extends KripkeEdge<SL>> {

    private final Map<String, KripkeState<SL>> states;
    private final Map<KripkeState<SL>, Set<E>> edges;
    private KripkeState<SL> init;
    private final String name;

    public KripkeStructure(String name) {
        this.states = new HashMap<>();
        this.edges = new HashMap<>();
        this.name = name;
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

    public Map<KripkeState<SL>, Set<E>> getEdges() {
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
        for (Set<E> edges : edges.values()) {
            for (E edge : edges) {
                sb.append(edge.toDot());
            }
        }
        sb.append("overlap=false\n");
        sb.append("label=\"").append("Kripke structure: ").append(name).append("\"\n");
        sb.append("fontsize=12\n\n");
        sb.append("}");
        return sb.toString();
    }

    public String getName() {
        return name;
    }

}

package uniolunisaar.adam.ds.kripkestructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.ds.automata.ILabel;
import uniolunisaar.adam.util.IDotSaveable;

/**
 *
 * @author Manuel Gieseking
 * @param <SL> state label
 * @param <E> edge
 */
public class KripkeStructure<SL extends ILabel, E extends KripkeEdge<SL>> implements IDotSaveable {

    private final Map<String, KripkeState<SL>> states;
    private final Map<KripkeState<SL>, Set<E>> edges;
    private final Set<KripkeState<SL>> inits;
    private final String name;

    public KripkeStructure(String name) {
        this.states = new HashMap<>();
        this.edges = new HashMap<>();
        this.name = name;
        this.inits = new HashSet<>();
    }

    public Set<KripkeState<SL>> getInitialStates() {
        return inits;
    }

    public void clearInitialStates() {
        inits.clear();
    }

    public void addInitialState(KripkeState<SL> init) {
        if (!states.containsKey(init.getId())) {
            throw new StructureException("The state '" + init.getId() + "' does not belong to the Kripke structure.");
        }
        this.inits.add(init);
    }

    public Map<String, KripkeState<SL>> getStates() {
        return states;
    }

    public Map<KripkeState<SL>, Set<E>> getEdges() {
        return edges;
    }

    public int getNumberOfEdges() {
        int nb_edges = 0;
        for (Set<E> post : edges.values()) {
            nb_edges += post.size();
        }
        return nb_edges;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph KripkeStructure {\n");

        // States
        sb.append("#states\n");
        int counter = 0;
        for (KripkeState<SL> init : inits) {
            sb.append(this.hashCode()).append("").append(counter++).append(" [label=\"\", shape=point]").append("\n"); // for showing an initial arc
        }
        for (String id : states.keySet()) {
            KripkeState<SL> state = states.get(id);
            sb.append(state.toDot());
        }

        // Edges
        sb.append("\n#flows\n");
        counter = 0;
        for (KripkeState<SL> init : inits) {
            sb.append(this.hashCode()).append("").append(counter++).append("->").append(init.getId().hashCode()).append("\n");// add the inits arc
        }
        for (Set<E> es : edges.values()) {
            for (E edge : es) {
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

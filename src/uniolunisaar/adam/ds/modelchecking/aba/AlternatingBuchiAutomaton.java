package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.DotSaveable;

/**
 * A deterministic alternating Buchi automaton
 *
 * @author Manuel Gieseking
 * @param <EDGE>
 */
public class AlternatingBuchiAutomaton<EDGE extends IABAEdge> implements DotSaveable {

    private final Map<String, ABAState> states;
    private final Map<ABAState, Set<EDGE>> edges;
    private final String name;
    private final Set<ABAState> inits;

    public AlternatingBuchiAutomaton(String name) {
        states = new HashMap<>();
        edges = new HashMap<>();
        this.name = name;
        this.inits = new HashSet<>();
    }

    public Set<ABAState> getInitialStates() {
        return inits;
    }

    public void clearInitialStates() {
        inits.clear();
    }

    public void addInitialStates(ABAState... inits) {
        for (ABAState init : inits) {
            if (!states.containsKey(init.getId())) {
                throw new StructureException("The state '" + init.getId() + "' does not belong to the alternating automaton '" + name + "'.");
            }
            this.inits.add(init);
        }
    }

    public String getName() {
        return name;
    }

    public void setBuchi(boolean buchi, ABAState... states) {
        for (ABAState state : states) {
            state.setBuchi(buchi);
        }
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

    Map<String, ABAState> getStates() {
        return states;
    }

    Map<ABAState, Set<EDGE>> getEdges() {
        return edges;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph AlternatingBuchiAutomaton {\n");

        // States
        sb.append("#states\n");
        int counter = 0;
        for (ABAState init : inits) {
            sb.append(this.hashCode()).append("").append(counter++).append(" [label=\"\", shape=point]").append("\n"); // for showing an initial arc
        }
        for (String id : states.keySet()) {
            ABAState state = states.get(id);
            sb.append(state.toDot());
        }

        // Edges
        sb.append("\n#flows\n");
        counter = 0;
        for (ABAState init : inits) {
            sb.append(this.hashCode()).append("").append(counter++).append("->").append(init.getId().hashCode()).append("\n");// add the inits arc
        }
        for (Set<EDGE> es : edges.values()) {
            for (EDGE edge : es) {
                sb.append(edge.toDot());
            }
        }
        sb.append("overlap=false\n");
        sb.append("label=\"").append("Alternating Buchi automaton: ").append(name).append("\"\n");
        sb.append("fontsize=12\n\n");
        sb.append("}");
        return sb.toString();
    }
}

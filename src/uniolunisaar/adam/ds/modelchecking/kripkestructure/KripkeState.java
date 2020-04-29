package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Manuel Gieseking
 * @param <L>
 */
public class KripkeState<L extends ILabel> {

    private final Set<L> labels = new HashSet<>();
    private final String id;

    KripkeState(String id, L[] labels) {
        this.id = id;
        this.labels.addAll(Arrays.asList(labels));
    }

    KripkeState(String id, L label) {
        this.id = id;
        this.labels.add(label);
    }

    public Set<L> getLabels() {
        return labels;
    }

    public String getId() {
        return id;
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String color = "black";
        sb.append(id.hashCode()).append("[shape=circle, color=").append(color);
        sb.append(", height=0.5, width=0.5, fixedsize=false,  penwidth=").append(1);
        sb.append(", label=\"").append(id).append("\"");
        sb.append(", xlabel=").append("\"").append(labels.toString()).append("\"");
        sb.append("];\n");
        return sb.toString();
    }

}

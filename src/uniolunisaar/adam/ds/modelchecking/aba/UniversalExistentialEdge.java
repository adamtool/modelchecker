package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.Set;

/**
 * This method covers all edges in one for the subclass of having only
 * existential or universal choices.
 *
 * @author Manuel Gieseking
 */
public class UniversalExistentialEdge implements IABAEdge {

    public enum TYPE {
        ALL,
        EXISTS
    }

    public enum Special {
        TRUE,
        FALSE
    }

    private final ABAState pre;
    private final TYPE type;
    private final String label;
    private final Set<ABAState> post;
    private final Special special;

    public UniversalExistentialEdge(ABAState pre, Special special) {
        this.pre = pre;
        this.type = null;
        this.label = null;
        this.post = null;
        this.special = special;
    }

    UniversalExistentialEdge(ABAState pre, TYPE type, String label, Set<ABAState> post) {
        this.pre = pre;
        this.type = type;
        this.label = label;
        this.post = post;
        this.special = null;
    }

    public ABAState getPre() {
        return pre;
    }

    public TYPE getType() {
        return type;
    }

    public Set<ABAState> getPost() {
        return post;
    }

    public boolean isSpecial() {
        return special != null;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("#states\n");
        if (special != null) {
            sb.append(this.hashCode()).append(" [label=\"").append(special.name()).append("\", color=white]").append("\n");
            sb.append("\n#flows\n");
            sb.append(getPre().getId().hashCode()).append("->").append(this.hashCode());
        } else {
            // edge to the node
            String typeLabel = type == TYPE.ALL ? "⋀" : "⋁";
            sb.append(this.hashCode()).append(" [label=\"").append(typeLabel).append("\", shape=square]").append("\n");
            sb.append("\n#flows\n");
            sb.append(getPre().getId().hashCode()).append("->").append(this.hashCode()).append("\n");
            sb.append("[label=\"").append(label).append("\"]");
            // edge to the successors
            sb.append(this.hashCode()).append("->").append("{");
            for (ABAState aBAState : post) {
                sb.append(aBAState.getId().hashCode()).append(",");
            }
            sb.replace(sb.length() - 1, sb.length(), "}");
        }
        sb.append("\n");
        return sb.toString();
    }

}

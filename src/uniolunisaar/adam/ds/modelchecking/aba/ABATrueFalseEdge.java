package uniolunisaar.adam.ds.modelchecking.aba;

/**
 *
 * @author Manuel Gieseking
 */
public class ABATrueFalseEdge implements IABAHyperEdge {

    public enum Type {
        TRUE,
        FALSE
    }

    private final ABAState pre;
    private final Type type;

    ABATrueFalseEdge(ABAState pre, Type type) {
        this.pre = pre;
        this.type = type;
    }

    public ABAState getPre() {
        return pre;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ABATrueFalseEdge{" + "pre=" + pre + ", type=" + type + '}';
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("#states\n");
        sb.append(this.hashCode()).append(" [label=\"").append(type.name()).append("\", color=white]").append("\n");
        sb.append("\n#flows\n");
        sb.append(getPre().getId().hashCode()).append("->").append(this.hashCode());
        sb.append("\n");
        return sb.toString();
    }
}

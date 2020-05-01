package uniolunisaar.adam.ds.modelchecking.aba;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAOperatorState implements IABANode {

    public enum TYPE {
        ALL,
        EXISTS
    }
    private final TYPE type;

    ABAOperatorState(TYPE type) {
        this.type = type;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("#states\n");
        String typeLabel = type == TYPE.ALL ? "⋀" : "⋁";
        sb.append(this.hashCode()).append(" [label=\"").append(typeLabel).append("\", shape=square]").append("\n");
        return sb.toString();
    }

    @Override
    public int getDotIdentifier() {
        return this.hashCode();
    }

    @Override
    public String getId() {
        return "" + this.hashCode();
    }

    public TYPE getType() {
        return type;
    }

}

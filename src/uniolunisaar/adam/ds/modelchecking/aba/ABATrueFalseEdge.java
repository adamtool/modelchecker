package uniolunisaar.adam.ds.modelchecking.aba;

import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;

/**
 *
 * @author Manuel Gieseking
 */
public class ABATrueFalseEdge implements IABALabeledHyperEdge {

    public enum Type {
        TRUE,
        FALSE
    }

    private final ABAState pre;
    private final Type type;
    private final String label;

    ABATrueFalseEdge(ABAState pre, Type type, String label) {
        this.pre = pre;
        this.type = type;
        this.label = label;
    }

    public ABAState getPre() {
        return pre;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Successors getSuccessors() {
        return new Successors(type);
    }

    @Override
    public IPositiveBooleanFormula getPositiveBooleanFormula() {
        return type == Type.TRUE ? PositiveBooleanFormulaFactory.createTrue() : PositiveBooleanFormulaFactory.createFalse();
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
        sb.append("[label=\"").append(label).append("\"]");
        sb.append("\n");
        return sb.toString();
    }
}

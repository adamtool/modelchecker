package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.List;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators;

/**
 * This method covers all edges in one for the subclass of having only
 * existential or universal choices.
 *
 * @author Manuel Gieseking
 */
public class UniversalExistentialEdge implements IABALabeledEdge {

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
    private final List<ABAState> post;
    private final Special special;

    public UniversalExistentialEdge(ABAState pre, Special special, String label) {
        this.pre = pre;
        this.type = null;
        this.label = label;
        this.post = null;
        this.special = special;
    }

    UniversalExistentialEdge(ABAState pre, TYPE type, String label, List<ABAState> post) {
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

    public List<ABAState> getPost() {
        return post;
    }

    public boolean isSpecial() {
        return special != null;
    }

    @Override
    public Successors getSuccessors() {
        if (special == Special.TRUE) {
            return new Successors(ABATrueFalseEdge.Type.TRUE);
        }
        if (special == Special.FALSE) {
            return new Successors(ABATrueFalseEdge.Type.FALSE);
        }
        if (type == TYPE.ALL) {
            Successors succs = new Successors();
            succs.add(post, true);
            return succs;
        }
        if (type == TYPE.EXISTS) {
            Successors succs = new Successors();
            succs.add(post, false);
            return succs;
        }
        throw new StructureException("Not all cases for the edge are considered.");
    }

    @Override
    public IPositiveBooleanFormula getPositiveBooleanFormula() {
        if (special == Special.TRUE) {
            return PositiveBooleanFormulaFactory.createTrue();
        }
        if (special == Special.FALSE) {
            return PositiveBooleanFormulaFactory.createFalse();
        }
        IPositiveBooleanFormula[] formulas = new IPositiveBooleanFormula[post.size()];
        for (int i = 0; i < formulas.length; i++) {
            formulas[i] = post.get(i);
        }
        PositiveBooleanFormulaOperators.Binary op = (type == TYPE.ALL) ? PositiveBooleanFormulaOperators.Binary.AND : PositiveBooleanFormulaOperators.Binary.OR;
        return PositiveBooleanFormulaFactory.createBinaryFormula(op, formulas);
    }

    @Override
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

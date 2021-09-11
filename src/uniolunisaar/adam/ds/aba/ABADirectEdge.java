package uniolunisaar.adam.ds.aba;

import java.util.ArrayList;
import java.util.List;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;

/**
 *
 * @author Manuel Gieseking
 */
public class ABADirectEdge implements IABALabeledHyperEdge {

    private final ABAState pre;
    private final String label;
    private final ABAState post;

    public ABADirectEdge(ABAState pre, String label, ABAState post) {
        this.pre = pre;
        this.label = label;
        this.post = post;
    }

    @Override
    public IPositiveBooleanFormula getPositiveBooleanFormula() {
        return post;
    }

    @Override
    public Successors getSuccessors() {
        Successors successors = new Successors();
        List<ABAState> succ = new ArrayList<>();
        succ.add(post);
        successors.add(succ, true);
        return successors;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(pre.getDotIdentifier()).append("->").append(post.getDotIdentifier()).append("\n");
        sb.append("[label=\"").append(label).append("\"]");
        sb.append("\n");
        return sb.toString();
    }

    public ABAState getPre() {
        return pre;
    }

    public String getLabel() {
        return label;
    }

    public ABAState getPost() {
        return post;
    }

}

package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.Set;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAEdge {

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

    public ABAEdge(Special special) {
        this.pre = null;
        this.type = null;
        this.label = null;
        this.post = null;
        this.special = special;
    }

    ABAEdge(ABAState pre, TYPE type, String label, Set<ABAState> post) {
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

}

package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import uniolunisaar.adam.ds.automata.ILabel;

/**
 *
 * @author Manuel Gieseking
 * @param <L>
 */
public class KripkeEdge<L extends ILabel> {

    private final KripkeState<L> pre;
    private final KripkeState<L> post;

    KripkeEdge(KripkeState<L> pre, KripkeState<L> post) {
        this.pre = pre;
        this.post = post;
    }

    public KripkeState<L> getPre() {
        return pre;
    }

    public KripkeState<L> getPost() {
        return post;
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre().getId().hashCode()).append("->").append(getPost().getId().hashCode());
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return pre.getId() + "->" + post.getId();
    }

}

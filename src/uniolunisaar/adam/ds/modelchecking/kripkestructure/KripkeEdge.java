package uniolunisaar.adam.ds.modelchecking.kripkestructure;

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
        sb.append(pre.getId()).append("->").append(post.getId());
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return pre.getId() + "->" + post.getId();
    }

}

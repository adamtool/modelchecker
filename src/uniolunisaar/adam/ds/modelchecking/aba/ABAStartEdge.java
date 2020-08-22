package uniolunisaar.adam.ds.modelchecking.aba;

/**
 *
 * @author Manuel Gieseking
 */
//public class ABAStartEdge implements IABASubEdge {
public class ABAStartEdge {

    private final ABAState pre;
    private final String label;
    private final ABAOperatorState post;

    ABAStartEdge(ABAState pre, String label, ABAOperatorState post) {
        this.pre = pre;
        this.label = label;
        this.post = post;
    }

//    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(post.toDot());
        sb.append("\n#flows\n");
        sb.append(pre.getDotIdentifier()).append("->").append(post.getDotIdentifier()).append("\n");
        sb.append("[label=\"").append(label).append("\"]");
        sb.append("\n");
        return sb.toString();
    }

//    @Override
    public ABAState getPre() {
        return pre;
    }

    public String getLabel() {
        return label;
    }

//    @Override
    public ABAOperatorState getPost() {
        return post;
    }

    @Override
    public String toString() {
        return "ABAStartEdge{" + "pre=" + pre + ", label=" + label + ", post=" + post + '}';
    }

}

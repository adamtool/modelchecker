package uniolunisaar.adam.ds.aba;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAIntermediateEndEdge implements IABASubEdge {

    private final ABAOperatorState pre;
    private final IABANode post;

    ABAIntermediateEndEdge(ABAOperatorState pre, IABANode post) {
        this.pre = pre;
        this.post = post;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        if (post instanceof ABAOperatorState) {
            sb.append(post.toDot());
            sb.append("\n#flows\n");
        }
//        sb.append("\n#flows\n");
        // edge to all successors
//        sb.append(pre.getDotIdentifier()).append("->").append("{");
//        for (IABANode node : post) {
//            sb.append(node.getDotIdentifier()).append(",");
//        }
//        sb.replace(sb.length() - 1, sb.length(), "}");
        sb.append(pre.getDotIdentifier()).append("->").append(post.getDotIdentifier());
        return sb.toString();
    }

    @Override
    public ABAOperatorState getPre() {
        return pre;
    }

    @Override
    public IABANode getPost() {
        return post;
    }

    @Override
    public String toString() {
        return "ABAIntermediateEndEdge{" + "pre=" + pre + ", post=" + post + '}';
    }

}

package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import uniol.apt.adt.pn.Node;

/**
 *
 * @author Manuel Gieseking
 */
public class NodeLabel implements ILabel {

    private final Node node;

    public NodeLabel(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return node.getId();
    }

}

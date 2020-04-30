package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.Objects;
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
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.node.getId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeLabel other = (NodeLabel) obj;
        if (!Objects.equals(this.node.getId(), other.node.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return node.getId();
    }

}

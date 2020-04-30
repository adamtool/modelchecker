package uniolunisaar.adam.ds.modelchecking.aba;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.exception.StructureException;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAHyperEdge implements IABAHyperEdge {

    private final ABAStartEdge start;
    private final List<IABASubEdge> edges;

    ABAHyperEdge(ABAStartEdge start) {
        this.edges = new ArrayList<>();
        this.start = start;
    }

    public boolean addABAEdge(IABASubEdge edge) {
        return edges.add(edge);
    }

    public boolean addABAEdgeAndCheckConnectivity(IABASubEdge edge) {
        // find preset
        IABANode pre = edge.getPre();
        if (pre.equals(start.getPost())) {
            return addABAEdge(edge);
        } else {
            for (IABASubEdge edge1 : edges) {
                if (edge1.getPost() != null && pre.equals(edge1.getPost())) {
                    return addABAEdge(edge);
                }
            }
        }
        throw new StructureException("The edge '" + edge.toString() + "' is not connected.");
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(start.toDot());
        for (IABASubEdge edge : edges) {
            sb.append(edge.toDot()).append("\n");
        }
        return sb.toString();
    }
}

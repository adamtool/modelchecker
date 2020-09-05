package uniolunisaar.adam.ds.aba;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAHyperEdge implements IABALabeledHyperEdge {

    private final ABAStartEdge start;
    private final Map<ABAOperatorState, List<IABASubEdge>> edges;

    ABAHyperEdge(ABAStartEdge start) {
        this.edges = new HashMap<>();
        this.start = start;
    }

    public boolean addABAEdge(ABAOperatorState op, IABASubEdge edge) {
        List<IABASubEdge> subedges = this.edges.get(op);
        if (subedges == null) {
            subedges = new ArrayList<>();
            this.edges.put(op, subedges);
        }
        return subedges.add(edge);
    }

    public boolean addABAEdgeAndCheckConnectivity(ABAOperatorState op, IABASubEdge edge) {
        // find preset
        IABANode pre = edge.getPre();
        if (pre.equals(start.getPost())) {
            return addABAEdge(op, edge);
        } else {
            for (List<IABASubEdge> subedges : edges.values()) {
                for (IABASubEdge edge1 : subedges) {
                    if (edge1.getPost() != null && pre.equals(edge1.getPost())) {
                        return addABAEdge(op, edge);
                    }
                }
            }
        }
        throw new StructureException("The edge '" + edge.toString() + "' is not connected.");
    }

    @Override
    public IPositiveBooleanFormula getPositiveBooleanFormula() {
        return getPositiveBooleanFormula(start.getPost());
    }

    private IPositiveBooleanFormula getPositiveBooleanFormula(ABAOperatorState op) {
        List<IABASubEdge> subedges = edges.get(op);
        IPositiveBooleanFormula[] formulas = new IPositiveBooleanFormula[subedges.size()];
        int i = 0;
        for (IABASubEdge subedge : subedges) {
            IABANode post = subedge.getPost();
            if (post instanceof ABAState) {
                formulas[i++] = (ABAState) post;
            } else {
                formulas[i++] = getPositiveBooleanFormula((ABAOperatorState) post);
            }
        }
        if (op.getType() == ABAOperatorState.TYPE.ALL) {
            return PositiveBooleanFormulaFactory.createBinaryFormula(PositiveBooleanFormulaOperators.Binary.AND, formulas);
        } else {
            return PositiveBooleanFormulaFactory.createBinaryFormula(PositiveBooleanFormulaOperators.Binary.OR, formulas);
        }
    }

    @Override
    public Successors getSuccessors() {
        return getSuccessorsDirectByEdges();
    }

    private Successors getSuccessorsDirectByEdges() {
        throw new RuntimeException("Not yet implemented");
//        Successors succ = new Successors();
//        ABAOperatorState op = start.getPost();
//        if (op.getType() == ABAOperatorState.TYPE.ALL) {
//            List<List<ABAState>> states = new ArrayList<>();
//            for (IABASubEdge subedge : edges.get(op)) {
////                subedge.
//            }
////            succ.
//        } else {
//
//        }
//        return succ;
    }

    public ABAStartEdge getStart() {
        return start;
    }

    @Override
    public String getLabel() {
        return start.getLabel();
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(start.toDot());
        for (List<IABASubEdge> subedges : edges.values()) {
            for (IABASubEdge edge : subedges) {
                sb.append(edge.toDot()).append("\n");
            }
        }
        return sb.toString();
    }

}

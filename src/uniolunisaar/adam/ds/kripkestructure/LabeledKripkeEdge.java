package uniolunisaar.adam.ds.kripkestructure;

import uniolunisaar.adam.ds.automata.ILabel;

/**
 *
 * @author Manuel Gieseking
 * @param <EL> edge label
 * @param <SL> state label
 */
public class LabeledKripkeEdge<SL extends ILabel, EL extends ILabel> extends KripkeEdge<SL> {

    private final EL label;

    LabeledKripkeEdge(KripkeState<SL> pre, EL label, KripkeState<SL> post) {
        super(pre, post);
        this.label = label;
    }

    EL getLabel() {
        return label;
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPre().getId().hashCode()).append("->").append(getPost().getId().hashCode());
        if (label != null) {
            sb.append("[label=\"").append(label.toString()).append("\"]");
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getPre().getId() + "-" + label.toString() + "->" + getPost().getId();
    }
}

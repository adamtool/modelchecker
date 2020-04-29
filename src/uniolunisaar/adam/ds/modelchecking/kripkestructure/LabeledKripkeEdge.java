package uniolunisaar.adam.ds.modelchecking.kripkestructure;

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

}

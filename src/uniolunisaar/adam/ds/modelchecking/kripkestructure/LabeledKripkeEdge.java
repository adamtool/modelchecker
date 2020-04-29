package uniolunisaar.adam.ds.modelchecking.kripkestructure;

/**
 *
 * @author Manuel Gieseking
 * @param <EL> edge label
 * @param <SL> state label
 */
public class LabeledKripkeEdge<SL extends ILabel, EL extends ILabel> extends KripkeEdge<SL> {

    private final EL label;

    public LabeledKripkeEdge(EL label, KripkeState<SL> pre, KripkeState<SL> post) {
        super(pre, post);
        this.label = label;
    }

    public EL getLabel() {
        return label;
    }

}

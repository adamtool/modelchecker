package uniolunisaar.adam.ds.modelchecking.kripkestructure;

/**
 *
 * @author Manuel Gieseking
 * @param <SL>
 * @param <EL>
 */
public class LabeledKripkeStructure<SL extends ILabel, EL extends ILabel> extends KripkeStructure<SL, LabeledKripkeEdge<SL, EL>> {

    public LabeledKripkeStructure(String name) {
        super(name);
    }

}

package uniolunisaar.adam.ds.modelchecking.aba;

import uniolunisaar.adam.ds.automaton.BuchiState;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAState extends BuchiState {

    ABAState(String id) {
        super(id);
    }

    @Override
    protected void setBuchi(boolean buchi) {
        super.setBuchi(buchi);
    }

}

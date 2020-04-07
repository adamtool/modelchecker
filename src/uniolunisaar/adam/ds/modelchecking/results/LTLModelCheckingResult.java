package uniolunisaar.adam.ds.modelchecking.results;

import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;

/**
 *
 * @author Manuel Gieseking
 */
public class LTLModelCheckingResult extends ModelCheckingResult {

    private CounterExample cex = null;
    private Abc.VerificationAlgo algo;

    @Override
    public CounterExample getCex() {
        return cex;
    }

    @Override
    public void setCex(CounterExample cex) {
        if (cex != null) {
            this.cex = cex;
            setSat(Satisfied.FALSE);
        }
    }

    public Abc.VerificationAlgo getAlgo() {
        return algo;
    }

    public void setAlgo(Abc.VerificationAlgo algo) {
        this.algo = algo;
    }

}

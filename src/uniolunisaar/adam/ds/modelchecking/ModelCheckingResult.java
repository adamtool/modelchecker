package uniolunisaar.adam.ds.modelchecking;

import uniolunisaar.adam.ds.modelchecking.CounterExample;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckingResult {

    public enum Satisfied {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private CounterExample cex = null;
    private Satisfied sat;

    public CounterExample getCex() {
        return cex;
    }

    public void setCex(CounterExample cex) {
        if (cex != null) {
            this.cex = cex;
            sat = Satisfied.FALSE;
        }
    }

    public void setSat(Satisfied sat) {
        this.sat = sat;
    }

    public Satisfied getSatisfied() {
        return sat;
    }
}

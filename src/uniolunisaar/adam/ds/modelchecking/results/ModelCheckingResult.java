package uniolunisaar.adam.ds.modelchecking.results;

import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;

/**
 *
 * @author Manuel Gieseking
 */
public abstract class ModelCheckingResult {

    public enum Satisfied {
        TRUE,
        FALSE,
        UNKNOWN
    }

    private Satisfied sat;

    public abstract CounterExample getCex();

    public abstract void setCex(CounterExample cex);

    public void setSat(Satisfied sat) {
        this.sat = sat;
    }

    public Satisfied getSatisfied() {
        return sat;
    }

}

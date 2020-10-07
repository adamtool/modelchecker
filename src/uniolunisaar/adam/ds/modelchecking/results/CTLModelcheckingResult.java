package uniolunisaar.adam.ds.modelchecking.results;

import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;

/**
 *
 * @author Manuel Gieseking
 */
public class CTLModelcheckingResult extends ModelCheckingResult {

    private final String lolaOutput;
    private final String lolaError;
    private String witnessState = null;
    private String witnessPath = null;

    public CTLModelcheckingResult(String lolaOutput, String lolaError) {
        this.lolaOutput = lolaOutput;
        this.lolaError = lolaError;
    }

    @Override
    public CounterExample getCex() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCex(CounterExample cex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getLolaOutput() {
        return lolaOutput;
    }

    public String getLolaError() {
        return lolaError;
    }

    public String getWitnessState() {
        return witnessState;
    }

    public void setWitnessState(String witnessState) {
        this.witnessState = witnessState;
    }

    public String getWitnessPath() {
        return witnessPath;
    }

    public void setWitnessPath(String witnessPath) {
        this.witnessPath = witnessPath;
    }

}

package uniolunisaar.adam.ds.modelchecking.output;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitLTLMCOutputData {

    private String outputPath;
    private boolean outputCircuit;
    private boolean verbose;

    public AdamCircuitLTLMCOutputData(String path, boolean outputCircuit, boolean verbose) {
        this.outputPath = path;
        this.outputCircuit = outputCircuit;
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getPath() {
        return outputPath;
    }

    public void setPath(String path) {
        this.outputPath = path;
    }

    public boolean isOutputCircuit() {
        return outputCircuit;
    }

    public void setOutputCircuit(boolean outputCircuit) {
        this.outputCircuit = outputCircuit;
    }

}

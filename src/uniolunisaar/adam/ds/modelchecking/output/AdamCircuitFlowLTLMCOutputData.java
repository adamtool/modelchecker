package uniolunisaar.adam.ds.modelchecking.output;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitFlowLTLMCOutputData extends AdamCircuitLTLMCOutputData {

    private boolean outputTransformedNet;

    public AdamCircuitFlowLTLMCOutputData(String path, boolean outputCircuit, boolean outputTransformedNet, boolean verbose) {
        super(path, outputCircuit, verbose);
        this.outputTransformedNet = outputTransformedNet;
    }

    public boolean isOutputTransformedNet() {
        return outputTransformedNet;
    }

    public void setOutputTransformedNet(boolean outputTransformedNet) {
        this.outputTransformedNet = outputTransformedNet;
    }

}

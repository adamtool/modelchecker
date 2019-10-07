package uniolunisaar.adam.ds.modelchecking.settings;

import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;

/**
 *
 * @author Manuel Gieseking
 */
public class AbcSettings {

    private String inputFile;
    private String parameters = "";
    private VerificationAlgo[] verificationAlgos;
    private boolean verbose = false;
    private String processFamilyID = "";
    private boolean circuitReduction = false;
    private String preProcessing = null;

    public AbcSettings() {
        verificationAlgos = new VerificationAlgo[]{VerificationAlgo.IC3};
    }

    public AbcSettings(String inputFile, String parameters, boolean verbose, String processFamilyID, VerificationAlgo... verificationAlgos) {
        this.inputFile = inputFile;
        this.parameters = parameters;
        this.verificationAlgos = verificationAlgos;
        this.verbose = verbose;
        this.processFamilyID = processFamilyID;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getParameters() {
        return parameters;
    }

    public VerificationAlgo[] getVerificationAlgos() {
        return verificationAlgos;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String getProcessFamilyID() {
        return processFamilyID;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public void setVerificationAlgos(VerificationAlgo[] verificationAlgos) {
        this.verificationAlgos = verificationAlgos;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setProcessFamilyID(String processFamilyID) {
        this.processFamilyID = processFamilyID;
    }

    public boolean isCircuitReduction() {
        return circuitReduction;
    }

    public void setCircuitReduction(boolean circuitReduction) {
        this.circuitReduction = circuitReduction;
    }

    public String getPreProcessing() {
        return preProcessing;
    }

    public void setPreProcessing(String preProcessing) {
        this.preProcessing = preProcessing;
    }

}

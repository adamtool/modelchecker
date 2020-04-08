package uniolunisaar.adam.ds.modelchecking.settings.ctl;

/**
 *
 * @author Manuel Gieseking
 */
public class CTLModelcheckingSettings {

    private final String outputPath;
    private final boolean verbose;

    public CTLModelcheckingSettings(String outputPath) {
        this.outputPath = outputPath;
        this.verbose = false;
    }

    public CTLModelcheckingSettings(String outputPath, boolean verbose) {
        this.outputPath = outputPath;
        this.verbose = verbose;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public boolean isVerbose() {
        return verbose;
    }

}

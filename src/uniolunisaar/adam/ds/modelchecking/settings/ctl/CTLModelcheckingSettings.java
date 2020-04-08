package uniolunisaar.adam.ds.modelchecking.settings.ctl;

import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;

/**
 *
 * @author Manuel Gieseking
 */
public class CTLModelcheckingSettings extends ModelCheckingSettings {

    private final String outputPath;
    private final boolean verbose;

    public CTLModelcheckingSettings(Solver solver, String outputPath) {
        super(solver, Logic.CTL);
        this.outputPath = outputPath;
        this.verbose = false;
    }

    public CTLModelcheckingSettings(Solver solver, String outputPath, boolean verbose) {
        super(solver, Logic.CTL);
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

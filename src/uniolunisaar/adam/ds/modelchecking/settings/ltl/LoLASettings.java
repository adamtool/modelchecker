package uniolunisaar.adam.ds.modelchecking.settings.ltl;

/**
 *
 * @author Manuel Gieseking
 */
public class LoLASettings extends ModelCheckingSettings {

    private Approach approach = Approach.SEQUENTIAL_INHIBITOR;
    private String inputFile;
    private String outputPath;

    public LoLASettings() {
        super(Solver.LOLA);
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

}

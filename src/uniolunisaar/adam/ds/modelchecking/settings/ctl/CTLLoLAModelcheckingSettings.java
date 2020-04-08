package uniolunisaar.adam.ds.modelchecking.settings.ctl;

/**
 *
 * @author Manuel Gieseking
 */
public class CTLLoLAModelcheckingSettings extends CTLModelcheckingSettings {

    private final String jsonPath;
    private final String witnessStatePath;
    private final String witnessPathPath;
//    private final String familyID;

//    public CTLLoLAModelcheckingSettings(String outputPath, String familyID) {
//        super(outputPath);
//        this.familyID = familyID;
//        jsonPath = outputPath + ".json";
//        witnessStatePath = outputPath + "_witness_state.txt";
//        witnessPathPath = outputPath + "_witness.txt";
//    }
    public CTLLoLAModelcheckingSettings(String outputPath) {
        super(Solver.LOLA, outputPath);
        jsonPath = outputPath + ".json";
        witnessStatePath = outputPath + "_witness_state.txt";
        witnessPathPath = outputPath + "_witness.txt";
    }

    public CTLLoLAModelcheckingSettings(String outputPath, boolean verbose) {
        super(Solver.LOLA, outputPath, verbose);
        jsonPath = outputPath + ".json";
        witnessStatePath = outputPath + "_witness_state.txt";
        witnessPathPath = outputPath + "_witness.txt";
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public String getWitnessStatePath() {
        return witnessStatePath;
    }

    public String getWitnessPathPath() {
        return witnessPathPath;
    }

//    public String getFamilyID() {
//        return familyID;
//    }
}

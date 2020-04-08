package uniolunisaar.adam.ds.modelchecking.settings.ctl;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLLoLAModelcheckingSettings extends CTLLoLAModelcheckingSettings {

    private Approach approach = Approach.SEQUENTIAL_INHIBITOR;

    public FlowCTLLoLAModelcheckingSettings(String outputPath) {
        super(outputPath);
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

}

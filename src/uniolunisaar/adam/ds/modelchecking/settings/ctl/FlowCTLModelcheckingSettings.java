package uniolunisaar.adam.ds.modelchecking.settings.ctl;

import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLModelcheckingSettings extends AdamCircuitFlowLTLMCSettings {

//    private boolean verbose = false;

    public FlowCTLModelcheckingSettings() {
    }

    public FlowCTLModelcheckingSettings(AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(optsSys, optsComp);
    }

    public FlowCTLModelcheckingSettings(LogicsTools.TransitionSemantics semantics, Approach approach, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, boolean initFirst, Abc.VerificationAlgo... algo) {
        super(semantics, approach, maximality, stuttering, optsSys, optsComp, initFirst, algo);
    }

//    public boolean isVerbose() {
//        return verbose;
//    }
//
//    public void setVerbose(boolean verbose) {
//        this.verbose = verbose;
//    }

}

package uniolunisaar.adam.ds.modelchecking.settings;

import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitLTLMCSettings extends AdamCircuitMCSettings<AdamCircuitLTLMCOutputData, AdamCircuitLTLMCStatistics> {

    public AdamCircuitLTLMCSettings() {
        super(new AdamCircuitLTLMCOutputData("./", false, false));
    }

    public AdamCircuitLTLMCSettings(AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(new AdamCircuitLTLMCOutputData("./", false, false), optsSys, optsComp);
    }

    public AdamCircuitLTLMCSettings(LogicsTools.TransitionSemantics semantics, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(new AdamCircuitLTLMCOutputData("./", false, false), semantics, maximality, stuttering, optsSys, optsComp, algos);
    }

}

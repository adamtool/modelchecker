package uniolunisaar.adam.ds.modelchecking.settings.ltl;

import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitLTLMCSettings extends AdamCircuitMCSettings<AdamCircuitLTLMCOutputData, AdamCircuitLTLMCStatistics> {

    public AdamCircuitLTLMCSettings(AdamCircuitLTLMCOutputData outputData) {
        super(outputData);
    }

    public AdamCircuitLTLMCSettings(AdamCircuitLTLMCOutputData outputData, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(outputData, optsSys, optsComp);
    }

    public AdamCircuitLTLMCSettings(AdamCircuitLTLMCOutputData outputData, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, maximality, stuttering, optsSys, optsComp, algos);
    }

    public AdamCircuitLTLMCSettings(AdamCircuitLTLMCOutputData outputData, Maximality maximality, Stuttering stuttering, CircuitRendererSettings.TransitionSemantics transitionSemantics, CircuitRendererSettings.TransitionEncoding transitionEncoding, CircuitRendererSettings.AtomicPropositions atomicPropositions, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, maximality, stuttering, transitionSemantics, transitionEncoding, atomicPropositions, optsSys, optsComp, algos);
    }

}

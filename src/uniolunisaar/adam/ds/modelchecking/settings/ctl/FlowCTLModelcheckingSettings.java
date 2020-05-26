package uniolunisaar.adam.ds.modelchecking.settings.ctl;

import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLModelcheckingSettings extends AdamCircuitFlowLTLMCSettings {

    public FlowCTLModelcheckingSettings(AdamCircuitFlowLTLMCOutputData outputData) {
        super(outputData);
    }

    public FlowCTLModelcheckingSettings(AdamCircuitFlowLTLMCOutputData outputData, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(outputData, optsSys, optsComp);
    }

    public FlowCTLModelcheckingSettings(AdamCircuitFlowLTLMCOutputData outputData, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, maximality, stuttering, optsSys, optsComp, algos);
    }

    public FlowCTLModelcheckingSettings(AdamCircuitFlowLTLMCOutputData outputData, Approach approach, Maximality maximality, Stuttering stuttering, CircuitRendererSettings.TransitionSemantics transitionSemantics, CircuitRendererSettings.TransitionEncoding transitionEncoding, CircuitRendererSettings.AtomicPropositions atomicPropositions, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, approach, maximality, stuttering, transitionSemantics, transitionEncoding, atomicPropositions, optsSys, optsComp, algos);
    }

}

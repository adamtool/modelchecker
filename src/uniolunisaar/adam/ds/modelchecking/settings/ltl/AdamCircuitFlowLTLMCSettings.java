package uniolunisaar.adam.ds.modelchecking.settings.ltl;

import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitFlowLTLMCSettings extends AdamCircuitMCSettings<AdamCircuitFlowLTLMCOutputData, AdamCircuitFlowLTLMCStatistics> {

    public enum Stucking {
        GFo,
        GFANDNpi,
        ANDGFNpi,
        GFANDNpiAndo
    }

    private Approach approach = Approach.PARALLEL_INHIBITOR;

    private boolean initFirst = true;
    private Stucking stucking = Stucking.GFANDNpi;
    private boolean newChainsBySkippingTransitions = false;
    private boolean notStuckingAlsoByMaxInCircuit = false;
//    private boolean useNextToReplaceXandTransitionsInRunPart = true; // this is wrong for transitions but could work for just the next operator
    private boolean useNextToReplaceXandTransitionsInRunPart = false;
//
//    public AdamCircuitFlowLTLMCSettings() {
//        super(new AdamCircuitFlowLTLMCOutputData("./", false, false, false));
//    }
//
//    public AdamCircuitFlowLTLMCSettings(AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
//        super(new AdamCircuitFlowLTLMCOutputData("./", false, false, false), optsSys, optsComp);
//    }
//
//    public AdamCircuitFlowLTLMCSettings(LogicsTools.TransitionSemantics semantics, Approach approach, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, boolean initFirst, Abc.VerificationAlgo... algo) {
//        super(new AdamCircuitFlowLTLMCOutputData("./", false, false, false), semantics, maximality, stuttering, optsSys, optsComp, algo);
//        this.approach = approach;
//        this.initFirst = initFirst;
//    }

    public AdamCircuitFlowLTLMCSettings(AdamCircuitFlowLTLMCOutputData outputData) {
        super(outputData);
    }

    public AdamCircuitFlowLTLMCSettings(AdamCircuitFlowLTLMCOutputData outputData, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(outputData, optsSys, optsComp);
    }

    public AdamCircuitFlowLTLMCSettings(AdamCircuitFlowLTLMCOutputData outputData, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, maximality, stuttering, optsSys, optsComp, algos);
    }

    public AdamCircuitFlowLTLMCSettings(AdamCircuitFlowLTLMCOutputData outputData, Approach approach, Maximality maximality, Stuttering stuttering, CircuitRendererSettings.TransitionSemantics transitionSemantics, CircuitRendererSettings.TransitionEncoding transitionEncoding, CircuitRendererSettings.AtomicPropositions atomicPropositions, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(outputData, maximality, stuttering, transitionSemantics, transitionEncoding, atomicPropositions, optsSys, optsComp, algos);
        this.approach = approach;
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

    public boolean isInitFirst() {
        return initFirst;
    }

    public void setInitFirst(boolean initFirst) {
        this.initFirst = initFirst;
    }

    public boolean isNewChainsBySkippingTransitions() {
        return newChainsBySkippingTransitions;
    }

    public void setNewChainsBySkippingTransitions(boolean newChainsBySkippingTransitions) {
        this.newChainsBySkippingTransitions = newChainsBySkippingTransitions;
    }

    public Stucking getStucking() {
        return stucking;
    }

    public void setStucking(Stucking stucking) {
        this.stucking = stucking;
    }

    public boolean isNotStuckingAlsoByMaxInCircuit() {
        return notStuckingAlsoByMaxInCircuit;
    }

    public void setNotStuckingAlsoByMaxInCircuit(boolean notStuckingAlsoByMaxInCircuit) {
        this.notStuckingAlsoByMaxInCircuit = notStuckingAlsoByMaxInCircuit;
    }

    public boolean isUseNextToReplaceXandTransitionsInRunPart() {
        return useNextToReplaceXandTransitionsInRunPart;
    }

    public void setUseNextToReplaceXandTransitionsInRunPart(boolean useNextToReplaceXandTransitionsInRunPart) {
        this.useNextToReplaceXandTransitionsInRunPart = useNextToReplaceXandTransitionsInRunPart;
    }

}

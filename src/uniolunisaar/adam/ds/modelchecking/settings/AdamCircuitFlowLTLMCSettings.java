package uniolunisaar.adam.ds.modelchecking.settings;

import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitFlowLTLMCSettings extends AdamCircuitLTLMCSettings<AdamCircuitFlowLTLMCOutputData, AdamCircuitFlowLTLMCStatistics> {

    public enum Stucking {
        GFo,
        GFANDNpi,
        ANDGFNpi,
        GFANDNpiAndo
    }

    private Approach approach = Approach.SEQUENTIAL_INHIBITOR;

    private boolean initFirst = true;
    private Stucking stucking = Stucking.GFANDNpi;
    private boolean newChainsBySkippingTransitions = false;
    private boolean notStuckingAlsoByMaxInCircuit = false;

    public AdamCircuitFlowLTLMCSettings() {
    }

    public AdamCircuitFlowLTLMCSettings(AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(optsSys, optsComp);
    }

    public AdamCircuitFlowLTLMCSettings(LogicsTools.TransitionSemantics semantics, Approach approach, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, boolean initFirst, Abc.VerificationAlgo... algo) {
        super(semantics, maximality, stuttering, optsSys, optsComp, algo);
        this.approach = approach;
        this.initFirst = initFirst;
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

}

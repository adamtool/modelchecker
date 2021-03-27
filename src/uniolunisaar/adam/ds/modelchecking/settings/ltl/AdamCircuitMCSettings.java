package uniolunisaar.adam.ds.modelchecking.settings.ltl;

import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.petrinet.pn2aiger.AigerRenderer;

/**
 *
 * @author Manuel Gieseking
 * @param <D>
 * @param <S>
 */
public class AdamCircuitMCSettings<D extends AdamCircuitLTLMCOutputData, S extends AdamCircuitLTLMCStatistics> extends ModelCheckingSettings {

    /**
     *
     * The replacement idea is way to expensive, since we replace each
     * transition with an until to jump over all stutter steps.
     *
     * The register idea should be also way faster, since we don't have to
     * consider all the transitions in the formula but only have one additional
     * register.
     *
     * The error register uses one globally less, but it still has to be tested
     * if this is really faster (currently there could still be a problem for
     * the logEncoding)
     *
     */
    public enum Stuttering {
        REPLACEMENT,
        REPLACEMENT_REGISTER,
        PREFIX,
        PREFIX_REGISTER,
        ERROR_REGISTER
    }

    public enum Maximality {
        MAX_CONCURRENT,
        MAX_INTERLEAVING,
        MAX_INTERLEAVING_IN_CIRCUIT,
        MAX_NONE
    }

    private Maximality maximality = Maximality.MAX_INTERLEAVING_IN_CIRCUIT;
    private Stuttering stuttering = Stuttering.PREFIX_REGISTER;
    private AigerRenderer.OptimizationsSystem optsSys = AigerRenderer.OptimizationsSystem.NONE;
    private AigerRenderer.OptimizationsComplete optsComp = AigerRenderer.OptimizationsComplete.NONE;
    private final AbcSettings abcSettings = new AbcSettings();
    private D outputData;
    private S statistics = null;
    private boolean useFormulaFileForMcHyper = true;
    private CircuitRendererSettings rendererSettings = new CircuitRendererSettings(
            CircuitRendererSettings.TransitionSemantics.OUTGOING,
            CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
            CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS);

    public AdamCircuitMCSettings(D outputData) {
        super(Solver.ADAM_CIRCUIT);
        this.outputData = outputData;
        rendererSettings.setMaxInterleaving(maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
    }

    public AdamCircuitMCSettings(D outputData, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(Solver.ADAM_CIRCUIT);
        this.optsSys = optsSys;
        this.optsComp = optsComp;
        this.outputData = outputData;
        rendererSettings.setMaxInterleaving(maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
    }

    public AdamCircuitMCSettings(D outputData, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(Solver.ADAM_CIRCUIT);
        this.maximality = maximality;
        this.stuttering = stuttering;
        this.optsSys = optsSys;
        this.optsComp = optsComp;
        this.abcSettings.setVerificationAlgos(algos);
        this.outputData = outputData;
        rendererSettings.setMaxInterleaving(maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
    }

    public AdamCircuitMCSettings(D outputData,
            Maximality maximality, Stuttering stuttering,
            CircuitRendererSettings.TransitionSemantics transitionSemantics,
            CircuitRendererSettings.TransitionEncoding transitionEncoding,
            CircuitRendererSettings.AtomicPropositions atomicPropositions,
            AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp,
            Abc.VerificationAlgo... algos) {
        super(Solver.ADAM_CIRCUIT);
        this.maximality = maximality;
        this.stuttering = stuttering;
        this.optsSys = optsSys;
        this.optsComp = optsComp;
        this.abcSettings.setVerificationAlgos(algos);
        this.outputData = outputData;
        rendererSettings = new CircuitRendererSettings(transitionSemantics, transitionEncoding, atomicPropositions);
        rendererSettings.setMaxInterleaving(maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
    }

    public void fillAbcData(PetriNet net) {
        String procID = PetriNetExtensionHandler.getProcessFamilyID(net);
        abcSettings.setInputFile(outputData.getPath() + procID.hashCode() + ".aig");
        abcSettings.setVerbose(outputData.isVerbose());
        abcSettings.setProcessFamilyID(procID);
    }

    public CircuitRendererSettings getRendererSettings() {
        return rendererSettings;
    }

    public Maximality getMaximality() {
        return maximality;
    }

    public void setMaximality(Maximality maximality) {
        this.maximality = maximality;
        rendererSettings.setMaxInterleaving(this.maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
    }

    public Stuttering getStuttering() {
        return stuttering;
    }

    public void setOptsSys(AigerRenderer.OptimizationsSystem optsSys) {
        this.optsSys = optsSys;
    }

    public void setOptsComp(AigerRenderer.OptimizationsComplete optsComp) {
        this.optsComp = optsComp;
    }

    public AigerRenderer.OptimizationsSystem getOptsSys() {
        return optsSys;
    }

    public AigerRenderer.OptimizationsComplete getOptsComp() {
        return optsComp;
    }

    public AbcSettings getAbcSettings() {
        return abcSettings;
    }

    public D getOutputData() {
        return outputData;
    }

    public S getStatistics() {
        return statistics;
    }

    public void setVerificationAlgo(Abc.VerificationAlgo... algos) {
        abcSettings.setVerificationAlgos(algos);
    }

    public void setAbcParameters(String abcParameter) {
        abcSettings.setParameters(abcParameter);
    }

    public void setOutputData(D data) {
        outputData = data;
    }

    public void setStatistics(S stats) {
        statistics = stats;
    }

    public void setStuttering(Stuttering stuttering) {
        this.stuttering = stuttering;
    }

    public boolean isUseFormulaFileForMcHyper() {
        return useFormulaFileForMcHyper;
    }

    public void setUseFormulaFileForMcHyper(boolean useFormulaFileForMcHyper) {
        this.useFormulaFileForMcHyper = useFormulaFileForMcHyper;
    }

    public boolean isCircuitReductionABC() {
        return abcSettings.isCircuitReduction();
    }

    public void setCircuitReductionABC(boolean circuitReductionABC) {
        abcSettings.setCircuitReduction(circuitReductionABC);
    }

    public void setABCPreprocessingCommands(String cmds) {
        abcSettings.setPreProcessing(cmds);
    }

    public void setTransitionEncoding(CircuitRendererSettings.TransitionEncoding encoding) {
        rendererSettings.setEncoding(encoding);
    }

    public void setCircuitAtoms(CircuitRendererSettings.AtomicPropositions atoms) {
        rendererSettings.setAtoms(atoms);
    }

    public void setTransitionSemantics(CircuitRendererSettings.TransitionSemantics semantics) {
        rendererSettings.setSemantics(semantics);
    }
}

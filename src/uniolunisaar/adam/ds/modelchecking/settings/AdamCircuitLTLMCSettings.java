package uniolunisaar.adam.ds.modelchecking.settings;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.PetriNetExtensionHandler;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 * @param <D>
 * @param <S>
 */
public class AdamCircuitLTLMCSettings<D extends AdamCircuitLTLMCOutputData, S extends AdamCircuitLTLMCStatistics> extends ModelCheckingSettings {

    
    public enum Stuttering {
        REPLACEMENT,
        REPLACEMENT_REGISTER,
        PREFIX,
        PREFIX_REGISTER
    }

    public enum Maximality {
        MAX_CONCURRENT,
        MAX_INTERLEAVING,
        MAX_INTERLEAVING_IN_CIRCUIT,
        MAX_NONE
    }

    private LogicsTools.TransitionSemantics semantics = LogicsTools.TransitionSemantics.OUTGOING;
    private Maximality maximality = Maximality.MAX_INTERLEAVING_IN_CIRCUIT;
    private Stuttering stuttering = Stuttering.PREFIX_REGISTER;
    private AigerRenderer.OptimizationsSystem optsSys = AigerRenderer.OptimizationsSystem.NONE;
    private AigerRenderer.OptimizationsComplete optsComp = AigerRenderer.OptimizationsComplete.NONE;
    private final AbcSettings abcSettings = new AbcSettings();
    private D outputData = null;
    private S statistics = null;
    private boolean codeInputTransitionsBinary = false;

    public AdamCircuitLTLMCSettings() {
        super(Solver.ADAM_CIRCUIT);
    }

    public AdamCircuitLTLMCSettings(AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp) {
        super(Solver.ADAM_CIRCUIT);
        this.optsSys = optsSys;
        this.optsComp = optsComp;
    }

    public AdamCircuitLTLMCSettings(LogicsTools.TransitionSemantics semantics, Maximality maximality, Stuttering stuttering, AigerRenderer.OptimizationsSystem optsSys, AigerRenderer.OptimizationsComplete optsComp, Abc.VerificationAlgo... algos) {
        super(Solver.ADAM_CIRCUIT);
        this.semantics = semantics;
        this.maximality = maximality;
        this.stuttering = stuttering;
        this.optsSys = optsSys;
        this.optsComp = optsComp;
        this.abcSettings.setVerificationAlgos(algos);
    }

    public void fillAbcData(PetriNet net) {
        abcSettings.setInputFile(outputData.getPath() + ".aig");
        abcSettings.setVerbose(outputData.isVerbose());
        abcSettings.setProcessFamilyID(PetriNetExtensionHandler.getProcessFamilyID(net));
    }

    public LogicsTools.TransitionSemantics getSemantics() {
        return semantics;
    }

    public void setSemantics(LogicsTools.TransitionSemantics semantics) {
        this.semantics = semantics;
    }

    public Maximality getMaximality() {
        return maximality;
    }

    public void setMaximality(Maximality maximality) {
        this.maximality = maximality;
    }

    public Stuttering getStuttering() {
        return stuttering;
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

    public boolean isCodeInputTransitionsBinary() {
        return codeInputTransitionsBinary;
    }

    public void setCodeInputTransitionsBinary(boolean codeInputTransitionsBinary) {
        this.codeInputTransitionsBinary = codeInputTransitionsBinary;
    }
    
    

}

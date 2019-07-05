package uniolunisaar.adam.logic.modelchecking.circuits;

import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.util.logics.transformers.logics.ModelCheckingOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Maximality;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Stuttering;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.TransitionSemantics;
import uniolunisaar.adam.ds.modelchecking.ModelcheckingStatistics;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsComplete;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLTL {

    private final PnAndLTLtoCircuit circuitTransformer;
    private VerificationAlgo verificationAlgo = VerificationAlgo.IC3;
    private String abcParameters = "";

    public ModelCheckerLTL() {
        circuitTransformer = new PnAndLTLtoCircuit();
    }

    public ModelCheckerLTL(TransitionSemantics semantics, Maximality maximality, Stuttering stuttering, OptimizationsSystem optsSys, OptimizationsComplete optsComp, VerificationAlgo verificationAlgo) {
        circuitTransformer = new PnAndLTLtoCircuit(semantics, maximality, stuttering, optsSys, optsComp);
        this.verificationAlgo = verificationAlgo;
    }

    /**
     *
     * @param net
     * @param formula
     * @param data
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNetWithTransits net, ILTLFormula formula, ModelCheckingOutputData data) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        return check(net, formula, data, null);
    }

    /**
     *
     * @param net
     * @param formula
     * @param data
     * @param stats
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNetWithTransits net, ILTLFormula formula, ModelCheckingOutputData data, ModelcheckingStatistics stats) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + circuitTransformer.getMaximality()
                + " semantics: " + circuitTransformer.getSemantics()
                + " stuttering: " + circuitTransformer.getStuttering()
                + " verification/falsification algorithm: " + verificationAlgo, true);

        AigerRenderer renderer = circuitTransformer.createCircuit(net, formula, data, stats);
        return PetriNetModelChecker.check(data.getPath() + ".aig", verificationAlgo, net, renderer, data.getPath(), stats, abcParameters, data.isVerbose());
    }

    public VerificationAlgo getVerificationAlgo() {
        return verificationAlgo;
    }

    public void setVerificationAlgo(VerificationAlgo verificationAlgo) {
        this.verificationAlgo = verificationAlgo;
    }

    public String getAbcParameters() {
        return abcParameters;
    }

    public void setAbcParameters(String abcParameters) {
        this.abcParameters = abcParameters;
    }

    public TransitionSemantics getSemantics() {
        return circuitTransformer.getSemantics();
    }

    public void setSemantics(TransitionSemantics semantics) {
        circuitTransformer.setSemantics(semantics);
    }

    public Maximality getMaximality() {
        return circuitTransformer.getMaximality();
    }

    public void setMaximality(Maximality maximality) {
        circuitTransformer.setMaximality(maximality);
    }

    public Stuttering getStuttering() {
        return circuitTransformer.getStuttering();
    }

    public void setStuttering(Stuttering stuttering) {
        circuitTransformer.setStuttering(stuttering);
    }

}

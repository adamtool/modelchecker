package uniolunisaar.adam.logic.modelchecking.circuits;

import java.io.FileNotFoundException;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.IOException;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.LoLASettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.lola.ModelCheckerLoLA;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLTL {

    private final ModelCheckingSettings settings;

    public ModelCheckerLTL(ModelCheckingSettings settings) {
        this.settings = settings;
    }

    /**
     *
     * @param net
     * @param formula
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNet net, ILTLFormula formula) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        switch (settings.getSolver()) {
            case ADAM_CIRCUIT:
                AdamCircuitLTLMCSettings props = (AdamCircuitLTLMCSettings) settings;
                PnAndLTLtoCircuit.createCircuitWithFairnessAndMaximality(net, formula, props);
                props.fillAbcData(net);
                return Abc.call(props.getAbcSettings(), props.getOutputData(), props.getStatistics());
            case LOLA:
                try {
                    Logger.getInstance().addWarning("You would like to use LoLA for the standard LTL model checking of Petri nets."
                            + "We would recommend to do so by directly calling LoLA, to exploit all of her power.");
                    return ModelCheckerLoLA.check(net, formula.toLoLA(), ((LoLASettings) settings).getOutputPath());
                } catch (RenderException | FileNotFoundException | NotConvertableException ex) {
                    throw new ExternalToolException("LoLA didn't finish correctly.", ex);
                }
            default:
                throw new UnsupportedOperationException("Solver " + settings.getSolver() + " is not supported yet.");
        }
    }

//    public ModelCheckerLTL(TransitionSemantics semantics, Maximality maximality, Stuttering stuttering, OptimizationsSystem optsSys, OptimizationsComplete optsComp, VerificationAlgo verificationAlgo) {
//        circuitTransformer = new PnAndLTLtoCircuit(semantics, maximality, stuttering, optsSys, optsComp);
//        this.verificationAlgo = verificationAlgo;
//    }
//    /**
//     *
//     * @param net
//     * @param formula
//     * @param data
//     * @return null iff the formula holds, otherwise a counter example violating
//     * the formula.
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws uniol.apt.io.parser.ParseException
//     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
//     * @throws uniolunisaar.adam.exceptions.ExternalToolException
//     */
//    public ModelCheckingResult check(PetriNetWithTransits net, ILTLFormula formula, AdamCircuitLTLMCOutputData data) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
//        return check(net, formula, data, null);
//    }
//    /**
//     *
//     * @param net
//     * @param formula
//     * @param data
//     * @param stats
//     * @return null iff the formula holds, otherwise a counter example violating
//     * the formula.
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws uniol.apt.io.parser.ParseException
//     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
//     * @throws uniolunisaar.adam.exceptions.ExternalToolException
//     */
//    public ModelCheckingResult check(PetriNetWithTransits net, ILTLFormula formula, AdamCircuitLTLMCOutputData data, AdamCircuitFlowLTLMCStatistics stats) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
//        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
//                + " With maximality term: " + circuitTransformer.getMaximality()
//                + " semantics: " + circuitTransformer.getSemantics()
//                + " stuttering: " + circuitTransformer.getStuttering()
//                + " verification/falsification algorithm: " + verificationAlgo, true);
//
//        circuitTransformer.createCircuit(net, formula, data, stats);
//        return PetriNetModelChecker.check(data.getPath() + ".aig", verificationAlgo, net, data.getPath(), stats, abcParameters, data.isVerbose());
//    }
//    public VerificationAlgo getVerificationAlgo() {
//        return verificationAlgo;
//    }
//
//    public void setVerificationAlgo(VerificationAlgo verificationAlgo) {
//        this.verificationAlgo = verificationAlgo;
//    }
//
//    public String getAbcParameters() {
//        return abcParameters;
//    }
//
//    public void setAbcParameters(String abcParameters) {
//        this.abcParameters = abcParameters;
//    }
//
//    public TransitionSemantics getSemantics() {
//        return circuitTransformer.getSemantics();
//    }
//
//    public void setSemantics(TransitionSemantics semantics) {
//        circuitTransformer.setSemantics(semantics);
//    }
//
//    public Maximality getMaximality() {
//        return circuitTransformer.getMaximality();
//    }
//
//    public void setMaximality(Maximality maximality) {
//        circuitTransformer.setMaximality(maximality);
//    }
//
//    public Stuttering getStuttering() {
//        return circuitTransformer.getStuttering();
//    }
//
//    public void setStuttering(Stuttering stuttering) {
//        circuitTransformer.setStuttering(stuttering);
//    }
}

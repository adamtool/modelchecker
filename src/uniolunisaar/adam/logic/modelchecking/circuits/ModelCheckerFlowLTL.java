package uniolunisaar.adam.logic.modelchecking.circuits;

import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exception.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerParallel;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.CircuitAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndFlowLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndFlowLTLtoCircuit.Approach;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Maximality;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Stuttering;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.TransitionSemantics;
import uniolunisaar.adam.logic.transformers.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.ds.modelchecking.ModelcheckingStatistics;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowLTL {

    private final PnAndFlowLTLtoCircuit circuitTransformer;
    private VerificationAlgo verificationAlgo = VerificationAlgo.IC3;
    private String abcParameters = "";

    public ModelCheckerFlowLTL() {
        circuitTransformer = new PnAndFlowLTLtoCircuit();
    }

    public ModelCheckerFlowLTL(TransitionSemantics semantics, Approach approach, Maximality maximality, Stuttering stuttering, VerificationAlgo verificationAlgo, boolean initFirst) {
        circuitTransformer = new PnAndFlowLTLtoCircuit(semantics, approach, maximality, stuttering, initFirst);
    }

    /**
     *
     * @param net
     * @param formula
     * @param path
     * @param verbose
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exception.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNetWithTransits net, RunFormula formula, String path, boolean verbose) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        return check(net, formula, path, verbose, null);
    }

    /**
     *
     * @param net
     * @param formula
     * @param path
     * @param verbose
     * @param stats
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exception.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNetWithTransits net, RunFormula formula, String path, boolean verbose, ModelcheckingStatistics stats) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + circuitTransformer.getMaximality()
                + " approach: " + circuitTransformer.getApproach() + " semantics: " + circuitTransformer.getSemantics() + " stuttering: " + circuitTransformer.getStuttering()
                + " initialization first step: " + circuitTransformer.isInitFirst()
                + " verification/falsification algorithm: " + verificationAlgo, true);

        AigerRenderer renderer = circuitTransformer.createCircuit(net, formula, path, verbose, stats);
        return PetriNetModelChecker.check(path + ".aig", verificationAlgo, net, renderer, path, stats, abcParameters, verbose);
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

    public Approach getApproach() {
        return circuitTransformer.getApproach();
    }

    public void setApproach(Approach approach) {
        circuitTransformer.setApproach(approach);
    }

    public boolean isInitFirst() {
        return circuitTransformer.isInitFirst();
    }

    public void setInitFirst(boolean initFirst) {
        circuitTransformer.setInitFirst(initFirst);
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

    /**
     * Returns null iff the formula holds.
     *
     * This only works for formulas with at most one flow formula.
     *
     * It uses the MAX_PARALLEL approach (the first idea), where the flow and
     * the orignal net are succeed simulaneously.
     *
     * @param net
     * @param formula
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static ModelCheckingResult checkWithParallelApproach(PetriNetWithTransits net, IRunFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula + "'.", true);
        PetriNetWithTransits gameMC = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        ILTLFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(net, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        }
        CircuitAndLTLtoCircuit.createCircuit(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName(), null, false);

        String inputFile = "./" + gameMC.getName() + ".aig";
        return PetriNetModelChecker.check(inputFile, VerificationAlgo.IC3, gameMC, renderer, "./" + gameMC.getName(), "");
    }
}

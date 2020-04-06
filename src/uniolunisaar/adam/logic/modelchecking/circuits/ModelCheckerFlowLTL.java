package uniolunisaar.adam.logic.modelchecking.circuits;

import java.io.FileNotFoundException;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl.FlowLTLTransformerParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger.CircuitAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.LoLASettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.lola.ModelCheckerLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger.PnAndFlowLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.lola.FlowLTLTransformerLoLA;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowLTL {

    private final ModelCheckingSettings settings;

    public ModelCheckerFlowLTL(ModelCheckingSettings settings) {
        this.settings = settings;
    }

    /**
     *
     * @param net
     * @param formula
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public ModelCheckingResult check(PetriNetWithTransits net, RunLTLFormula formula) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        switch (settings.getSolver()) {
            case ADAM_CIRCUIT:
                AdamCircuitFlowLTLMCSettings props = (AdamCircuitFlowLTLMCSettings) settings;
                PnAndFlowLTLtoCircuit.createCircuit(net, formula, props);
                props.fillAbcData(net);
                return Abc.call(props.getAbcSettings(), props.getOutputData(), props.getStatistics());
            case LOLA:
                PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
                String f = FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mcNet, formula);
                try {
                    return ModelCheckerLoLA.check(mcNet, f, ((LoLASettings) settings).getOutputPath());
                } catch (RenderException | FileNotFoundException ex) {
                    throw new ExternalToolException("LoLA didn't finish correctly.", ex);
                }
            default:
                throw new UnsupportedOperationException("Solver " + settings.getSolver() + " is not supported yet.");
        }

    }

    /**
     * Returns null iff the formula holds.
     *
     * This only works for formulas with at most one flow formula.
     *
     * It uses the MAX_PARALLEL approach (the first idea), where the flow and
     * the original net are succeed simultaneously.
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
    public static ModelCheckingResult checkWithParallelApproach(PetriNetWithTransits net, RunLTLFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula + "'.", true);
        PetriNetWithTransits gameMC = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        ILTLFormula formulaMC = new FlowLTLTransformerParallel().createFormula4ModelChecking4CircuitParallel(net, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING, gameMC);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER, gameMC);
        }
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData("./" + net.getName(), false, false, false);

        CircuitAndLTLtoCircuit.createCircuit(renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), data, null, false, PetriNetExtensionHandler.getProcessFamilyID(gameMC));

        String inputFile = "./" + gameMC.getName() + ".aig";
        return PetriNetModelChecker.check(inputFile, VerificationAlgo.IC3, gameMC, renderer, "./" + gameMC.getName(), "", data);
    }
}

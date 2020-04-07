package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger;

import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;

/**
 *
 * @author Manuel Gieseking
 */
public class PnAndLTLtoCircuit {

//
//    /**
//     *
//     * @param net
//     * @param formula
//     * @param data
//     * @return
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws uniol.apt.io.parser.ParseException
//     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
//     * @throws uniolunisaar.adam.exceptions.ExternalToolException
//     */
//    public AigerRenderer createCircuit(PetriNetWithTransits net, ILTLFormula formula, AdamCircuitLTLMCOutputData data) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
//        return createCircuit(net, formula, data, null);
//    }
//
//    /**
//     *
//     * @param net
//     * @param formula
//     * @param data
//     * @param stats
//     * @return
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws uniol.apt.io.parser.ParseException
//     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
//     * @throws uniolunisaar.adam.exceptions.ExternalToolException
//     */
//    public AigerRenderer createCircuit(PetriNetWithTransits net, ILTLFormula formula, AdamCircuitLTLMCOutputData data, AdamCircuitLTLMCStatistics stats) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
//        return createCircuit(net, formula, data, stats, false);
//    }
    public static AigerRenderer createCircuitWithFairnessAndMaximality(PetriNet net, ILTLFormula formula, AdamCircuitMCSettings<? extends AdamCircuitLTLMCOutputData, ? extends AdamCircuitLTLMCStatistics> settings) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitLTLMCSettings.Maximality maximality = settings.getMaximality();
        TransitionSemantics semantics = settings.getSemantics();

        // Add Fairness
        formula = LogicsTools.addFairness(net, formula);

        // Add Maximality
        ILTLFormula max = null;
        switch (maximality) {
            case MAX_INTERLEAVING:
                max = FormulaCreator.getInterleavingMaximality(semantics, net);
                break;
            case MAX_CONCURRENT:
                max = FormulaCreator.getConcurrentMaximality(semantics, net);
                break;
        }
        if (max != null) {
            formula = new LTLFormula(max, LTLOperators.Binary.IMP, formula);
        }
        return createCircuit(net, formula, settings);
    }

    public static AigerRenderer createCircuitWithoutFairnessAndMaximality(PetriNet net, ILTLFormula formula, AdamCircuitMCSettings<? extends AdamCircuitLTLMCOutputData, ? extends AdamCircuitLTLMCStatistics> settings) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        return createCircuit(net, formula, settings);
    }

    /**
     *
     * @param net
     * @param formula
     * @param settings
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws ParseException
     * @throws ProcessNotStartedException
     * @throws ExternalToolException
     */
    private static AigerRenderer createCircuit(PetriNet net, ILTLFormula formula, AdamCircuitMCSettings<? extends AdamCircuitLTLMCOutputData, ? extends AdamCircuitLTLMCStatistics> settings) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitLTLMCSettings.Maximality maximality = settings.getMaximality();
        TransitionSemantics semantics = settings.getSemantics();
        AdamCircuitLTLMCSettings.Stuttering stuttering = settings.getStuttering();
        AigerRenderer.OptimizationsSystem optsSys = settings.getOptsSys();
        AigerRenderer.OptimizationsComplete optsComp = settings.getOptsComp();
        AdamCircuitLTLMCStatistics stats = settings.getStatistics();
        AdamCircuitLTLMCOutputData data = settings.getOutputData();
        Logger.getInstance().addMessage("Creating the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + maximality
                + " semantics: " + semantics
                + " stuttering: " + stuttering, true);

        int f_size = formula.getSize();

        // Set the Real net what is checked to ABC for parsing the CEX
        settings.getAbcSettings().setNet(net);

        // Choose renderer and add the corresponding stuttering
        AigerRenderer renderer;
        String mcHyperFormula;
        if (semantics == TransitionSemantics.INGOING) {
            // todo: do the stuttering here
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING, net);
            mcHyperFormula = FlowLTLTransformerHyperLTL.toMCHyperFormat(formula);
        } else {
            formula = LTL2CircuitFormula.handleStutteringOutGoingSemantics(net, formula, stuttering, maximality);
            try {
                mcHyperFormula = FlowLTLTransformerHyperLTL.toMCHyperFormat(formula);
            } catch (StackOverflowError exp) { // formula size is too huge
                // todo: maybe we can add here a little smarter algo which uses the fallback to inCircuit, when the reason for the too huge
                //      formula is due to the maximality in formula approach
                throw exp;
            }
            if (maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
                switch (settings.getAtomicPropositionType()) {
                    case PLACES_AND_TRANSITIONS:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_BIN_TRANS_MAX_INTERLEAVING, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_MAX_INTERLEAVING, net);
                        }
                        break;
                    case PLACES:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_ONLYPLACES_BIN_TRANS_MAX_INTERLEAVING, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_ONLYPLACES_MAX_INTERLEAVING, net);
                        }
                        break;
                    case FIREABILITY:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_FIREABILITY_BIN_TRANS_MAX_INTERLEAVING, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_FIREABILITY_MAX_INTERLEAVING, net);
                        }
                        break;
                    default:
                        throw new RuntimeException("Not yet implemented: " + settings.getAtomicPropositionType().name());
                }
            } else {
                switch (settings.getAtomicPropositionType()) {
                    case PLACES_AND_TRANSITIONS:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_BIN_TRANS, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER, net);
                        }
                        break;
                    case PLACES:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_ONLYPLACES_BIN_TRANS, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_ONLYPLACES, net);
                        }
                        break;
                    case FIREABILITY:
                        if (settings.isCodeInputTransitionsBinary()) {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_FIREABILITY_BIN_TRANS, net);
                        } else {
                            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_FIREABILITY, net);
                        }
                        break;
                    default:
                        throw new RuntimeException("Not yet implemented: " + settings.getAtomicPropositionType().name());
                }
            }
        }
        renderer.setSystemOptimizations(optsSys);
        renderer.setMCHyperResultOptimizations(optsComp);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            if (!(stats instanceof AdamCircuitFlowLTLMCStatistics)) {
                // input model checking net
                stats.setIn_nb_places(net.getPlaces().size());
                stats.setIn_nb_transitions(net.getTransitions().size());
                stats.setIn_size_formula(f_size);
            }
            stats.setFormulaToCheck(formula);
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS

        Logger.getInstance().addMessage("This means we create the product for F='" + formula.toSymbolString() + "'.");
        CircuitAndLTLtoCircuit.createCircuit(renderer, mcHyperFormula, data, stats, settings.isUseFormulaFileForMcHyper(), PetriNetExtensionHandler.getProcessFamilyID(net));
        return renderer;
    }

}

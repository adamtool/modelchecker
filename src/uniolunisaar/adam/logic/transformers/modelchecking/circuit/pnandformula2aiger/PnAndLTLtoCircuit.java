package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger;

import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformer;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.util.logics.FormulaCreator;
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
    /**
     *
     * @param net
     * @param formula
     * @param settings
     * @param skipMax - used if this method is called from the FlowLTL-Part and
     * the maximality is already handled there.
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws ParseException
     * @throws ProcessNotStartedException
     * @throws ExternalToolException
     */
    public static AigerRenderer createCircuit(PetriNetWithTransits net, ILTLFormula formula, AdamCircuitLTLMCSettings<? extends AdamCircuitLTLMCOutputData, ? extends AdamCircuitLTLMCStatistics> settings, boolean skipMax) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
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

        // Add Fairness
        formula = FlowLTLTransformer.addFairness(net, formula);

        // Add Maximality
        if (!skipMax) {
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
        }
        int f_size = formula.getSize();

        // Choose renderer and add the corresponding stuttering
        AigerRenderer renderer;
        if (semantics == TransitionSemantics.INGOING) {
            // todo: do the stuttering here
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            formula = LTL2CircuitFormula.handleStutteringOutGoingSemantics(net, formula, stuttering, maximality);
            if (maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_MAX_INTERLEAVING);
            } else {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
            }
        }
        renderer.setSystemOptimizations(optsSys);
        renderer.setMCHyperResultOptimizations(optsComp);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            // input model checking net
            stats.setIn_nb_places(net.getPlaces().size());
            stats.setIn_nb_transitions(net.getTransitions().size());
            stats.setIn_size_formula(f_size);
            stats.setFormulaToCheck(formula);
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS

        Logger.getInstance().addMessage("This means we create the product for F='" + formula.toSymbolString() + "'.");
        CircuitAndLTLtoCircuit.createCircuit(net, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), data, stats);
        return renderer;
    }

}

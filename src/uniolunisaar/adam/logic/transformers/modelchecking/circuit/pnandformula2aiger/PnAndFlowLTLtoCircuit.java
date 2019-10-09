package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl.FlowLTLTransformerParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl.FlowLTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequentialInhibitor;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.util.benchmarks.modelchecking.BenchmarksMC;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;

/**
 *
 * @author Manuel Gieseking
 */
public class PnAndFlowLTLtoCircuit extends PnAndLTLtoCircuit {

//    /**
//     *
//     * @param net
//     * @param formula
//     * @param settings
//     * @param data
//     * @return
//     * @throws InterruptedException
//     * @throws IOException
//     * @throws uniol.apt.io.parser.ParseException
//     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
//     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
//     * @throws uniolunisaar.adam.exceptions.ExternalToolException
//     */
//    public static AigerRenderer createCircuit(PetriNetWithTransits net, RunFormula formula, AdamCircuitFlowLTLMCSettings settings, AdamCircuitLTLMCOutputData data) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
//        return createCircuit(net, formula, data, null);
//    }
    /**
     *
     * @param net
     * @param formula
     * @param settings
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public static AigerRenderer createCircuit(PetriNetWithTransits net, RunFormula formula, AdamCircuitFlowLTLMCSettings settings) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitLTLMCSettings.Maximality maximality = settings.getMaximality();
        TransitionSemantics semantics = settings.getSemantics();
        AdamCircuitLTLMCSettings.Stuttering stuttering = settings.getStuttering();
        AdamCircuitFlowLTLMCStatistics stats = settings.getStatistics();
        AdamCircuitFlowLTLMCOutputData data = settings.getOutputData();
        AdamCircuitFlowLTLMCSettings.Approach approach = settings.getApproach();
        boolean initFirst = settings.isInitFirst();
        Logger.getInstance().addMessage("We create the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + maximality + " approach: " + approach + " semantics: " + semantics + " stuttering: " + stuttering
                + " initialization first step: " + initFirst, true);

        // If we have the LTL fragment just use the standard LTLModelchecker
        List<FlowFormula> flowFormulas = LogicsTools.getFlowFormulas(formula);
        if (flowFormulas.isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula.toSymbolString() + "'. Thus, we use the standard model checking algorithm for LTL.");
            return PnAndLTLtoCircuit.createCircuitWithFairnessAndMaximality(net, formula.toLTLFormula(), settings);
        }

        // Add Fairness
        RunFormula f = LogicsTools.addFairness(net, formula);

        // Get the formula for the maximality (null if MAX_NONE)
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
            f = new RunFormula(max, RunOperators.Implication.IMP, f);
        }
//        IRunFormula f = formula;

        PetriNetWithTransits netMC = null;
        ILTLFormula formulaMC = null;
        if (null != approach) {
            switch (approach) {
                case PARALLEL:
                    netMC = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
                    if (data.isOutputTransformedNet()) {
                        PNWTTools.savePnwt2PDF(data.getPath(), netMC, true);
                    }
                    formulaMC = new FlowLTLTransformerParallel().createFormula4ModelChecking4CircuitParallel(net, netMC, f);
//            Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC.toSymbolString() + "'.", false);
                    break;
                case SEQUENTIAL:
                    netMC = PnwtAndFlowLTLtoPNSequential.createNet4ModelCheckingSequential(net, f, initFirst);
                    if (data.isOutputTransformedNet()) {
                        // color all original places
                        for (Place p : netMC.getPlaces()) {
                            if (!netMC.hasPartition(p)) {
                                netMC.setPartition(p, 0);
                            }
                        }
                        try {
                            PNWTTools.saveAPT(data.getPath() + "_mc", netMC, false);
                        } catch (RenderException | FileNotFoundException ex) {
                        }
                        PNWTTools.savePnwt2PDF(data.getPath() + "_mc", netMC, true, flowFormulas.size());
                    }
                    formulaMC = new FlowLTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
//                    formulaMC = FlowLTLTransformerSequentialBackup.createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    break;
                case SEQUENTIAL_INHIBITOR:
                    netMC = PnwtAndFlowLTLtoPNSequentialInhibitor.createNet4ModelCheckingSequential(net, f, initFirst);
                    if (data.isOutputTransformedNet()) {
                        // color all original places
                        for (Place p : netMC.getPlaces()) {
                            if (!netMC.hasPartition(p)) {
                                netMC.setPartition(p, 0);
                            }
                        }
                        try {
                            PNWTTools.saveAPT(data.getPath() + "_mc", netMC, false);
                        } catch (RenderException | FileNotFoundException ex) {
                        }
                        PNWTTools.savePnwt2PDF(data.getPath() + "_mc", netMC, true, flowFormulas.size());
                    }
                    formulaMC = new FlowLTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
//                    formulaMC = FlowLTLTransformerSequentialBackup.createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    break;
                default:
                    throw new RuntimeException("Didn't provided a solution for all approaches yet. Approach '" + approach + "' is missing; sry.");
            }
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            int nb_places = net.getPlaces().size();
            int nb_transitions = net.getTransitions().size();
            int size_f = f.getSize();
            // Set the input sizes
            // input orignal net
            stats.setIn_nb_places(nb_places);
            stats.setIn_nb_transitions(nb_transitions);
            stats.setIn_size_formula(size_f);
            // input model checking net
            stats.setMc_net(netMC);
            stats.setMc_formula(formulaMC);
            if (BenchmarksMC.EDACC) {
                Logger.getInstance().addMessage("nb_mc_places: " + stats.getMc_nb_places(), "edacc");
                Logger.getInstance().addMessage("nb_mc_transitions: " + stats.getMc_nb_transitions(), "edacc");
                Logger.getInstance().addMessage("size_mc_f: " + stats.getMc_size_formula(), "edacc");
            }
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS
        return PnAndLTLtoCircuit.createCircuitWithoutFairnessAndMaximality(netMC, formulaMC, settings);
    }

}

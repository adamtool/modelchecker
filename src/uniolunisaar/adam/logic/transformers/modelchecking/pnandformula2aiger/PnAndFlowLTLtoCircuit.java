package uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings.TransitionSemantics;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.petrinet.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNSequentialInhibitor;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl.FlowLTLTransformerIngoingParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl.FlowLTLTransformerIngoingSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl.FlowLTLTransformerOutgoingParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl.FlowLTLTransformerOutgoingSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallelInhibitor;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNParInhibitorNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNParallelNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNSeqInhibitorNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNSequentialNoInit;
import uniolunisaar.adam.util.MCTools;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.benchmarks.modelchecking.BenchmarksMC;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

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
    public static AigerRenderer createCircuit(PetriNetWithTransits net, RunLTLFormula formula, AdamCircuitFlowLTLMCSettings settings) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitLTLMCSettings.Maximality maximality = settings.getMaximality();
        TransitionSemantics semantics = settings.getRendererSettings().getSemantics();
        AdamCircuitFlowLTLMCStatistics stats = settings.getStatistics();
        AdamCircuitFlowLTLMCOutputData data = settings.getOutputData();
        AdamCircuitFlowLTLMCSettings.Approach approach = settings.getApproach();
        boolean initFirst = settings.isInitFirst();
        Logger.getInstance().addMessage("We create the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + maximality + " approach: " + approach + " semantics: " + semantics
                + " initialization first step: " + initFirst, true);

        // If we have the LTL fragment just use the standard LTLModelchecker
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(formula);
        if (flowFormulas.isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula.toSymbolString() + "'. Thus, we use the standard model checking algorithm for LTL.");
            return PnAndLTLtoCircuit.createCircuitWithFairnessAndMaximality(net, LogicsTools.convert2LTL(formula), settings);
        }

        // Add Fairness
        RunLTLFormula f = LogicsTools.addFairness(net, formula);

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
            f = new RunLTLFormula(max, RunOperators.Implication.IMP, f);
        }
//        IRunFormula f = formula;

        PetriNetWithTransits netMC = null;
        ILTLFormula formulaMC = null;
        if (null != approach) {
            switch (approach) {
                case PARALLEL:
                    if (flowFormulas.size() > 1) {
                        throw new NotConvertableException("The parallel approach (without inhibitor arcs) is not implemented for more than one flow subformula!. Please use another approach.");
                    }
                    if (semantics == TransitionSemantics.INGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNParallelNoInit.createNet4ModelCheckingParallelOneFlowFormula(net);
                    } else if (semantics == TransitionSemantics.OUTGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                        if (semantics == TransitionSemantics.OUTGOING) {
                            formulaMC = new FlowLTLTransformerOutgoingParallel().createFormula4ModelChecking4CircuitParallelOneFlowFormula(net, netMC, f, settings);
                        } else if (semantics == TransitionSemantics.INGOING) {
                            formulaMC = new FlowLTLTransformerIngoingParallel().createFormula4ModelChecking4CircuitParallelOneFlowFormula(net, netMC, f, settings);
                        } else {
                            throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                        }
                    } else { // currently cannot occur
                        if (semantics == TransitionSemantics.OUTGOING) {
                            formulaMC = new FlowLTLTransformerOutgoingParallel().createFormula4ModelChecking4CircuitParallel(net, netMC, f, settings);
                        } else if (semantics == TransitionSemantics.INGOING) {
                            formulaMC = new FlowLTLTransformerIngoingParallel().createFormula4ModelChecking4CircuitParallel(net, netMC, f, settings);
                        } else {
                            throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                        }
                    }
                    break;
                case PARALLEL_INHIBITOR:
                    if (semantics == TransitionSemantics.INGOING) {
                        if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                            netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallelOneFlowFormula(net);
                        } else {
                            netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallel(net, flowFormulas.size());
                        }
                    } else if (semantics == TransitionSemantics.OUTGOING) {
                        if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                            netMC = PnwtAndNbFlowFormulas2PNParallelInhibitor.createNet4ModelCheckingParallelOneFlowFormula(net);
//                            netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallelOneFlowFormula(net);
                        } else {
                            netMC = PnwtAndNbFlowFormulas2PNParallelInhibitor.createNet4ModelCheckingParallel(net, flowFormulas.size());
//                            netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallel(net, flowFormulas.size());
                        }
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                        if (semantics == TransitionSemantics.OUTGOING) {
                            formulaMC = new FlowLTLTransformerOutgoingParallel().createFormula4ModelChecking4CircuitParallelOneFlowFormula(net, netMC, f, settings);
                        } else if (semantics == TransitionSemantics.INGOING) {
                            formulaMC = new FlowLTLTransformerIngoingParallel().createFormula4ModelChecking4CircuitParallelOneFlowFormula(net, netMC, f, settings);
                        } else {
                            throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                        }

                    } else {
                        if (semantics == TransitionSemantics.OUTGOING) {
                            formulaMC = new FlowLTLTransformerOutgoingParallel().createFormula4ModelChecking4CircuitParallel(net, netMC, f, settings);
                        } else if (semantics == TransitionSemantics.INGOING) {
                            formulaMC = new FlowLTLTransformerIngoingParallel().createFormula4ModelChecking4CircuitParallel(net, netMC, f, settings);
                        } else {
                            throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                        }
                    }
                    break;
                case SEQUENTIAL: {
                    if (semantics == TransitionSemantics.INGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNSequentialNoInit.createNet4ModelCheckingSequential(net, flowFormulas.size());
                    } else if (semantics == TransitionSemantics.OUTGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNSequential.createNet4ModelCheckingSequential(net, flowFormulas.size(), initFirst);
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    if (semantics == TransitionSemantics.OUTGOING) {
                        formulaMC = new FlowLTLTransformerOutgoingSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    } else if (semantics == TransitionSemantics.INGOING) {
                        formulaMC = new FlowLTLTransformerIngoingSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    break;
                }
                case SEQUENTIAL_INHIBITOR:
                    if (semantics == TransitionSemantics.INGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNSeqInhibitorNoInit.createNet4ModelCheckingSequential(net, flowFormulas.size());
                    } else if (semantics == TransitionSemantics.OUTGOING) {
                        netMC = PnwtAndNbFlowFormulas2PNSequentialInhibitor.createNet4ModelCheckingSequential(net, flowFormulas.size(), initFirst);
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    if (semantics == TransitionSemantics.OUTGOING) {
                        formulaMC = new FlowLTLTransformerOutgoingSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    } else if (semantics == TransitionSemantics.INGOING) {
                        formulaMC = new FlowLTLTransformerIngoingSequential().createFormula4ModelChecking4CircuitSequential(net, netMC, f, settings);
                    } else {
                        throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                    }
                    break;
                default:
                    throw new RuntimeException("Didn't provide a solution for all approaches yet. Approach '" + approach + "' is missing; sry.");
            }
            MCTools.addCoordinates(net, netMC);
            if (data.isOutputTransformedNet()) {
                // color all original places
                for (Place p : netMC.getPlaces()) {
                    if (!netMC.hasPartition(p)) {
                        netMC.setPartition(p, 0);
                    }
                }
                try {
                    PNWTTools.saveAPT(data.getPath() + "_mc", netMC, true, true);
                } catch (RenderException | FileNotFoundException ex) {
                }
                PNWTTools.savePnwt2PDF(data.getPath() + "_mc", netMC, true, flowFormulas.size() + 1);
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

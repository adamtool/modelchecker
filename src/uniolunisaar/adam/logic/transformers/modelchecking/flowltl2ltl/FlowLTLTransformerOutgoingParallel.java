package uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerOutgoingParallel extends FlowLTLTransformer {

    @Override
    ILTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, int nb_ff, boolean scopeEventually) {
        if (phi.isTransition()) {
            // It is possible to fire original transition (concurrently s.th. is happening)
            // also if more than one subformula transition not moving my flow
            // until a transition concerning my flow is fired
            Collection<ILTLFormula> other = new ArrayList<>();
            Collection<ILTLFormula> mine = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                // this is only for one subformula
//                if (t.getLabel().equals(phi.getId()) && !t.getLabel().equals(t.getId())) { // not the original trans, which is also labelled 
                // for all subnet transition the extension saves the participating subnets              
                if (t.hasExtension("subnet") && ((List<Integer>) t.getExtension("subnet")).contains(nb_ff)) { // todo: check ! NOt only the equally labelled?
                    mine.add(new LTLAtomicProposition(t));
                } else {
                    if (!t.hasExtension("initSubnet")) { // not the init transitions
                        other.add(new LTLAtomicProposition(t));
                    }
                }
            }
            return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(other, false), LTLOperators.Binary.U, FormulaCreator.bigWedgeOrVeeObject(mine, false));
        } else if (phi.isPlace()) {
            String id = phi.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
            if (!net.containsPlace(id)) {
                return new LTLConstants.False();
            }
            Place p = net.getPlace(id);
            return new LTLAtomicProposition(p);
        }
        return phi;
    }

    @Override
    ILTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, int nb_ff, boolean scopeEventually) {
        if (!scopeEventually && phi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && phi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }
        ILTLFormula subst = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
        if (subst instanceof LTLFormula && ((LTLFormula) subst).getPhi() instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> substCast = phi;
            if (substCast.getOp() == LTLOperators.Unary.X) {
                // next means next for our flow, thus we can first skip all other transitions
                // which are not moving our chain until some transition is used which 
                // moves the tracked chain, then really in the next step phi has to hold
                // in the case that never a transition fires again which moves our chain
                // (the chain is finite and we stutter), the formula directly has to hold
                Collection<ILTLFormula> other = new ArrayList<>();
                Collection<ILTLFormula> mine = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    // for all subnet transition the extension saves the participating subnets              
                    if (t.hasExtension("subnet") && ((List<Integer>) t.getExtension("subnet")).contains(nb_ff)) {
                        mine.add(new LTLAtomicProposition(t));
                    } else {
                        if (!t.hasExtension("initSubnet")) { // not the init transitions
                            other.add(new LTLAtomicProposition(t));
                        }
                    }
                }
                ILTLFormula Vmine = FormulaCreator.bigWedgeOrVeeObject(mine, false);
                LTLFormula mineAndNext = new LTLFormula(Vmine, LTLOperators.Binary.AND, new LTLFormula(phi.getOp(), subst));
                LTLFormula neverMine = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, Vmine)), LTLOperators.Binary.AND, subst);

                return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(other, false), LTLOperators.Binary.U, new LTLFormula(mineAndNext, LTLOperators.Binary.OR, neverMine));
            }
        }
        return new LTLFormula(phi.getOp(), subst);
    }

    @Override
    IFormula replaceLTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, boolean scopeEventually, int nbFlowFormulas) {
        if (phi.isTransition()) {
            // in the run part when a t transition should fire, it's ok when any t labeled transition can fire
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (t.getLabel().equals(phi.getId())) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            return FormulaCreator.bigWedgeOrVeeObject(elements, false);
        }
        return phi;
    }

    public ILTLFormula createFormula4ModelChecking4CircuitParallel(PetriNetWithTransits orig, PetriNet net, RunLTLFormula formula, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
        int nbFlowFormulas = LogicsTools.getFlowLTLFormulas(formula).size();
        if (nbFlowFormulas == 0) {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
            return LogicsTools.convert2LTL(formula);
        }

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);
        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowLTLFormula flowFormula = flowFormulas.get(i);
            flowFormula = replaceInFlowFormula(orig, net, flowFormula, i);
            try {
//                LTLAtomicProposition init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID + "_" + i));
                LTLAtomicProposition newChains = new LTLAtomicProposition(net.getPlace(PnwtAndNbFlowFormulas2PN.NEW_TOKENFLOW_ID + "_" + i));

                //INITPLACES: it is also OK to chose two consider newly created chains, but newer do so
                //(init will be left as long as one transition is taken (not putting the init marking without deciding for new chain or init chain)   
                f = f.substitute(flowFormulas.get(i), new RunLTLFormula(new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, newChains),
                        LTLOperators.Binary.OR,
                        new LTLFormula(newChains, LTLOperators.Binary.U,
                                // the new chain starts with a transition which has to be skipped
                                // so we really have to be in the last step where newChains hold
                                new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, newChains), LTLOperators.Binary.AND, flowFormula.getPhi())))));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
            }
        }
        ILTLFormula retF = LogicsTools.convert2LTL(f);
        //INITPLACES: should skip the first init step (since we cannot force that the first step is really done (for MAX=NONE), we omit those runs)
        //              so only consider the runs where in the next step init not holds
        return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(new LTLAtomicProposition(net.getPlace(INIT_TOKENFLOW_ID + "_0")), LTLOperators.Binary.OR, retF));
    }

    /**
     * This is only done for ONE flow formula
     *
     * Check if this earlier implemented special case has any advantages
     * compared to the general case!
     *
     * @param orig
     * @param net
     * @param formula
     * @return
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     */
    public ILTLFormula createFormula4ModelChecking4CircuitParallelOneFlowFormula(PetriNetWithTransits orig, PetriNet net, RunLTLFormula formula, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
        int nbFlowFormulas = LogicsTools.getFlowLTLFormulas(formula).size();

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            FlowLTLFormula flowF = replaceInFlowFormula(orig, net, flowFormulas.get(0), 0);
            try {
//// VERSION: This version would need an additional forcing of the firing of the first transition (instead of the globally init_tfl)
////          in cases where maximality is not stated in the circuit.
//                //INITPLACES: it is also OK to chose two consider newly created chains, but newer do so
////                ILTLFormula init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID));
//                ILTLFormula init = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID)), LTLOperators.Binary.OR, new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID)));
//                f = f.substitute(flowFormulas.getId(0), new RunFormula(new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.G, init),
//                        LTLOperators.Binary.OR,
//                        new LTLFormula(init, LTLOperators.Binary.U, flowF.getPhi()))));
//                ILTLFormula retF = convert2LTL(f);
//                //INITPLACES: should skip the first init step
//                retF = new LTLFormula(LTLOperators.Unary.X, retF);
//// END VERSION
// VERSION: here we only consider runs where the initialization had been done
                //INITPLACES: it is also OK to chose to consider newly created chains, but newer do so (init will be left as long as one transition is taken (not putting the init marking without deciding for new chain or init chain)               
                ILTLFormula newTokenFlow = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndNbFlowFormulas2PN.NEW_TOKENFLOW_ID + "_0")));
                f = f.substitute(flowFormulas.get(0), new RunLTLFormula(new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, newTokenFlow),
                        LTLOperators.Binary.OR,
                        new LTLFormula(newTokenFlow, LTLOperators.Binary.U,
                                // the new chain starts with a transition which has to be skipped
                                // so we really have to be in the last step where newChains hold                                
                                new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, newTokenFlow), LTLOperators.Binary.AND, flowF.getPhi())))));
                ILTLFormula retF = LogicsTools.convert2LTL(f);
                //INITPLACES: should skip the first init step (since we cannot force that the first step is really done, we omit those runs)
                //              so only consider the runs where in the next step init not holds
                retF = new LTLFormula(LTLOperators.Unary.X, new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndNbFlowFormulas2PN.INIT_TOKENFLOW_ID + "_0")),
                        LTLOperators.Binary.OR, retF));
// END VERSION
                return retF;
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
            }
        } else {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
            return LogicsTools.convert2LTL(f);
        }
    }

}

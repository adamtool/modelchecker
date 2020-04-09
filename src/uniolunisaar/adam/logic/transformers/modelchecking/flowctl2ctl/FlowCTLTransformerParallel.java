package uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNNoInit;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLTransformerParallel extends FlowCTLTransformer {

    @Override
    ICTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nb_ff) throws NotConvertableException {
        if (phi.isTransition()) {
            throw new NotConvertableException("FlowCTL formulas with transitions can only be transformed into CTL*.");
//            // It is possible to fire original transition (concurrently s.th. is happening)
//            // also if more than one subformula transition not moving my flow
//            // until a transition concerning my flow is fired
//            Collection<ILTLFormula> other = new ArrayList<>();
//            Collection<ILTLFormula> mine = new ArrayList<>();
//            for (Transition t : net.getTransitions()) {
//                // this is only for one subformula
////                if (t.getLabel().equals(phi.get()) && !t.getLabel().equals(t.getId())) { // not the original trans, which is also labelled 
//                // for all subnet transition the extension saves the participating subnets              
//                if (t.hasExtension("subnet") && ((List<Integer>) t.getExtension("subnet")).contains(nb_ff)) {
//                    mine.add(new LTLAtomicProposition(t));
//                } else {
//                    if (!t.hasExtension("initSubnet")) { // not the init transitions
//                        other.add(new LTLAtomicProposition(t));
//                    }
//                }
//            }
//            return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(other, false), LTLOperators.Binary.U, FormulaCreator.bigWedgeOrVeeObject(mine, false));
        } else if (phi.isPlace()) {
            String id = phi.get() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
            if (!net.containsPlace(id)) {
                return new CTLConstants.False();
            }
            Place p = net.getPlace(id);
            return new CTLAtomicProposition(p);
        }
        return phi;
    }

    @Override
    ICTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ICTLFormula, CTLOperators.Unary> phi, int nb_ff) throws NotConvertableException {
        ICTLFormula subst = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff);
        if (subst instanceof CTLFormula && ((CTLFormula) subst).getPhi() instanceof FormulaUnary) {
            FormulaUnary<ICTLFormula, CTLOperators.Unary> substCast = phi;
            if (substCast.getOp() == CTLOperators.Unary.AX || substCast.getOp() == CTLOperators.Unary.EX) {
                throw new NotConvertableException("FlowCTL formulas with a next operator can only be transformed into CTL*.");
//                // next means next for our flow, thus we can first skip all other transitions
//                // which are not moving our chain until some transition is used which 
//                // moves the tracked chain, then really in the next step phi has to hold
//                // in the case that never a transition fires again which moves our chain
//                // (the chain is finite and we stutter), the formula directly has to hold
//                Collection<ILTLFormula> other = new ArrayList<>();
//                Collection<ILTLFormula> mine = new ArrayList<>();
//                for (Transition t : net.getTransitions()) {
//                    // for all subnet transition the extension saves the participating subnets              
//                    if (t.hasExtension("subnet") && ((List<Integer>) t.getExtension("subnet")).contains(nb_ff)) {
//                        mine.add(new LTLAtomicProposition(t));
//                    } else {
//                        if (!t.hasExtension("initSubnet")) { // not the init transitions
//                            other.add(new LTLAtomicProposition(t));
//                        }
//                    }
//                }
//                ILTLFormula Vmine = FormulaCreator.bigWedgeOrVeeObject(mine, false);
//                LTLFormula mineAndNext = new LTLFormula(Vmine, LTLOperators.Binary.AND, new LTLFormula(phi.getOp(), subst));
//                LTLFormula neverMine = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, Vmine)), LTLOperators.Binary.AND, subst);
//
//                return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(other, false), LTLOperators.Binary.U, new LTLFormula(mineAndNext, LTLOperators.Binary.OR, neverMine));
            }
        }
        return new CTLFormula(phi.getOp(), subst);
    }

    @Override
    IFormula replaceCTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nbFlowFormulas) throws NotConvertableException {
        if (phi.isTransition()) {
            // in the run part when a t transition should fire, it's ok when any t labeled transition can fire
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (t.getLabel().equals(phi.get())) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            return FormulaCreator.bigWedgeOrVeeObject(elements, false);
        }
        return phi;
    }

    public ICTLFormula createFormula4ModelChecking4CircuitParallel(PetriNetWithTransits orig, PetriNet net, RunCTLFormula formula) throws NotConvertableException {
        int nbFlowFormulas = LogicsTools.getFlowCTLFormulas(formula).size();
        if (nbFlowFormulas == 0) {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net modelchecker should be used.", false);
            return LogicsTools.convert2CTL(formula);
        }

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);
        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowCTLFormula> flowFormulas = LogicsTools.getFlowCTLFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowCTLFormula flowFormula = flowFormulas.get(i);
            flowFormula = replaceInFlowFormula(orig, net, flowFormula, i);
            try {
                CTLAtomicProposition init = new CTLAtomicProposition(net.getPlace(PnwtAndFlowCTLStarToPNNoInit.INIT_TOKENFLOW_ID + "_" + i));
                CTLFormula phi2 = new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, init), CTLOperators.Binary.AND, flowFormula.getPhi());
                CTLFormula subs = null;
                if (flowFormula.getOp() == FlowCTLFormula.FlowCTLOperator.All) {
                    // not working, would need CTL* to have as second disjunct only G init
//                    subs = new CTLFormula(init, CTLOperators.Binary.AU, phi2);
//                    subs = new CTLFormula(subs, CTLOperators.Binary.OR, new CTLFormula(CTLOperators.Unary.AG, init));
                    subs = new CTLFormula(CTLOperators.Unary.NEG, 
                            new CTLFormula(init, CTLOperators.Binary.EU, new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, init), CTLOperators.Binary.AND,
                            new CTLFormula(CTLOperators.Unary.NEG, flowFormula.getPhi()))));
                } else if (flowFormula.getOp() == FlowCTLFormula.FlowCTLOperator.Exists) {
                    subs = new CTLFormula(init, CTLOperators.Binary.EU, phi2);
                }
                f = f.substitute(flowFormulas.get(i), new RunCTLFormula(subs));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
            }
        }
        return LogicsTools.convert2CTL(f);
    }
//
//    /**
//     * This is only done for ONE flow formula
//     *
//     * Check if this earlier implemented special case has any advantages
//     * compared to the general case!
//     *
//     * @param orig
//     * @param net
//     * @param formula
//     * @return
//     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
//     */
//    public ILTLFormula createFormula4ModelChecking4CircuitParallelOneFlowFormula(PetriNetWithTransits orig, PetriNet net, RunLTLFormula formula) throws NotConvertableException {
//        int nbFlowFormulas = LogicsTools.getFlowLTLFormulas(formula).size();
//
//        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
//        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);
//
//        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
//        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(f);
//        if (flowFormulas.size() > 1) {
//            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
//        }
//        if (flowFormulas.size() == 1) {
//            FlowLTLFormula flowF = replaceInFlowFormula(orig, net, flowFormulas.get(0), 0);
//            try {
////// VERSION: This version would need an additional forcing of the firing of the first transition (instead of the globally init_tfl)
//////          in cases where maximality is not stated in the circuit.
////                //INITPLACES: it is also OK to chose two consider newly created chains, but newer do so
//////                ILTLFormula init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID));
////                ILTLFormula init = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID)), LTLOperators.Binary.OR, new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID)));
////                f = f.substitute(flowFormulas.get(0), new RunFormula(new LTLFormula(
////                        new LTLFormula(LTLOperators.Unary.G, init),
////                        LTLOperators.Binary.OR,
////                        new LTLFormula(init, LTLOperators.Binary.U, flowF.getPhi()))));
////                ILTLFormula retF = convert2LTL(f);
////                //INITPLACES: should skip the first init step
////                retF = new LTLFormula(LTLOperators.Unary.X, retF);
////// END VERSION
//// VERSION: here we only consider runs where the initialization had been done
//                //INITPLACES: it is also OK to chose to consider newly created chains, but newer do so (init will be left as long as one transition is taken (not putting the init marking without deciding for new chain or init chain)               
//                ILTLFormula newTokenFlow = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "_0")));
//                f = f.substitute(flowFormulas.get(0), new RunLTLFormula(new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.G, newTokenFlow),
//                        LTLOperators.Binary.OR,
//                        new LTLFormula(newTokenFlow, LTLOperators.Binary.U,
//                                // the new chain starts with a transition which has to be skipped
//                                // so we really have to be in the last step where newChains hold                                
//                                new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, newTokenFlow), LTLOperators.Binary.AND, flowF.getPhi())))));
//                ILTLFormula retF = LogicsTools.convert2LTL(f);
//                //INITPLACES: should skip the first init step (since we cannot force that the first step is really done, we omit those runs)
//                //              so only consider the runs where in the next step init not holds
//                retF = new LTLFormula(LTLOperators.Unary.X, new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID + "_0")),
//                        LTLOperators.Binary.OR, retF));
//// END VERSION
//                return retF;
//            } catch (NotSubstitutableException ex) {
//                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
//            }
//        } else {
//            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
//            return LogicsTools.convert2LTL(f);
//        }
//    }
}

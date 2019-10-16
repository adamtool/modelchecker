package uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerParallel extends FlowLTLTransformer {

    @Override
    ILTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, int nb_ff, boolean scopeEventually) {
        if (phi.isTransition()) {
            // It is possible to fire original transition (concurrently s.th. is happening) until a transition concerning my flow is fired
            Collection<ILTLFormula> origT = new ArrayList<>();
            for (Transition t : orig.getTransitions()) {
                origT.add(new LTLAtomicProposition(t));
            }
            Collection<ILTLFormula> mine = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (t.getLabel().equals(phi.get()) && !t.getLabel().equals(t.getId())) { // not the original trans, which is also labelled
                    mine.add(new LTLAtomicProposition(t));
                }
            }
            return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(origT, false), LTLOperators.Binary.U, FormulaCreator.bigWedgeOrVeeObject(mine, false));
        } else if (phi.isPlace()) {
            String id = phi.get() + TOKENFLOW_SUFFIX_ID;
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
            FormulaUnary<ILTLFormula, LTLOperators.Unary> substCast = ((FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi);
            if (substCast.getOp() == LTLOperators.Unary.X) {
                List<Transition> newTransitions = new ArrayList<>();
                for (Place place : orig.getPlaces()) {
                    if (place.getInitialToken().getValue() > 0) {
                        String id = INIT_TOKENFLOW_ID + "-" + place.getId();
                        if (net.containsTransition(id)) { // checks if initial transit
                            newTransitions.add(net.getTransition(id));
                        }
                    }
                }

                // all original transitions
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : orig.getTransitions()) {
                    elements.add(new LTLAtomicProposition(t));
                }
                // and the new transitions
                for (Transition t : newTransitions) {
                    elements.add(new LTLAtomicProposition(t));
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                elements = new ArrayList<>();
                // all transitions which are not original or the new ones
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId()) || !newTransitions.contains(t)) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }
                LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, phi.getPhi());
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
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
                if (t.getLabel().equals(phi.get())) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            return FormulaCreator.bigWedgeOrVeeObject(elements, false);
        }
        return phi;
    }

    @Override
    ILTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, boolean scopeEventually, int nbFlowFormulas) {
        // check if it's in the scope of an eventually
        if (!scopeEventually && phi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && phi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }
        ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        if (phi.getOp() == LTLOperators.Unary.X) {
            // all init transitions can be skipped
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (t.getId().startsWith(INIT_TOKENFLOW_ID)) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }

            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
            elements = new ArrayList<>();
            // all transitions which are original
            for (Transition t : orig.getTransitions()) {
                elements.add(new LTLAtomicProposition(t));
            }
            LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, phi.getPhi());
//                ILTLFormula untilSecond =  castPhi.getPhi();
            return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
        }
        return new LTLFormula(phi.getOp(), substChildPhi);
    }

    /**
     * This is only done for ONE flow formula
     *
     * @param orig
     * @param net
     * @param formula
     * @return
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     */
    public ILTLFormula createFormula4ModelChecking4CircuitParallel(PetriNetWithTransits orig, PetriNet net, RunFormula formula) throws NotConvertableException {
        int nbFlowFormulas = LogicsTools.getFlowFormulas(formula).size();

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowFormula> flowFormulas = LogicsTools.getFlowFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            FlowFormula flowF = replaceInFlowFormula(orig, net, flowFormulas.get(0), 0);
            try {
//// VERSION: This version would need an additional forcing of the firing of the first transition (instead of the globally init_tfl)
////          in cases where maximality is not stated in the circuit.
//                //INITPLACES: it is also OK to chose two consider newly created chains, but newer do so
////                ILTLFormula init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID));
//                ILTLFormula init = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID)), LTLOperators.Binary.OR, new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID)));
//                f = f.substitute(flowFormulas.get(0), new RunFormula(new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.G, init),
//                        LTLOperators.Binary.OR,
//                        new LTLFormula(init, LTLOperators.Binary.U, flowF.getPhi()))));
//                ILTLFormula retF = convert(f);
//                //INITPLACES: should skip the first init step
//                retF = new LTLFormula(LTLOperators.Unary.X, retF);
//// END VERSION
// VERSION: here we only consider runs where the initialization had been done
                //INITPLACES: it is also OK to chose two consider newly created chains, but newer do so
                ILTLFormula init = new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID)));
                f = f.substitute(flowFormulas.get(0), new RunFormula(new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, init),
                        LTLOperators.Binary.OR,
                        new LTLFormula(init, LTLOperators.Binary.U, flowF.getPhi()))));
                ILTLFormula retF = LogicsTools.convert(f);
                //INITPLACES: should skip the first init step (since we cannot force that the first step is really done, we omit those runs)
                //              so only consider the runs where in the next step init not holds
                retF = new LTLFormula(LTLOperators.Unary.X, new LTLFormula(new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID)),
                        LTLOperators.Binary.OR, retF));
// END VERSION
                return retF;
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
            }
        } else {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
            return LogicsTools.convert(f);
        }
    }

}

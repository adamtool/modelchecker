package uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.AtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.FormulaBinary;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IAtomicProposition;
import uniolunisaar.adam.ds.logics.flowlogics.IFlowFormula;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.IOperatorBinary;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPN;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerParallelBackup extends FlowLTLTransformer {

    private static FlowLTLFormula replaceNextInFlowFormulaParallel(PetriNetWithTransits orig, PetriNet net, FlowLTLFormula flowFormula) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowLTLFormula(replaceNextWithinFlowFormulaParallel(orig, net, phi));
    }

    private static ILTLFormula replaceNextWithinFlowFormulaParallel(PetriNetWithTransits orig, PetriNet net, ILTLFormula phi) {
        if (phi instanceof IAtomicProposition) {
            return phi;
        } else if (phi instanceof LTLFormula) {
            return new LTLFormula(replaceNextWithinFlowFormulaParallel(orig, net, ((LTLFormula) phi).getPhi()));
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi;
            ILTLFormula subst = replaceNextWithinFlowFormulaParallel(orig, net, castPhi.getPhi());
            if (subst instanceof LTLFormula && ((LTLFormula) subst).getPhi() instanceof FormulaUnary) {
                FormulaUnary<ILTLFormula, LTLOperators.Unary> substCast = ((FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi);
                if (substCast.getOp() == LTLOperators.Unary.X) {
                    List<Transition> newTransitions = new ArrayList<>();
                    for (Place place : orig.getPlaces()) {
                        if (place.getInitialToken().getValue() > 0 && orig.isInitialTransit(place)) {
                            String id = INIT_TOKENFLOW_ID + "-" + place.getId();
                            newTransitions.add(net.getTransition(id));
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
                    LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
                    return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
                }
            }
            return new LTLFormula(castPhi.getOp(), subst);
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> castPhi = (FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula>) phi;
            ILTLFormula subst1 = replaceNextWithinFlowFormulaParallel(orig, net, castPhi.getPhi1());
            ILTLFormula subst2 = replaceNextWithinFlowFormulaParallel(orig, net, castPhi.getPhi2());
            return new LTLFormula(subst1, castPhi.getOp(), subst2);
        }
        throw new RuntimeException("The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    private static IFormula replaceNextWithinRunFormulaParallel(PetriNet orig, PetriNet net, IFormula phi) {
        if (phi instanceof IAtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunLTLFormula) {
            return new RunLTLFormula(replaceNextWithinRunFormulaParallel(orig, net, ((RunLTLFormula) phi).getPhi()));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormulaParallel(orig, net, ((LTLFormula) phi).getPhi());
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary<?,?>) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked
            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormulaParallel(orig, net, castPhi.getPhi()); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            if (castPhi.getOp() == LTLOperators.Unary.X) {
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
                LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
//                ILTLFormula untilSecond =  castPhi.getPhi();
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
            }
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary<?,?,?>) {
            IFormula subst1 = replaceNextWithinRunFormulaParallel(orig, net, ((FormulaBinary) phi).getPhi1());
            IFormula subst2 = replaceNextWithinRunFormulaParallel(orig, net, ((FormulaBinary) phi).getPhi2());
            IOperatorBinary<?,?> op = ((FormulaBinary<?,?,?>) phi).getOp();
            if (phi instanceof ILTLFormula) {
                return new LTLFormula((ILTLFormula) subst1, (LTLOperators.Binary) op, (ILTLFormula) subst2);
            } else if (phi instanceof IRunFormula) {
                if (op instanceof RunOperators.Binary) {
                    return new RunLTLFormula((IRunFormula) subst1, (RunOperators.Binary) op, (IRunFormula) subst2);
                } else {
                    return new RunLTLFormula((ILTLFormula) subst1, (RunOperators.Implication) op, (IRunFormula) subst2);
                }
            }
        }
        throw new RuntimeException(
                "The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
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
    public static ILTLFormula createFormula4ModelChecking4CircuitParallel(PetriNetWithTransits orig, PetriNet net, IRunFormula formula) throws NotConvertableException {
        // replace the next operator in the run-part
        IFormula f = replaceNextWithinRunFormulaParallel(orig, net, formula);

        // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
        // Replace the transitions with the big-or of all accordingly labelled transitions
        for (Transition transition : orig.getTransitions()) {
            try {
                AtomicProposition trans = new LTLAtomicProposition(transition);
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (t.getLabel().equals(transition.getId())) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }
                f = f.substitute(trans, FormulaCreator.bigWedgeOrVeeObject(elements, false));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the transitions. (Should not happen).", ex);
            }
        }

        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            try {
                // replace the places within the flow formula accordingly (the transitions can be replaced for the whole formula and is done later)
                // NOT ANYMORE, do it later because of the next: and replace the flow formula by the ltl formula with G(initfl> 0)\vee LTL-Part-Of-FlowFormula
//                IFormula flowF = ((FlowFormula) flowFormulas.get(0)).getPhi();
                FlowLTLFormula flowF = flowFormulas.get(0);
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    if (net.containsNode(place.getId() + TOKENFLOW_SUFFIX_ID)) { // only if the place is part of the subnet                   
                        AtomicProposition p = new LTLAtomicProposition(place);
                        AtomicProposition psub = new LTLAtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID));
                        flowF = (FlowLTLFormula) flowF.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                    }
                }

                // Replace the next operator within the flow formula
                flowF = replaceNextInFlowFormulaParallel(orig, net, flowF);
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
                f = f.substitute(flowFormulas.get(0), new RunLTLFormula(new LTLFormula(
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
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
        } else {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
            return LogicsTools.convert(f);
        }
    }

}

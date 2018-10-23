package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.exceptions.NotSubstitutableException;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.FormulaBinary;
import uniolunisaar.adam.logic.flowltl.FormulaUnary;
import uniolunisaar.adam.logic.flowltl.IFlowFormula;
import uniolunisaar.adam.logic.flowltl.IFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IOperatorBinary;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.util.FormulaCreator;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import static uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerFlowLTL.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerFlowLTL.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerParallel extends FlowLTLTransformer {

    private static FlowFormula replaceNextInFlowFormulaParallel(PetriGame orig, PetriNet net, FlowFormula flowFormula) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowFormula(replaceNextWithinFlowFormulaParallel(orig, net, phi));
    }

    private static ILTLFormula replaceNextWithinFlowFormulaParallel(PetriGame orig, PetriNet net, ILTLFormula phi) {
        if (phi instanceof AtomicProposition) {
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
                        if (place.getInitialToken().getValue() > 0 && orig.isInitialTokenflow(place)) {
                            String id = INIT_TOKENFLOW_ID + "-" + place.getId();
                            newTransitions.add(net.getTransition(id));
                        }
                    }

                    // all original transitions
                    Collection<ILTLFormula> elements = new ArrayList<>();
                    for (Transition t : orig.getTransitions()) {
                        elements.add(new AtomicProposition(t));
                    }
                    // and the new transitions
                    for (Transition t : newTransitions) {
                        elements.add(new AtomicProposition(t));
                    }

                    ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                    elements = new ArrayList<>();
                    // all transitions which are not original or the new ones
                    for (Transition t : net.getTransitions()) {
                        if (!orig.containsTransition(t.getId()) || !newTransitions.contains(t)) {
                            elements.add(new AtomicProposition(t));
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

    private static IFormula replaceNextWithinRunFormulaParallel(PetriGame orig, PetriNet net, IFormula phi) {
        if (phi instanceof AtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunFormula) {
            return new RunFormula(replaceNextWithinRunFormulaParallel(orig, net, ((RunFormula) phi).getPhi()));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormulaParallel(orig, net, ((LTLFormula) phi).getPhi());
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked
            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormulaParallel(orig, net, castPhi.getPhi()); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // all init transitions can be skipped
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (t.getId().startsWith(INIT_TOKENFLOW_ID)) {
                        elements.add(new AtomicProposition(t));
                    }
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                elements = new ArrayList<>();
                // all transitions which are original
                for (Transition t : orig.getTransitions()) {
                    elements.add(new AtomicProposition(t));
                }
                LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
//                ILTLFormula untilSecond =  castPhi.getPhi();
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
            }
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            IFormula subst1 = replaceNextWithinRunFormulaParallel(orig, net, ((FormulaBinary) phi).getPhi1());
            IFormula subst2 = replaceNextWithinRunFormulaParallel(orig, net, ((FormulaBinary) phi).getPhi2());
            IOperatorBinary op = ((FormulaBinary) phi).getOp();
            if (phi instanceof ILTLFormula) {
                return new LTLFormula((ILTLFormula) subst1, (LTLOperators.Binary) op, (ILTLFormula) subst2);
            } else if (phi instanceof IRunFormula) {
                if (op instanceof RunOperators.Binary) {
                    return new RunFormula((IRunFormula) subst1, (RunOperators.Binary) op, (IRunFormula) subst2);
                } else {
                    return new RunFormula((ILTLFormula) subst1, (RunOperators.Implication) op, (IRunFormula) subst2);
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
     */
    public static ILTLFormula createFormula4ModelChecking4CircuitParallel(PetriGame orig, PetriNet net, IRunFormula formula) throws NotConvertableException {
        // replace the next operator in the run-part
        IFormula f = replaceNextWithinRunFormulaParallel(orig, net, formula);

        // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
        // Replace the transitions with the big-or of all accordingly labelled transitions
        for (Transition transition : orig.getTransitions()) {
            try {
                AtomicProposition trans = new AtomicProposition(transition);
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (t.getLabel().equals(transition.getId())) {
                        elements.add(new AtomicProposition(t));
                    }
                }
                f = f.substitute(trans, FormulaCreator.bigWedgeOrVeeObject(elements, false));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the transitions. (Should not happen).", ex);
            }
        }

        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            try {
                // replace the places within the flow formula accordingly (the transitions can be replaced for the whole formula and is done later)
                // NOT ANYMORE, do it later because of the next: and replace the flow formula by the ltl formula with G(initfl> 0)\vee LTL-Part-Of-FlowFormula
//                IFormula flowF = ((FlowFormula) flowFormulas.get(0)).getPhi();
                FlowFormula flowF = flowFormulas.get(0);
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    AtomicProposition p = new AtomicProposition(place);
                    AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID));
                    flowF = (FlowFormula) flowF.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                }

                // Replace the next operator within the flow formula
                flowF = replaceNextInFlowFormulaParallel(orig, net, flowF);

                f = f.substitute(flowFormulas.get(0), new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.INIT_TOKENFLOW_ID))),
                        LTLOperators.Binary.OR,
                        flowF.getPhi()));
                return convert(f);
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
        } else {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
            return convert(f);
        }
    }

}

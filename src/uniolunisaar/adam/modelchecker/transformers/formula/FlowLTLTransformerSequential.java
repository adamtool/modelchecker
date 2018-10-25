package uniolunisaar.adam.modelchecker.transformers.formula;

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
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTL;
import static uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTL.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTL.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTL.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerSequential extends FlowLTLTransformer {

    private static FlowFormula replaceNextInFlowFormulaSequential(PetriGame orig, PetriNet net, FlowFormula flowFormula, int nb_ff) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowFormula(replaceNextWithinFlowFormulaSequential(orig, net, phi, nb_ff));
    }

    private static ILTLFormula replaceNextWithinFlowFormulaSequential(PetriGame orig, PetriNet net, ILTLFormula phi, int nb_ff) {
        if (phi instanceof AtomicProposition) {
            return phi;
        } else if (phi instanceof LTLFormula) {
            return new LTLFormula(replaceNextWithinFlowFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), nb_ff));
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // since we are in the scope of a FlowFormula
            ILTLFormula substChildPhi = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi(), nb_ff);
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // all transitions apart from those which are dependent of my act place (but the transition which just moves the token to the next formula)
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    Place act = net.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                    if (!act.getPostset().contains(t) || t.getId().equals(t.getLabel() + PetriNetTransformerFlowLTL.NEXT_ID)) {
                        elements.add(new AtomicProposition(t));
                    }
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // all transitions which are dependent on my act place (but the transition which just moves the token to the next formula)
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    Place act = net.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                    if (act.getPostset().contains(t) && !t.getId().equals(t.getLabel() + PetriNetTransformerFlowLTL.NEXT_ID)) {
                        elements.add(new AtomicProposition(t));
                    }
                }
                LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
            }
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> castPhi = (FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula>) phi;
            ILTLFormula subst1 = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi1(), nb_ff);
            ILTLFormula subst2 = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi2(), nb_ff);
            return new LTLFormula(subst1, castPhi.getOp(), subst2);
        }
        throw new RuntimeException("The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    private static IFormula replaceNextWithinRunFormulaSequential(PetriGame orig, PetriNet net, IFormula phi) {
        if (phi instanceof AtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunFormula) {
            return new RunFormula(replaceNextWithinRunFormulaSequential(orig, net, ((RunFormula) phi).getPhi()));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormulaSequential(orig, net, ((LTLFormula) phi).getPhi());
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked
            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormulaSequential(orig, net, castPhi.getPhi()); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                Collection<ILTLFormula> outer = new ArrayList<>();
                for (Place p : orig.getPlaces()) {
                    // all not original transitions and those which not succeed this place
                    Collection<ILTLFormula> elements = new ArrayList<>();
                    for (Transition t : net.getTransitions()) {
                        if (!orig.containsTransition(t.getId()) || !p.getPostset().contains(t)) {
                            elements.add(new AtomicProposition(t));
                        }
                    }

                    ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                    elements = new ArrayList<>();
//                     all transitions which are original
//                    for (Transition t : orig.getTransitions()) {
                    // only those which are succeeding the chosen place
                    for (Transition t : p.getPostset()) {
                        elements.add(new AtomicProposition(t));
                    }
                    LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
                    LTLFormula secondConjunct = new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
                    outer.add(new LTLFormula(new AtomicProposition(p), LTLOperators.Binary.AND, secondConjunct));
                }
                return FormulaCreator.bigWedgeOrVeeObject(outer, false);
            }
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            IFormula subst1 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi1());
            IFormula subst2 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi2());
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
     * @param initFirst
     * @return
     * @throws uniolunisaar.adam.modelchecker.exceptions.NotConvertableException
     */
    public static ILTLFormula createFormula4ModelChecking4CircuitSequential(PetriGame orig, PetriNet net, IRunFormula formula, boolean initFirst) throws NotConvertableException {
        // replace the next operator in the run-part
        IFormula f = replaceNextWithinRunFormulaSequential(orig, net, formula);

        List<AtomicProposition> allInitTransitions = new ArrayList<>();
        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowFormula flowFormula = flowFormulas.get(i);
            try {
                // replace the transitions and places within the flow formula accordingly                 
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    if (net.containsNode(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i)) {
                        AtomicProposition p = new AtomicProposition(place);
                        AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i));
                        flowFormula = (FlowFormula) flowFormula.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                    }
                }
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
            // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
            // Replace the transitions with the big-or of all accordingly labelled transitions
            for (Transition transition : orig.getTransitions()) {
                try {
                    AtomicProposition trans = new AtomicProposition(transition);
                    Place act = net.getPlace(ACTIVATION_PREFIX_ID + transition.getId() + TOKENFLOW_SUFFIX_ID + "-" + i);
                    Collection<ILTLFormula> elements = new ArrayList<>();
                    for (Transition t : act.getPostset()) {
                        elements.add(new AtomicProposition(t));
                    }
                    flowFormula = (FlowFormula) flowFormula.substitute(trans, FormulaCreator.bigWedgeOrVeeObject(elements, false));
                } catch (NotSubstitutableException ex) {
                    throw new RuntimeException("Cannot substitute the transitions. (Should not happen).", ex);
                }
            }

            // Replace the next operator within the flow formula
            flowFormula = replaceNextInFlowFormulaSequential(orig, net, flowFormula, i);

            try {
                LTLFormula flowLTL = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.NO_CHAIN_ID + "-" + i))), // it's OK when there is no chain
                        LTLOperators.Binary.OR,
                        flowFormula.getPhi());
                if (net.containsPlace(PetriNetTransformerFlowLTL.NEW_TOKENFLOW_ID + "-" + i)) {
                    // it's also OK if when I chosed to have a new chain but this run doesn't get to it
                    flowLTL = new LTLFormula(flowLTL,
                            LTLOperators.Binary.OR,
                            new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.NEW_TOKENFLOW_ID + "-" + i))));
                }

                f = f.substitute(flowFormulas.get(i), new RunFormula(flowLTL));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the flow formula. (Should not happen).", ex);
            }
            // add all init transitions
            Place init = net.getPlace(INIT_TOKENFLOW_ID + "-" + i);
            for (Transition t : init.getPostset()) {
                allInitTransitions.add(new AtomicProposition(t));
            }
        }

        ILTLFormula ret = convert(f);
        // in the beginning all init transitions are allowed to fire until the original formula has to hold
        return new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(allInitTransitions, false), LTLOperators.Binary.U, ret);
    }

}

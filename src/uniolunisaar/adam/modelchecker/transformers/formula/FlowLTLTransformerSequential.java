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
import uniolunisaar.adam.logic.flowltl.Constants;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.FormulaBinary;
import uniolunisaar.adam.logic.flowltl.FormulaUnary;
import uniolunisaar.adam.logic.flowltl.IAtomicProposition;
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
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTLSequential;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerSequential extends FlowLTLTransformer {

    private static FlowFormula replaceNextInFlowFormulaSequential(PetriGame orig, PetriNet net, FlowFormula flowFormula, int nb_ff) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowFormula(replaceNextWithinFlowFormulaSequential(orig, net, phi, nb_ff, false));
    }

    private static ILTLFormula replaceNextWithinFlowFormulaSequential(PetriGame orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually) {
        if (phi instanceof IAtomicProposition) {
            return phi;
        } else if (phi instanceof LTLFormula) {
            return new LTLFormula(replaceNextWithinFlowFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), nb_ff, scopeEventually));
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // since we are in the scope of a FlowFormula
            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }
            ILTLFormula substChildPhi = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi(), nb_ff, scopeEventually);
            LTLFormula substPhi = new LTLFormula(castPhi.getOp(), substChildPhi);
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
                // just delete the next, meaning just return the child
                if (scopeEventually) {
                    return substChildPhi;
                }
                // if it's not in the last scope of an eventually, then replace it according to the document
                // all transitions apart from those which are dependent of some of my act place (but the transition which just moves the token to the next formula)
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    String actid = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                    if (net.containsPlace(actid)) {
                        Place act = net.getPlace(actid);
                        if (!act.getPostset().contains(t) || t.getId().equals(t.getLabel() + PetriNetTransformerFlowLTLSequential.NEXT_ID + "-" + nb_ff)) { // todo: dependent on the sequential approach
                            elements.add(new AtomicProposition(t));
                        }
                    } else {// if it is not dependent on the act place it could still be the initial transitions 
                        Place act = net.getPlace(ACTIVATION_PREFIX_ID + INIT_TOKENFLOW_ID + "-" + nb_ff);
                        if (!act.getPostset().contains(t)) {
                            elements.add(new AtomicProposition(t));
                        }
                    }
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // all transitions which are dependent on my act place (but the transition which just moves the token to the next formula)
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    String actid = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                    if (net.containsPlace(actid)) {
                        Place act = net.getPlace(actid);
                        if (act.getPostset().contains(t) && !t.getId().equals(t.getLabel() + PetriNetTransformerFlowLTLSequential.NEXT_ID + "-" + nb_ff)) {// todo: dependent on the sequential approach
                            elements.add(new AtomicProposition(t));
                        }
                    } // don't want to add the initial transitions since the have to be happend before (for not initial we have to change it here!)
                }
                ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                LTLFormula untilSecond = new LTLFormula(myTransitions, LTLOperators.Binary.AND, substPhi);
                LTLFormula until = new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond);
                LTLFormula secondDisjunct = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.NEG, myTransitions)
                        ),
                        LTLOperators.Binary.AND,
                        substChildPhi
                );
                return new LTLFormula(until, LTLOperators.Binary.OR, secondDisjunct);
            }
            return substPhi;
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> castPhi = (FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula>) phi;
            ILTLFormula subst1 = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi1(), nb_ff, scopeEventually);
            ILTLFormula subst2 = replaceNextWithinFlowFormulaSequential(orig, net, castPhi.getPhi2(), nb_ff, scopeEventually);
            return new LTLFormula(subst1, castPhi.getOp(), subst2);
        }
        throw new RuntimeException("The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    private static FlowFormula replaceTransitionsInFlowFormulaSequential(PetriGame orig, PetriNet net, FlowFormula flowFormula, int nb_ff, boolean initFirst) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowFormula(replaceTransitionsWithinFlowFormulaSequential(orig, net, phi, nb_ff, false, initFirst));
    }

    private static ILTLFormula replaceTransitionsWithinFlowFormulaSequential(PetriGame orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually, boolean initFirst) {
        if (phi instanceof Constants) {
            return phi;
        } else if (phi instanceof AtomicProposition) {
            AtomicProposition atom = (AtomicProposition) phi;
            if (atom.isTransition()) {
                // if it's in the direct scope of an eventually we don't need the until
                if (scopeEventually) {
                    return phi;
                }
                // All other transitions then those belonging to nb_ff, apart from the next transitions
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) { // all transitions
                    if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                            || // not of the subformula
                            t.getId().endsWith(PetriNetTransformerFlowLTLSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
                            ) {
                        elements.add(new AtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // all of my transitions which have the same label as the atomic proposition and are not the next transition
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                            && // the transitions of my subnet
                            !t.getId().endsWith(PetriNetTransformerFlowLTLSequential.NEXT_ID + "-" + nb_ff)
                            &&// which are not the nxt transitions
                            t.getLabel().equals(atom.toString())) { // with the same label as the atom
                        elements.add(new AtomicProposition(t));
                    }
                }
                ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                return new LTLFormula(untilFirst, LTLOperators.Binary.U, myTransitions);
            }
            return phi;
        } else if (phi instanceof LTLFormula) {
            return new LTLFormula(replaceTransitionsWithinFlowFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), nb_ff, scopeEventually, initFirst));
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // since we are in the scope of a FlowFormula
            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }
            ILTLFormula substChildPhi = replaceTransitionsWithinFlowFormulaSequential(orig, net, castPhi.getPhi(), nb_ff, scopeEventually, initFirst);
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> castPhi = (FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula>) phi;
            ILTLFormula subst1 = replaceTransitionsWithinFlowFormulaSequential(orig, net, castPhi.getPhi1(), nb_ff, scopeEventually, initFirst);
            ILTLFormula subst2 = replaceTransitionsWithinFlowFormulaSequential(orig, net, castPhi.getPhi2(), nb_ff, scopeEventually, initFirst);
            return new LTLFormula(subst1, castPhi.getOp(), subst2);
        }

        throw new RuntimeException(
                "The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    private static IFormula replaceNextWithinRunFormulaSequential(PetriGame orig, PetriNet net, IFormula phi, boolean scopeEventually) {
        if (phi instanceof IAtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunFormula) {
            return new RunFormula(replaceNextWithinRunFormulaSequential(orig, net, ((RunFormula) phi).getPhi(), scopeEventually));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), scopeEventually);
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked

            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }

            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormulaSequential(orig, net, castPhi.getPhi(), scopeEventually); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            LTLFormula substPhi = new LTLFormula(castPhi.getOp(), substChildPhi);
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
                // just delete the next, meaning just return the child
                if (scopeEventually) {
                    return substChildPhi;
                }
                // if it's not in the last scope of an eventually, then replace it according to the document
                // all transitions of the subnet
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId())) {
                        elements.add(new AtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                // all original transitions
                elements = new ArrayList<>();
                for (Transition t : orig.getTransitions()) {
                    elements.add(new AtomicProposition(t));
                }
                ILTLFormula origTrans = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                LTLFormula untilSecond = new LTLFormula(origTrans, LTLOperators.Binary.AND, substPhi);
                LTLFormula until = new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond);
                LTLFormula secondDisjunct = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.NEG, origTrans)
                        ),
                        LTLOperators.Binary.AND,
                        substChildPhi
                );
                return new LTLFormula(until, LTLOperators.Binary.OR, secondDisjunct);
// part could have interesting ideas for the concurrency semantics 
//                Collection<ILTLFormula> outer = new ArrayList<>();
//                for (Place p : orig.getPlaces()) {
//                    // all not original transitions and those which not succeed this place
//                    Collection<ILTLFormula> elements = new ArrayList<>();
//                    for (Transition t : net.getTransitions()) {
//                        if (!orig.containsTransition(t.getId()) || !p.getPostset().contains(t)) {
//                            elements.add(new AtomicProposition(t));
//                        }
//                    }
//
//                    ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//                    elements = new ArrayList<>();
////                     all transitions which are original
////                    for (Transition t : orig.getTransitions()) {
//                    // only those which are succeeding the chosen place
//                    for (Transition t : p.getPostset()) {
//                        elements.add(new AtomicProposition(t));
//                    }
//                    LTLFormula untilSecond = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND, castPhi.getPhi());
//                    LTLFormula secondConjunct = new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
//                    outer.add(new LTLFormula(new AtomicProposition(p), LTLOperators.Binary.AND, secondConjunct));
//                }
//                return FormulaCreator.bigWedgeOrVeeObject(outer, false);
            }
            return substPhi;
        } else if (phi instanceof FormulaBinary) {
            IFormula subst1 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi1(), scopeEventually);
            IFormula subst2 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi2(), scopeEventually);
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

    private static RunFormula replaceNextWithinRunFormulaSequential(PetriGame orig, PetriNet net, RunFormula phi) {
        return new RunFormula(replaceNextWithinRunFormulaSequential(orig, net, phi.getPhi(), false));
    }

    private static IFormula replaceTransitionsWithinRunFormulaSequential(PetriGame orig, PetriNet net, IFormula phi, boolean scopeEventually) {
        if (phi instanceof Constants) {
            return phi;
        } else if (phi instanceof AtomicProposition) {
            AtomicProposition atom = (AtomicProposition) phi;
            if (atom.isTransition()) {
                // if it's in the direct scope of an eventually we don't need the until
                if (scopeEventually) {
                    return phi;
                }
                // if it's not in the last scope of an eventually, then replace it according to the document
                // all transitions of the subnets
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId())) {
                        elements.add(new AtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                return new LTLFormula(untilFirst, LTLOperators.Binary.U, atom);
            }
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunFormula) {
            return new RunFormula(replaceTransitionsWithinRunFormulaSequential(orig, net, ((RunFormula) phi).getPhi(), scopeEventually));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceTransitionsWithinRunFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), scopeEventually);
            return new LTLFormula((ILTLFormula) f);
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked

            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }

            ILTLFormula substChildPhi = (ILTLFormula) replaceTransitionsWithinRunFormulaSequential(orig, net, castPhi.getPhi(), scopeEventually); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            IFormula subst1 = replaceTransitionsWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi1(), scopeEventually);
            IFormula subst2 = replaceTransitionsWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi2(), scopeEventually);
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

    private static RunFormula replaceTransitionsWithinRunFormulaSequential(PetriGame orig, PetriNet net, RunFormula phi) {
        return new RunFormula(replaceTransitionsWithinRunFormulaSequential(orig, net, phi.getPhi(), false));
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
    public static ILTLFormula createFormula4ModelChecking4CircuitSequential(PetriGame orig, PetriNet net, RunFormula formula, boolean initFirst) throws NotConvertableException {
        // replace the transitions
        RunFormula runF = replaceTransitionsWithinRunFormulaSequential(orig, net, formula);
        // replace the next operator in the run-part
        IFormula f = replaceNextWithinRunFormulaSequential(orig, net, runF);

//        List<AtomicProposition> allInitTransitions = new ArrayList<>();
//        List<LTLFormula> allInitPlaces = new ArrayList<>();
        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowFormula flowFormula = flowFormulas.get(i);
            try {
                // replace the places within the flow formula accordingly                 
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    if (net.containsNode(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i)) { // only if the place is part of the subnet
                        AtomicProposition p = new AtomicProposition(place);
                        AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i));
                        flowFormula = (FlowFormula) flowFormula.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                    }
                }
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
            // Replace the transitions
            flowFormula = replaceTransitionsInFlowFormulaSequential(orig, net, flowFormula, i, initFirst);
            // Replace the next operator within the flow formula
            flowFormula = replaceNextInFlowFormulaSequential(orig, net, flowFormula, i);

            try {
                // this is replaced by the stuff below since the no_chain case is subsumed by the new_tokenflow
//                LTLFormula flowLTL = new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.NO_CHAIN_ID + "-" + i))), // it's OK when there is no chain
//                        LTLOperators.Binary.OR,
//                        flowFormula.getPhi());
//                if (net.containsPlace(PetriNetTransformerFlowLTL.NEW_TOKENFLOW_ID + "-" + i)) {
//                    // it's also OK if when I chosed to have a new chain but this run doesn't get to it
//                    flowLTL = new LTLFormula(flowLTL,
//                            LTLOperators.Binary.OR,
//                            new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.NEW_TOKENFLOW_ID + "-" + i))));
//                }
                LTLFormula skipping = null;
                if (initFirst) {
                    // it's OK when there is no chain or the subformula decided to consider a newly created chain but this doesn't exists in this run.
                    skipping = new LTLFormula(LTLOperators.Unary.G,
                            new LTLFormula(new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.NEW_TOKENFLOW_ID + "-" + i)),
                                    LTLOperators.Binary.OR,
                                    new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.INIT_TOKENFLOW_ID + "-" + i))
                            )
                    );
                } else {
                    // it's OK to consider no chain
                    skipping = new LTLFormula(LTLOperators.Unary.G,
                            new AtomicProposition(net.getPlace(PetriNetTransformerFlowLTL.INIT_TOKENFLOW_ID + "-" + i))
                    );
                }

                LTLFormula flowLTL = new LTLFormula(
                        skipping,
                        LTLOperators.Binary.OR,
                        flowFormula.getPhi());

                f = f.substitute(flowFormulas.get(i), new RunFormula(flowLTL));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the flow formula. (Should not happen).", ex);
            }
// don't need it anymore since it is done with all the other act places
//            if (initFirst) {
//                Place init = net.getPlace(INIT_TOKENFLOW_ID + "-" + i);
//                // add the init place
//                allInitPlaces.add(new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(init)));
//// to expensive we use the next  
////                // add all init transitions
////                for (Transition t : init.getPostset()) {
////                    allInitTransitions.add(new AtomicProposition(t));
////                }
//            }
        }

        ILTLFormula ret = convert(f);

        if (initFirst) {
            // don't need the forcing of the firing since it is later done with the activ places
            // in the beginning all init transitions are allowed to fire until the original formula has to hold
// to expensive better use the next            
//            ILTLFormula init = new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(allInitPlaces, true));
//            return new LTLFormula(init, LTLOperators.Binary.IMP, new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(allInitTransitions, false), LTLOperators.Binary.U, ret));
            ILTLFormula next = ret;
            for (int i = 0; i < flowFormulas.size(); i++) {
                next = new LTLFormula(LTLOperators.Unary.X, next);
            }
//            return new LTLFormula(init, LTLOperators.Binary.IMP, next);
        }

        // since we don't want to stop within the subnets, omit these runs
        // this means we demand for every activation place of the subnets
        // that have to be left
        List<LTLFormula> elements = new ArrayList<>();
        for (Place p : net.getPlaces()) {
            String id = p.getId();
            if (id.startsWith(ACTIVATION_PREFIX_ID)) {
                // this is the version when every original transition has its own activation token
                // if it's not a orignal one (meaning the rest of the string is not a transition of the original net
//                if (!orig.containsTransition(id.substring(ACTIVATION_PREFIX_ID.length()))) {
                if (!id.equals(ACTIVATION_PREFIX_ID + "orig")) { // not the activation place of the original transitions
                    LTLFormula inf = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(p))));
                    elements.add(inf);
                }
            }
        }
        ret = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, true), LTLOperators.Binary.IMP, ret);

        return ret;
    }

}

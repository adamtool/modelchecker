package uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.AtomicProposition;
import uniolunisaar.adam.ds.logics.Constants;
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
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings.Stucking;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequential;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerSequentialBackup extends FlowLTLTransformer {

    private static FlowLTLFormula replaceNextInFlowFormulaSequential(PetriNet orig, PetriNet net, FlowLTLFormula flowFormula, int nb_ff) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowLTLFormula(replaceNextWithinFlowFormulaSequential(orig, net, phi, nb_ff, false));
    }

    private static ILTLFormula replaceNextWithinFlowFormulaSequential(PetriNet orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually) {
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
                // All other transitions then those belonging to nb_ff, apart from the next transitions
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) { // all transitions
                    if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                            || // not of the subformula
                            t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
                            ) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // All of the transitions belonging to nb_ff, apart from the next transitions
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) { // all transitions
                    if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff)) //  of the subformula
                            && !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // not the nxt transitions
                            ) {
                        elements.add(new LTLAtomicProposition(t));
                    }
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

    private static FlowLTLFormula replaceTransitionsInFlowFormulaSequential(PetriNet orig, PetriNet net, FlowLTLFormula flowFormula, int nb_ff, boolean initFirst) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowLTLFormula(replaceTransitionsWithinFlowFormulaSequential(orig, net, phi, nb_ff, false, initFirst));
    }

    private static ILTLFormula replaceTransitionsWithinFlowFormulaSequential(PetriNet orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually, boolean initFirst) {
        if (phi instanceof Constants) {
            return phi;
        } else if (phi instanceof AtomicProposition) {
            AtomicProposition atom = (AtomicProposition) phi;
            if (atom.isTransition()) {
                // if it's in the direct scope of an eventually we don't need the until
                if (scopeEventually) { //todo: error here, have to be my trnasitions (the same for next)
                    return phi;
                }
                // All other transitions then those belonging to nb_ff, apart from the next transitions
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) { // all transitions
                    if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                            || // not of the subformula
                            t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
                            ) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // all of my transitions which have the same label as the atomic proposition and are not the next transition
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                            && // the transitions of my subnet
                            !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff)
                            &&// which are not the nxt transitions
                            t.getLabel().equals(atom.toString())) { // with the same label as the atom
                        elements.add(new LTLAtomicProposition(t));
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

    private static IFormula replaceNextWithinRunFormulaSequential(PetriNet orig, PetriNet net, IFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        if (phi instanceof IAtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunLTLFormula) {
            return new RunLTLFormula(replaceNextWithinRunFormulaSequential(orig, net, ((RunLTLFormula) phi).getPhi(), scopeEventually, nbFlowFormulas));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), scopeEventually, nbFlowFormulas);
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked

            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }

            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormulaSequential(orig, net, castPhi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            LTLFormula substPhi = new LTLFormula(castPhi.getOp(), substChildPhi);
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
                // just delete the next, meaning just return the child
                if (scopeEventually) {
                    return substChildPhi;
                }
                if (nbFlowFormulas > 0) {
                    ILTLFormula retPhi = substChildPhi;
                    for (int i = 0; i < nbFlowFormulas; i++) {
                        retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
                    }
                    return retPhi;
                } else {
                    // if it's not in the last scope of an eventually, then replace it according to the document
                    // all transitions of the subnet
                    Collection<ILTLFormula> elements = new ArrayList<>();
                    for (Transition t : net.getTransitions()) {
                        if (!orig.containsTransition(t.getId())) {
                            elements.add(new LTLAtomicProposition(t));
                        }
                    }
                    ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                    // all original transitions
                    elements = new ArrayList<>();
                    for (Transition t : orig.getTransitions()) {
                        elements.add(new LTLAtomicProposition(t));
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
            }
            return substPhi;
        } else if (phi instanceof FormulaBinary<?, ?, ?>) {
            IFormula subst1 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi1(), scopeEventually, nbFlowFormulas);
            IFormula subst2 = replaceNextWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi2(), scopeEventually, nbFlowFormulas);
            IOperatorBinary<?, ?> op = ((FormulaBinary<?, ?, ?>) phi).getOp();
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

    private static RunLTLFormula replaceNextWithinRunFormulaSequential(PetriNet orig, PetriNet net, RunLTLFormula phi, int nbFlowFormulas) {
        return new RunLTLFormula(replaceNextWithinRunFormulaSequential(orig, net, phi.getPhi(), false, nbFlowFormulas));
    }

    private static IFormula replaceTransitionsWithinRunFormulaSequential(PetriNet orig, PetriNet net, IFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        if (phi instanceof Constants) {
            return phi;
        } else if (phi instanceof LTLAtomicProposition) {
            LTLAtomicProposition atom = (LTLAtomicProposition) phi;
            if (atom.isTransition()) {
                // if it's in the direct scope of an eventually we don't need the until
                if (scopeEventually) {
                    return phi;
                }
                if (nbFlowFormulas > 0) {
                    ILTLFormula retPhi = atom;
                    for (int i = 0; i < nbFlowFormulas; i++) {
                        retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
                    }
                    return retPhi;
                } else {
                    // if it's not in the last scope of an eventually, then replace it according to the document
                    // all transitions of the subnets
                    Collection<ILTLFormula> elements = new ArrayList<>();
                    for (Transition t : net.getTransitions()) {
                        if (!orig.containsTransition(t.getId())) {
                            elements.add(new LTLAtomicProposition(t));
                        }
                    }
                    ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                    return new LTLFormula(untilFirst, LTLOperators.Binary.U, atom);
                }
            }
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunLTLFormula) {
            return new RunLTLFormula(replaceTransitionsWithinRunFormulaSequential(orig, net, ((RunLTLFormula) phi).getPhi(), scopeEventually, nbFlowFormulas));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceTransitionsWithinRunFormulaSequential(orig, net, ((LTLFormula) phi).getPhi(), scopeEventually, nbFlowFormulas);
            return new LTLFormula((ILTLFormula) f);
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked

            // check if it's in the scope of an eventually
            if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
                scopeEventually = true;
            } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
                scopeEventually = false;
            }

            ILTLFormula substChildPhi = (ILTLFormula) replaceTransitionsWithinRunFormulaSequential(orig, net, castPhi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary<?, ?, ?>) {
            IFormula subst1 = replaceTransitionsWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi1(), scopeEventually, nbFlowFormulas);
            IFormula subst2 = replaceTransitionsWithinRunFormulaSequential(orig, net, ((FormulaBinary) phi).getPhi2(), scopeEventually, nbFlowFormulas);
            IOperatorBinary<?, ?> op = ((FormulaBinary<?, ?, ?>) phi).getOp();
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

    private static RunLTLFormula replaceTransitionsWithinRunFormulaSequential(PetriNet orig, PetriNet net, RunLTLFormula phi, int nbFlowFormulas) {
        return new RunLTLFormula(replaceTransitionsWithinRunFormulaSequential(orig, net, phi.getPhi(), false, nbFlowFormulas));
    }

    /**
     *
     * @param orig
     * @param net
     * @param formula
     * @param settings
     * @return
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     */
    public static ILTLFormula createFormula4ModelChecking4CircuitSequential(PetriNet orig, PetriNet net, RunLTLFormula formula, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
//        System.out.println("before "+formula.toString());
        boolean initFirst = settings.isInitFirst();
        boolean useNext = settings.isUseNextToReplaceXandTransitionsInRunPart();
        int nbFlowFormulas = useNext ? LogicsTools.getFlowFormulas(formula).size()
                : // todo: maybe expensive, could make this smarter
                -1;
        // %%%%%%%%%%%%%%%%%  RUN REPLACE TRANSITIONS
        RunLTLFormula runF = replaceTransitionsWithinRunFormulaSequential(orig, net, formula, nbFlowFormulas);
        // %%%%%%%%%%%%%%%%%  RUN REPLACE NEXT 
        IFormula f = replaceNextWithinRunFormulaSequential(orig, net, runF, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%% FLOW PART
//        List<AtomicProposition> allInitTransitions = new ArrayList<>();
//        List<LTLFormula> allInitPlaces = new ArrayList<>();
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            // %%%%%%%%%%%%%%%%%%% FLOW REPLACE PLACES
            FlowLTLFormula flowFormula = flowFormulas.get(i);
            try {
                // replace the places within the flow formula accordingly                 
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    if (net.containsNode(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i)) { // only if the place is part of the subnet
                        LTLAtomicProposition p = new LTLAtomicProposition(place);
                        LTLAtomicProposition psub = new LTLAtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i));
                        flowFormula = (FlowLTLFormula) flowFormula.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                    }
                }
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
            // %%%%%%%%%%%%% FLOW REPLACE TRANSITIONS
            flowFormula = replaceTransitionsInFlowFormulaSequential(orig, net, flowFormula, i, initFirst);
            // %%%%%%%%%%%%% FLOW REPLACE NEXT
            flowFormula = replaceNextInFlowFormulaSequential(orig, net, flowFormula, i);

            // %%%%%%%%%%%%%% REPLACEMENT OF FLOW FORMULA
            // %%%%%%%% NEWLY CREATED CHAINS CASE
            try {
                LTLAtomicProposition init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID + "-" + i));
                LTLAtomicProposition newChains = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i));
                // this is replaced by the stuff below since the no_chain case is subsumed by the new_tokenflow
                //                LTLFormula flowLTL = new LTLFormula(
                //                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NO_CHAIN_ID + "-" + i))), // it's OK when there is no chain
                //                        LTLOperators.Binary.OR,
                //                        flowFormula.getPhi());
                //                if (net.containsPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i)) {
                //                    // it's also OK if when I chosed to have a new chain but this run doesn't get to it
                //                    flowLTL = new LTLFormula(flowLTL,
                //                            LTLOperators.Binary.OR,
                //                            new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i))));
                //                }
                // is the first operator an eventually? Then do nothing.
                boolean eventually = false;
                if (flowFormulas.get(i).getPhi() instanceof LTLFormula) {
                    ILTLFormula phi = ((LTLFormula) flowFormulas.get(i).getPhi()).getPhi();
                    if (phi instanceof FormulaUnary && (((FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi).getOp() == LTLOperators.Unary.F)) {
                        eventually = true;
                    }
                }
                ILTLFormula newlyCreatedChains = null;
                if (!eventually) { // the whole flowformula is not in the scope of an eventually, we have to skip as long as the new chain is created
                    // all transition starting a flow
                    if (!net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i).getPostset().isEmpty()) { // only if new chains are created during the game
                        if (settings.isNewChainsBySkippingTransitions()) {
                            // %%%%%%%%%%%%%%% OLD AND MORE EXPENSIVE VERSION: we skip all other transition than those which newly starts a chain, only in the next state the formula must hold
                            List<ILTLFormula> elements = new ArrayList<>();
                            Place p = net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i);
                            for (Transition t : p.getPostset()) {
                                elements.add(new LTLAtomicProposition(t));
                            }
                            List<ILTLFormula> others = new ArrayList<>();
                            // All other transitions then those belonging to i, apart from the next transitions
                            for (Transition t : net.getTransitions()) { // all transitions
                                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(i))
                                        || // not of the subformula
                                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + i) // or its one of the nxt transitions
                                        ) {
                                    others.add(new LTLAtomicProposition(t));
                                }
                            }
                            newlyCreatedChains = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(others, false), LTLOperators.Binary.U,
                                    new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND,
                                            new LTLFormula(LTLOperators.Unary.X, flowFormula.getPhi())));
                        } else {
                            // %%% %%%%%%%%%%% NEW VERSION: We skip as long as no transition creating a new chain has been token by this subnet
                            newlyCreatedChains = new LTLFormula(
                                    new LTLFormula(newChains, LTLOperators.Binary.OR, init),
                                    LTLOperators.Binary.U, flowFormula.getPhi());
                        }
                    }
                }

                // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NO CHAIN, OR WRONGLY DECIDED FOR NEW CHAIN
                LTLFormula skipping;
                if (initFirst) {
                    // it's OK when there is no chain or the subformula decided to consider a newly created chain but this doesn't exists in this run.
                    skipping = new LTLFormula(LTLOperators.Unary.G,
                            new LTLFormula(newChains, LTLOperators.Binary.OR, init));
                } else {
                    // it's OK to consider no chain
                    skipping = new LTLFormula(LTLOperators.Unary.G, init);
                }

                // %%%%% PUT TOGETHER FLOW FORMULA                
                ILTLFormula chainFormula = (newlyCreatedChains == null) ? flowFormula.getPhi() : newlyCreatedChains;
                LTLFormula flowLTL = new LTLFormula(
                        skipping,
                        LTLOperators.Binary.OR,
                        chainFormula);
                // %%%%% REPLACE THE FLOW FORMULA
                f = f.substitute(flowFormulas.get(i), new RunLTLFormula(flowLTL));

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

        ILTLFormula ret = LogicsTools.convert(f);

        // %%%%%%%%%%%%%%%% JUMP OVER INITIALIZATION
        if (initFirst) {
//            // don't need the forcing of the firing since it is later done with the activ places
//            // in the beginning all init transitions are allowed to fire until the original formula has to hold
//// to expensive better use the next            
////            ILTLFormula init = new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(allInitPlaces, true));
////            return new LTLFormula(init, LTLOperators.Binary.IMP, new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(allInitTransitions, false), LTLOperators.Binary.U, ret));
            for (int i = 0; i < flowFormulas.size(); i++) {
                ret = new LTLFormula(LTLOperators.Unary.X, ret);
            }
//            return new LTLFormula(init, LTLOperators.Binary.IMP, next);
        }

        // %%%%%%%%%%%%%%%%%%%%%%%  ACTIVATION PART
        // since we don't want to stop within the subnets, omit these runs
        // this is not necessary when we have MAX_IN_CIRCUIT
        if (settings.getMaximality() != AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT || settings.isNotStuckingAlsoByMaxInCircuit()) {
            if (settings.getStucking() != Stucking.GFo) {
                // %%%%% OLD VERSION: 
                // this means we demand for every activation place of the subnets
                // that it has to be left
                List<LTLFormula> elements = new ArrayList<>();
                for (Place p : net.getPlaces()) {
                    String id = p.getId();
                    if (id.startsWith(ACTIVATION_PREFIX_ID)) {
                        // this is the version when every original transition has its own activation token
                        // if it's not a orignal one (meaning the rest of the string is not a transition of the original net
//                if (!orig.containsTransition(id.substring(ACTIVATION_PREFIX_ID.length()))) {
                        // this is the version where there is only one activation token for all original transitions together
                        if (!id.equals(ACTIVATION_PREFIX_ID + "orig")) { // not the activation place of the original transitions
                            LTLFormula inf;
                            if (settings.getStucking() == Stucking.ANDGFNpi) {
                                inf = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p))));
                            } else {
                                inf = new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p));
                            }
                            elements.add(inf);
                        }
                    }
                }
                if (settings.getStucking() == Stucking.ANDGFNpi) {
                    ret = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, true), LTLOperators.Binary.IMP, ret);
                } else {
                    ILTLFormula and = FormulaCreator.bigWedgeOrVeeObject(elements, true);
                    if (settings.getStucking() == Stucking.GFANDNpiAndo) {
                        and = new LTLFormula(and, LTLOperators.Binary.AND, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")));

                    }
                    ret = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, and)), LTLOperators.Binary.IMP, ret);

                }
            } else {
                // %%%%% NEW VERSION:
                // smaller is to just asked that again and again the activation token of the original net has to be occupied
                // Also in the final setting this works since then it is an globally
                ret = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")))),
                        LTLOperators.Binary.IMP, ret);
            }
        }
//        System.out.println("after "+ret.toString());

        return ret;
    }

}

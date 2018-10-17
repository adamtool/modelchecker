package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.exceptions.NotSubstitutableException;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.Formula;
import uniolunisaar.adam.logic.flowltl.FormulaBinary;
import uniolunisaar.adam.logic.flowltl.FormulaUnary;
import uniolunisaar.adam.logic.flowltl.IFlowFormula;
import uniolunisaar.adam.logic.flowltl.IFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IOperatorBinary;
import uniolunisaar.adam.logic.flowltl.IOperatorUnary;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.FormulaCreator;
import static uniolunisaar.adam.modelchecker.transformers.PetriNetTransformer.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.modelchecker.transformers.PetriNetTransformer.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformer {

    public static String toMCHyperFormat(IOperatorUnary op) {
        if (op instanceof LTLOperators.Unary) {
            switch ((LTLOperators.Unary) op) {
                case F:
                    return "F";
                case G:
                    return "G";
                case NEG:
                    return "Neg";
                case X:
                    return "X";
            }
        }
        // todo throw exception but should never happen
        return "error";

    }

    public static String toMCHyperFormat(IOperatorBinary op) {
        if (op instanceof LTLOperators.Binary) {
            switch ((LTLOperators.Binary) op) {
                case AND:
                    return "And";
                case OR:
                    return "Or";
                case IMP:
                    return "Implies";
                case BIMP:
                    return "Eq";
                case U:
                    return "Until";
                case W:
                    return "WUntil";
                case R:
                    return "Release";
            }
            // todo throw exception but should never happen
            return "error";
        } else if (op instanceof RunOperators.Binary) {
            if (op == RunOperators.Binary.AND) {
                return "And";
            } else {
                return "Or";
            }
        } else if (op instanceof RunOperators.Implication) {
            return "Implies";
        } else {
            // todo throw exception but should never happen
            return "error";
        }
    }

    private static String subformulaToMCHyperFormat(IFormula formula) {
        if (formula instanceof AtomicProposition) {
            return "(AP \"#out#_" + ((AtomicProposition) formula).toString() + "\" 0)";
        } else if (formula instanceof Formula) {
            return subformulaToMCHyperFormat(((Formula) formula).getPhi());
        } else if (formula instanceof FormulaUnary) {
            FormulaUnary f = (FormulaUnary) formula;
            return "(" + toMCHyperFormat(f.getOp()) + " " + subformulaToMCHyperFormat(f.getPhi()) + ")";
        } else if (formula instanceof FormulaBinary) {
            FormulaBinary f = (FormulaBinary) formula;
            return "(" + toMCHyperFormat(f.getOp()) + " " + subformulaToMCHyperFormat(f.getPhi1()) + " " + subformulaToMCHyperFormat(f.getPhi2()) + ")";
        } else {
            // todo throw exception but should not happen.
            return "error";
        }
    }

    public static String toMCHyperFormat(IFormula formula) {
        return "Forall " + subformulaToMCHyperFormat(formula);
    }

    /**
     * The replacement of atomic propositions is not that nice and fault
     * tolerant.
     *
     * @param net
     * @param formula
     * @return
     * @throws ParseException
     * @deprecated
     */
    @Deprecated
    public static String toMCHyperFormat(PetriNet net, String formula) throws ParseException {
        // convert to prefix
        formula = FlowLTLParser.parse(net, formula).toPrefixString();
//        System.out.println(formula);
        formula = formula.replace("IMP(", "(Implies ");
        formula = formula.replace("AND(", "(And ");
        formula = formula.replace("OR(", "(Or ");
        formula = formula.replace("NEG(", "(Neg ");
        formula = formula.replace("G(", "(G ");
        formula = formula.replace("F(", "(F ");
        formula = formula.replace(",", " ");

        for (Place p : net.getPlaces()) {
            formula = formula.replace(" " + p.getId() + " ", "@#" + p.getId() + "#@ "); //todo; make it better with regular expressions
            formula = formula.replace(" " + p.getId() + ")", "@#" + p.getId() + "#@)");
        }
        for (Transition t : net.getTransitions()) {
            formula = formula.replace(" " + t.getId() + " ", "@#" + t.getId() + "#@ ");//todo; make it better with regular expressions
            formula = formula.replace(" " + t.getId() + ")", "@#" + t.getId() + "#@)");
        }
        for (Place p : net.getPlaces()) {
            formula = formula.replace("@#" + p.getId() + "#@", "(AP \"#out#_" + p.getId() + "\" 0)");
        }
        for (Transition t : net.getTransitions()) {
            formula = formula.replace("@#" + t.getId() + "#@", "(AP \"#out#_" + t.getId() + "\" 0)");
        }

        formula = "Forall " + formula;
        return formula;
    }

    public static List<FlowFormula> getFlowFormulas(IFormula formula) {
        List<FlowFormula> flowFormulas = new ArrayList<>();
        if (formula instanceof FlowFormula) {
            flowFormulas.add((FlowFormula) formula);
            return flowFormulas;
        } else if (formula instanceof ILTLFormula) {
            return flowFormulas;
        } else if (formula instanceof RunFormula) {
            return getFlowFormulas(((RunFormula) formula).getPhi());
        } else if (formula instanceof FormulaBinary) {
            FormulaBinary binF = (FormulaBinary) formula;
            flowFormulas.addAll(getFlowFormulas(binF.getPhi1()));
            flowFormulas.addAll(getFlowFormulas(binF.getPhi2()));
        }
        return flowFormulas;
    }

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
                    if (!act.getPostset().contains(t) || t.getId().equals(t.getLabel() + PetriNetTransformer.NEXT_ID)) {
                        elements.add(new AtomicProposition(t));
                    }
                }

                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

                // all transitions which are dependent on my act place (but the transition which just moves the token to the next formula)
                elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    Place act = net.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                    if (act.getPostset().contains(t) && !t.getId().equals(t.getLabel() + PetriNetTransformer.NEXT_ID)) {
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
                            String id = "t_" + place.getId() + TOKENFLOW_SUFFIX_ID + TOKENFLOW_SUFFIX_ID + "_new";
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

    private static IFormula replaceNextWithinRunFormula(PetriGame orig, PetriNet net, IFormula phi) {
        if (phi instanceof AtomicProposition) {
            return phi;
        } else if (phi instanceof IFlowFormula) {
            return phi;
        } else if (phi instanceof RunFormula) {
            return new RunFormula(replaceNextWithinRunFormula(orig, net, ((RunFormula) phi).getPhi()));
        } else if (phi instanceof LTLFormula) {
            IFormula f = replaceNextWithinRunFormula(orig, net, ((LTLFormula) phi).getPhi());
            return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked
            ILTLFormula substChildPhi = (ILTLFormula) replaceNextWithinRunFormula(orig, net, castPhi.getPhi()); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            if (castPhi.getOp() == LTLOperators.Unary.X) {
                // all not original transitions
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId())) {
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
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond));
            }
            return new LTLFormula(castPhi.getOp(), substChildPhi);
        } else if (phi instanceof FormulaBinary) {
            IFormula subst1 = replaceNextWithinRunFormula(orig, net, ((FormulaBinary) phi).getPhi1());
            IFormula subst2 = replaceNextWithinRunFormula(orig, net, ((FormulaBinary) phi).getPhi2());
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

    public static IRunFormula addFairness(PetriGame net, IRunFormula formula) {
        // Add Fairness to the formula
        ILTLFormula fairness = null;
        for (Transition t : net.getTransitions()) {
            if (net.isStrongFair(t)) {
                if (fairness == null) {
                    fairness = FormulaCreator.createStrongFairness(t);
                } else {
                    fairness = new LTLFormula(fairness, LTLOperators.Binary.AND, FormulaCreator.createStrongFairness(t));
                }
            }
            if (net.isWeakFair(t)) {
                if (fairness == null) {
                    fairness = FormulaCreator.createWeakFairness(t);
                } else {
                    fairness = new LTLFormula(fairness, LTLOperators.Binary.AND, FormulaCreator.createWeakFairness(t));
                }
            }
        }
        return (fairness != null) ? new RunFormula(fairness, RunOperators.Implication.IMP, formula) : formula;
    }

    /**
     * This is only done for ONE flow formula
     *
     * @param orig
     * @param net
     * @param formula
     * @return
     */
    public static IRunFormula createFormula4ModelChecking4CircuitSequential(PetriGame orig, PetriNet net, IRunFormula formula) {
        // Add the fairness from the transitions to the formula
        IFormula f = addFairness(orig, formula);

        // replace the next operator in the run-part
        f = replaceNextWithinRunFormula(orig, net, f);

        List<FlowFormula> flowFormulas = getFlowFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowFormula flowFormula = flowFormulas.get(i);
            try {
                // replace the transitions and places within the flow formula accordingly                 
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    AtomicProposition p = new AtomicProposition(place);
                    AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + i));
                    flowFormula = (FlowFormula) flowFormula.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
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

                f = f.substitute(flowFormulas.get(i), new RunFormula(
                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformer.INIT_TOKENFLOW_ID + "-" + i))),
                        LTLOperators.Binary.OR,
                        flowFormula.getPhi()));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
        }

        return new RunFormula(f);
    }

    /**
     * This is only done for ONE flow formula
     *
     * @param orig
     * @param net
     * @param formula
     * @return
     */
    public static IRunFormula createFormula4ModelChecking4CircuitParallel(PetriGame orig, PetriNet net, IRunFormula formula) {
        // Add the fairness from the transitions to the formula
        IFormula f = addFairness(orig, formula);

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

        List<FlowFormula> flowFormulas = getFlowFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            try {
                // replace the places within the flow formula accordingly (the transitions can be replaced for the whole formula and is done later)
                // NOT ANYMORE, do it later because of the next: and replace the flow formula by the ltl formula with G(initfl> 0)\vee LTL-Part-Of-FlowFormula
//                IFormula flowF = ((FlowFormula) flowFormulas.get(0)).getPhi();
                FlowFormula flowF = ((FlowFormula) flowFormulas.get(0));
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    AtomicProposition p = new AtomicProposition(place);
                    AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID));
                    flowF = (FlowFormula) flowF.substitute(p, psub); // no cast error since the substitution of propositions should preserve the types of the formula
                }

                // Replace the next operator within the flow formula
                flowF = replaceNextInFlowFormulaParallel(orig, net, flowF);

                f = f.substitute(flowFormulas.get(0), new RunFormula(
                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformer.INIT_TOKENFLOW_ID))),
                        LTLOperators.Binary.OR,
                        flowF.getPhi()));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
        } else {
            Logger.getInstance().addMessage("[WARNING] There is no flow formula within '" + formula.toString() + "'. The normal net model checker should be used.", false);
        }

        return new RunFormula(f);
    }

    public static String createFormula4ModelChecking4LoLA(PetriGame game, String formula) {
        for (Place p : game.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf");
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }

}

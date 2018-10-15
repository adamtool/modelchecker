package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
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

    private static List<FlowFormula> getFlowFormulas(IFormula formula) {
        List<FlowFormula> flowFormulas = new ArrayList<>();
        if (formula instanceof FlowFormula) {
            flowFormulas.add((FlowFormula) formula);
            return flowFormulas;
        } else if (formula instanceof ILTLFormula) {
            return flowFormulas;
        } else if (formula instanceof FormulaBinary) {
            FormulaBinary binF = (FormulaBinary) formula;
            flowFormulas.addAll(getFlowFormulas(binF.getPhi1()));
            flowFormulas.addAll(getFlowFormulas(binF.getPhi2()));
        }
        return flowFormulas;
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
        // Add Fairness to the formula
        ILTLFormula fairness = null;
        for (Transition t : orig.getTransitions()) {
            if (orig.isStrongFair(t)) {
                if (fairness == null) {
                    fairness = FormulaCreator.createStrongFairness(t);
                } else {
                    fairness = new LTLFormula(fairness, LTLOperators.Binary.AND, FormulaCreator.createStrongFairness(t));
                }
            }
            if (orig.isWeakFair(t)) {
                if (fairness == null) {
                    fairness = FormulaCreator.createWeakFairness(t);
                } else {
                    fairness = new LTLFormula(fairness, LTLOperators.Binary.AND, FormulaCreator.createWeakFairness(t));
                }
            }
        }
        IFormula f = (fairness != null)
                ? new RunFormula(fairness, RunOperators.Implication.IMP, formula) : formula;

        List<FlowFormula> flowFormulas = getFlowFormulas(f);
        if (flowFormulas.size() > 1) {
            throw new RuntimeException("Not yet implemented for more than one token flow formula. You gave me " + flowFormulas.size() + ": " + flowFormulas.toString());
        }
        if (flowFormulas.size() == 1) {
            try {
                // replace the places within the flow formula accordingly (the transitions can be replaced for the whole formula and is done later)
                // and replace the flow formula by the ltl formula with G(initfl> 0)\vee LTL-Part-Of-FlowFormula
                IFormula flowF = ((FlowFormula) flowFormulas.get(0)).getPhi();
                // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
                // Replace the place with the ones belonging to the guessing of the chain
                for (Place place : orig.getPlaces()) {
                    AtomicProposition p = new AtomicProposition(place);
                    AtomicProposition psub = new AtomicProposition(net.getPlace(place.getId() + "_tfl"));
                    flowF = flowF.substitute(p, psub);
                }
                f = f.substitute(flowFormulas.get(0), new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(net.getPlace(PetriNetTransformer.INIT_TOKENFLOW_ID))),
                        LTLOperators.Binary.OR,
                        (ILTLFormula) flowF)); // no cast error since ((FlowFormula) flowFormulas.get(0)).getPhi(); returns a ILTLFormula and the substitutions of an ILTLFormula with 
                // Atomic propositions yields an ILTLFormula
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the places. (Should not happen).", ex);
            }
        }
        // todo:  the replacements are expensive, think of going recursivly through the formula and replace it there accordingly
        // Replace the transitions with the big-or of all accordingly labelled transitions
        for (Transition transition : orig.getTransitions()) {
            try {
                AtomicProposition trans = new AtomicProposition(transition);
                ILTLFormula disjunct = trans;
                for (Transition t : net.getTransitions()) {
                    if (t.getLabel().equals(transition.getId())) {
                        disjunct = new LTLFormula(disjunct, LTLOperators.Binary.OR, new AtomicProposition(t));
                    }
                }
                f = f.substitute(trans, disjunct);
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the transitions. (Should not happen).", ex);
            }
        }

        // Replace the next operator
        return new RunFormula(f);
    }

    public static String createFormula4ModelChecking4LoLA(PetriGame game, String formula) {
        for (Place p : game.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf");
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }
}

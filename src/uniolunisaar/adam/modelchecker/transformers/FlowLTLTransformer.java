package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.Collection;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
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

    public static ILTLFormula getFairness(PetriGame net) {
        // Add Fairness to the formula
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            if (net.isStrongFair(t)) {
                elements.add(FormulaCreator.createStrongFairness(t));
            }
            if (net.isWeakFair(t)) {
                elements.add(FormulaCreator.createWeakFairness(t));
            }
        }
        return FormulaCreator.bigWedgeOrVeeObject(elements, true);
    }

    public static IRunFormula addFairness(PetriGame net, IRunFormula formula) {
        ILTLFormula fairness = getFairness(net);
        return (!fairness.toString().equals("TRUE")) ? new RunFormula(fairness, RunOperators.Implication.IMP, formula) : formula;
    }

    public static ILTLFormula addFairness(PetriGame net, ILTLFormula formula) {
        ILTLFormula fairness = getFairness(net);
        return (!fairness.toString().equals("TRUE")) ? new LTLFormula(fairness, LTLOperators.Binary.IMP, formula) : formula;
    }

    public static ILTLFormula handleStutteringOutGoingSemantics(PetriGame net, ILTLFormula formula) throws ParseException {
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            elements.add(new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(t)));
        }
        ILTLFormula allNotTrue = FormulaCreator.bigWedgeOrVeeObject(elements, true);
//        // does not work since we need simulatanous replacement
//        ILTLFormula f = formula;
//        // todo: expensive may go through the formula recursivly and only replace those which are existent
//        for (Transition t : net.getTransitions()) {
//            ILTLFormula tProp = new AtomicProposition(t);
//            f = (ILTLFormula) f.substitute(tProp, new LTLFormula(allNotTrue, LTLOperators.Binary.U, tProp));
//        }
        // todo: write method for simultanous replacements not on String replacement
        // replace all transitions with a buffer name
        String f = formula.toReplacableString();
        for (Transition t : net.getTransitions()) {
            f = f.replace("'" + t.getId() + "'", "'~#@" + t.getId() + "@#~'");
        }
        // now replace all step by step
        for (Transition t : net.getTransitions()) {
            ILTLFormula tProp = new AtomicProposition(t);
            f = f.replace("'~#@" + t.getId() + "@#~'", new LTLFormula(allNotTrue, LTLOperators.Binary.U, tProp).toString());
        }
        // clean up
        for (Place p : net.getPlaces()) {
            f = f.replace("'" + p.getId() + "'", p.getId());
        }
        return FlowLTLParser.parse(net, f).toLTLFormula();
    }

}

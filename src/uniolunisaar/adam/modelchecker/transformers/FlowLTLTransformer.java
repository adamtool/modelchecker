package uniolunisaar.adam.modelchecker.transformers;

import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.util.FormulaCreator;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformer {

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

}

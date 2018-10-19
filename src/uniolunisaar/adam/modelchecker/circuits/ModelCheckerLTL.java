package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.util.FormulaCreatorNxtSemantics;
import uniolunisaar.adam.logic.util.FormulaCreatorPrevSemantics;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLTL {

    public enum TransitionSemantics {
        PREV,
        NXT
    }

    public enum Maximality {
        REISIG,
        STANDARD,
        NONE
    }

    private TransitionSemantics semantics = TransitionSemantics.NXT;
    private Maximality maximality = Maximality.STANDARD;

    public ModelCheckerLTL() {
    }

    public ModelCheckerLTL(TransitionSemantics semantics, Maximality maximality) {
        this.semantics = semantics;
        this.maximality = maximality;
    }

    /**
     *
     * @param net
     * @param formula
     * @param path
     * @param verbose
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     */
    public CounterExample check(PetriGame net, ILTLFormula formula, String path, boolean verbose) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula + "'."
                + " With maximality term: " + maximality
                + " semantics: " + semantics, true);
        switch (maximality) {
            case STANDARD:
                if (semantics == TransitionSemantics.PREV) {
                    formula = new LTLFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                } else {
                    formula = new LTLFormula(FormulaCreatorNxtSemantics.getMaximaliltyStandardDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                }
                break;
            case REISIG:
                if (semantics == TransitionSemantics.PREV) {
                    formula = new LTLFormula(FormulaCreatorPrevSemantics.getMaximaliltyReisigDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                } else {
                    formula = new LTLFormula(FormulaCreatorNxtSemantics.getMaximaliltyReisigDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                }
                break;
        }

        formula = FlowLTLTransformer.addFairness(net, formula);
        return ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), path, (semantics == TransitionSemantics.PREV));
    }

    public TransitionSemantics getSemantics() {
        return semantics;
    }

    public void setSemantics(TransitionSemantics semantics) {
        this.semantics = semantics;
    }

    public Maximality getMaximality() {
        return maximality;
    }

    public void setMaximality(Maximality maximality) {
        this.maximality = maximality;
    }
}

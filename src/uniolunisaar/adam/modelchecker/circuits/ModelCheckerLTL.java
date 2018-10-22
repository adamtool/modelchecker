package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.exceptions.NotSubstitutableException;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.util.FormulaCreatorOutgoingSemantics;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLTL {

    public enum TransitionSemantics {
        INGOING,
        OUTGOING
    }

    public enum Maximality {
        MAX_PARALLEL,
        MAX_INTERLEAVING,
        MAX_NONE
    }

    private TransitionSemantics semantics = TransitionSemantics.OUTGOING;
    private Maximality maximality = Maximality.MAX_INTERLEAVING;

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
    public CounterExample check(PetriGame net, ILTLFormula formula, String path, boolean verbose) throws InterruptedException, IOException, NotSubstitutableException, ParseException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'."
                + " With maximality term: " + maximality
                + " semantics: " + semantics, true);
        formula = FlowLTLTransformer.addFairness(net, formula);
        switch (maximality) {
            case MAX_INTERLEAVING:
                if (semantics == TransitionSemantics.INGOING) {
                    formula = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximaliltyInterleavingDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                } else {
                    formula = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximaliltyInterleavingDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                    formula = FlowLTLTransformer.handleStutteringOutGoingSemantics(net, formula);
                }
                break;
            case MAX_PARALLEL:
                if (semantics == TransitionSemantics.INGOING) {
                    formula = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximaliltyParallelDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                } else {
                    formula = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximaliltyParallelDirectAsObject(net), LTLOperators.Binary.IMP, formula);
                    formula = FlowLTLTransformer.handleStutteringOutGoingSemantics(net, formula);
                }
                break;
            case MAX_NONE:
                if (semantics == TransitionSemantics.INGOING) {
                    // todo: when INGOING finished do the formula transformation here
                } else {
                    formula = FlowLTLTransformer.handleStutteringOutGoingSemantics(net, formula);
                }
                break;
        }
        Logger.getInstance().addMessage("This means we check F='" + formula.toSymbolString() + "'.");
        return ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), path, (semantics == TransitionSemantics.INGOING));
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

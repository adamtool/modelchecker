package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLTL {

    public enum Stuttering {
        REPLACEMENT,
        REPLACEMENT_REGISTER,
        PREFIX,
        PREFIX_REGISTER
    }

    public enum TransitionSemantics {
        INGOING,
        OUTGOING
    }

    public enum Maximality {
        MAX_CONCURRENT,
        MAX_INTERLEAVING,
        MAX_INTERLEAVING_IN_CIRCUIT,
        MAX_NONE
    }

    private TransitionSemantics semantics = TransitionSemantics.OUTGOING;
    private Maximality maximality = Maximality.MAX_INTERLEAVING;
    private Stuttering stuttering = Stuttering.PREFIX_REGISTER;

    public ModelCheckerLTL() {
    }

    public ModelCheckerLTL(TransitionSemantics semantics, Maximality maximality, Stuttering stuttering) {
        this.semantics = semantics;
        this.maximality = maximality;
        this.stuttering = stuttering;
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
     * @throws uniol.apt.io.parser.ParseException
     */
    public CounterExample check(PetriGame net, ILTLFormula formula, String path, boolean verbose) throws InterruptedException, IOException, ParseException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + maximality
                + " semantics: " + semantics
                + " stuttering: " + stuttering, true);

        // Add Fairness
        formula = FlowLTLTransformer.addFairness(net, formula);

        // Add Maximality
        ILTLFormula max = ModelCheckerTools.getMaximality(maximality, semantics, net);
        if (max != null) {
            formula = new LTLFormula(max, LTLOperators.Binary.IMP, formula);
        }

        // Choose renderer and add the corresponding stuttering
        AigerRenderer renderer;
        if (semantics == TransitionSemantics.INGOING) {
            // todo: do the stuttering here
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            formula = FlowLTLTransformer.handleStutteringOutGoingSemantics(net, formula, stuttering, maximality);
            if (maximality == Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER_MAX_INTERLEAVING);
            } else {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
            }
        }

        Logger.getInstance().addMessage("This means we check F='" + formula.toSymbolString() + "'.");
        return ModelCheckerMCHyper.check(net, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), path);
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

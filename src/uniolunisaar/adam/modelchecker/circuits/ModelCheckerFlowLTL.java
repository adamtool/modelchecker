package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreatorOutgoingSemantics;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerParallel;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerSequential;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerFlowLTLParallel;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerFlowLTLSequential;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowLTL {

    public enum TransitionSemantics {
        INGOING,
        OUTGOING
    }

    public enum Approach {
        PARALLEL,
        SEQUENTIAL
    }

    public enum Maximality {
        MAX_PARALLEL,
        MAX_INTERLEAVING,
        MAX_NONE
    }

    private TransitionSemantics semantics = TransitionSemantics.OUTGOING;
    private Approach approach = Approach.SEQUENTIAL;
    private Maximality maximality = Maximality.MAX_INTERLEAVING;

    public ModelCheckerFlowLTL() {
    }

    public ModelCheckerFlowLTL(TransitionSemantics semantics, Approach approach, Maximality maximality) {
        this.semantics = semantics;
        this.approach = approach;
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
    public CounterExample check(PetriGame net, IRunFormula formula, String path, boolean verbose) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'."
                + " With maximality term: " + maximality
                + " approach: " + approach + " semantics: " + semantics, true);
        AigerRenderer renderer = null;
        switch (maximality) {
            case MAX_INTERLEAVING:
                if (semantics == TransitionSemantics.INGOING) {
                    formula = new RunFormula(FormulaCreatorIngoingSemantics.getMaximaliltyInterleavingDirectAsObject(net), RunOperators.Implication.IMP, formula);
                    renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
                } else {
                    formula = new RunFormula(FormulaCreatorOutgoingSemantics.getMaximaliltyInterleavingDirectAsObject(net), RunOperators.Implication.IMP, formula);
                    renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
                }
                break;
            case MAX_PARALLEL:
                if (semantics == TransitionSemantics.INGOING) {
                    formula = new RunFormula(FormulaCreatorIngoingSemantics.getMaximaliltyConcurrentDirectAsObject(net), RunOperators.Implication.IMP, formula);
                    renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
                } else {
                    formula = new RunFormula(FormulaCreatorOutgoingSemantics.getMaximaliltyConcurrentDirectAsObject(net), RunOperators.Implication.IMP, formula);
                    renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
                }
                break;
        }
        if (ModelCheckerTools.getFlowFormulas(formula).isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula.toSymbolString() + "'. Thus, we use the standard model checking algorithm for LTL.");
            formula = FlowLTLTransformer.addFairness(net, formula);
            return ModelCheckerMCHyper.check(net, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), path);
        }
        if (approach == Approach.PARALLEL) {
            PetriGame gameMC = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
            if (verbose) {
                AdamTools.savePG2PDF(path + "_mc", gameMC, true);
            }
            IRunFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(net, gameMC, formula);
            Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC.toSymbolString() + "'.", false);
            return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), path + "_mc");
        } else {
            PetriGame gameMC = PetriNetTransformerFlowLTLSequential.createNet4ModelCheckingSequential(net, formula);
            if (verbose) {
                AdamTools.savePG2PDF(path + "_mc", gameMC, true);
            }
            IRunFormula formulaMC = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(net, gameMC, formula);
            Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC.toSymbolString() + "'.", false);
            return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), path + "_mc");
        }
    }

    public TransitionSemantics getSemantics() {
        return semantics;
    }

    public void setSemantics(TransitionSemantics semantics) {
        this.semantics = semantics;
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

    public Maximality getMaximality() {
        return maximality;
    }

    public void setMaximality(Maximality maximality) {
        this.maximality = maximality;
    }

    /**
     * Returns null iff the formula holds.
     *
     * This only works for formulas with at most one flow formula.
     *
     * It uses the MAX_PARALLEL approach (the first idea), where the flow and
     * the orignal net are succeed simulaneously.
     *
     * @param game
     * @param formula
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static CounterExample checkWithParallelApproach(PetriGame game, IRunFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        PetriGame gameMC = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(game);
        IRunFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        }
        return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

    /**
     * Returns null iff the formula holds.
     *
     *
     * It uses the SEQUENTIAL approach, where a token is passed to each sub net
     * representing a flow subformula
     *
     * @param game
     * @param formula
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static CounterExample checkWithSequentialApproach(PetriGame game, IRunFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        if (ModelCheckerTools.getFlowFormulas(formula).isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula + "'. Thus, we use the standard model checking algorithm for LTL.");
            formula = FlowLTLTransformer.addFairness(game, formula);
            AigerRenderer renderer;
            if (previousSemantics) {
                renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
            } else {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
            }
            return ModelCheckerMCHyper.check(game, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), "./" + game.getName());
        }
        PetriGame gameMC = PetriNetTransformerFlowLTLSequential.createNet4ModelCheckingSequential(game, formula);
        IRunFormula formulaMC = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        }
        return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

}

package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerParallel;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerSequential;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerParallel;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerSequential;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelChecker {

    /**
     * Returns null iff the formula holds.
     *
     * This only works for formulas with at most one flow formula.
     *
     * It uses the parallel approach (the first idea), where the flow and the
     * orignal net are succeed simulaneously.
     *
     * @param game
     * @param formula
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static CounterExample checkWithParallelApproach(PetriGame game, IRunFormula formula, String path) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        PetriGame gameMC = PetriNetTransformerParallel.createNet4ModelCheckingParallel(game);
        IRunFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        return ModelCheckerMCHyper.check(gameMC, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

    /**
     * Returns null iff the formula holds.
     *
     *
     * It uses the sequential approach, where a token is passed to each sub net
     * representing a flow subformula
     *
     * @param game
     * @param formula
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static CounterExample checkWithSequentialApproach(PetriGame game, IRunFormula formula, String path) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        if (ModelCheckerTools.getFlowFormulas(formula).isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula + "'. Thus, we use the standard model checking algorithm for LTL.");
            formula = FlowLTLTransformer.addFairness(game, formula);
            return ModelCheckerMCHyper.check(game, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), "./" + game.getName());
        }
        PetriGame gameMC = PetriNetTransformerSequential.createNet4ModelCheckingSequential(game, formula);
        IRunFormula formulaMC = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        return ModelCheckerMCHyper.check(gameMC, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

}

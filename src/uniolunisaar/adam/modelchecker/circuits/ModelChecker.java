package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformer;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelChecker {

    /**
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
    public static boolean checkWithParallelApproach(PetriGame game, IRunFormula formula, String path) throws InterruptedException, IOException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        PetriGame gameMC = PetriNetTransformer.createNet4ModelCheckingParallel(game);
        IRunFormula formulaMC = FlowLTLTransformer.createFormula4ModelChecking4CircuitParallel(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        return ModelCheckerMCHyper.check(gameMC, FlowLTLTransformer.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }
    
}

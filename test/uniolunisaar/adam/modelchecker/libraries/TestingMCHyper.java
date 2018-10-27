package uniolunisaar.adam.modelchecker.libraries;

import java.io.IOException;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.modelchecker.circuits.Circuit;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerMCHyper;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMCHyper {

    @Test
    void testFormulaParser() throws RenderException, InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        PetriGame game = new PetriGame("testing");
        Place init = game.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = game.createTransition("tB");
        game.createFlow(init, tstolen);
        Place out = game.createPlace("out");
        game.createFlow(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = game.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = game.createTransition("tC");
        game.createFlow(init3, t2);
        game.createFlow(t2, init3);

        AdamTools.savePG2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
//        String formula = "Forall (F (AP \"#out#_out\" 0))";
//        String formula = "Forall (Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall (Implies (X (AP \"#out#_tB\" 0)) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall (G (Implies (And (Neg (AP \"#out#_tB\", 0)) (Neg (AP \"#out#_tC\", 0))) (And (Neg (AP \"#out#_inittfl\", 0)) (Neg ((AP \"#out#_inittflB\", 0))))))";
//        String formula = "Forall (G (Implies (And (Neg (AP \"#out#_tB\", 0)) (Neg (AP \"#out#_tC\", 0))) (And (Neg (AP \"#out#_inittfl\", 0)) (Neg ((AP \"#out#_inittflB\", 0))))))";
//        String formula = "Forall ("+"Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall ((AP \"#out#_out\" 0))"; // correct
//        String formula = "Forall (AP \"#out#_out\" 0)"; // also correct
//        String formula = "Forall (And ((AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))"; // incorrect
//        String formula = "Forall (And (AP \"#out#_out\" 0) (AP \"#out#_out\" 0))"; // correct
//        String formula = "Forall (F (Eq (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))"; // correkt

        String formula = "Forall (Until (Or (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)) (Or (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))";

        ModelCheckerMCHyper.check(game, Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER), formula, "./" + game.getName());
    }

}

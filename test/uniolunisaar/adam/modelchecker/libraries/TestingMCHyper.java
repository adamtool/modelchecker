package uniolunisaar.adam.modelchecker.libraries;

import java.io.IOException;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.util.logics.transformers.logics.ModelCheckingOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.CircuitAndLTLtoCircuit;
import uniolunisaar.adam.logic.modelchecking.circuits.PetriNetModelChecker;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMCHyper {

    @Test
    void testFormulaParser() throws RenderException, InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("testing");
        Place init = net.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = net.createTransition("tB");
        net.createFlow(init, tstolen);
        Place out = net.createPlace("out");
        net.createFlow(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = net.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = net.createTransition("tC");
        net.createFlow(init3, t2);
        net.createFlow(t2, init3);

        PNWTTools.savePnwt2PDF(net.getName(), net, true);
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
        AigerRenderer renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        ModelCheckingOutputData data = new ModelCheckingOutputData("./" + net.getName(), false, false, false);

        CircuitAndLTLtoCircuit.createCircuit(net, renderer, formula, data, null);

        String inputFile = "./" + net.getName() + ".aig";
        PetriNetModelChecker.check(inputFile, VerificationAlgo.IC3, net, renderer, "./" + net.getName(), "");
    }

}

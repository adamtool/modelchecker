package uniolunisaar.adam.modelchecker.transformers;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.circuits.Circuit;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerMCHyper;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingFlowLTLTransformer {

    @Test
    public void testMCHyperTransformation() throws ParseException, InterruptedException, IOException {
        PetriGame net = new PetriGame("testing");
        Place init = net.createPlace("a");
        Place end = net.createPlace("b");
        Transition t = net.createTransition();
        net.createFlow(init, t);
//        String formula = "F (TRUE OR b)";
//        String formula = "F (b)";
        String formula = "F TRUE";
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(net, formula);
//        System.out.println(formula);
        CounterExample output = ModelCheckerMCHyper.check(net, Circuit.getRenderer(Circuit.Renderer.INGOING), formula, "./" + net.getName());
        Assert.assertNull(output);
    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ParseException {
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

        // test maximality
        // standard
        IRunFormula f = FormulaCreatorIngoingSemantics.getMaximaliltyStandardObject(game);
//        System.out.println("Maximality standard:");
//        System.out.println(f.toSymbolString());
        // reisig
        ILTLFormula f1 = FormulaCreatorIngoingSemantics.getMaximaliltyConcurrentDirectAsObject(game);
//        System.out.println("Maximality Reisig:");
//        System.out.println(f1.toSymbolString());
        // reisig
        f = FormulaCreatorIngoingSemantics.getMaximaliltyReisigObject(game);
//        System.out.println("Maximality Reisig:");
//        System.out.println(f.toSymbolString());

        // test to hyper ltl
        String formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(game, f.toString());
//        String formula = FlowLTLTransformer.toMCHyperFormat(f); // working
        Assert.assertEquals(formula, "Forall (And (G (F (Or (Neg (AP \"#out#_inittfl\" 0)) (X (AP \"#out#_tB\" 0))))) (G (F (Or (Neg (AP \"#out#_inittflB\" 0)) (X (AP \"#out#_tC\" 0))))))");

        CounterExample output = ModelCheckerMCHyper.check(game, Circuit.getRenderer(Circuit.Renderer.INGOING), formula, "./" + game.getName());
        Assert.assertNotNull(output);

        // new version
        String formula2 = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        Assert.assertEquals(formula.replace(" ", ""), formula2.replace(" ", ""));
    }
}

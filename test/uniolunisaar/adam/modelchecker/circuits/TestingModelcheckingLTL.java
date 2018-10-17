package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreator;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLTL {

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
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
        String formula = "Forall (G (AP \"#out#_inittflB\" 0))";
        CounterExample check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (AP \"#out#_inittfl\" 0)";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (F (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNotNull(check);
        formula = "Forall (Neg (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);

        ILTLFormula f = FormulaCreator.enabledObject(tstolen);
        formula = FlowLTLTransformer.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);

        f = new LTLFormula(LTLOperators.Unary.G, f);
        formula = FlowLTLTransformer.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNotNull(check);

        formula = "Forall (Neg (AP \"#out#_tB\" 0))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Neg (AP \"#out#_tC\" 0))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
//        formula = "Forall (Implies (AP \"#out#_tB\" 0) false)"; // syntactic 'false' ? don't now how to give it to MCHyper
//        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
//        Assert.assertFalse(check);

        formula = "Forall (Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Implies (AP \"#out#_tB\" 0) (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Implies (X (AP \"#out#_tB\" 0)) (F (AP \"#out#_out\" 0)))";
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);

        f = FormulaCreator.getMaximaliltyReisigDirectAsObject(game);
        ILTLFormula reachOut = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(out));
        f = new LTLFormula(f, LTLOperators.Binary.IMP, reachOut);
        formula = FlowLTLTransformer.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNull(check);

        RunFormula maxStandard = FormulaCreator.getMaximaliltyStandardObject(game);
        LTLFormula ftest = new LTLFormula((ILTLFormula) maxStandard.getPhi(), LTLOperators.Binary.IMP, reachOut);
        System.out.println(ftest.toSymbolString());
        formula = FlowLTLTransformer.toMCHyperFormat(ftest);
        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
        Assert.assertNotNull(check);
    }

    @Test(enabled = true)
    void testFirstExamplePaper() throws ParseException, IOException, InterruptedException, NotSupportedGameException {
        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        AdamTools.savePG2PDF(pn.getName(), new PetriGame(pn), false);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.F, new LTLFormula(new AtomicProposition(pn.getPlace("A")), LTLOperators.Binary.OR, new AtomicProposition(pn.getPlace("B"))));
        f = new LTLFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        CounterExample cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformer.toMCHyperFormat(f), "./" + pn.getName());
        Assert.assertNull(cex);

        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbad"))));
        f = new LTLFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformer.toMCHyperFormat(f), "./" + pn.getName());
        Assert.assertNotNull(cex);

        LTLFormula bothA = new LTLFormula(
                new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(pn.getPlace("A"))),
                LTLOperators.Binary.AND,
                new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(pn.getPlace("A_")))
        );
        LTLFormula bothB = new LTLFormula(
                new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(pn.getPlace("B"))),
                LTLOperators.Binary.AND,
                new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(pn.getPlace("B_")))
        );
        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbad"))));
        f = new LTLFormula(new LTLFormula(bothA, LTLOperators.Binary.OR, bothB), LTLOperators.Binary.IMP, f);
        f = new LTLFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformer.toMCHyperFormat(f), "./" + pn.getName());
        Assert.assertNull(cex);
    }

    @Test(enabled = true)
    public void testBurglar() throws ParseException, IOException, RenderException, InterruptedException, NotSupportedGameException {
        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "burglar.apt"));
        AdamTools.savePG2PDF(pn.getName(), new PetriGame(pn), false);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.G,
                new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbadA"))),
                        LTLOperators.Binary.AND,
                        new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbadB")))
                ));
        f = new LTLFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        CounterExample cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformer.toMCHyperFormat(f), "./" + pn.getName());
        Assert.assertNotNull(cex);
    }

    @Test
    void testMaximality() throws RenderException, InterruptedException, IOException, ParseException {
        PetriGame net = new PetriGame("testMaximalityA");
        Place A = net.createPlace("A");
        A.setInitialToken(1);
        Place A2 = net.createPlace("A2");
        Transition t = net.createTransition("tA");
        net.createFlow(A, t);
        net.createFlow(t, A2);

        Place B = net.createPlace("B");
        B.setInitialToken(1);
        Place B2 = net.createPlace("B2");
        Transition tB = net.createTransition("tB");
        net.createFlow(B, tB);
        net.createFlow(tB, B2);
        Transition tloop = net.createTransition("tl");
        net.createFlow(B, tloop);
        net.createFlow(tloop, B);
        AdamTools.savePG2PDF(net.getName(), new PetriGame(net), false);

        ILTLFormula maxReisig = FormulaCreator.getMaximaliltyReisigDirectAsObject(net);
        ILTLFormula maxStandard = FormulaCreator.getMaximaliltyStandardDirectAsObject(net);

        LTLFormula evA2 = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(A2));
        LTLFormula evB2 = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(B2));

        LTLFormula f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evA2);
        CounterExample cex = ModelCheckerMCHyper.check(net, FlowLTLTransformer.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evB2);
        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformer.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evA2);
        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformer.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evB2);
        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformer.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNotNull(cex);
    }
}

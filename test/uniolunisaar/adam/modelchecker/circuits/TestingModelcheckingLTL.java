package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRendererSafeOutStutterRegister;
import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.exceptions.NotSubstitutableException;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.Constants;
import uniolunisaar.adam.logic.flowltl.IAtomicProposition;
import uniolunisaar.adam.logic.flowltl.IFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreator;
import uniolunisaar.adam.logic.util.FormulaCreatorOutgoingSemantics;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.tools.ProcessNotStartedException;

import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLTL {

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @Test
    void testByJesko() throws RenderException, InterruptedException, IOException, ParseException, NotSupportedGameException, ProcessNotStartedException {
        PetriNet net = new PetriNet("jesko");
        Place init = net.createPlace("in");
        init.setInitialToken(1);
        Transition t1 = net.createTransition("t1");
        net.createFlow(init, t1);
        net.createFlow(t1, init);
        Transition t2 = net.createTransition("t2");
        net.createFlow(init, t2);
        net.createFlow(t2, init);

        AdamTools.savePG2PDF(net.getName(), new PetriGame(net), false);

        AigerRenderer renderer = Circuit.getRenderer(Circuit.Renderer.INGOING); // MCHyper should not directly be used anymore

        String formula = "Forall (G (AP \"#out#_in\" 0))";
        CounterExample check = ModelCheckerMCHyper.check(net, renderer, formula, "./" + net.getName());
        Assert.assertNull(check);

        formula = "Forall (G (AP \"#out#_t1\" 0))";
        check = ModelCheckerMCHyper.check(net, renderer, formula, "./" + net.getName());
        Assert.assertNotNull(check);

        formula = "Forall (AP \"#out#_in\" 0)";
        check = ModelCheckerMCHyper.check(net, renderer, formula, "./" + net.getName());
        Assert.assertNull(check);

        PetriNet doublediamond = new PetriNet("doublediamond");
        Place in = doublediamond.createPlace("in");
        in.setInitialToken(1);

        // first diamond
        Place L = doublediamond.createPlace("L");
        Transition l = doublediamond.createTransition("l");
        doublediamond.createFlow(in, l);
        doublediamond.createFlow(l, L);
        Place R = doublediamond.createPlace("R");
        Transition r = doublediamond.createTransition("r");
        doublediamond.createFlow(in, r);
        doublediamond.createFlow(r, R);
        Place M = doublediamond.createPlace("M");
        Transition ll = doublediamond.createTransition("ll");
        doublediamond.createFlow(L, ll);
        doublediamond.createFlow(ll, M);
        Transition rr = doublediamond.createTransition("rr");
        doublediamond.createFlow(R, rr);
        doublediamond.createFlow(rr, M);

        // second diamond
        Place LL = doublediamond.createPlace("LL");
        Transition lll = doublediamond.createTransition("lll");
        doublediamond.createFlow(M, lll);
        doublediamond.createFlow(lll, LL);
        Place RR = doublediamond.createPlace("RR");
        Transition rrr = doublediamond.createTransition("rrr");
        doublediamond.createFlow(M, rrr);
        doublediamond.createFlow(rrr, RR);
        Place MM = doublediamond.createPlace("MM");
        Transition llll = doublediamond.createTransition("llll");
        doublediamond.createFlow(LL, llll);
        doublediamond.createFlow(llll, MM);
        Transition rrrr = doublediamond.createTransition("rrrr");
        doublediamond.createFlow(RR, rrrr);
        doublediamond.createFlow(rrrr, MM);

        AdamTools.savePG2PDF(doublediamond.getName(), new PetriGame(doublediamond), false);

        // formula: in
        LTLFormula f = new LTLFormula(new AtomicProposition(doublediamond.getPlace("in")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = ModelCheckerMCHyper.check(doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNull(check);

        // F M
        f = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(doublediamond.getPlace("M")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = ModelCheckerMCHyper.check(doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNull(check);

        // F MM
        f = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(doublediamond.getPlace("MM")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = ModelCheckerMCHyper.check(doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNull(check);

        // Fairness
        f = new LTLFormula(FormulaCreator.createStrongFairness(l));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = ModelCheckerMCHyper.check(doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName());
        Assert.assertNull(check);
    }

    @Test
    void testOutgoingSemantics() throws InterruptedException, IOException, NotSubstitutableException, ParseException, ProcessNotStartedException {
        PetriGame game = new PetriGame("testNext");
        Place init = game.createPlace("pA");
        init.setInitialToken(1);
        Place init2 = game.createPlace("pA2");
        Place out = game.createPlace("pB");

        Transition t = game.createTransition("t");
        game.createFlow(init, t);
        game.createFlow(t, out);

//        Transition t2 = game.createTransition("t2");
//        game.createFlow(init2, t2);
//        game.createFlow(t2, init2);
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_NONE);
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);

        // initially the initial place
        ILTLFormula pA = new AtomicProposition(init);
        CounterExample cex = mc.check(game, pA, "./" + game.getName(), true);
        Assert.assertNull(cex);
        //// but not the other one
        // first test the stuttering formula         
//        IAtomicProposition initReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.INIT_LATCH);
        IAtomicProposition stutterReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.STUTT_LATCH);
        ILTLFormula stutt = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(stutterReg,
                LTLOperators.Binary.IMP,
                new LTLFormula(LTLOperators.Unary.G, stutterReg)))
        );
//        cex = mc.check(game, new LTLFormula(initReg, LTLOperators.Binary.IMP, stutt), "./" + game.getName(), true);
//        cex = mc.check(game, new LTLFormula(initReg, LTLOperators.Binary.IMP, stutterReg), "./" + game.getName(), true);
//        cex = mc.check(game, new Constants.False(), "./" + game.getName(), true);
        IFormula f = new Constants.False();
        cex = ModelCheckerMCHyper.check(game, Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER), FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + game.getName());
        Assert.assertNotNull(cex);
        f = new LTLFormula(LTLOperators.Unary.X, stutt);
        cex = ModelCheckerMCHyper.check(game, Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER), FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + game.getName());
        Assert.assertNotNull(cex);

        ILTLFormula pA2 = new AtomicProposition(init2);
        cex = mc.check(game, pA2, "./" + game.getName(), true);
        Assert.assertNotNull(cex);

        // not the transition is force to leave the state since we  can stay in the initial marking
        ILTLFormula propT = new AtomicProposition(t);
        cex = mc.check(game, propT, "./" + game.getName(), true);
        Assert.assertNotNull(cex);
        // but not when we demand maximality
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        cex = mc.check(game, propT, "./" + game.getName(), true);
        Assert.assertNull(cex);
        // Not all runs should be maximal
        cex = mc.check(game, new Constants.False(), "./" + game.getName(), true);
        Assert.assertNotNull(cex);
        AdamTools.savePG2PDF(game.getName(), game, true);
        // but not globally since the net is finite
        cex = mc.check(game, new LTLFormula(LTLOperators.Unary.G, propT), "./" + game.getName(), true);
        Assert.assertNotNull(cex);

    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ParseException, ProcessNotStartedException {
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

        AigerRenderer renderer = Circuit.getRenderer(Circuit.Renderer.INGOING); // MCHyper should not directly be used anymore
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
        String formula = "Forall (G (AP \"#out#_inittflB\" 0))";
        CounterExample check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (AP \"#out#_inittfl\" 0)";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (F (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNotNull(check);
        formula = "Forall (Neg (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);

        ILTLFormula f = FormulaCreator.enabledObject(tstolen);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);

        f = new LTLFormula(LTLOperators.Unary.G, f);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNotNull(check);

        formula = "Forall (Neg (AP \"#out#_tB\" 0))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Neg (AP \"#out#_tC\" 0))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
//        formula = "Forall (Implies (AP \"#out#_tB\" 0) false)"; // syntactic 'false' ? don't now how to give it to MCHyper
//        check = ModelCheckerMCHyper.check(game, formula, "./" + game.getName());
//        Assert.assertFalse(check);

        formula = "Forall (Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Implies (AP \"#out#_tB\" 0) (AP \"#out#_out\" 0))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);
        formula = "Forall (Implies (X (AP \"#out#_tB\" 0)) (F (AP \"#out#_out\" 0)))";
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);

        f = FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(game);
        ILTLFormula reachOut = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(out));
        f = new LTLFormula(f, LTLOperators.Binary.IMP, reachOut);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNull(check);

        RunFormula maxStandard = FormulaCreatorIngoingSemantics.getMaximalityInterleavingObject(game);
        LTLFormula ftest = new LTLFormula((ILTLFormula) maxStandard.getPhi(), LTLOperators.Binary.IMP, reachOut);
        System.out.println(ftest.toSymbolString());
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(ftest);
        check = ModelCheckerMCHyper.check(game, renderer, formula, "./" + game.getName());
        Assert.assertNotNull(check);
    }

    @Test
    void testStuttering() throws InterruptedException, IOException, NotSubstitutableException, ParseException {
        PetriGame game = new PetriGame("testStuttering");
        Place init = game.createPlace("a");
        init.setInitialToken(1);
        Transition tloop = game.createTransition("t");
        game.createFlow(init, tloop);
        game.createFlow(tloop, init);

        CounterExample cex;

        ModelCheckerLTL mc = new ModelCheckerLTL(ModelCheckerLTL.TransitionSemantics.OUTGOING, ModelCheckerLTL.Maximality.MAX_INTERLEAVING, ModelCheckerLTL.Stuttering.PREFIX_REGISTER);
        cex = mc.check(game, new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(tloop)), "./" + game.getName(), true);
        Assert.assertEquals(cex == null, true);
    }

    @Test(enabled = true)
    void testFirstExamplePaper() throws ParseException, IOException, InterruptedException, NotSupportedGameException, NotSubstitutableException {
        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        AdamTools.savePG2PDF(pn.getName(), new PetriGame(pn), false);

        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_NONE); // since it is done by hand
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.F, new LTLFormula(new AtomicProposition(pn.getPlace("A")), LTLOperators.Binary.OR, new AtomicProposition(pn.getPlace("B"))));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        CounterExample cex = mc.check(pn, f, "./" + pn.getName(), true);
        Assert.assertNull(cex);

        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbad"))));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        cex = mc.check(pn, f, "./" + pn.getName(), true);
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

        // test previous
        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        cex = mc.check(pn, maxf, "./" + pn.getName(), true);
        Assert.assertNull(cex);
        // test next
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        cex = mc.check(pn, maxf, "./" + pn.getName(), true);
        Assert.assertNull(cex);
    }

    @Test(enabled = true)
    public void testBurglar() throws ParseException, IOException, RenderException, InterruptedException, NotSupportedGameException, NotSubstitutableException {
        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "burglar.apt"));
        AdamTools.savePG2PDF(pn.getName(), new PetriGame(pn), false);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.G,
                new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbadA"))),
                        LTLOperators.Binary.AND,
                        new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(pn.getPlace("qbadB")))
                ));
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_NONE); // since it is done by hand
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        // test previous
        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        CounterExample cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), "./" + pn.getName(), true);
        CounterExample cex = mc.check(pn, maxf, "./" + pn.getName(), true);
        Assert.assertNotNull(cex);

        //test next
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        cex = ModelCheckerMCHyper.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), "./" + pn.getName(), false);
        cex = mc.check(pn, maxf, "./" + pn.getName(), false);
        Assert.assertNotNull(cex);
    }

    @Test
    void testMaximality() throws RenderException, InterruptedException, IOException, ParseException, NotSubstitutableException {
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

        // Check previous semantics
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_NONE); // since we do it by hand
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        ILTLFormula maxReisig = FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(net);
        ILTLFormula maxStandard = FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net);

        LTLFormula evA2 = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(A2));
        LTLFormula evB2 = new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(B2));

        LTLFormula f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evA2);

//        CounterExample cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), true);
        CounterExample cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evB2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), true); 
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evA2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), true);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evB2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), true);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);

        // Check next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        maxReisig = FormulaCreatorOutgoingSemantics.getMaximalityConcurrentDirectAsObject(net);
        maxStandard = FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(net);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evA2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evB2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evA2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(cex);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evB2);
//        cex = ModelCheckerMCHyper.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        cex = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(cex);
    }
}

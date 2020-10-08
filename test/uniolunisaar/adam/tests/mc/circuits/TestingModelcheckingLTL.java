package uniolunisaar.adam.tests.mc.circuits;

import uniolunisaar.adam.logic.modelchecking.ltl.circuits.PetriNetModelChecker;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerLTL;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import java.io.File;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeStutterRegister;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.PnmlPNParser;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings.TransitionSemantics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.Constants;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.FormulaCreatorOutgoingSemantics;
import uniolunisaar.adam.util.logics.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;
import uniolunisaar.adam.tools.Logger;

import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLTL {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ltl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @Test
    void testByJesko() throws RenderException, InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNet net = PNTools.createPetriNet("jesko");
        Place init = net.createPlace("in");
        init.setInitialToken(1);
        Transition t1 = net.createTransition("t1");
        net.createFlow(init, t1);
        net.createFlow(t1, init);
        Transition t2 = net.createTransition("t2");
        net.createFlow(init, t2);
        net.createFlow(t2, init);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), new PetriNetWithTransits(net), false);

        AigerRenderer renderer = new AigerRendererSafeStutterRegister(net, true, TransitionSemantics.OUTGOING); // MCHyper should not directly be used anymore

        String formula = "Forall (G (AP \"#out#_in\" 0))";
        LTLModelCheckingResult check = PetriNetModelChecker.check(VerificationAlgo.IC3, net, renderer, formula, outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        formula = "Forall (G (AP \"#out#_t1\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, net, renderer, formula, outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        formula = "Forall (AP \"#out#_in\" 0)";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, net, renderer, formula, outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        PetriNet doublediamond = PNTools.createPetriNet("doublediamond");

        renderer = new AigerRendererSafeStutterRegister(doublediamond, true, TransitionSemantics.OUTGOING); // the renderer are dependent of the net now 
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

        PNWTTools.savePnwt2PDF(outputDir + doublediamond.getName(), new PetriNetWithTransits(doublediamond), false);

        // formula: in
        LTLFormula f = new LTLFormula(new LTLAtomicProposition(doublediamond.getPlace("in")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // F M
        f = new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(doublediamond.getPlace("M")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // F MM
        f = new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(doublediamond.getPlace("MM")));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // Fairness
        f = new LTLFormula(FormulaCreator.createStrongFairness(l));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(doublediamond), LTLOperators.Binary.IMP, f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, doublediamond, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), outputDir + net.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    void testOutgoingSemantics() throws InterruptedException, IOException, NotSubstitutableException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits game = new PetriNetWithTransits("testNext");
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
        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + game.getName(), false, true);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(data);
        settings.setMaximality(Maximality.MAX_NONE);
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);

        // initially the initial place
        ILTLFormula pA = new LTLAtomicProposition(init);
        settings.setOutputData(data);
        LTLModelCheckingResult cex = mc.check(game, pA);
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        //// but not the other one
        // first test the stuttering formula         
//        IAtomicProposition initReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.INIT_LATCH);
        ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
        ILTLFormula stutt = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(stutterReg,
                LTLOperators.Binary.IMP,
                new LTLFormula(LTLOperators.Unary.G, stutterReg)))
        );
//        cex = mc.check(game, new LTLFormula(initReg, LTLOperators.Binary.IMP, stutt), data);
//        cex = mc.check(game, new LTLFormula(initReg, LTLOperators.Binary.IMP, stutterReg), data);
//        cex = mc.check(game, new Constants.False(), data);
        IFormula f = new Constants.False();
        cex = PetriNetModelChecker.check(VerificationAlgo.IC3, game, new AigerRendererSafeStutterRegister(game, true, TransitionSemantics.OUTGOING), FlowLTLTransformerHyperLTL.toMCHyperFormat(f), outputDir + game.getName(), "");
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        f = new LTLFormula(LTLOperators.Unary.X, stutt);
//        cex = PetriNetModelChecker.check(VerificationAlgo.IC3, game, Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER, game), FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + game.getName(), "");
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        ILTLFormula pA2 = new LTLAtomicProposition(init2);
        cex = mc.check(game, pA2);
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        // not the transition is force to leave the state since we  can stay in the initial marking
        ILTLFormula propT = new LTLAtomicProposition(t);
        cex = mc.check(game, propT);
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        // but not when we demand maximality
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        cex = mc.check(game, propT);
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        // Not all runs should be maximal
        cex = mc.check(game, new LTLConstants.False());
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        PNWTTools.savePnwt2PDF(outputDir + game.getName(), game, true);
        // but not globally since the net is finite
        cex = mc.check(game, new LTLFormula(LTLOperators.Unary.G, propT));
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits game = new PetriNetWithTransits("testing");
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

        PNWTTools.savePnwt2PDF(outputDir + game.getName(), game, true);

        AigerRenderer renderer = new AigerRendererSafeStutterRegister(game, true, TransitionSemantics.OUTGOING); // MCHyper should not directly be used anymore
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
        String formula = "Forall (G (AP \"#out#_inittflB\" 0))";
        LTLModelCheckingResult check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "Forall (AP \"#out#_inittfl\" 0)";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "Forall (F (AP \"#out#_out\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        formula = "Forall (Neg (AP \"#out#_out\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        ILTLFormula f = FormulaCreator.enabledObject(tstolen);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = new LTLFormula(LTLOperators.Unary.G, f);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        formula = "Forall (Neg (AP \"#out#_tB\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "Forall (Neg (AP \"#out#_tC\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
//        formula = "Forall (Implies (AP \"#out#_tB\" 0) false)"; // syntactic 'false' ? don't now how to give it to MCHyper
//        check = PetriNetModelChecker.check(game, formula, "./" + game.getName());
//        Assert.assertFalse(check);

        formula = "Forall (Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "Forall (Implies (AP \"#out#_tB\" 0) (AP \"#out#_out\" 0))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "Forall (Implies (X (AP \"#out#_tB\" 0)) (F (AP \"#out#_out\" 0)))";
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(game);
        ILTLFormula reachOut = new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(out));
        f = new LTLFormula(f, LTLOperators.Binary.IMP, reachOut);
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        RunLTLFormula maxStandard = FormulaCreatorIngoingSemantics.getMaximalityInterleavingObject(game);
        LTLFormula ftest = new LTLFormula((ILTLFormula) maxStandard.getPhi(), LTLOperators.Binary.IMP, reachOut);
//        System.out.println(ftest.toSymbolString());
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(ftest);
        check = PetriNetModelChecker.check(VerificationAlgo.IC3, game, renderer, formula, outputDir + game.getName(), "");
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    void testStuttering() throws InterruptedException, IOException, NotSubstitutableException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits game = new PetriNetWithTransits("testStuttering");
        Place init = game.createPlace("a");
        init.setInitialToken(1);
        Transition tloop = game.createTransition("t");
        game.createFlow(init, tloop);
        game.createFlow(tloop, init);

        LTLModelCheckingResult cex;

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + game.getName(), false, false);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(
                data,
                Maximality.MAX_INTERLEAVING,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        cex = mc.check(game, new LTLFormula(LTLOperators.Unary.G, new LTLAtomicProposition(tloop)));
        Assert.assertEquals(cex.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    void testFirstExamplePaper() throws ParseException, IOException, InterruptedException, NotSubstitutableException, ProcessNotStartedException, ExternalToolException {
        final String path = System.getProperty("examplesfolder") + "/synthesis/forallsafety/firstExamplePaper/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        PNWTTools.savePnwt2PDF(outputDir + pn.getName(), new PetriNetWithTransits(pn), false);

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), true, true);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(data);
        settings.setMaximality(Maximality.MAX_NONE); // since it is done by hand
        settings.setTransitionSemantics(TransitionSemantics.INGOING);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.F, new LTLFormula(new LTLAtomicProposition(pn.getPlace("A")), LTLOperators.Binary.OR, new LTLAtomicProposition(pn.getPlace("B"))));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);

        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        LTLModelCheckingResult check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbad"))));
        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        LTLFormula bothA = new LTLFormula(
                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("A"))),
                LTLOperators.Binary.AND,
                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("AA")))
        );
        LTLFormula bothB = new LTLFormula(
                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("B"))),
                LTLOperators.Binary.AND,
                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("BB")))
        );
        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbad"))));
        f = new LTLFormula(new LTLFormula(bothA, LTLOperators.Binary.OR, bothB), LTLOperators.Binary.IMP, f);

        // test previous
        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        check = mc.check(pn, maxf);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        // test next
        settings.setTransitionSemantics(TransitionSemantics.OUTGOING);
        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void testBurglar() throws ParseException, IOException, RenderException, InterruptedException, NotSubstitutableException, ExternalToolException, ProcessNotStartedException {
        final String path = System.getProperty("examplesfolder") + "/synthesis/forallsafety/burglar/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "burglar.apt"));
        PNWTTools.savePnwt2PDF(outputDir + pn.getName(), new PetriNetWithTransits(pn), false);

        LTLFormula f = new LTLFormula(LTLOperators.Unary.G,
                new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbadA"))),
                        LTLOperators.Binary.AND,
                        new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbadB")))
                ));

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), false, false);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(data);

        settings.setMaximality(Maximality.MAX_NONE); // since it is done by hand
        settings.setTransitionSemantics(TransitionSemantics.INGOING);

        // test previous
        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        CounterExample cex = PetriNetModelChecker.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), data);

        settings.setOutputData(data);
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        LTLModelCheckingResult check = mc.check(pn, maxf);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        //test next
        settings.setTransitionSemantics(TransitionSemantics.OUTGOING);
        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        cex = PetriNetModelChecker.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), "./" + pn.getName(), false);    
        check = mc.check(pn, maxf);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    void testMaximality() throws RenderException, InterruptedException, IOException, ParseException, NotSubstitutableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("testMaximalityA");
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
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), new PetriNetWithTransits(net), false);

        // Check previous semantics
        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + net.getName(), false, true);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(data);

        settings.setMaximality(Maximality.MAX_NONE); // since we do it by hand
        settings.setTransitionSemantics(TransitionSemantics.INGOING);
        ILTLFormula maxReisig = FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(net);
        ILTLFormula maxStandard = FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net);

        LTLFormula evA2 = new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(A2));
        LTLFormula evB2 = new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(B2));

        LTLFormula f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evA2);

//        CounterExample cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), data);
        settings.setOutputData(data);
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        LTLModelCheckingResult check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evB2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), data); 
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evA2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), data);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evB2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), data);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        // Check next semantics
        settings.setTransitionSemantics(TransitionSemantics.OUTGOING);
        maxReisig = FormulaCreatorOutgoingSemantics.getMaximalityConcurrentDirectAsObject(net);
        maxStandard = FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(net);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evA2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        f = new LTLFormula(maxStandard, LTLOperators.Binary.IMP, evB2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evA2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = new LTLFormula(maxReisig, LTLOperators.Binary.IMP, evB2);
//        cex = PetriNetModelChecker.check(net, FlowLTLTransformerHyperLTL.toMCHyperFormat(f), "./" + net.getName(), false);
        check = mc.check(net, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test(enabled = false)
    void testMcCASLink() throws ParseException, IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        String folder = System.getProperty("examplesfolder") + "/modelchecking/ltl/mcc/ASLink/PT/";
        String output = System.getProperty("testoutputfolder") + "/modelchecking/ltl/mcc/ASLink/PT/";
        PetriNet net = new PnmlPNParser().parseFile(folder + "aslink_01_a.pnml");
        PetriNetWithTransits game = new PetriNetWithTransits(net);
        game.setName("asLink01a");
        (new File(output)).mkdirs();

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(output + game.getName() + "_deadlock", false, false);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(
                data,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                VerificationAlgo.IC3
        );
        ILTLFormula deadlock = FormulaCreator.deadlock(net);
        settings.setOutputData(data);
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        LTLModelCheckingResult check = mc.check(game, deadlock);
        Tools.saveFile(output + game.getName() + "_deadlock.cex", (check == null) ? "not existend." : check.toString());

        ILTLFormula reversible = FormulaCreator.reversible(net);
        data = new AdamCircuitLTLMCOutputData(output + game.getName() + "_reversible", false, false);
        settings.setOutputData(data);
        check = mc.check(game, reversible);
        Tools.saveFile(output + game.getName() + "_reversible.cex", (check == null) ? "not existend." : check.toString());

        ILTLFormula quasiLive = FormulaCreator.quasiLive(net);
        data = new AdamCircuitLTLMCOutputData(output + game.getName() + "_quasiLive", false, false);
        settings.setOutputData(data);
        check = mc.check(game, quasiLive);
        Tools.saveFile(output + game.getName() + "_quasiLive.cex", (check == null) ? "not existend." : check.toString());

        ILTLFormula live = FormulaCreator.live(net);
        data = new AdamCircuitLTLMCOutputData(output + game.getName() + "_live", false, false);
        settings.setOutputData(data);
        check = mc.check(game, live);
        Tools.saveFile(output + game.getName() + "_live.cex", (check == null) ? "not existend." : check.toString());
    }

    @Test
    public void testOptimizations() throws InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNet net = PNTools.createPetriNet("jesko");
        Place init = net.createPlace("in");
        init.setInitialToken(1);
        Transition t1 = net.createTransition("t1");
        net.createFlow(init, t1);
        net.createFlow(t1, init);
        Transition t2 = net.createTransition("t2");
        net.createFlow(init, t2);
        net.createFlow(t2, init);

        String output = System.getProperty("testoutputfolder") + "/modelchecking/ltl/";
        (new File(output)).mkdirs();

        PNWTTools.savePnwt2PDF(output + net.getName(), new PetriNetWithTransits(net), false);

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(output + net.getName(), true, true);
        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings(
                data,
                Maximality.MAX_NONE,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.INGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                VerificationAlgo.IC3
        );
        ILTLFormula f = new LTLFormula(LTLOperators.Unary.G, new LTLAtomicProposition(init));

        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        LTLModelCheckingResult check = mc.check(new PetriNetWithTransits(net), f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        Tools.saveFile(output + net.getName() + ".cex", (check == null) ? "not existend." : check.toString());
    }
}

package uniolunisaar.adam.modelchecker.ctl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ltl.FlowCTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwtandflowctl2pn.PnwtAndFlowCTL2PN;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMCFlowCTLForAll {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/flowCTLforAll/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
    }

    static FlowCTLModelcheckingSettings settings;

    public static final boolean verbose = true;

    @BeforeMethod
    public void initMCSettings() {
        settings = new FlowCTLModelcheckingSettings(
                new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, verbose),
                ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                CircuitRendererSettings.TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                Abc.VerificationAlgo.IC3);
    }

    @Test
    public void testLateCreation() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        //RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, new CTLFormula(CTLOperators.Unary.EF, new CTLAtomicProposition(pnwt.getPlace("p")))));
        // the negation in positive normalform 
        // check: A EF p
        // ergo: A A false U_ !p
        // FALSE: since we can go the tq also already in the begining
        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(pnwt.getPlace("p"))))));
        check(pnwt, formula, settings, LTLModelCheckingResult.Satisfied.FALSE);

        // check: G !tq -> A EF p
        // ergo: G !tq -> A A false U_ !p
        // FALSE: since we have the run only firing ts
        LTLFormula neverAbove = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pnwt.getTransition("tq"))));
        formula = new RunCTLForAllFormula(neverAbove, RunOperators.Implication.IMP, formula);
        check(pnwt, formula, settings, LTLModelCheckingResult.Satisfied.FALSE);

        // check: G !tq -> A EF p
        // ergo: G !tq -> A A false U_ !p
        // FALSE: this is still false because we can first fire tp without a chain, and then tp would never be enabled anymore
        pnwt.setStrongFair(pnwt.getTransition("tp"));
        check(pnwt, formula, settings, LTLModelCheckingResult.Satisfied.FALSE);

    }

    @Test
    public void exampleLTLToolPaper() throws Exception {
        PetriNetWithTransits net = new PetriNetWithTransits("toolPaper");
        Place in = net.createPlace("in");
        in.setInitialToken(1);
        Place out = net.createPlace("out");
        out.setInitialToken(1);
        Transition init = net.createTransition("s");
        Transition t = net.createTransition("t");
        net.createFlow(init, in);
        net.createFlow(in, init);
        net.createFlow(in, t);
        net.createFlow(t, in);
        net.createFlow(t, out);
        net.createFlow(out, t);
        net.createTransit(out, t, out);
        net.createTransit(in, t, out);
        net.createTransit(in, init, in);
        net.createInitialTransit(init, in);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // check: A in
        // ergo: A !in
        RunCTLForAllFormula formula = new RunCTLForAllFormula(
                new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                        new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(in))));
        check(net, formula, settings, ModelCheckingResult.Satisfied.TRUE);
        // check: A EG in
        // ergo: A A true U !in
        // FALSE: since each run which contains t violates this
        CTLFormula ctl = new CTLFormula(new CTLConstants.True(), CTLOperators.Binary.AU, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(in)));
        formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, ctl));
        check(net, formula, settings, ModelCheckingResult.Satisfied.FALSE);

        // check: A ((EG in) OR EF(out))
        // ergo: A ((A true U !in) AND (A false U_ !out)) 
        // TRUE: since now only the run looping with s is possible
        CTLFormula ctl2 = new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(out)));
        formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, new CTLFormula(ctl, CTLOperators.Binary.AND, ctl2)));
        check(net, formula, settings, ModelCheckingResult.Satisfied.TRUE);

        // check: G!t -> A EG in
        // ergo: G!t -> A A true U !in
        // TRUE: since now only the run looping with s is possible
        formula = new RunCTLForAllFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(t))),
                RunOperators.Implication.IMP, new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, ctl)));
        check(net, formula, settings, ModelCheckingResult.Satisfied.TRUE);

        net.setWeakFair(t);

    }

    @Test(enabled=false) // this is not a test, but 'ant test' tries to call it
    public static void check(PetriNetWithTransits pnwt, RunCTLForAllFormula formula, FlowCTLModelcheckingSettings settings, LTLModelCheckingResult.Satisfied sat) throws Exception {
        PetriNetWithTransits out = new PnwtAndFlowCTL2PN().createSequential(pnwt, formula, settings);
        if (settings.getOutputData().isVerbose()) {
//            for (Transition transition : out.getTransitions()) {
//                System.out.println("Label: "+transition.getLabel());
//            }
            PNWTTools.savePnwt2PDF(settings.getOutputData().getPath() + out.getName(), out, true);
        }

        // collect all ids of fair transitions
        List<String> fairTransitions = new ArrayList<>();
        for (Transition transition : pnwt.getTransitions()) {
            if (pnwt.isStrongFair(transition) || pnwt.isWeakFair(transition)) { // to also collect weak is not right, but a quick hack
                fairTransitions.add(transition.getId());
            }
        }

        // for the general approach I have to do it here,
        // but for strong I don't need the replacement
//            ILTLFormula fairness = LogicsTools.getFairness(pnwt);
//            if (!fairness.toString().equals("TRUE")) { // todo: quick hack
//                formula = new RunCTLForAllFormula(fairness, RunOperators.Implication.IMP, formula);
//            }
        ILTLFormula ltlFormula = new FlowCTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(pnwt, out, formula, settings);

        // for the fast hack add now the fairness
        if (!fairTransitions.isEmpty()) {
            for (String fairTransition : fairTransitions) {
                out.setStrongFair(out.getTransition(fairTransition));
            }
            ILTLFormula fairness = LogicsTools.getFairness(out);
            if (!fairness.toString().equals("TRUE")) { // todo: quick hack
                ltlFormula = new LTLFormula(fairness, LTLOperators.Binary.IMP, ltlFormula);
            }
        }
        System.out.println("formula to check: " + ltlFormula.toSymbolString());
        AigerRenderer renderer = PnAndLTLtoCircuit.createCircuitWithoutFairnessAndMaximality(out, ltlFormula, settings);
        settings.fillAbcData(out);
        LTLModelCheckingResult result = Abc.call(settings, settings.getOutputData(), settings.getStatistics());
//        System.out.println(result.getSatisfied().toString());
        if (result.getCex() != null) {
            System.out.println(result.getCex().toString());
        }
        Assert.assertEquals(result.getSatisfied(), sat);
    }
}

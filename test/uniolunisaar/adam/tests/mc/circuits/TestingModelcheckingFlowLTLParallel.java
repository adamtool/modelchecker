package uniolunisaar.adam.tests.mc.circuits;

import java.io.File;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings.TransitionSemantics;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.modelchecking.cex.CounterExample.CounterExampleIterator;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallel;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tests.mc.util.TestModelCheckerTools;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLParallel {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static final String outDir = outputDir + "parallel/";

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
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

    @BeforeClass
    public void createFolder() {
        (new File(outDir)).mkdirs();
    }

    AdamCircuitFlowLTLMCSettings[] settings;

    @BeforeMethod
    public void initMCSettings() {
//        settings = { };
        settings = new AdamCircuitFlowLTLMCSettings[]{
            //                        TestModelCheckerTools.mcSettings_Seq_IntF, TestModelCheckerTools.mcSettings_Seq_IntC,
            //                        TestModelCheckerTools.mcSettings_SeqI_IntF, TestModelCheckerTools.mcSettings_SeqI_IntC,
            TestModelCheckerTools.mcSettings_Par_IntF, TestModelCheckerTools.mcSettings_Par_IntC,
            TestModelCheckerTools.mcSettings_ParI_IntF, TestModelCheckerTools.mcSettings_ParI_IntC
        //            TestModelCheckerTools.mcSettings_Par_N, TestModelCheckerTools.mcSettings_ParI_N
//            TestModelCheckerTools.mcSettings_ParI_IntC, //            TestModelCheckerTools.mcSettings_Seq_IntF
//                                    TestModelCheckerTools.mcSettings_SeqI_IntC, //            TestModelCheckerTools.mcSettings_Seq_IntF
        //              TestModelCheckerTools.mcSettings_Par_IntF,  TestModelCheckerTools.mcSettings_ParI_IntF
        };
    }

    @Test
    public void exampleToolPaper() throws Exception {
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
        net.setWeakFair(t);

        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);

        String formula = "A F out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName() + "data", false, false, true);

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                data,
                Approach.PARALLEL_INHIBITOR,
                Maximality.MAX_INTERLEAVING,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);

        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test
    public void testSeveralSubformulas() throws Exception {
        PetriNetWithTransits net = new PetriNetWithTransits("serveralSub");
        Place init = net.createPlace("init");
        init.setInitialToken(1);

        ModelCheckerFlowLTL mc;
        LTLModelCheckingResult ret;

        FlowLTLFormula flowTrue = new FlowLTLFormula(new LTLConstants.True());
        FlowLTLFormula flowFalse = new FlowLTLFormula(new LTLConstants.False());

        RunLTLFormula f1 = new RunLTLFormula(new LTLAtomicProposition(init), RunOperators.Binary.AND, new RunLTLFormula(flowTrue, RunOperators.Binary.OR, flowFalse));

        // show mcNet
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName(), false, true, true);
//        settings.setOutputData(data);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f1, LTLModelCheckingResult.Satisfied.TRUE, settings);

        RunLTLFormula f2 = new RunLTLFormula(new LTLAtomicProposition(init), RunOperators.Binary.AND, new RunLTLFormula(flowTrue, RunOperators.Binary.AND, flowFalse));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f2, LTLModelCheckingResult.Satisfied.TRUE, settings);
        net.setInitialTransit(init);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f1, LTLModelCheckingResult.Satisfied.TRUE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f2, LTLModelCheckingResult.Satisfied.FALSE, settings);

        // net extension
        Place init2 = net.createPlace("init2");
        init2.setInitialToken(1);
        net.setInitialTransit(init2);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f1, LTLModelCheckingResult.Satisfied.TRUE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f2, LTLModelCheckingResult.Satisfied.FALSE, settings);

        LTLAtomicProposition fInit1 = new LTLAtomicProposition(init);
        LTLAtomicProposition fInit2 = new LTLAtomicProposition(init2);
        RunLTLFormula f3 = new RunLTLFormula(fInit1, RunOperators.Binary.OR, fInit2);
        RunLTLFormula f4 = new RunLTLFormula(fInit1, RunOperators.Binary.AND, fInit2);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f3, LTLModelCheckingResult.Satisfied.TRUE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f4, LTLModelCheckingResult.Satisfied.TRUE, settings);

        FlowLTLFormula flowInit1 = new FlowLTLFormula(fInit1);
        FlowLTLFormula flowInit2 = new FlowLTLFormula(fInit2);

        RunLTLFormula f5 = new RunLTLFormula(flowInit1, RunOperators.Binary.OR, flowInit2);
        RunLTLFormula f6 = new RunLTLFormula(flowInit1, RunOperators.Binary.AND, flowInit2);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f6, LTLModelCheckingResult.Satisfied.FALSE, data, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f5, LTLModelCheckingResult.Satisfied.FALSE, settings);

        RunLTLFormula f7 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(fInit1, LTLOperators.Binary.AND, fInit2)));
        RunLTLFormula f8 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(fInit1, LTLOperators.Binary.OR, fInit2)));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f7, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f8, LTLModelCheckingResult.Satisfied.TRUE, settings);

        // net extension
        Place out1 = net.createPlace("out1");
        Transition t = net.createTransition();
        net.createFlow(init, t);
        net.createFlow(t, out1);
        net.createTransit(init, t, out1);

        LTLAtomicProposition fOut1 = new LTLAtomicProposition(out1);
        LTLAtomicProposition ft = new LTLAtomicProposition(t);

        RunLTLFormula f9 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLFormula(LTLOperators.Unary.F, fOut1), LTLOperators.Binary.AND, fInit2)));
        RunLTLFormula f10 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLFormula(LTLOperators.Unary.F, fOut1), LTLOperators.Binary.OR, fInit2)));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f9, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f10, LTLModelCheckingResult.Satisfied.TRUE, settings);

        RunLTLFormula f11 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(ft, LTLOperators.Binary.AND, fInit2))); // A (t AND init2)
        RunLTLFormula f12 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(ft, LTLOperators.Binary.OR, fInit2)));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f11, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f12, LTLModelCheckingResult.Satisfied.TRUE, settings);

        // net extension
        net.createInitialTransit(t, out1);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f7, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f8, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f9, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f10, LTLModelCheckingResult.Satisfied.TRUE, settings);

        // net extension
        Place out2 = net.createPlace("out2");
        Transition t2 = net.createTransition();
        net.createFlow(init2, t2);
        net.createFlow(t2, out2);
        net.createTransit(init2, t2, out2);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_net", net, false);

        LTLAtomicProposition fOut2 = new LTLAtomicProposition(out2);

        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f7, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f8, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f9, LTLModelCheckingResult.Satisfied.FALSE, settings);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f10, LTLModelCheckingResult.Satisfied.TRUE, settings);

        RunLTLFormula f13 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLFormula(LTLOperators.Unary.F, fOut1), LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, fOut2))));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f13, LTLModelCheckingResult.Satisfied.TRUE, settings);

        RunLTLFormula f14 = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(ft, LTLOperators.Binary.OR, new LTLAtomicProposition(t2))));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f14, LTLModelCheckingResult.Satisfied.FALSE, settings);
    }

    @Test
    public void testInitFirstStep() throws Exception {
        PetriNetWithTransits net = new PetriNetWithTransits("initFirst");
        Place init = net.createPlace("init");
        init.setInitialToken(1);

        ModelCheckerFlowLTL mc;
        LTLModelCheckingResult ret;

        FlowLTLFormula flowF = new FlowLTLFormula(new LTLConstants.True());
        RunLTLFormula f = new RunLTLFormula(new LTLAtomicProposition(init), RunOperators.Binary.AND, flowF);

        // show mcNet
//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName(), false, true, true);
//        settings.setOutputData(data);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

        net.setInitialTransit(init);
        RunLTLFormula f2 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(init)));
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f2, LTLModelCheckingResult.Satisfied.TRUE, settings);

    }

    @Test(enabled = true)
    public void testCounterExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunLTLFormula f;
        LTLModelCheckingResult ret;
        String name;

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                dataInCircuit,
                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
                Maximality.MAX_INTERLEAVING,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                CircuitRendererSettings.TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);

        settings.getAbcSettings().setDetailedCEX(true);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        System.out.println(ret.getCex().toString());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        settings.getAbcSettings().setDetailedCEX(false);
        ret = mc.check(net, f);
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        System.out.println(ret.getCex().toString());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createIntroductoryExample();
        settings.getAbcSettings().setDetailedCEX(true);
        f = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(net.getPlace("E"))));
        ret = mc.check(net, f);
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        System.out.println(ret.getCex().toString());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        settings.getAbcSettings().setDetailedCEX(false);
        ret = mc.check(net, f);
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        System.out.println(ret.getCex().toString());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    public void testMaxInCircuitVsFormula() throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);

        String formula;
        RunLTLFormula f;
        LTLModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit 
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                dataInCircuit,
                Approach.PARALLEL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);

        AdamCircuitFlowLTLMCStatistics statsInCircuit = new AdamCircuitFlowLTLMCStatistics();
        settings.setStatistics(statsInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // maximality in formula        
        AdamCircuitFlowLTLMCStatistics statsInFormula = new AdamCircuitFlowLTLMCStatistics();
        settings.setMaximality(Maximality.MAX_INTERLEAVING);

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);
        settings.setOutputData(dataInFormula);
        settings.setStatistics(statsInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

//        System.out.println(statsInCircuit.toString());
//        System.out.println("-------");
//        System.out.println(statsInFormula.toString());
    }

    @Test
    public void testNewlyFlowCreation() throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("infFlows");
        Transition tin = net.createTransition("createFlows");
        Place init = net.createPlace("pIn");
        init.setInitialToken(1);
        net.createFlow(tin, init);
        net.createFlow(init, tin);
        net.createTransit(init, tin, init);
        net.createInitialTransit(tin, init);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunLTLFormula formula;
        String name;
        LTLModelCheckingResult ret;

        formula = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(init))); // should be true since the first place of each chain is pIn
        name = net.getName() + "_" + formula.toString().replace(" ", "");

//        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);

        formula = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(init))));  //should still be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test(enabled = true)
    public void introducingExampleTransitions() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTransit(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTransit(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition t1 = net.createTransition("o1");
        Transition t2 = net.createTransition("o2");
        net.createFlow(a, t1);
        net.createFlow(b, t1);
        net.createFlow(t1, d);
        net.createFlow(c, t2);
        net.createFlow(d, t2);
        net.createFlow(t2, e);
        net.createFlow(t2, f);
        net.createFlow(t2, b);
        net.createTransit(a, t1, d);
        net.createTransit(b, t1, d);
        net.createTransit(d, t2, e, b);
        net.createInitialTransit(t2, f);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_net", net, false);

        RunLTLFormula formula;
        String name;
        LTLModelCheckingResult ret;

        // %%%%%%%%%%%%%%%%%%%%%%%%%    
        RunLTLFormula a1 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(t1)));
        RunLTLFormula a2 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(t2)));
        formula = new RunLTLFormula(a1, RunOperators.Binary.OR, a2); // should not hold since the newly created flow does not start with a transition, but a place
        name = net.getName() + "_" + formula.toString().replace(" ", "");

//        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outDir + name + "_init", false, true, true);
//
//        // check in circuit
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//
//        settings.setOutputData(dataInCircuit);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula); // cannot check since this are two flow formulas
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, dataInCircuit, settings);

        // %%%%%%% newly added (not done for all cases)
        formula = new RunLTLFormula(a1, RunOperators.Binary.OR, new FlowLTLFormula(new LTLAtomicProposition(f))); // should not hold because each case has the other case as counter example
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula); // cannot check since this are two flow formulas
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);

        // %%%%%%%% newly added (not done for all cases)
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLAtomicProposition(t1), LTLOperators.Binary.OR, new LTLAtomicProposition(f)))); // should hold then the initial one start with a1 and the new one starts with f
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new LTLAtomicProposition(t1)); // should  hold since we test it on the run and there is no other transition enabled and we demand maximality
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);        
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new LTLAtomicProposition(t2)); // should not hold since the flows starting in A and B
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(t1))); // should not hold since t2 generates a new one which directly dies
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void introducingExamplePlaces() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTransit(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTransit(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition t1 = net.createTransition("o1");
        Transition t2 = net.createTransition("o2");
        net.createFlow(a, t1);
        net.createFlow(b, t1);
        net.createFlow(t1, d);
        net.createFlow(c, t2);
        net.createFlow(d, t2);
        net.createFlow(t2, e);
        net.createFlow(t2, f);
        net.createFlow(t2, b);
        net.createTransit(a, t1, d);
        net.createTransit(b, t1, d);
        net.createTransit(d, t2, e, b);
        net.createInitialTransit(t2, f);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunLTLFormula formula;
        String name;
        LTLModelCheckingResult ret;

        //%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(e))); // should not be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//
//        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);

        // check interleaving in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
        ILTLFormula ltlE = new LTLAtomicProposition(e);
        ILTLFormula finallyE = new LTLFormula(LTLOperators.Unary.F, ltlE);
        ILTLFormula ltlF = new LTLAtomicProposition(f);
        ILTLFormula finallyF = new LTLFormula(LTLOperators.Unary.F, ltlF);
        ILTLFormula ltlB = new LTLAtomicProposition(b);
        ILTLFormula inifintelyB = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlB));

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, ltlB))); // should not be true since the new chain in F exists
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);

//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
//        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should not be true, since flow A->D->B
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, ltlB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);

//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
////        System.out.println(ret.getCex().toString());
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, ltlB)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);

////        // without init
////        settings.setInitFirst(false);
////        ret = mc.check(net, formula, outputDirInCircuit + name, false);
////        Assert.assertNull(ret); // todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
//        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true since the infinitely B is the last place of the run and it is the whole time stuttering
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);

//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // %%%%%%%%%%%%%%%%%%%%%%%%%
        LTLAtomicProposition ltlD = new LTLAtomicProposition(d);
        formula = new RunLTLFormula( // should not be true since the net is finite and D is not a place of all final markings
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        //%%%%%%%%%%%%%%%%%%%%%
        // add a transition such that it is not finite anymore
        net.setName(net.getName() + "_reset");
        Transition restart = net.createTransition("reset");
        net.createFlow(restart, a);
        net.createFlow(restart, c);
        net.createFlow(e, restart);
        net.createFlow(f, restart);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should still be true, since the chains end in B
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should still not be true since the chain in E terminates after one round
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        // let the flows be alive
        net.setName(net.getName() + "_alive");
        net.createTransit(e, restart, a);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true since now all apart of the newly created chain in F will be alive in each round
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + formula.toString().replace(" ", "");
//        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check in circuit
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
//        settings.setOutputData(dataInCircuit);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, formula);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void checkToyExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(false);
        net.setName(net.getName() + "_infinite");
        //add creation of flows
        Place in = net.getPlace("in");
        net.removeInitialTransit(in);
        Transition create = net.createTransition("createFlows");
        net.createFlow(in, create);
        net.createFlow(create, in);
        net.createTransit(in, create, in);
        net.createInitialTransit(create, in);
        net.setWeakFair(net.getTransition("t"));

        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        String name;
        RunLTLFormula f;
        LTLModelCheckingResult ret;

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

//        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//
//        // check interleaving in circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(dataInCircuit);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

        // check interleaving in formula
//        settings.setInitFirst(true);
//        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
//        settings.setOutputData(dataInFormula);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);

        String formula = "F out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);
//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);
//
//        // check maximal initerleaving in the circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(data);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);
//        ret = mc.check(net, f);
////        System.out.println(ret.getCex().toString());
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        String formula = "A F out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);

//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);
//
//        // check maximal initerleaving in the circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(data);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);

//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE); 
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        String formula = "A F out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);

//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);
//
//        // check maximal initerleaving in the circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(data);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);
//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        String formula = "A F pOut";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);
//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);
//
//        // check maximal initerleaving in the circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(data);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test(enabled = true)
    public void redundantFlowExampleFix() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        RunLTLFormula f = new RunLTLFormula(
                new LTLFormula(LTLOperators.Unary.NEG,
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.F,
                                        new LTLFormula(new LTLAtomicProposition(net.getTransition("tupD")), LTLOperators.Binary.OR, new LTLAtomicProposition(net.getTransition("tupU")))
                                )
                        )
                ), RunOperators.Implication.IMP,
                new FlowLTLFormula(
                        new LTLFormula(LTLOperators.Unary.F,
                                new LTLAtomicProposition(net.getPlace("out"))
                        ))
        );
//        System.out.println(f.toString());
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true, Abc.VerificationAlgo.IC3
//        );
//
//        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName() + "_init", false, false, true);
//
//        settings.setOutputData(dataInCircuit);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test(enabled = true)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        String formula = "A F out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);
//        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);
//
//        // check maximal initerleaving in the circuit
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(data);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ModelCheckingResult ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        mcNet = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(outDir + net.getName() + "_mc", mcNet, true);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);

        net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
//        ret = mc.check(net, f);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);
    }

    @Test
    public void testNoChains() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/accessControl.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true, Abc.VerificationAlgo.IC3
//        );
//        ModelCheckingResult ret;
        RunLTLFormula f = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(net.getPlace("bureau"))));
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName(), false, false, true);

//        settings.setOutputData(dataInCircuit);
//        settings.setStatistics(stats);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, f);
////        System.out.println(stats.toString());
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

//        settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_NONE,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true,
//                Abc.VerificationAlgo.IC3);
//        settings.setOutputData(dataInCircuit);
//        settings.setStatistics(stats);
//        mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, f);
////        System.out.println(stats.toString());
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void testTransitions() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/Net.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true, Abc.VerificationAlgo.IC3);
//        ModelCheckingResult ret;
        RunLTLFormula f;
        AdamCircuitFlowLTLMCStatistics stats;

        f = FlowLTLParser.parse(net, "𝔸 ◇ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
//        ret = mc.check(net, f); // takes some time
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.TRUE, settings);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
//        ret = mc.check(net, f); // takes some time
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ sw002fwdTosw000");

        stats = new AdamCircuitFlowLTLMCStatistics();
//        ret = mc.check(net, f);  // takes some time
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, f, LTLModelCheckingResult.Satisfied.FALSE, settings);
    }

    @Test
    public void testWeakfairness() throws Exception {
        PetriNetWithTransits pnwt = new PetriNetWithTransits("simpleWeakFairness");
        Place in = pnwt.createPlace("in");
        in.setInitialToken(1);
        Place out = pnwt.createPlace("out");
        out.setInitialToken(1);
        Transition init = pnwt.createTransition("init");
        pnwt.createFlow(in, init);
        pnwt.createFlow(init, in);
        pnwt.createTransit(in, init, in);
        pnwt.createInitialTransit(init, in);
        Transition t = pnwt.createTransition("t");
        pnwt.setWeakFair(t);
        pnwt.createFlow(in, t);
        pnwt.createFlow(t, in);
        pnwt.createFlow(out, t);
        pnwt.createFlow(t, out);
        pnwt.createTransit(in, t, out);
        pnwt.createTransit(out, t, out);
//        PNWTTools.savePnwt2PDF(outputDir+pnwt.getName(), pnwt, false);

        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(out)));

//        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
//                LogicsTools.TransitionSemantics.OUTGOING,
//                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
//                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
//                AigerRenderer.OptimizationsSystem.NONE,
//                AigerRenderer.OptimizationsComplete.NONE,
//                true, Abc.VerificationAlgo.IC3);
//
//        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();
//        settings.setStatistics(stats);
//        settings.setOutputData(new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, false)); // Todo: error when this is not added.
//
//        ModelCheckerFlowLTL checker = new ModelCheckerFlowLTL(settings);
//        ModelCheckingResult res = checker.check(pnwt, formula);
////        System.out.println(stats.getMc_formula());
//        Assert.assertEquals(res.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(pnwt, formula, LTLModelCheckingResult.Satisfied.TRUE, settings);
    }

    @Test
    public void testCounterExampleATVA() throws Exception {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/ATVA19_motivatingExample.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("p6"))));

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName() + "data", false, false, true);

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                data,
                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
                Maximality.MAX_INTERLEAVING,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);

        settings.getAbcSettings().setDetailedCEX(false);

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        LTLModelCheckingResult ret = mc.check(net, formula);
//        System.out.println(ret.getCex());
        for (CounterExampleIterator iterator = ret.getCex().iterator(); iterator.hasNext();) {
            String next = iterator.next();
            Transition t = net.getTransition(next);
//            System.out.println(t.toString());
//            System.out.println("is in loop" + iterator.isInLoop());
        }
    }
}

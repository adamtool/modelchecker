package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Maximality;
import static uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING;
import static uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Stuttering;
import static uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import static uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach.SEQUENTIAL;
import static uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsComplete;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;
import static uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics.OUTGOING;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLSequential {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static final String outputDirInCircuit = outputDir + "sequential/max_in_circuit/";
    private static final String outputDirInFormula = outputDir + "sequential/max_in_formula/";

    private static final OptimizationsSystem optSys = OptimizationsSystem.NONE;
    private static final OptimizationsComplete optCom = OptimizationsComplete.NB_GATES_BY_REGEX_WITH_IDX_SQUEEZING;

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
//               Logger.getInstance().setVerbose(true);
//        Logger.getInstance().addMessageStream(LOGGER_ABC_OUT, System.out);
//        Logger.getInstance().addMessageStream(LOGGER_ABC_ERR, System.err);
    }

    @BeforeClass
    public void createFolder() {
        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();
    }

    @Test
    public void testMaxInCircuitVsFormula() throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit 
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                OUTGOING,
                SEQUENTIAL,
                MAX_INTERLEAVING_IN_CIRCUIT,
                PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);
        AdamCircuitFlowLTLMCStatistics statsInCircuit = new AdamCircuitFlowLTLMCStatistics();
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);
        settings.setOutputData(dataInCircuit);
        settings.setStatistics(statsInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula        
        AdamCircuitFlowLTLMCStatistics statsInFormula = new AdamCircuitFlowLTLMCStatistics();
        settings.setMaximality(Maximality.MAX_INTERLEAVING);

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        settings.setOutputData(dataInFormula);
        settings.setStatistics(statsInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

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

        RunFormula formula;
        String name;
        ModelCheckingResult ret;

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                OUTGOING,
                SEQUENTIAL_INHIBITOR,
                MAX_INTERLEAVING,
                PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);

        formula = new RunFormula(new FlowFormula(new LTLAtomicProposition(init))); // should be true since the first place of each chain is pIn
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = new RunFormula(new FlowFormula(new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(init))));  //should still be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
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
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunFormula formula;
        String name;
        ModelCheckingResult ret;

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);

        // %%%%%%%%%%%%%%%%%%%%%%%%%    
        RunFormula a1 = new RunFormula(new FlowFormula(new LTLAtomicProposition(t1)));
        RunFormula a2 = new RunFormula(new FlowFormula(new LTLAtomicProposition(t2)));
        formula = new RunFormula(a1, RunOperators.Binary.OR, a2); // should not hold since the newly created flow does not start with a transition, but a place
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        // %%%%%%% newly added (not done for all cases)
        formula = new RunFormula(a1, RunOperators.Binary.OR, new FlowFormula(new LTLAtomicProposition(f))); // should not hold because each case has the other case as counter example
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);
        // check in circuit
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        // %%%%%%%% newly added (not done for all cases)
        formula = new RunFormula(new FlowFormula(new LTLFormula(new LTLAtomicProposition(t1), LTLOperators.Binary.OR, new LTLAtomicProposition(f)))); // should hold then the initial one start with a1 and the new one starts with f
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setMaximality(Maximality.MAX_INTERLEAVING);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new LTLAtomicProposition(t1)); // should  hold since we test it on the run and there is no other transition enabled and we demand maximality
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new LTLAtomicProposition(t2)); // should not hold since the flows starting in A and B
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);

        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new FlowFormula(new LTLAtomicProposition(t1))); // should not hold since t2 generates a new one which directly dies
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
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
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunFormula formula;
        String name;
        ModelCheckingResult ret;

        //%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new FlowFormula(new LTLAtomicProposition(e))); // should not be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);

        // check interleaving in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
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
        formula = new RunFormula(
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, ltlB))); // should not be true since the new chain in F exists
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);

        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should not be true, since flow A->D->B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, ltlB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, ltlB)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret); // todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since the infinitely B is the last place of the run and it is the whole time stuttering
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        LTLAtomicProposition ltlD = new LTLAtomicProposition(d);
        formula = new RunFormula( // should not be true since the net is finite and D is not a place of all final markings
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
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
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still be true, since the chains end in B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still not be true since the chain in E terminates after one round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        // let the flows be alive
        net.setName(net.getName() + "_alive");
        net.createTransit(e, restart, a);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since now all apart of the newly created chain in F will be alive in each round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + formula.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
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

        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        String name;
        RunFormula f;
        ModelCheckingResult ret;

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check interleaving in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // check interleaving in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // check interleaving in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // previous semantics
//        mc.setSemantics(TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), false);
//        Assert.assertNotNull(ret);
        // check standard maximality
        // + sequential
        // + next semantics
//        mc.setSemantics(TransitionSemantics.OUTGOING);
        net = ToyExamples.createFirstExample(false);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        PNWTTools.saveAPT(outputDir + net.getName() + "_" + formula, net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName() + "_" + formula, net, false);
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // Check maximality in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // check it outside
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // previous semantics
//        mc.setSemantics(TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), true);
//        Assert.assertNull(ret);
        // check to flow formulas
//        mc.setSemantics(TransitionSemantics.OUTGOING);
        f = new RunFormula(f, RunOperators.Binary.OR, f);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        settings.setInitFirst(true);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        String name;
        RunFormula f;
        ModelCheckingResult ret;

        formula = "A F out ";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // in maximality in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // maximality in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3
        );
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();
        settings.setOutputData(dataInCircuit);
        settings.setStatistics(stats);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println(stats.toString());
//        System.out.println(stats.getAbc_mem());
//        System.out.println(stats.getAbc_sec());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        
        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        // maximality in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3
        );
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = false)
    public void redundantFlowExampleFix() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        RunFormula f = new RunFormula(
                new LTLFormula(LTLOperators.Unary.NEG,
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.F,
                                        new LTLFormula(new LTLAtomicProposition(net.getTransition("tupD")), LTLOperators.Binary.OR, new LTLAtomicProposition(net.getTransition("tupU")))
                                )
                        )
                ), RunOperators.Implication.IMP,
                new FlowFormula(
                        new LTLFormula(LTLOperators.Unary.F,
                                new LTLAtomicProposition(net.getPlace("out"))
                        ))
        );
//        System.out.println(f.toString());
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3
        );

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + net.getName() + "_init", false, false, true);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F out";

        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3
        );

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, false);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        settings.setOutputData(dataInCircuit);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);

        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test
    public void testNetwork() {

    }

    @Test
    public void testNoChains() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/accessControl.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3
        );
        ModelCheckingResult ret;
        RunFormula f = new RunFormula(new FlowFormula(new LTLAtomicProposition(net.getPlace("bureau"))));
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + net.getName(), false, false, true);

        settings.setOutputData(dataInCircuit);
        settings.setStatistics(stats);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println(stats.toString());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_NONE,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true,
                VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        settings.setStatistics(stats);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println(stats.toString());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    public void testTransitions() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/Net.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                true, VerificationAlgo.IC3);
        ModelCheckingResult ret;
        RunFormula f;
        AdamCircuitFlowLTLMCStatistics stats;

        f = FlowLTLParser.parse(net, "𝔸 ◇ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(), true, stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(), true, stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ sw002fwdTosw000");

        stats = new AdamCircuitFlowLTLMCStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(), true, stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

//        f = FlowLTLParser.parse(net, "𝔸 (⬜ (sw000fwdTosw001))");
//        stats = new ModelcheckingStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(),  stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//
//        f = FlowLTLParser.parse(net, "𝔸 (⬜ (((pOut ⋎ sw002fwdTosw000) ⋎ sw000fwdTosw001) ⋎ sw002fwdTosw000)))");
//        stats = new ModelcheckingStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(), true, stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//
//        f = FlowLTLParser.parse(net, "𝔸 (◇ pOut ⋏ ⬜ (((pOut ⋎ sw002fwdTosw000) ⋎ sw000fwdTosw001) ⋎ sw002fwdTosw000))");
//        stats = new ModelcheckingStatistics();
//        ret = mc.check(net, f, outputDirInCircuit + net.getName(), true, stats);
//        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
    }
}

package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.logics.LogicsTools;

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

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);

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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, formula);
        System.out.println(ret.getCex().toString());
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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
        dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check in circuit
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
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
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
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

        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);

        // check interleaving in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // check interleaving in formula
        settings.setInitFirst(true);
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING);
        settings.setOutputData(dataInFormula);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        settings.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);

        String formula = "F out";
        RunFormula f = FlowLTLParser.parse(net, formula);
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,// todo: here is an error, for MAX_INTERLEAVING
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f);
//        System.out.println(ret.getCex().toString());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);

        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE); // todo: error results true
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F pOut";
        RunFormula f = FlowLTLParser.parse(net, formula);
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
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
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true, Abc.VerificationAlgo.IC3
        );

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName() + "_init", false, false, true);

        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outDir + net.getName() + "_init", false, false, true);

        // check maximal initerleaving in the circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        PetriNetWithTransits mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    public void testNoChains() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/accessControl.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true, Abc.VerificationAlgo.IC3
        );
        ModelCheckingResult ret;
        RunFormula f = new RunFormula(new FlowFormula(new LTLAtomicProposition(net.getPlace("bureau"))));
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName(), false, false, true);

        settings.setOutputData(dataInCircuit);
        settings.setStatistics(stats);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println(stats.toString());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_NONE,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(dataInCircuit);
        settings.setStatistics(stats);
        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
//        System.out.println(stats.toString());
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void testTransitions() throws ParseException, IOException, RenderException, InterruptedException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = PNWTTools.getPetriNetWithTransitsFromParsedPetriNet(Tools.getPetriNet(System.getProperty("examplesfolder") + "/modelchecking/ltl/Net.apt"), false);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true, Abc.VerificationAlgo.IC3);
        ModelCheckingResult ret;
        RunFormula f;
        AdamCircuitFlowLTLMCStatistics stats;

        f = FlowLTLParser.parse(net, "𝔸 ◇ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f); // takes some time
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ pOut");
        stats = new AdamCircuitFlowLTLMCStatistics();
        ret = mc.check(net, f); // takes some time
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        f = FlowLTLParser.parse(net, "𝔸 ⬜ sw002fwdTosw000");

        stats = new AdamCircuitFlowLTLMCStatistics();
        ret = mc.check(net, f);  // takes some time
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
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

        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(out)));

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.PARALLEL,
                AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitLTLMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true, Abc.VerificationAlgo.IC3);

        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();
        settings.setStatistics(stats);
        settings.setOutputData(new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, false)); // Todo: error when this is not added.

        ModelCheckerFlowLTL checker = new ModelCheckerFlowLTL(settings);
        ModelCheckingResult res = checker.check(pnwt, formula);
//        System.out.println(stats.getMc_formula());
        Assert.assertEquals(res.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

}

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
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
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

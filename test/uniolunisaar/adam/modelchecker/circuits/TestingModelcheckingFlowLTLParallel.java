package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.Logger;
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

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mcNet, true);
        ret = mc.check(net, f);
    }

}

package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings.Stuttering;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class MCFlowLTLSeqGenerators {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static String outputDirInCircuit = outputDir + "sequential/generators/max_in_circuit/";
    private static String outputDirInFormula = outputDir + "sequential/generators/max_in_formula/";

    private static final OptimizationsSystem optSys = OptimizationsSystem.NONE;
//    private static final AigerRenderer.OptimizationsComplete optCom = AigerRenderer.OptimizationsComplete.NB_GATES_BY_DS_WITH_IDX_SQUEEZING_AND_EXTRA_LIST;
    private static final AigerRenderer.OptimizationsComplete optCom = AigerRenderer.OptimizationsComplete.NB_GATES_BY_REGEX_WITH_IDX_SQUEEZING;

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
//        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().addMessageStream(LOGGER_ABC_OUT, System.out);
//        Logger.getInstance().addMessageStream(AigToAig.LOGGER_AIGER_OUT, System.out);
//        Logger.getInstance().addMessageStream(AigToAig.LOGGER_AIGER_ERR, System.err);
    }

    @BeforeClass
    public void createFolder() {
        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();
    }

    @Test(enabled = true)
    public void updatingNetworkBenchmark() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        outputDirInCircuit += "updatingNetwork/";
        outputDirInFormula += "updatingNetwork/";
        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,
                true,
                VerificationAlgo.IC3);

        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        settings.setOutputData(dataInFormula);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    public void testParallelChecking() throws ParseException, FileNotFoundException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,
                true,
                //                VerificationAlgo.IC3, VerificationAlgo.BMC, VerificationAlgo.BMC2, VerificationAlgo.BMC3, VerificationAlgo.INT);
                                VerificationAlgo.IC3, VerificationAlgo.BMC3);
//                VerificationAlgo.IC3, VerificationAlgo.BMC3, VerificationAlgo.BMC2);
//                VerificationAlgo.BMC3);
//        PetriNetWithTransits net = RedundantNetwork.getUpdatingNetwork(3, 3);
        PetriNetWithTransits net = RedundantNetwork.getUpdatingNetwork(1, 1);
        String formula = "A F out";

        RunFormula f = FlowLTLParser.parse(net, formula);
        String name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        settings.setOutputData(dataInCircuit);
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();
        settings.setStatistics(stats);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        System.out.println(ret.getAlgo());
//        System.out.println("ABC sec: " + stats.getAbc_sec());
    }

}

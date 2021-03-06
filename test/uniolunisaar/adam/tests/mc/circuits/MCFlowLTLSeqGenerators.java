package uniolunisaar.adam.tests.mc.circuits;

import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings.TransitionSemantics;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering;
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
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.transformers.petrinet.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.petrinet.pn2aiger.AigerRenderer.OptimizationsSystem;

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
    AdamCircuitFlowLTLMCSettings settings;

    @BeforeMethod
    public void createSettings() {
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + "buffer", false, false, true);
        settings = new AdamCircuitFlowLTLMCSettings(
                data,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                optSys,
                optCom,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);
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
        RunLTLFormula f;
        LTLModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit       
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);
        settings.setOutputData(dataInCircuit);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        settings.setInitFirst(true);
        settings.setMaximality(Maximality.MAX_INTERLEAVING);
        AdamCircuitFlowLTLMCOutputData dataInFormula = new AdamCircuitFlowLTLMCOutputData(outputDirInFormula + name + "_init", false, false, true);
        settings.setOutputData(dataInFormula);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    public void testParallelChecking() throws ParseException, FileNotFoundException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        settings.setVerificationAlgo(VerificationAlgo.IC3, VerificationAlgo.BMC3);

//          PetriNetWithTransits net = RedundantNetwork.getUpdatingNetwork(3, 3);
        PetriNetWithTransits net = RedundantNetwork.getUpdatingNetwork(1, 1);
        String formula = "A F out";

        RunLTLFormula f = FlowLTLParser.parse(net, formula);
        String name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamCircuitFlowLTLMCOutputData dataInCircuit = new AdamCircuitFlowLTLMCOutputData(outputDirInCircuit + name + "_init", false, false, true);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);
        settings.setOutputData(dataInCircuit);
        AdamCircuitFlowLTLMCStatistics stats = new AdamCircuitFlowLTLMCStatistics();
        settings.setStatistics(stats);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        LTLModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
//        System.out.println(ret.getAlgo());
//        System.out.println("ABC sec: " + stats.getAbc_sec());
    }

}

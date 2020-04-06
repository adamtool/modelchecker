package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsComplete;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingOptimizations {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";

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
        (new File(outputDir)).mkdirs();
    }

    @Test
    public void testNbGates() throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3);

        String formula;
        RunLTLFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A F pOut";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
//        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
//                TransitionSemantics.OUTGOING,
//                Approach.SEQUENTIAL,
//                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                Stuttering.PREFIX_REGISTER,
//                OptimizationsSystem.NB_GATES_AND_INDICES,
//                OptimizationsComplete.NONE,
//                VerificationAlgo.IC3,
//                true);
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(OptimizationsSystem.NB_GATES_AND_INDICES, OptimizationsComplete.NONE);
        AdamCircuitFlowLTLMCStatistics statsGS = new AdamCircuitFlowLTLMCStatistics();
        AdamCircuitFlowLTLMCOutputData dataGS = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "GS", false, false, true);
        settings.setOutputData(dataGS);
        settings.setStatistics(statsGS);

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        System.out.println(statsGS.toString()
//        );

        // maximality in circuit
//         mc = new ModelCheckerFlowLTL(
//                TransitionSemantics.OUTGOING,
//                Approach.SEQUENTIAL,
//                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
//                Stuttering.PREFIX_REGISTER,
//                OptimizationsSystem.NB_GATES_AND_INDICES_AND_EQCOM,
//                OptimizationsComplete.NONE,
//                VerificationAlgo.IC3,
//                true);
        settings = new AdamCircuitFlowLTLMCSettings(OptimizationsSystem.NB_GATES_AND_INDICES_AND_EQCOM, OptimizationsComplete.NONE);
        AdamCircuitFlowLTLMCStatistics statsGSEQ = new AdamCircuitFlowLTLMCStatistics();
        AdamCircuitFlowLTLMCOutputData dataGSEQ = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "GS", false, false, true);
        settings.setOutputData(dataGSEQ);
        settings.setStatistics(statsGSEQ);

        mc = new ModelCheckerFlowLTL(settings);
        ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        System.out.println(statsGSEQ.toString());
    }

}

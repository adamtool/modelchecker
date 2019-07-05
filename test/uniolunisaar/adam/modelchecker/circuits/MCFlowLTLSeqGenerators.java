package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.util.logics.transformers.logics.ModelCheckingOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndFlowLTLtoCircuit.Approach;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Maximality;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Stuttering;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.TransitionSemantics;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.externaltools.logics.AigToAig;
import static uniolunisaar.adam.logic.externaltools.modelchecking.Abc.LOGGER_ABC_OUT;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;

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
//        Logger.getInstance().setVerbose(false);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().addMessageStream(LOGGER_ABC_OUT, System.out);
        Logger.getInstance().addMessageStream(AigToAig.LOGGER_AIGER_OUT, System.out);
        Logger.getInstance().addMessageStream(AigToAig.LOGGER_AIGER_ERR, System.err);
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
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                TransitionSemantics.OUTGOING,
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                optSys,
                optCom,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,
                VerificationAlgo.IC3,
                true);

        ModelCheckingOutputData dataInCircuit = new ModelCheckingOutputData(outputDirInCircuit + name + "_init", false, false, true);
        ret = mc.check(net, f, dataInCircuit);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(Maximality.MAX_INTERLEAVING);
        ModelCheckingOutputData dataInFormula = new ModelCheckingOutputData(outputDirInFormula + name + "_init", false, false, true);
        ret = mc.check(net, f, dataInFormula);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

}

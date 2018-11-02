package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.modelchecking.UpdatingNetwork;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class MCFlowLTLSeqGenerators {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static String outputDirInCircuit = outputDir + "sequential/generators/max_in_circuit/";
    private static String outputDirInFormula = outputDir + "sequential/generators/max_in_formula/";

    @BeforeClass
    public void createFolder() {
        Logger.getInstance().setVerbose(true);
        Logger.getInstance().addMessageStream(ModelCheckerMCHyper.LOGGER_ABC_OUT, System.out);

        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();
    }

    @Test(enabled = true)
    public void updatingNetworkBenchmark() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = UpdatingNetwork.create(7);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);
        outputDirInCircuit += "updatingNetwork/";
        outputDirInFormula += "updatingNetwork/";
        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();

        String formula;
        RunFormula f;
        CounterExample ret;
        String name;

        formula = "A(F(p7)";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.INT,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertNull(ret);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertNull(ret);
    }

}

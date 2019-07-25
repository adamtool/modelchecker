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
import uniolunisaar.adam.ds.modelchecking.ModelcheckingStatistics;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsComplete;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsSystem;
import uniolunisaar.adam.tools.Tools;

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
        RunFormula f;
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
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(OptimizationsSystem.NB_GATES_AND_INDICES, OptimizationsComplete.NONE);
        ModelcheckingStatistics statsGS = new ModelcheckingStatistics();
        ModelCheckingOutputData dataGS = new ModelCheckingOutputData(outputDir + name + "GS", false, false, true);
        ret = mc.check(net, f, dataGS, statsGS);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        System.out.println(statsGS.toString()
        );

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
        mc = new ModelCheckerFlowLTL(OptimizationsSystem.NB_GATES_AND_INDICES_AND_EQCOM, OptimizationsComplete.NONE);
        ModelcheckingStatistics statsGSEQ = new ModelcheckingStatistics();
        ModelCheckingOutputData dataGSEQ = new ModelCheckingOutputData(outputDir + name + "GS", false, false, true);
        ret = mc.check(net, f, dataGSEQ, statsGSEQ);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
        System.out.println(statsGSEQ.toString());
    }

}

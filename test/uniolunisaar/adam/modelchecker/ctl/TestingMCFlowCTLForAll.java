package uniolunisaar.adam.modelchecker.ctl;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ltl.FlowCTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwtandflowctl2pn.PnwtAndFlowCTL2PN;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMCFlowCTLForAll {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/flowCTLforAll/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
    }

    static FlowCTLModelcheckingSettings settings;

    public static final boolean verbose = false;

    @BeforeMethod
    public void initMCSettings() {
        settings = new FlowCTLModelcheckingSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
                AdamCircuitMCSettings.Maximality.MAX_NONE,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
        settings.setOutputData(new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, verbose));
    }

    @Test
    public void testLateCreation() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        //RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, new CTLFormula(CTLOperators.Unary.EF, new CTLAtomicProposition(pnwt.getPlace("p")))));
        // the negation in positive normalform 
        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(pnwt.getPlace("p"))))));
        check(pnwt, formula, LTLModelCheckingResult.Satisfied.FALSE);

        LTLFormula neverAbove = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pnwt.getTransition("tq"))));
        formula = new RunCTLForAllFormula(neverAbove, RunOperators.Implication.IMP, formula);
        check(pnwt, formula, LTLModelCheckingResult.Satisfied.FALSE);

        pnwt.setWeakFair(pnwt.getTransition("tp"));
        check(pnwt, formula, LTLModelCheckingResult.Satisfied.TRUE);

    }

    public static void check(PetriNetWithTransits pnwt, RunCTLForAllFormula formula, LTLModelCheckingResult.Satisfied sat) throws Exception {
        PetriNetWithTransits out = new PnwtAndFlowCTL2PN().createSequential(pnwt, formula, settings);
        PNWTTools.savePnwt2PDF(outputDir + out.getName(), out, false);

        ILTLFormula fairness = LogicsTools.getFairness(pnwt);
        formula = new RunCTLForAllFormula(fairness, RunOperators.Implication.IMP, formula);

        ILTLFormula ltlFormula = new FlowCTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(pnwt, out, formula, settings);
        System.out.println("formula to check: " + ltlFormula.toSymbolString());
        PnAndLTLtoCircuit.createCircuitWithoutFairnessAndMaximality(out, ltlFormula, settings);
        settings.fillAbcData(out);
        LTLModelCheckingResult result = Abc.call(settings.getAbcSettings(), settings.getOutputData(), settings.getStatistics());
//        System.out.println(result.getSatisfied().toString());
        System.out.println(result.getCex().toString());
        Assert.assertEquals(result.getSatisfied(), sat);
    }
}

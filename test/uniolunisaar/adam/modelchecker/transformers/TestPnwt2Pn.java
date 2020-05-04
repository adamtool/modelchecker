package uniolunisaar.adam.modelchecker.transformers;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ltl.FlowCTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwtandflowctl2pn.PnwtAndFlowCTL2PN;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestPnwt2Pn {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/transformers/pnwt2pn/";
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

    @Test
    public void testLateCreation() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        //RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, new CTLFormula(CTLOperators.Unary.EF, new CTLAtomicProposition(pnwt.getPlace("p")))));
        // the negation in positive normalform 
        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(pnwt.getPlace("p"))))));
        check(pnwt, formula,  LTLModelCheckingResult.Satisfied.FALSE);
        
//        RunCTLForAllFormula formula = 
//                new 
//        check(pnwt, formula,  LTLModelCheckingResult.Satisfied.FALSE);
        

    }

    private void check(PetriNetWithTransits pnwt, RunCTLForAllFormula formula, LTLModelCheckingResult.Satisfied sat) throws Exception {
        PetriNetWithTransits out = new PnwtAndFlowCTL2PN().createSequential(pnwt, formula);
        PNWTTools.savePnwt2PDF(outputDir + out.getName(), out, false);

        ILTLFormula fairness = LogicsTools.getFairness(pnwt);
        formula = new RunCTLForAllFormula(fairness, RunOperators.Implication.IMP, formula);

        ILTLFormula ltlFormula = new FlowCTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(pnwt, out, formula, new AdamCircuitFlowLTLMCSettings());
        AdamCircuitLTLMCSettings props = new AdamCircuitLTLMCSettings();
        PnAndLTLtoCircuit.createCircuitWithoutFairnessAndMaximality(out, ltlFormula, props);
        props.fillAbcData(out);
        LTLModelCheckingResult result = Abc.call(props.getAbcSettings(), props.getOutputData(), props.getStatistics());
        Assert.assertEquals(result.getSatisfied(), sat);
//        System.out.println(result.getSatisfied().toString());
//        System.out.println(result.getCex().toString());
    }
}

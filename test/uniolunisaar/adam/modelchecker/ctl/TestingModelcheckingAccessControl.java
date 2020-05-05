package uniolunisaar.adam.modelchecker.ctl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.generators.pnwt.util.acencoding.AccessControlChainSplitAtTransitions;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.ctl.ModelCheckerFlowCTL;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingAccessControl {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//      Logger.getInstance().setVerbose(true);
//      Logger.getInstance().setShortMessageStream(null);
//      Logger.getInstance().setVerboseMessageStream(null);
//      Logger.getInstance().setWarningStream(null);
    }

    @BeforeClass
    public void initMCSettings() {
    	TestingMCFlowCTLForAll.settings = new FlowCTLModelcheckingSettings(
                LogicsTools.TransitionSemantics.OUTGOING,
                ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
                AdamCircuitMCSettings.Maximality.MAX_NONE,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                true,
                Abc.VerificationAlgo.IC3);
    	TestingMCFlowCTLForAll.settings.setOutputData(new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, TestingMCFlowCTLForAll.verbose));
    }
    
    public ModelCheckerFlowCTL getModelChecker(String name) {
        FlowCTLLoLAModelcheckingSettings settings = new FlowCTLLoLAModelcheckingSettings(outputDir + name, true);
//        settings.setApproach(ModelCheckingSettings.Approach.PARALLEL_INHIBITOR);
        settings.setApproach(ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR);
        return new ModelCheckerFlowCTL(settings);
    }
    
    private PetriNetWithTransits createOfficeSmall() {
    	String name = "officeSmall";
		Set<String> groups = new HashSet<>();
		groups.add("it");
		
		Set<String> locations = new HashSet<>();
		locations.add("out");
		locations.add("lob");
		locations.add("cor");
		locations.add("mr");
		locations.add("bur");
		
		Map<String, String> starts = new HashMap<>();
		starts.put("it", "out");
		
		Set<Pair<String, String>> connections = new HashSet<>();
		connections.add(new Pair<String, String>("out", "lob"));
		connections.add(new Pair<String, String>("lob", "out"));
		
		connections.add(new Pair<String, String>("out", "cor"));
		connections.add(new Pair<String, String>("cor", "out"));
		
		connections.add(new Pair<String, String>("lob", "cor"));
		connections.add(new Pair<String, String>("cor", "lob"));

		connections.add(new Pair<String, String>("cor", "mr"));
		connections.add(new Pair<String, String>("mr", "cor"));
		
		connections.add(new Pair<String, String>("cor", "bur"));
		connections.add(new Pair<String, String>("bur", "cor"));
		
		Map<String, Set<Pair<String, String>>> open = new HashMap<>();
		
		Set<Pair<String, String>> itOpen = new HashSet<>();
		itOpen.add(new Pair<String, String>("out", "lob"));
		itOpen.add(new Pair<String, String>("lob", "out"));
		itOpen.add(new Pair<String, String>("out", "cor"));
		itOpen.add(new Pair<String, String>("cor", "out"));
		itOpen.add(new Pair<String, String>("lob", "cor"));
		itOpen.add(new Pair<String, String>("cor", "lob"));
		itOpen.add(new Pair<String, String>("cor", "mr"));
		itOpen.add(new Pair<String, String>("mr", "cor"));
		itOpen.add(new Pair<String, String>("cor", "bur"));
		itOpen.add(new Pair<String, String>("bur", "cor"));
		open.put("it", itOpen);
		
		return new AccessControlChainSplitAtTransitions(name, groups, locations, starts, connections, open).createAccessControlExample();
    }

    @Test
    void testAccessControl() throws Exception {
    	PetriNetWithTransits pnwt = createOfficeSmall();
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);
		
        // want to check "𝔸EF itATbur", with negation: "𝔸( (⊥ 𝓤_ ¬itATbur) ) "
        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(pnwt.getPlace("itATbur"))))));
        TestingMCFlowCTLForAll.check(pnwt, formula, LTLModelCheckingResult.Satisfied.TRUE);
	}
}

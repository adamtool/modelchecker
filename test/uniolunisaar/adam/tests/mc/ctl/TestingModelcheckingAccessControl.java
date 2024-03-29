package uniolunisaar.adam.tests.mc.ctl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;

import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings.TransitionSemantics;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.generators.pnwt.util.acencoding.AccessControlChainSplitAtTransitions;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.logic.transformers.petrinet.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingAccessControl {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/accessControl/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//      Logger.getInstance().setVerbose(true);
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    public FlowCTLModelcheckingSettings initMCSettings() {
        FlowCTLModelcheckingSettings settings = new FlowCTLModelcheckingSettings(
                new AdamCircuitFlowLTLMCOutputData(outputDir, false, false, TestingMCFlowCTLForAll.verbose),
                Approach.SEQUENTIAL_INHIBITOR,
                Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                Stuttering.PREFIX_REGISTER,
                TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                VerificationAlgo.IC3);

        return settings;
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

    @Test(enabled = false)
    void testAccessControl() throws Exception {
        PetriNetWithTransits pnwt = createOfficeSmall();
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        // want to check "𝔸EF itATbur", with negation: "𝔸( A (⊥ 𝓤_ ¬itATbur) ) "
        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.AUD, new CTLFormula(CTLOperators.Unary.NEG, new CTLAtomicProposition(pnwt.getPlace("itATbur"))))));

        for (Transition t : pnwt.getTransitions()) {
            if (!t.getId().startsWith("FLOWCREATECHAIN")) {
                pnwt.setWeakFair(t);
            }
        }

        TestingMCFlowCTLForAll.check(pnwt, formula, initMCSettings(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    void testLectureHall() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "lectureHall.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        Place yard = pnwt.getPlace("yard");
        Transition emergency = pnwt.getTransition("emergency");
        CTLAtomicProposition y = new CTLAtomicProposition(yard);
        CTLAtomicProposition em = new CTLAtomicProposition(emergency);

        // want to check "𝔸 AG(emergency -> EF yard)"
        // transformed is this "𝔸 E(false U' \neg emergency v E(true U yard))
//        ICTLFormula ftrans = new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.EUD, new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, em), CTLOperators.Binary.OR,
//                new CTLFormula(new CTLConstants.True(), CTLOperators.Binary.EU, y)));        
        // todo: don't I have to take the negation?
        // negation: "𝔸 \neg AG(emergency -> EF yard) = E(true U emergency \wedge E(false U' \neg yard))"
        ICTLFormula ftrans = new CTLFormula(new CTLConstants.True(), CTLOperators.Binary.EU, new CTLFormula(em, CTLOperators.Binary.AND,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.EUD, new CTLFormula(CTLOperators.Unary.NEG, y))));

        RunCTLForAllFormula formula = new RunCTLForAllFormula(new FlowCTLFormula(FlowCTLFormula.FlowCTLOperator.All, ftrans));

        TestingMCFlowCTLForAll.check(pnwt, formula, initMCSettings(), LTLModelCheckingResult.Satisfied.TRUE);
        TestingMCFlowCTLForAll.check(pnwt, formula, initMCSettings(), LTLModelCheckingResult.Satisfied.TRUE);
    }

}

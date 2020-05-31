package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import static uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo.IC3;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerLTL;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNTools;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.FormulaCreatorIngoingSemantics;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingIngoingLTL {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ingoingLTL/";

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    private AdamCircuitLTLMCSettings settings;

    @BeforeMethod
    public void initMCSettings() {
        settings = new AdamCircuitLTLMCSettings(
                new AdamCircuitLTLMCOutputData(outputDir + "buf", false, false),
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                CircuitRendererSettings.TransitionSemantics.INGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                IC3);
    }

    @Test(enabled = true)
    void testFirstExamplePaper() throws Exception {
        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        PNWTTools.savePnwt2PDF(outputDir + pn.getName(), new PetriNetWithTransits(pn), false);
        pn.rename(pn.getPlace("A"), "a");

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), false, true);
        settings.setOutputData(data);

        ModelCheckerLTL mc = new ModelCheckerLTL(settings);

        // no transition should be satisfied in the first step
        Collection<ILTLFormula> transitions = new ArrayList<>();
        for (Transition transition : pn.getTransitions()) {
            transitions.add(new LTLAtomicProposition(transition));
        }
        ILTLFormula f = FormulaCreator.bigWedgeOrVeeObject(transitions, false);
        f = new LTLFormula(LTLOperators.Unary.NEG, f);

        LTLModelCheckingResult check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // test the first transitions
        String formula = "X ((AA -> t11) AND (BB->t22))";
        f = FlowLTLParser.parse(pn, formula).toLTLFormula();
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        formula = "X ((a -> t1) AND (B->t2))";
        f = FlowLTLParser.parse(pn, formula).toLTLFormula();
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // test it globally (should both be false because of concurrent transitions)
        formula = "G ((AA -> t11) AND (BB->t22))";
        f = FlowLTLParser.parse(pn, formula).toLTLFormula();
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        formula = "G ((a -> t1) AND (B->t2))";
        f = FlowLTLParser.parse(pn, formula).toLTLFormula();
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    public void testStuttering() throws Exception {
        settings.setMaximality(AdamCircuitMCSettings.Maximality.MAX_NONE);
        settings.setTransitionEncoding(CircuitRendererSettings.TransitionEncoding.EXPLICIT);
//        settings.setTransitionSemantics(CircuitRendererSettings.TransitionSemantics.OUTGOING);
        PetriNet pn = PNTools.createPetriNet("testStuttering");
        Place p = pn.createPlace("start");
        p.setInitialToken(1);

        // infinite runs don't use stuttering
        Transition t = pn.createTransition("t");
        pn.createFlow(p, t);
        pn.createFlow(t, p);
        PNTools.savePN2PDF(outputDir + pn.getName(), pn, false, false);

        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), false, true);
        settings.setOutputData(data);

        ModelCheckerLTL mc = new ModelCheckerLTL(settings);

        ILTLFormula f = new LTLFormula(LTLOperators.Unary.G, new LTLAtomicProposition(p));
        LTLModelCheckingResult check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        // add an unmarked place and put the transition there (because there are still problems for nets without transitions)
        Place buf = pn.createPlace("buf");
        pn.removeFlow(p, t);
        pn.removeFlow(t, p);
        pn.createFlow(buf, t);
        PNTools.savePN2PDF(outputDir + pn.getName(), pn, false, false);

        data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), false, true);
        settings.setOutputData(data);

        mc = new ModelCheckerLTL(settings);

        f = new LTLFormula(LTLOperators.Unary.G, new LTLAtomicProposition(p));
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

        f = new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, f));
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        // mark the place
        buf.setInitialToken(1);
        check = mc.check(pn, f);
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        // use maximality
        ILTLFormula max = FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn);
        check = mc.check(pn, new LTLFormula(max, LTLOperators.Binary.IMP, f));
        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

    }

}

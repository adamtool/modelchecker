package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import java.io.IOException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.modelchecker.util.TestModelCheckerTools;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingIngoingFlowLTL {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ingoingFlowLTL/";

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setVerbose(false);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
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

    private AdamCircuitFlowLTLMCSettings[] settings;

    @BeforeMethod
    public void initMCSettings() {
        AdamCircuitFlowLTLMCSettings mcSettings_ParI_IntC = TestModelCheckerTools.mcSettings_ParI_IntC;
        mcSettings_ParI_IntC.setTransitionSemantics(CircuitRendererSettings.TransitionSemantics.INGOING);

        AdamCircuitFlowLTLMCSettings mcSettings_SeqI_IntC = TestModelCheckerTools.mcSettings_SeqI_IntC;
        mcSettings_SeqI_IntC.setTransitionSemantics(CircuitRendererSettings.TransitionSemantics.INGOING);
        mcSettings_SeqI_IntC.setTransitionEncoding(CircuitRendererSettings.TransitionEncoding.EXPLICIT);

        AdamCircuitFlowLTLMCSettings mcSettings_SeqI_IntF = TestModelCheckerTools.mcSettings_SeqI_IntF;
        mcSettings_SeqI_IntF.setTransitionSemantics(CircuitRendererSettings.TransitionSemantics.INGOING);

        settings = new AdamCircuitFlowLTLMCSettings[]{
            //            mcSettings_ParI_IntC,
            mcSettings_SeqI_IntC, //            mcSettings_SeqI_IntF
        };

    }

    @Test(enabled = true)
    public void introducingExampleTransitions() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("introduction");
        Place a = net.createPlace("a");
//        a.setInitialToken(1);
//        net.setInitialTransit(a);// not possible for ingoing, thus
        Place start = net.createPlace("start");
        start.setInitialToken(1);
        Transition init = net.createTransition();
        net.createFlow(start, init);
        net.createFlow(init, a);
        net.createInitialTransit(init, a);
//        
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTransit(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition o1 = net.createTransition("o1");
        Transition o2 = net.createTransition("o2");
        net.createFlow(a, o1);
        net.createFlow(b, o1);
        net.createFlow(o1, d);
        net.createFlow(c, o2);
        net.createFlow(d, o2);
        net.createFlow(o2, e);
        net.createFlow(o2, f);
        net.createFlow(o2, b);
        net.createTransit(a, o1, d);
        net.createTransit(b, o1, d);
        net.createTransit(d, o2, e, b);
        net.createInitialTransit(o2, f);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunLTLFormula formula;
        String name;
        LTLModelCheckingResult ret;

        // %%%%%%%%%%%%%%%%%%%%%%%%%    
        RunLTLFormula a1 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(init)));
        RunLTLFormula a2 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(o2)));
        formula = new RunLTLFormula(a1, RunOperators.Binary.OR, a2); // should not hold because those are the only two transitions starting a flow chain, but for each A part the other one is missing
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settings);

        //%%%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(
                new LTLFormula(new LTLAtomicProposition(init), LTLOperators.Binary.OR,
                        (new LTLAtomicProposition(o2))))); // should hold because those are the only two transitions starting a flow chain
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, true, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settings);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(a1, RunOperators.Binary.OR, new FlowLTLFormula(new LTLAtomicProposition(f))); // should not hold because each case has the other case as counter example
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLAtomicProposition(o1), LTLOperators.Binary.OR, new LTLAtomicProposition(f)))); // should hold then the initial one start with a1 and the new one starts with f
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settings);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new LTLAtomicProposition(o1)); // should  hold since we test it on the run and there is no other transition enabled and we demand maximality
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new LTLAtomicProposition(o2)); // should not hold since the flows starting in A and B
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(o1))); // should not hold since t2 generates a new one which directly dies
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);
    }

    @Test(enabled = true)
    public void introducingExamplePlaces() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createIntroductoryExample();
        Place a = net.getPlace("a");
        Place c = net.getPlace("C");
        Place f = net.getPlace("F");
        Place b = net.getPlace("B");
        Place d = net.getPlace("D");
        Place e = net.getPlace("E");
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        RunLTLFormula formula;
        String name;
        LTLModelCheckingResult ret;

        //%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(e))); // should not be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%
        ILTLFormula ltlE = new LTLAtomicProposition(e);
        ILTLFormula finallyE = new LTLFormula(LTLOperators.Unary.F, ltlE);
        ILTLFormula ltlF = new LTLAtomicProposition(f);
        ILTLFormula finallyF = new LTLFormula(LTLOperators.Unary.F, ltlF);
        ILTLFormula ltlB = new LTLAtomicProposition(b);
        ILTLFormula inifintelyB = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlB));

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula(
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, ltlB))); // should not be true since the new chain in F exists
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should not be true, since flow A->D->B
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, ltlB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, ltlB)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true since the infinitely B is the last place of the run and it is the whole time stuttering
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        LTLAtomicProposition ltlD = new LTLAtomicProposition(d);
        formula = new RunLTLFormula( // should not be true since the net is finite and D is not a place of all final markings
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settingsFlowLTL);

        //%%%%%%%%%%%%%%%%%%%%%
        // add a transition such that it is not finite anymore
        net.setName(net.getName() + "_reset");
        Transition restart = net.createTransition("reset");
        net.createFlow(restart, a);
        net.createFlow(restart, c);
        net.createFlow(e, restart);
        net.createFlow(f, restart);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should still be true, since the chains end in B
                new FlowLTLFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
//        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settingsFlowLTL);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should still not be true since the chain in E terminates after one round
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.FALSE, data, settings);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        // let the flows be alive
        net.setName(net.getName() + "_alive");
        net.createTransit(e, restart, a);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunLTLFormula( // should be true since now all apart of the newly created chain in F will be alive in each round
                new FlowLTLFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + formula.toString().replace(" ", "");
        data = new AdamCircuitFlowLTLMCOutputData(outputDir + name + "_init", false, false, true);
        TestModelCheckerTools.checkFlowLTLFormulaWithSeveralSettings(net, formula, ModelCheckingResult.Satisfied.TRUE, data, settings);
    }

}

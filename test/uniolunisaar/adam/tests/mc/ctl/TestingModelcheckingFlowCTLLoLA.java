package uniolunisaar.adam.tests.mc.ctl;

import java.io.File;
import org.testng.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.ctl.flowctl.separate.RunCTLSeparateFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.logic.modelchecking.ctl.ModelCheckerFlowCTL;
import uniolunisaar.adam.logic.parser.logics.flowctl.separate.FlowCTLSeparateParser;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowCTLLoLA {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

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

    public ModelCheckerFlowCTL getModelChecker(String name) {
        FlowCTLLoLAModelcheckingSettings settings = new FlowCTLLoLAModelcheckingSettings(outputDir + name, true);
//        settings.setApproach(ModelCheckingSettings.Approach.PARALLEL_INHIBITOR);
        settings.setApproach(ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR);
        return new ModelCheckerFlowCTL(settings);
    }

    @Test
    void testToyExample() throws Exception {
        PetriNetWithTransits net = new PetriNetWithTransits("testing");
        Place init = net.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = net.createTransition("tB");
        net.createFlow(init, tstolen);
        Place out = net.createPlace("out");
        net.createFlow(tstolen, out);
        net.createInitialTransit(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = net.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = net.createTransition("tC");
        net.createFlow(init3, t2);
        net.createFlow(t2, init3);
        net.createInitialTransit(t2, init3);

        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, true);

        ModelCheckerFlowCTL mc = getModelChecker(net.getName());

        String witnessPath, witnessState;

        RunCTLSeparateFormula formula = FlowCTLSeparateParser.parse(net, "𝔸AF TRUE");
        CTLModelcheckingResult result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = FlowCTLSeparateParser.parse(net, "𝔼EF out");
        result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = FlowCTLSeparateParser.parse(net, "𝔸AF out");
        result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.FALSE); // should be TRUE for concurrency maximality

        formula = FlowCTLSeparateParser.parse(net, "(𝔼EF out AND 𝔼EF inittflB)"); // true
        formula = FlowCTLSeparateParser.parse(net, "(𝔸AF out AND 𝔸AF inittflB)"); // false
        formula = FlowCTLSeparateParser.parse(net, "(𝔼AF out AND 𝔼AF inittflB)"); // why is this true? (hm it's really OK, since I chose the flow chain with the E and branch then, really have to think about that)
//        formula = FlowCTLSeparateParser.parse(net, "All EF inittflB"); // why is this true? (hm it's really OK, since I chose the flow chain with the E and branch then, really have to think about that)
        result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%% TODO: Problem with fairness, how should those be translated to the transitions in the mc net?
//        PetriNetExtensionHandler.setWeakFair(tstolen);
//        result = mc.check(net, formula);
////        Logger.getInstance().addMessage("ERROR:");
////        Logger.getInstance().addMessage(result.getLolaError());
////        Logger.getInstance().addMessage("OUTPUT:");
////        Logger.getInstance().addMessage(result.getLolaOutput());
//        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
//        witnessState = result.getWitnessState();
//        if (witnessState != null) {
//            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
//        }
//        witnessPath = result.getWitnessPath();
//        if (witnessPath != null) {
//            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
//        }
//        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//
//        formula = FlowCTLSeparateParser.parse(net, "𝔸A(inittfl U out)");
//        result = mc.check(net, formula);
////        Logger.getInstance().addMessage("ERROR:");
////        Logger.getInstance().addMessage(result.getLolaError());
////        Logger.getInstance().addMessage("OUTPUT:");
////        Logger.getInstance().addMessage(result.getLolaOutput());
//        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
//        witnessState = result.getWitnessState();
//        if (witnessState != null) {
//            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
//        }
//        witnessPath = result.getWitnessPath();
//        if (witnessPath != null) {
//            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
//        }
//        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    void testFirstExamplePaper() throws Exception {
        final String path = System.getProperty("examplesfolder") + "/forallsafety/firstExamplePaper/";
        PetriNetWithTransits net = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        Transition start = net.createTransition("startFlow");
        Place sys = net.getPlace("Sys");
        net.createFlow(start, sys);
        net.createFlow(sys, start);
        net.createInitialTransit(start, sys);
        net.createTransit(sys, start, sys);
        net.createTransit(sys, net.getTransition("t11"), net.getPlace("AA"));
        net.createTransit(sys, net.getTransition("t22"), net.getPlace("BB"));
        net.createTransit(net.getPlace("AA"), net.getTransition("tbad3"), net.getPlace("qbad"));
        net.createTransit(net.getPlace("AA"), net.getTransition("tbad1"), net.getPlace("qbad"));
        net.createTransit(net.getPlace("BB"), net.getTransition("tbad2"), net.getPlace("qbad"));
        net.createTransit(net.getPlace("BB"), net.getTransition("tbad4"), net.getPlace("qbad"));
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), new PetriNetWithTransits(net), false);

        ModelCheckerFlowCTL mc = getModelChecker(net.getName());

        String witnessPath, witnessState;

        RunCTLSeparateFormula formula = FlowCTLSeparateParser.parse(net, "(𝔼EG (AA -> AG NEG qbad) AND 𝔼EG (BB -> AG NEG qbad))");
        CTLModelcheckingResult result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = FlowCTLSeparateParser.parse(net, "𝔸AG((AA -> AG NEG qbad) AND (BB -> AG NEG qbad))");
        result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
    }
//
//    @Test(enabled = true)
//    public void testBurglar() throws ParseException, IOException, RenderException, InterruptedException, NotSubstitutableException, ExternalToolException, ProcessNotStartedException {
//        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
//        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "burglar.apt"));
//        PNWTTools.savePnwt2PDF(outputDir + pn.getName(), new PetriNetWithTransits(pn), false);
//
//        LTLFormula f = new LTLFormula(LTLOperators.Unary.G,
//                new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbadA"))),
//                        LTLOperators.Binary.AND,
//                        new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbadB")))
//                ));
//
//        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings();
//
//        settings.setMaximality(Maximality.MAX_NONE); // since it is done by hand
//        settings.setSemantics(TransitionSemantics.INGOING);
//        // test previous
//        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
////        CounterExample cex = PetriNetModelChecker.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), data);
//        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), false, false);
//
//        settings.setOutputData(data);
//        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
//        LTLModelCheckingResult check = mc.check(pn, maxf);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
//
//        //test next
//        settings.setSemantics(TransitionSemantics.OUTGOING);
//        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
////        cex = PetriNetModelChecker.check(pn, FlowLTLTransformerHyperLTL.toMCHyperFormat(maxf), "./" + pn.getName(), false);    
//        check = mc.check(pn, maxf);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
//    }
}

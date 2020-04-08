package uniolunisaar.adam.modelchecker.ctl;

import java.io.File;
import org.testng.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.logic.modelchecking.ctl.ModelCheckerFlowCTL;
import uniolunisaar.adam.logic.parser.logics.flowctl.FlowCTLParser;
import uniolunisaar.adam.tools.Logger;

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

        FlowCTLLoLAModelcheckingSettings settings = new FlowCTLLoLAModelcheckingSettings(outputDir + net.getName(), true);
        settings.setApproach(ModelCheckingSettings.Approach.PARALLEL_INHIBITOR);
        ModelCheckerFlowCTL mc = new ModelCheckerFlowCTL(settings);

        String witnessPath, witnessState;

        RunCTLFormula formula = FlowCTLParser.parse(net, "AF TRUE");
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

        formula = FlowCTLParser.parse(net, "EF out");
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

        formula = FlowCTLParser.parse(net, "AF out");
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

        PetriNetExtensionHandler.setWeakFair(tstolen);
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

        formula = FlowCTLParser.parse(net, "A(inittfl U out)");
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
    }

//    @Test(enabled = true)
//    void testFirstExamplePaper() throws ParseException, IOException, InterruptedException, NotSubstitutableException, ProcessNotStartedException, ExternalToolException {
//        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
//        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
//        PNWTTools.savePnwt2PDF(outputDir + pn.getName(), new PetriNetWithTransits(pn), false);
//
//        AdamCircuitLTLMCSettings settings = new AdamCircuitLTLMCSettings();
//        settings.setMaximality(Maximality.MAX_NONE); // since it is done by hand
//        settings.setSemantics(TransitionSemantics.INGOING);
//
//        LTLFormula f = new LTLFormula(LTLOperators.Unary.F, new LTLFormula(new LTLAtomicProposition(pn.getPlace("A")), LTLOperators.Binary.OR, new LTLAtomicProposition(pn.getPlace("B"))));
//        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(outputDir + pn.getName(), true, true);
//
//        settings.setOutputData(data);
//        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
//        LTLModelCheckingResult check = mc.check(pn, f);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
//
//        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbad"))));
//        f = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        check = mc.check(pn, f);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
//
//        LTLFormula bothA = new LTLFormula(
//                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("A"))),
//                LTLOperators.Binary.AND,
//                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("AA")))
//        );
//        LTLFormula bothB = new LTLFormula(
//                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("B"))),
//                LTLOperators.Binary.AND,
//                new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(pn.getPlace("BB")))
//        );
//        f = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(pn.getPlace("qbad"))));
//        f = new LTLFormula(new LTLFormula(bothA, LTLOperators.Binary.OR, bothB), LTLOperators.Binary.IMP, f);
//
//        // test previous
//        LTLFormula maxf = new LTLFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        check = mc.check(pn, maxf);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
//        // test next
//        settings.setSemantics(TransitionSemantics.OUTGOING);
//        maxf = new LTLFormula(FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(pn), LTLOperators.Binary.IMP, f);
//        Assert.assertEquals(check.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
//    }
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

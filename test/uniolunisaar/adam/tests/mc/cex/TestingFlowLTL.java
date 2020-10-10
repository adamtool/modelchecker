package uniolunisaar.adam.tests.mc.cex;

import java.io.File;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tests.mc.util.TestModelCheckerTools;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingFlowLTL {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static final String outDir = outputDir + "cex/";

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
        (new File(outDir)).mkdirs();
    }

    AdamCircuitFlowLTLMCSettings[] settings;

    @BeforeMethod
    public void initMCSettings() {
//        settings = { };
        settings = new AdamCircuitFlowLTLMCSettings[]{
            //                        TestModelCheckerTools.mcSettings_Seq_IntF, TestModelCheckerTools.mcSettings_Seq_IntC,
            //                        TestModelCheckerTools.mcSettings_SeqI_IntF, TestModelCheckerTools.mcSettings_SeqI_IntC,
            //            TestModelCheckerTools.mcSettings_Par_IntF, TestModelCheckerTools.mcSettings_Par_IntC,
            TestModelCheckerTools.mcSettings_ParI_IntF, TestModelCheckerTools.mcSettings_ParI_IntC
        //            TestModelCheckerTools.mcSettings_Par_N, TestModelCheckerTools.mcSettings_ParI_N
//            TestModelCheckerTools.mcSettings_ParI_IntC, //            TestModelCheckerTools.mcSettings_Seq_IntF
//                                    TestModelCheckerTools.mcSettings_SeqI_IntC, //            TestModelCheckerTools.mcSettings_Seq_IntF
        //              TestModelCheckerTools.mcSettings_Par_IntF,  TestModelCheckerTools.mcSettings_ParI_IntF
        };
    }

    @Test
    public void exampleToolPaper() throws Exception {
        PetriNetWithTransits net = new PetriNetWithTransits("toolPaper");
        Place in = net.createPlace("in");
        in.setInitialToken(1);
        Place out = net.createPlace("out");
        out.setInitialToken(1);
        Transition init = net.createTransition("s");
        Transition t = net.createTransition("t");
        net.createFlow(init, in);
        net.createFlow(in, init);
        net.createFlow(in, t);
        net.createFlow(t, in);
        net.createFlow(t, out);
        net.createFlow(out, t);
        net.createTransit(out, t, out);
        net.createTransit(in, t, out);
        net.createTransit(in, init, in);
        net.createInitialTransit(init, in);
        net.setWeakFair(t);

        PNWTTools.savePnwt2PDF(outDir + net.getName(), net, false);
        PNWTTools.saveAPT(outDir + net.getName(), net, false,false);

        String formula = "A  out";
        RunLTLFormula f = FlowLTLParser.parse(net, formula);

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + net.getName() + "data", false, false, true);

        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(
                data,
//                ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
                ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
                AdamCircuitMCSettings.Maximality.MAX_INTERLEAVING,
                AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
                CircuitRendererSettings.TransitionSemantics.OUTGOING,
                CircuitRendererSettings.TransitionEncoding.LOGARITHMIC,
                CircuitRendererSettings.AtomicPropositions.PLACES_AND_TRANSITIONS,
                AigerRenderer.OptimizationsSystem.NONE,
                AigerRenderer.OptimizationsComplete.NONE,
                //                ModelCheckerMCHyper.VerificationAlgo.INT,                
                Abc.VerificationAlgo.IC3);

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);
        LTLModelCheckingResult ret = mc.check(net, f);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        Logger.getInstance().addMessage(ret.getCex().toString());
        ReducedCounterExample cex = new ReducedCounterExample(net, ret.getCex(), false);
        List<List<Place>> markingSequence = cex.getMarkingSequence();
        List<Transition> firingSequence = cex.getFiringSequence();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < firingSequence.size(); i++) {
            Transition transition = firingSequence.get(i);
            List<Place> marking = markingSequence.get(i);
            sb.append(marking.toString().replace("[", "{").replace("]", "}")).append(" [").append(transition.getId()).append("> ");
        }
        Logger.getInstance().addMessage(sb.toString());
    }
}

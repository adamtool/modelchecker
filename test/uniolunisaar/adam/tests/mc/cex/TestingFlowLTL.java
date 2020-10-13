package uniolunisaar.adam.tests.mc.cex;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.DataFlowChain;
import uniolunisaar.adam.ds.petrinetwithtransits.DataFlowTree;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallelInhibitor;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNSequentialInhibitor;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNParInhibitorNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNParallelNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNSeqInhibitorNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNSequentialNoInit;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tests.mc.util.TestModelCheckerTools;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.MCTools;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

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
        PNWTTools.saveAPT(outDir + net.getName(), net, false, false);

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
        PetriNet mcNet = TestingFlowLTL.getModelCheckingNet(net, f, settings);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        Logger.getInstance().addMessage(ret.getCex().toString());

        ReducedCounterExample cex = new ReducedCounterExample(net, ret.getCex(), true);
        List<List<Place>> markingSequence = cex.getMarkingSequence();
        List<Transition> firingSequence = cex.getFiringSequence();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < firingSequence.size(); i++) {
            Transition transition = firingSequence.get(i);
            List<Place> marking = markingSequence.get(i);
            sb.append(marking.toString().replace("[", "{").replace("]", "}")).append(" [").append(transition.getId()).append("> ");
        }
        Logger.getInstance().addMessage(sb.toString());
        Logger.getInstance().addMessage(cex.toString());

        Logger.getInstance().addMessage("detailed:");
        ReducedCounterExample cexDetailed = new ReducedCounterExample(mcNet, ret.getCex(), true);
        List<List<Place>> markingSequenceDetailed = cexDetailed.getMarkingSequence();
        List<Transition> firingSequenceDetailed = cexDetailed.getFiringSequence();
        sb = new StringBuilder();
        for (int i = 0; i < firingSequenceDetailed.size(); i++) {
            Transition transition = firingSequenceDetailed.get(i);
            List<Place> marking = markingSequenceDetailed.get(i);
            sb.append("{");
            for (Place place : marking) {
                sb.append(place.getId()).append(",");
            }
            if (!marking.isEmpty()) {
                sb.replace(sb.length() - 1, sb.length(), "}");
            } else {
                sb.append("}");
            }

            sb.append(" [").append(transition.getId()).append("> ");
        }
        Logger.getInstance().addMessage(sb.toString());
        Logger.getInstance().addMessage("TOSTRING");
        Logger.getInstance().addMessage(cexDetailed.toString());
    }

    @Test
    public void dataFlowWitness() throws Exception {
        PetriNetWithTransits pnwt = ToyExamples.createIntroductoryExample();
        pnwt.removeInitialTransit(pnwt.getPlace("B"));
        pnwt.removeInitialTransit(pnwt.getPlace("a"));
        pnwt.getPlace("a").setInitialToken(0);
        Transition t = pnwt.createTransition("initTr");
        Place init = pnwt.createPlace("initPl");
        init.setInitialToken(1);
        pnwt.createFlow(init, t);
        pnwt.createFlow(t, pnwt.getPlace("a"));
        pnwt.createInitialTransit(t, pnwt.getPlace("a"));
        PNWTTools.savePnwt2PDF(outDir + pnwt.getName(), pnwt, false);
        PNWTTools.saveAPT(outDir + pnwt.getName(), pnwt, false, false);

//        String formula = "A  F";
//        RunLTLFormula f = FlowLTLParser.parse(pnwt, formula);
        RunLTLFormula f1 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(pnwt.getPlace("F")))); // should be false
        RunLTLFormula f2 = new RunLTLFormula(new FlowLTLFormula(new LTLAtomicProposition(pnwt.getPlace("a")))); // should be false
//        RunLTLFormula f = new RunLTLFormula(f1, RunOperators.Binary.AND, f2); // should be false
//        RunLTLFormula f = new RunLTLFormula(new FlowLTLFormula(new LTLFormula(new LTLAtomicProposition(pnwt.getPlace("F")), LTLOperators.Binary.OR, new LTLAtomicProposition(pnwt.getPlace("a"))))); // should be true
        RunLTLFormula f = new RunLTLFormula(f1, RunOperators.Binary.OR, f2); // should be false

        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(outputDir + pnwt.getName() + "data", false, false, true);

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
        LTLModelCheckingResult ret = mc.check(pnwt, f);
        PetriNet mcNet = TestingFlowLTL.getModelCheckingNet(pnwt, f, settings);
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
        Logger.getInstance().addMessage(ret.getCex().toString());

        ReducedCounterExample cex = new ReducedCounterExample(pnwt, ret.getCex(), false);
        Logger.getInstance().addMessage(cex.toString());
        ReducedCounterExample cexDetailed = new ReducedCounterExample(mcNet, ret.getCex(), true);
        Logger.getInstance().addMessage("Detailed:" + cexDetailed.toString());

        // data flow trees
        List<DataFlowTree> dataFlowTrees = PNWTTools.getDataFlowTrees(pnwt, cex.getFiringSequence());
        PNWTTools.saveDataFlowTreesToPDF(outputDir + pnwt.getName() + "_dft", dataFlowTrees, PetriNetExtensionHandler.getProcessFamilyID(pnwt));

        Map<Integer, DataFlowChain> witnessDataFlowChains = MCTools.getWitnessDataFlowChains(pnwt, cexDetailed);
        Logger.getInstance().addMessage(witnessDataFlowChains.toString());
    }

    public static PetriNet getModelCheckingNet(PetriNetWithTransits net, RunLTLFormula f, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
        if ((f.getPhi() instanceof ILTLFormula)) {
            return net;
        }
        ModelCheckingSettings.Approach approach = settings.getApproach();
        CircuitRendererSettings.TransitionSemantics semantics = settings.getRendererSettings().getSemantics();
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(f);
        PetriNet netMC;
        switch (approach) {
            case PARALLEL:
                if (flowFormulas.size() > 1) {
                    throw new NotConvertableException("The parallel approach (without inhibitor arcs) is not implemented for more than one flow subformula!."
                            + " Please use another approach.");
                }
                if (semantics == CircuitRendererSettings.TransitionSemantics.INGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNParallelNoInit.createNet4ModelCheckingParallelOneFlowFormula(net);
                } else if (semantics == CircuitRendererSettings.TransitionSemantics.OUTGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
                } else {
                    throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                }
                break;
            case PARALLEL_INHIBITOR:
                if (semantics == CircuitRendererSettings.TransitionSemantics.INGOING) {
                    if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                        netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallelOneFlowFormula(net);
                    } else {
                        netMC = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallel(net, flowFormulas.size());
                    }
                } else if (semantics == CircuitRendererSettings.TransitionSemantics.OUTGOING) {
                    if (flowFormulas.size() == 1) { // take the special case (todo: check if this has any advantages compared to the general one)
                        netMC = PnwtAndNbFlowFormulas2PNParallelInhibitor.createNet4ModelCheckingParallelOneFlowFormula(net);
                    } else {
                        netMC = PnwtAndNbFlowFormulas2PNParallelInhibitor.createNet4ModelCheckingParallel(net, flowFormulas.size());
                    }
                } else {
                    throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                }
                break;
            case SEQUENTIAL: {
                if (semantics == CircuitRendererSettings.TransitionSemantics.INGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNSequentialNoInit.createNet4ModelCheckingSequential(net, flowFormulas.size());
                } else if (semantics == CircuitRendererSettings.TransitionSemantics.OUTGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNSequential.createNet4ModelCheckingSequential(net, flowFormulas.size(), true);
                } else {
                    throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                }
                break;
            }
            case SEQUENTIAL_INHIBITOR:
                if (semantics == CircuitRendererSettings.TransitionSemantics.INGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNSeqInhibitorNoInit.createNet4ModelCheckingSequential(net, flowFormulas.size());
                } else if (semantics == CircuitRendererSettings.TransitionSemantics.OUTGOING) {
                    netMC = PnwtAndNbFlowFormulas2PNSequentialInhibitor.createNet4ModelCheckingSequential(net, flowFormulas.size(), true);
                } else {
                    throw new RuntimeException("The transitions semantics: '" + settings.getRendererSettings().getSemantics() + "' is not yet implemented.");
                }
                break;
            default:
                throw new RuntimeException("Didn't provide a solution for all approaches yet. Approach '" + approach + "' is missing; sry.");
        }
        MCTools.addCoordinates(net, netMC);
        return netMC;
    }
}

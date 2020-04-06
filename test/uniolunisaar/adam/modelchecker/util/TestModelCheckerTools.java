package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.LogicsTools;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;

/**
 *
 * @author Manuel Gieseking
 */
public class TestModelCheckerTools {

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Seq_N = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL,
            Maximality.MAX_NONE,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Seq_IntF = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL,
            Maximality.MAX_INTERLEAVING,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Seq_IntC = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL,
            Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_SeqI_N = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL,
            Maximality.MAX_NONE,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_SeqI_IntF = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
            Maximality.MAX_INTERLEAVING,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_SeqI_IntC = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR,
            Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Par_N = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL,
            Maximality.MAX_NONE,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Par_IntF = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL,
            Maximality.MAX_INTERLEAVING,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            //            AdamCircuitMCSettings.Stuttering.ERROR_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_Par_IntC = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL,
            Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_ParI_N = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
            Maximality.MAX_NONE,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_ParI_IntF = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
            Maximality.MAX_INTERLEAVING,
            //            AdamCircuitMCSettings.Stuttering.ERROR_REGISTER,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static final AdamCircuitFlowLTLMCSettings mcSettings_ParI_IntC = new AdamCircuitFlowLTLMCSettings(
            LogicsTools.TransitionSemantics.OUTGOING,
            ModelCheckingSettings.Approach.PARALLEL_INHIBITOR,
            Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
            AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER,
            AigerRenderer.OptimizationsSystem.NONE,
            AigerRenderer.OptimizationsComplete.NONE,
            true,
            Abc.VerificationAlgo.IC3);

    public static void checkFlowLTLFormulaWithSeveralSettings(PetriNetWithTransits pnwt, RunLTLFormula f, ModelCheckingResult.Satisfied expectedResult, AdamCircuitFlowLTLMCOutputData data, AdamCircuitFlowLTLMCSettings... settings) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, NotConvertableException, ExternalToolException {
        int i = 0;
        for (AdamCircuitFlowLTLMCSettings setting : settings) {
            data.setPath(data.getPath() + "_" + (i++));
            setting.setOutputData(data);
            checkFlowLTLFormulaOneSetting(pnwt, f, expectedResult, setting);
        }
    }

    public static void checkFlowLTLFormulaWithSeveralSettings(PetriNetWithTransits pnwt, RunLTLFormula f, ModelCheckingResult.Satisfied expectedResult, ModelCheckingSettings... settings) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, NotConvertableException, ExternalToolException {
        for (ModelCheckingSettings setting : settings) {
            checkFlowLTLFormulaOneSetting(pnwt, f, expectedResult, setting);
        }
    }

    private static void checkFlowLTLFormulaOneSetting(PetriNetWithTransits pnwt, RunLTLFormula f, ModelCheckingResult.Satisfied expectedResult, ModelCheckingSettings setting) throws InterruptedException, IOException, ParseException, ProcessNotStartedException, NotConvertableException, ExternalToolException {
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(setting);
        ModelCheckingResult ret = mc.check(pnwt, f);
        Assert.assertEquals(ret.getSatisfied(), expectedResult);
        if (ret.getSatisfied() == ModelCheckingResult.Satisfied.FALSE) {
            Logger.getInstance().addMessage(ret.getCex().toString());
        }
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, String formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, resMaxInterleaving, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, RunLTLFormula formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, formula, path, Maximality.MAX_INTERLEAVING, resMaxInterleaving);
        testModelCheckerFlowLTL(net, formula, path, Maximality.MAX_CONCURRENT, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, String formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, RunLTLFormula formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings();
        settings.setMaximality(max);

        ModelCheckingResult.Satisfied sat = (result) ? ModelCheckingResult.Satisfied.TRUE : ModelCheckingResult.Satisfied.FALSE;
        ModelCheckingResult check;
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(path, false, false, true);

        settings.setOutputData(data);
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(settings);

        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        settings.setApproach(Approach.SEQUENTIAL);
        settings.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        settings.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        settings.setApproach(Approach.PARALLEL);
        settings.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        settings.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
    }

    public static void testModelCheckerLTL(PetriNetWithTransits net, String formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws ParseException, InterruptedException, IOException, NotSubstitutableException, ExternalToolException, ProcessNotStartedException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, resMaxStandard, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriNetWithTransits net, ILTLFormula formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws InterruptedException, IOException, NotSubstitutableException, ParseException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerLTL(net, formula, path, Maximality.MAX_INTERLEAVING, resMaxStandard);
        testModelCheckerLTL(net, formula, path, Maximality.MAX_CONCURRENT, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriNetWithTransits net, String formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotSubstitutableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerLTL(PetriNetWithTransits net, ILTLFormula formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, NotSubstitutableException, ParseException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings();
        ModelCheckerLTL mc = new ModelCheckerLTL(settings);
        settings.setMaximality(max);

        ModelCheckingResult.Satisfied sat = (result) ? ModelCheckingResult.Satisfied.TRUE : ModelCheckingResult.Satisfied.FALSE;
        ModelCheckingResult check;
        AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(path, false, false, true);
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        settings.setSemantics(TransitionSemantics.OUTGOING);
        settings.setOutputData(data);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        settings.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        settings.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        settings.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula);
        Assert.assertEquals(check.getSatisfied(), sat);
    }
}

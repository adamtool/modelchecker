package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings.Approach;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.util.logics.LogicsTools.TransitionSemantics;

/**
 *
 * @author Manuel Gieseking
 */
public class TestModelCheckerTools {

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, String formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, resMaxInterleaving, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, RunFormula formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, formula, path, Maximality.MAX_INTERLEAVING, resMaxInterleaving);
        testModelCheckerFlowLTL(net, formula, path, Maximality.MAX_CONCURRENT, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, String formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerFlowLTL(PetriNetWithTransits net, RunFormula formula, String path, Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
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

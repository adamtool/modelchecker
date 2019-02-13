package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.exception.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndFlowLTLtoCircuit.Approach;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.Maximality;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit.TransitionSemantics;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exception.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;

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
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        mc.setMaximality(max);

        ModelCheckingResult.Satisfied sat = (result) ? ModelCheckingResult.Satisfied.TRUE : ModelCheckingResult.Satisfied.FALSE;
        ModelCheckingResult check;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setApproach(Approach.SEQUENTIAL);
        mc.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        mc.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setApproach(Approach.PARALLEL);
        mc.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        mc.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula, path, true);
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
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(max);

        ModelCheckingResult.Satisfied sat = (result) ? ModelCheckingResult.Satisfied.TRUE : ModelCheckingResult.Satisfied.FALSE;
        ModelCheckingResult check;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        mc.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setSemantics(TransitionSemantics.OUTGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
        //%%%% previous semantics
        mc.setSemantics(TransitionSemantics.INGOING);
        check = mc.check(net, formula, path, true);
        Assert.assertEquals(check.getSatisfied(), sat);
    }
}

package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.exceptions.NotSubstitutableException;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;

/**
 *
 * @author Manuel Gieseking
 */
public class TestModelCheckerTools {

    public static void testModelCheckerFlowLTL(PetriGame net, String formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws ParseException, InterruptedException, IOException, NotConvertableException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, resMaxInterleaving, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, RunFormula formula, String path, boolean resMaxInterleaving, boolean resMaxParallel) throws InterruptedException, IOException, ParseException, NotConvertableException {
        testModelCheckerFlowLTL(net, formula, path, ModelCheckerLTL.Maximality.MAX_INTERLEAVING, resMaxInterleaving);
        testModelCheckerFlowLTL(net, formula, path, ModelCheckerLTL.Maximality.MAX_CONCURRENT, resMaxParallel);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, String formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, RunFormula formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotConvertableException {
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        mc.setMaximality(max);

        CounterExample cex;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.SEQUENTIAL);
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.PARALLEL);
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
    }

    public static void testModelCheckerLTL(PetriGame net, String formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws ParseException, InterruptedException, IOException, NotSubstitutableException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, resMaxStandard, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriGame net, ILTLFormula formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws InterruptedException, IOException, NotSubstitutableException, ParseException {
        testModelCheckerLTL(net, formula, path, ModelCheckerLTL.Maximality.MAX_INTERLEAVING, resMaxStandard);
        testModelCheckerLTL(net, formula, path, ModelCheckerLTL.Maximality.MAX_CONCURRENT, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriGame net, String formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException, ParseException, NotSubstitutableException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerLTL(PetriGame net, ILTLFormula formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException, NotSubstitutableException, ParseException {
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(max);

        CounterExample cex;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
    }
}

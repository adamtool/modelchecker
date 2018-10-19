package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL;

/**
 *
 * @author Manuel Gieseking
 */
public class TestModelCheckerTools {

    public static void testModelCheckerFlowLTL(PetriGame net, String formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws ParseException, InterruptedException, IOException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, resMaxStandard, resMaxReisig);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, IRunFormula formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws InterruptedException, IOException {
        testModelCheckerFlowLTL(net, formula, path, ModelCheckerFlowLTL.Maximality.STANDARD, resMaxStandard);
        testModelCheckerFlowLTL(net, formula, path, ModelCheckerFlowLTL.Maximality.REISIG, resMaxReisig);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, String formula, String path, ModelCheckerFlowLTL.Maximality max, boolean result) throws InterruptedException, IOException, ParseException {
        testModelCheckerFlowLTL(net, FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerFlowLTL(PetriGame net, IRunFormula formula, String path, ModelCheckerFlowLTL.Maximality max, boolean result) throws InterruptedException, IOException {
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL();
        mc.setMaximality(max);

        CounterExample cex;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.SEQUENTIAL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.PARALLEL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
    }

    public static void testModelCheckerLTL(PetriGame net, String formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws ParseException, InterruptedException, IOException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, resMaxStandard, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriGame net, ILTLFormula formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws InterruptedException, IOException {
        testModelCheckerLTL(net, formula, path, ModelCheckerLTL.Maximality.STANDARD, resMaxStandard);
        testModelCheckerLTL(net, formula, path, ModelCheckerLTL.Maximality.REISIG, resMaxReisig);
    }

    public static void testModelCheckerLTL(PetriGame net, String formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException, ParseException {
        testModelCheckerLTL(net, (ILTLFormula) FlowLTLParser.parse(net, formula), path, max, result);
    }

    public static void testModelCheckerLTL(PetriGame net, ILTLFormula formula, String path, ModelCheckerLTL.Maximality max, boolean result) throws InterruptedException, IOException {
        ModelCheckerLTL mc = new ModelCheckerLTL();
        mc.setMaximality(max);

        CounterExample cex;
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, result);
    }
}

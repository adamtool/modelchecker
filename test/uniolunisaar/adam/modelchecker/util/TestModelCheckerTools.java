package uniolunisaar.adam.modelchecker.util;

import java.io.IOException;
import org.testng.Assert;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerFlowLTL;

/**
 *
 * @author Manuel Gieseking
 */
public class TestModelCheckerTools {

    public static void testModelChecker(ModelCheckerFlowLTL mc, PetriGame net, String formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws ParseException, InterruptedException, IOException {
        testModelChecker(mc, net, FlowLTLParser.parse(net, formula), path, resMaxStandard, resMaxReisig);
    }

    public static void testModelChecker(ModelCheckerFlowLTL mc, PetriGame net, IRunFormula formula, String path, boolean resMaxStandard, boolean resMaxReisig) throws InterruptedException, IOException {
        CounterExample cex;

        //%%%%%%%%%%%%%%%%%%%%%%% check standard maximality
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setMaximality(ModelCheckerFlowLTL.Maximality.STANDARD);
        mc.setApproach(ModelCheckerFlowLTL.Approach.SEQUENTIAL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxStandard);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxStandard);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.PARALLEL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxStandard);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxStandard);

        //%%%%%%%%%%%%%%%%%%%%%%% check reisig maximality
        //%%%%%%%%%%%% sequential
        //%%%%% next semantics
        mc.setMaximality(ModelCheckerFlowLTL.Maximality.REISIG);
        mc.setApproach(ModelCheckerFlowLTL.Approach.SEQUENTIAL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxReisig);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxReisig);
        //%%%%%%%%%%%% parallel
        //%%%%% next semantics
        mc.setApproach(ModelCheckerFlowLTL.Approach.PARALLEL);
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.NXT);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxReisig);
        //%%%% previous semantics
        mc.setSemantics(ModelCheckerFlowLTL.TransitionSemantics.PREV);
        cex = mc.check(net, formula, path, true);
        Assert.assertEquals(cex == null, resMaxReisig);
    }
}

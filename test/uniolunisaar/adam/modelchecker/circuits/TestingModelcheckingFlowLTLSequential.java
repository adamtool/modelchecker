package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.modelchecking.RedundantNetwork;
import uniolunisaar.adam.generators.modelchecking.ToyExamples;
import uniolunisaar.adam.generators.modelchecking.UpdatingNetwork;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLSequential {

    @Test
    public void checkToyExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = ToyExamples.createFirstExample(false);
        net.setName(net.getName() + "_infinite");
        //add creation of flows
        Place in = net.getPlace("in");
        net.removeInitialTokenflow(in);
        Transition create = net.createTransition("createFlows");
        net.createFlow(in, create);
        net.createFlow(create, in);
        net.createTokenFlow(in, create, in);
        net.createInitialTokenFlow(create, in);
        net.setWeakFair(net.getTransition("t"));

        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_NONE,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER_MAX_INTERLEAVING,
                true);

        // check standard maximality
        // + sequential
        // + next semantics
        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);

        // check standard maximality
        // + sequential
        // + next semantics
        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret); // here is an error when I do the maximality before the transformation of the formula and not afterwards (it is null)
        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), false);
//        Assert.assertNotNull(ret);

        // check standard maximality
        // + sequential
        // + next semantics
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName() + "_" + formula, net, false);
        AdamTools.savePG2PDF(net.getName() + "_" + formula, net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), true);
//        Assert.assertNull(ret);

        // check to flow formulas
        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        f = new RunFormula(f, RunOperators.Binary.OR, f);
        ret = mc.check(net, f, "./" + net.getName() + "_" + f.toString(), true);
        Assert.assertNull(ret);

    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true) // takes to long for MCHyper
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void updatingNetworkExampleMaxInterleavingInCircuit() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_NONE,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER_MAX_INTERLEAVING,
                true);

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_NONE,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER_MAX_INTERLEAVING,
                true);

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret); // error is null (possibly because of the fairness yields in every case false?)

        // not tested
        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
    }

    @Test(enabled = true)
    public void redundantFlowExampleMaxInterleavingInCircuit() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_NONE,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER_MAX_INTERLEAVING,
                true);

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = ModelCheckerFlowLTL.checkWithSequentialApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = ModelCheckerFlowLTL.checkWithSequentialApproach(net, f, "./" + net.getName(), true);
    }

}

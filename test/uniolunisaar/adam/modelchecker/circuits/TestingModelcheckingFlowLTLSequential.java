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
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
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

    @Test(enabled = true)
    public void introducingExampleTransitions() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = new PetriGame("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTokenflow(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTokenflow(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition t1 = net.createTransition("o1");
        Transition t2 = net.createTransition("o2");
        net.createFlow(a, t1);
        net.createFlow(b, t1);
        net.createFlow(t1, d);
        net.createFlow(c, t2);
        net.createFlow(d, t2);
        net.createFlow(t2, e);
        net.createFlow(t2, f);
        net.createFlow(t2, b);
        net.createTokenFlow(a, t1, d);
        net.createTokenFlow(b, t1, d);
        net.createTokenFlow(d, t2, e, b);
        net.createInitialTokenFlow(t2, f);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        RunFormula formula;
        CounterExample ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new AtomicProposition(t1)); // should not hold since t2 generates a new one which directly dies
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
//        Assert.assertNotNull(ret); // error is null
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
//        Assert.assertNotNull(ret); // error is null

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new AtomicProposition(t2)); // should not hold since the flows starting in A and B
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void introducingExamplePlaces() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = new PetriGame("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTokenflow(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTokenflow(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition t1 = net.createTransition("o1");
        Transition t2 = net.createTransition("o2");
        net.createFlow(a, t1);
        net.createFlow(b, t1);
        net.createFlow(t1, d);
        net.createFlow(c, t2);
        net.createFlow(d, t2);
        net.createFlow(t2, e);
        net.createFlow(t2, f);
        net.createFlow(t2, b);
        net.createTokenFlow(a, t1, d);
        net.createTokenFlow(b, t1, d);
        net.createTokenFlow(d, t2, e, b);
        net.createInitialTokenFlow(t2, f);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        RunFormula formula;
        CounterExample ret;

        //%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new FlowFormula(new AtomicProposition(e))); // should not be true

        // check in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // check interleaving in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        ILTLFormula ltlE = new AtomicProposition(e);
        ILTLFormula finallyE = new LTLFormula(LTLOperators.Unary.F, ltlE);
        ILTLFormula ltlF = new AtomicProposition(f);
        ILTLFormula finallyF = new LTLFormula(LTLOperators.Unary.F, ltlF);
        ILTLFormula ltlB = new AtomicProposition(b);
        ILTLFormula inifintelyB = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlB));

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, ltlB))); // should not be true since the new chain in F exists
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should not be true, since flow A->D->B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, ltlB))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, ltlB)))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since the infinitely B is the last place of the run and it is the whole time stuttering
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        AtomicProposition ltlD = new AtomicProposition(d);
        formula = new RunFormula( // should not be true since the net is finite and D is not a place of all final markings
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        //%%%%%%%%%%%%%%%%%%%%%
        // add a transition such that it is not finite anymore
        Transition restart = net.createTransition("reset");
        net.createFlow(restart, a);
        net.createFlow(restart, c);
        net.createFlow(e, restart);
        net.createFlow(f, restart);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still be true, since the chains end in B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still not be true since the chain in E terminates after one round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        // let the flows be alive
        net.createTokenFlow(e, restart, a);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since now all apart of the newly created chain in F will be alive in each round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);
        // check in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true)
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

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);

        // check interleaving in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // check interleaving in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);

        // check maximal initerleaving in the circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret); // here is an error when I do the maximality before the transformation of the formula and not afterwards (it is null)

        // check interleaving in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), false);
//        Assert.assertNotNull(ret);
        // check standard maximality
        // + sequential
        // + next semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName() + "_" + formula, net, false);
        AdamTools.savePG2PDF(net.getName() + "_" + formula, net, false);

        // Check maximality in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // check it outside
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), true);
//        Assert.assertNull(ret);
        // check to flow formulas
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
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

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);

        // in maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
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

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        RunFormula f;
        CounterExample ret;

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
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

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                true);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
//        ret = mc.check(net, f, "./" + net.getName(), true);
//        Assert.assertNotNull(ret); // error mchyper creates redundant names for the inputs of the circuit

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);// error mchyper creates redundant names for the inputs of the circuit

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret); // error is null (possibly because of the fairness yields in every case false?)

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        // maximality in formula
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

}

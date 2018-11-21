package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLSequential {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static final String outputDirInCircuit = outputDir + "sequential/max_in_circuit/";
    private static final String outputDirInFormula = outputDir + "sequential/max_in_formula/";

    @BeforeClass
    public void createFolder() {
        Logger.getInstance().setVerbose(true);
        Logger.getInstance().addMessageStream(ModelCheckerMCHyper.LOGGER_ABC_OUT, System.out);

        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();
    }

    @Test(enabled = true)
    public void introducingExampleTransitions() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
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
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        RunFormula formula;
        String name;
        ModelCheckingResult ret;

        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);

        // %%%%%%%%%%%%%%%%%%%%%%%%%    
        RunFormula a1 = new RunFormula(new FlowFormula(new AtomicProposition(t1)));
        RunFormula a2 = new RunFormula(new FlowFormula(new AtomicProposition(t2)));
        formula = new RunFormula(a1, RunOperators.Binary.OR, a2);
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new AtomicProposition(t1)); // should  hold since we test it on the run and there is no other transition enabled and we demand maximality
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        // check in circuit
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new AtomicProposition(t2)); // should not hold since the flows starting in A and B
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new FlowFormula(new AtomicProposition(t1))); // should not hold since t2 generates a new one which directly dies
        name = net.getName() + "_" + formula.toString().replace(" ", "");
        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void introducingExamplePlaces() throws IOException, RenderException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
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
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        RunFormula formula;
        String name;
        ModelCheckingResult ret;

        //%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula(new FlowFormula(new AtomicProposition(e))); // should not be true
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);

        // check interleaving in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

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
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);

        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should not be true, since flow A->D->B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, ltlB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.F, ltlB)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret); // todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since the infinitely B is the last place of the run and it is the whole time stuttering
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);// todo: it's a problem since when not init in the first step we could chose to consider the given chain, after the chain has started.

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        AtomicProposition ltlD = new AtomicProposition(d);
        formula = new RunFormula( // should not be true since the net is finite and D is not a place of all final markings
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        //%%%%%%%%%%%%%%%%%%%%%
        // add a transition such that it is not finite anymore
        net.setName(net.getName() + "_reset");
        Transition restart = net.createTransition("reset");
        net.createFlow(restart, a);
        net.createFlow(restart, c);
        net.createFlow(e, restart);
        net.createFlow(f, restart);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still be true, since the chains end in B
                new FlowFormula(
                        new LTLFormula(finallyE, LTLOperators.Binary.OR, new LTLFormula(finallyF, LTLOperators.Binary.OR, inifintelyB))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should still not be true since the chain in E terminates after one round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + "_" + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNotNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        // let the flows be alive
        net.setName(net.getName() + "_alive");
        net.createTokenFlow(e, restart, a);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%%%%%%
        formula = new RunFormula( // should be true since now all apart of the newly created chain in F will be alive in each round
                new FlowFormula(
                        new LTLFormula(finallyF, LTLOperators.Binary.OR, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, ltlD)))));
        name = net.getName() + formula.toString().replace(" ", "");

        // check in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, formula, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInCircuit + name, false);
//        Assert.assertNull(ret);
        // check in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, formula, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, formula, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void checkToyExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
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

        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        String name;
        RunFormula f;
        ModelCheckingResult ret;

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // check interleaving in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // check interleaving in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // check maximal initerleaving in the circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL_INHIBITOR,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // check interleaving in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), false);
//        Assert.assertNotNull(ret);
        // check standard maximality
        // + sequential
        // + next semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        net = ToyExamples.createFirstExample(false);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamTools.saveAPT(outputDir + net.getName() + "_" + formula, net, false);
        AdamTools.savePG2PDF(outputDir + net.getName() + "_" + formula, net, false);

        // Check maximality in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // check it outside
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // previous semantics
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.INGOING);
//        ret = mc.check(net, f, "./" + net.getName(), true);
//        Assert.assertNull(ret);
        // check to flow formulas
//        mc.setSemantics(ModelCheckerLTL.TransitionSemantics.OUTGOING);
        f = new RunFormula(f, RunOperators.Binary.OR, f);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        mc.setInitFirst(true);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        String name;
        RunFormula f;
        ModelCheckingResult ret;

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // in maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A(F(out)";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A(F(p3)";
        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);
    }

    @Test(enabled = true)
    public void redundantFlowExampleFix() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        RunFormula f = new RunFormula(
                new LTLFormula(LTLOperators.Unary.NEG,
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.F,
                                        new LTLFormula(new AtomicProposition(net.getTransition("tupD")), LTLOperators.Binary.OR, new AtomicProposition(net.getTransition("tupU")))
                                )
                        )
                ), RunOperators.Implication.IMP,
                new FlowFormula(
                        new LTLFormula(LTLOperators.Unary.F,
                                new AtomicProposition(net.getPlace("out"))
                        ))
        );
        System.out.println(f.toString());
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);

        ModelCheckingResult ret = mc.check(net, f, outputDirInCircuit + net.getName() + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test(enabled = true)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = RedundantNetwork.getBasis(1, 1);
        AdamTools.saveAPT(outputDir + net.getName(), net, false);
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        String formula;
        RunFormula f;
        ModelCheckingResult ret;
        String name;

        formula = "A(F(out)";

        f = FlowLTLParser.parse(net, formula);
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                ModelCheckerLTL.TransitionSemantics.OUTGOING,
                ModelCheckerFlowLTL.Approach.SEQUENTIAL,
                ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT,
                ModelCheckerLTL.Stuttering.PREFIX_REGISTER,
                ModelCheckerMCHyper.VerificationAlgo.IC3,
                true);

        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);

        net = RedundantNetwork.getUpdatingStillNotFixedMutexNetwork(1, 1);
        name = net.getName() + "_" + f.toString().replace(" ", "");
        AdamTools.savePG2PDF(outputDir + net.getName(), net, false);

        // %%%%%%%%%%%%%%%%%%%%% new net maximality in circuit
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT);
        ret = mc.check(net, f, outputDirInCircuit + name + "_init", true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
        // maximality in formula
        mc.setInitFirst(true);
        mc.setMaximality(ModelCheckerLTL.Maximality.MAX_INTERLEAVING);
        ret = mc.check(net, f, outputDirInFormula + name + "_init", false);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
//        // without init
//        mc.setInitFirst(false);
//        ret = mc.check(net, f, outputDirInFormula + name, false);
//        Assert.assertNotNull(ret);
    }

    @Test
    public void testNetwork() {

    }

}

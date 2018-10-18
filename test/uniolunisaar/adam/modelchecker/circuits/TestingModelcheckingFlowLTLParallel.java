package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.modelchecking.RedundantNetwork;
import uniolunisaar.adam.generators.modelchecking.ToyExamples;
import uniolunisaar.adam.generators.modelchecking.UpdatingNetwork;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreatorPrevSemantics;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerParallel;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLParallel {

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @Test
    void testByJesko() throws RenderException, InterruptedException, IOException, ParseException, NotSupportedGameException {
        PetriNet net = new PetriNet("jesko");
        Place init = net.createPlace("in");
        init.setInitialToken(1);
        Transition t1 = net.createTransition("t1");
        net.createFlow(init, t1);
        net.createFlow(t1, init);
        Transition t2 = net.createTransition("t2");
        net.createFlow(init, t2);
        net.createFlow(t2, init);

        String formula = "Forall (G (AP \"#out#_in\" 0))";
        CounterExample check = ModelCheckerMCHyper.check(net, formula, "./" + net.getName(), true);
        Assert.assertNull(check);

        formula = "Forall (G (AP \"#out#_t1\" 0))";
        check = ModelCheckerMCHyper.check(net, formula, "./" + net.getName(), true);
        Assert.assertNotNull(check);

        formula = "X( X( G( t1 AND t2)))";
        IRunFormula f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(new PetriGame(net), f, "./" + net.getName(), true);
        Assert.assertNull(ret);

    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula = "F out";
        IRunFormula f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        PetriGame mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret); // todo: here is an error, it is not null
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = false)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerParallel.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

}

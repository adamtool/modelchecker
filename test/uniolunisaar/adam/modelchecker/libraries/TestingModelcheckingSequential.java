package uniolunisaar.adam.modelchecker.libraries;

import uniolunisaar.adam.modelchecker.*;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformer;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.modelchecking.RedundantNetwork;
import uniolunisaar.adam.generators.modelchecking.ToyExamples;
import uniolunisaar.adam.generators.modelchecking.UpdatingNetwork;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreator;
import uniolunisaar.adam.modelchecker.circuits.ModelChecker;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerMCHyper;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformer;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingSequential {


    @Test(enabled = true)
    void testFirstExamplePaper() throws ParseException, IOException, InterruptedException, NotSupportedGameException {
        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        AdamTools.savePG2PDF("example", new PetriGame(pn), false);
        //todo:correct formula and all the other examples have to be adapted.
        ModelCheckerMCHyper.check(pn, "Forall (F (AP \"#out#_out\" 0))", "./" + pn.getName());
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula = "F(out)";
        IRunFormula f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        boolean ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertTrue(ret);

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertFalse(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertTrue(ret);
    }

    @Test
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
//        boolean ret = check(mc, FlowLTLTransformer.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
//        Assert.assertFalse(ret);
    }

    @Test
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
//        boolean ret = check(mc, FlowLTLTransformer.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
//        Assert.assertTrue(ret);
    }

    @Test
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
//        String formula = "F p3 > 0";
//        check(mc, FlowLTLTransformer.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
    }

    @Test
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F p3 > 0";
//        check(mc, createModelCheckingFormula(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
//        check(mc, createModelCheckingFormula(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
//        check(mc, createModelCheckingFormula(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
//        check(mc, createModelCheckingFormula(net, formula), "./" + net.getName());
    }

    @Test
    public void testBurglar() throws ParseException, IOException, RenderException, InterruptedException, NotSupportedGameException {
        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "burglar.apt"));
        final String formula = "EF qbadA > 0 OR qbadB > 0";
//        check(pn, formula, path + "burglar");
    }
}

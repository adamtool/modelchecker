package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformer;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.generators.modelchecking.RedundantNetwork;
import uniolunisaar.adam.generators.modelchecking.ToyExamples;
import uniolunisaar.adam.generators.modelchecking.UpdatingNetwork;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreator;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLParallel {

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula = "F out";
        IRunFormula f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertNull(ret);

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertNotNull(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertNull(ret); // todo: here is an error, it is not null
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
        Assert.assertNull(ret);
    }

    @Test(enabled = false)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreator.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformer.createNet4ModelCheckingParallel(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithParallelApproach(net, f, "./" + net.getName());
    }

}

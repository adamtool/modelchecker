package uniolunisaar.adam.modelchecker.circuits;

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
import uniolunisaar.adam.logic.util.FormulaCreatorPrevSemantics;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerSequential;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingFlowLTLSequential {

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula;
        IRunFormula f;
        PetriGame mc;
        CounterExample ret;

//        String formula = "F(out)";
//        IRunFormula f = FlowLTLParser.parse(net, formula);
//        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
//        PetriGame mc = PetriNetTransformer.createNet4ModelCheckingSequential(net, f);
//        AdamTools.savePG2PDF(net.getName() + "_mc" + formula, mc, true);
//        CounterExample ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName()+formula);
//        Assert.assertNull(ret);
        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc" + formula, mc, true);
        ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName() + formula, true);
        Assert.assertNotNull(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName() + "_" + formula, net, false);
        AdamTools.savePG2PDF(net.getName() + "_" + formula, net, false);
        mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc" + "_" + formula, mc, true);
        AdamTools.saveAPT(mc.getName() + "_" + formula, mc, false);
        ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName() + "_" + formula, true);
        Assert.assertNull(ret); // here is an error it is not null
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret); // here is an error it is null
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = false)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorPrevSemantics.getMaximaliltyStandardDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerSequential.createNet4ModelCheckingSequential(net, f);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelChecker.checkWithSequentialApproach(net, f, "./" + net.getName(), true);
    }

}

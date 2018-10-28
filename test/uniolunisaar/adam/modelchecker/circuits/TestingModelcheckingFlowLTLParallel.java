package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
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
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTLParallel;
import uniolunisaar.adam.tools.ProcessNotStartedException;

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
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

        String formula = "F out";
        IRunFormula f = FlowLTLParser.parse(net, formula);

        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        PetriGame mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        CounterExample ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);

        formula = "A(F(out))";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret); // todo: here is an error, it is not null
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNotNull(ret);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(out)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertNull(ret);
    }

    @Test(enabled = false)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        String formula = "A(F(p3)";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriGame mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        CounterExample ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

}

package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;

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
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);

        String formula = "F out";
        IRunFormula f = FlowLTLParser.parse(net, formula);

        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ModelCheckingResult ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        formula = "A F out";
        f = FlowLTLParser.parse(net, formula);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE); // todo: here is an error, it is not satisfied
    }

    @Test(enabled = true)
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ModelCheckingResult ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);
    }

    @Test(enabled = true)
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ModelCheckingResult ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE); // todo: here is an error, it is not satisfied
    }

    @Test(enabled = false)
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 2);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F p3";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ModelCheckingResult ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        String formula = "A F out";
        RunFormula f = FlowLTLParser.parse(net, formula);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        f = new RunFormula(FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net), RunOperators.Implication.IMP, f);
        ModelCheckingResult ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = ModelCheckerFlowLTL.checkWithParallelApproach(net, f, "./" + net.getName(), true);
    }

}

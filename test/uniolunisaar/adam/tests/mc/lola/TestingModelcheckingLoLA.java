package uniolunisaar.adam.tests.mc.lola;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.util.PNWTTools;
import static uniolunisaar.adam.logic.modelchecking.ltl.lola.ModelCheckerLoLA.check;
import uniolunisaar.adam.logic.transformers.modelchecking.lola.FlowLTLTransformerLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNLoLA;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLoLA {

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, NotConvertableException, Exception {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

    }

    @Test
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, NotConvertableException, Exception {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, NotConvertableException, Exception {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, NotConvertableException, Exception {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("pOut"))));
        PetriNetWithTransits mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
    }

    @Test(enabled = false)
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, NotConvertableException, Exception {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(net.getName(), net, false, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunLTLFormula formula = new RunLTLFormula(FlowLTLFormula.FlowLTLOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
    }

}

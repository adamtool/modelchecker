package uniolunisaar.adam.modelchecker.lola;

import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.util.PNWTTools;
import static uniolunisaar.adam.logic.modelchecking.lola.ModelCheckerLoLA.check;
import uniolunisaar.adam.logic.transformers.modelchecking.lola.FlowLTLTransformerLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNLoLA;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLoLA {

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, NotConvertableException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertFalse(ret);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false); 
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertTrue(ret);
    }

    @Test
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, NotConvertableException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertFalse(ret);
    }

    @Test
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, NotConvertableException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
        Assert.assertTrue(ret);
    }

    @Test
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, NotConvertableException {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("pOut"))));
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc,FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
    }

    @Test(enabled=false)  
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, NotConvertableException {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);        
        RunFormula formula = new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace("out"))));
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc,FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelCheckingSequential(net, formula);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mc, formula), "./" + net.getName());
    }

}

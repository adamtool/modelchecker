package uniolunisaar.adam.modelchecker.lola;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.generators.pnwt.RedundantNetwork;
import uniolunisaar.adam.generators.pnwt.ToyExamples;
import uniolunisaar.adam.generators.pnwt.UpdatingNetwork;
import uniolunisaar.adam.util.PNWTTools;
import static uniolunisaar.adam.logic.modelchecking.ltl.lola.ModelCheckerLoLA.check;
import uniolunisaar.adam.logic.transformers.modelchecking.lola.FlowLTLTransformerLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNLoLA;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLoLAOldApproach {

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test
    void testLoLa2() throws RenderException, InterruptedException, IOException, Exception {
        PetriNetWithTransits game = new PetriNetWithTransits("testing");
        Place init = game.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = game.createTransition("tB");
        game.createFlow(init, tstolen);
        Place out = game.createPlace("out");
        game.createFlow(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = game.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = game.createTransition("tC");
        game.createFlow(init3, t2);
        game.createFlow(t2, init3);
        check(game, "(F out > 0)", "./testing");

        PNWTTools.savePnwt2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");

    }

    @Test
    void testSemantics() throws RenderException, InterruptedException, IOException, Exception {
        PetriNetWithTransits game = new PetriNetWithTransits("testSemantics");
        Place init = game.createPlace("inA");
        init.setInitialToken(1);

        Transition tstolen = game.createTransition("tB");
        game.createFlow(init, tstolen);
        Place out = game.createPlace("out");
        game.createFlow(tstolen, out);

        // test finite        
        PNWTTools.savePnwt2PDF(game.getName() + "_finite", game, true);
        LTLModelCheckingResult ret = check(game, "A(inA > 0)", "./testing");
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        ret = check(game, "A(X(out > 0))", "./testing");
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);
        ret = check(game, "A(X(X(out > 0)))", "./testing");
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE); // this means LoLA is not stuttering at the end

        Place init2 = game.createPlace("inB");
        init2.setInitialToken(1);
        Transition t = game.createTransition("tA");
        game.createFlow(init2, t);
        game.createFlow(t, init2);
        ret = check(game, "A(X(out > 0))", "./testing");
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE); // this means LoLA considers the interleaving semantics also for the next

//
//        Place init2 = game.createPlace("inB");
//        init2.setInitialToken(1);
//        
//        Place init3 = game.createPlace("inittflB");
//        init3.setInitialToken(1);
////        
//        Transition t2 = game.createTransition("tC");
//        game.createFlow(init3, t2);
//        game.createFlow(t2, init3);
//        check(game, "(F out > 0)", "./testing");
//
//        PNWTTools.savePnwt2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
    }

    @Test
    public void testLoLa() throws ParseException, IOException, RenderException, InterruptedException, Exception {
        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "burglar.apt"));
        final String formula = "EF qbadA > 0 OR qbadB > 0";
        check(pn, formula, path + "burglar");
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException, FileNotFoundException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExample(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);

        net = ToyExamples.createFirstExample(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

    }

    @Test
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException, FileNotFoundException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(true);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.FALSE);
    }

    @Test
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException, FileNotFoundException, ExternalToolException {
        PetriNetWithTransits net = ToyExamples.createFirstExampleExtended(false);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        LTLModelCheckingResult ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertEquals(ret.getSatisfied(), LTLModelCheckingResult.Satisfied.TRUE);

    }

    @Test
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException, Exception {
        PetriNetWithTransits net = UpdatingNetwork.create(3, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        String formula = "F pOut > 0";
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
    }

    @Test
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException, Exception {
        PetriNetWithTransits net = RedundantNetwork.getBasis(1, 1);
        PNWTTools.saveAPT(net.getName(), net, false);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingIncorrectFixedMutexNetwork(1, 1);
        PNWTTools.savePnwt2PDF(net.getName(), net, false);
        mc = PnwtAndFlowLTLtoPNLoLA.createNet4ModelChecking4LoLA(net);
        PNWTTools.savePnwt2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
    }

}

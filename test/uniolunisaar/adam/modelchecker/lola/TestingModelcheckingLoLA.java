package uniolunisaar.adam.modelchecker.lola;

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
import uniolunisaar.adam.logic.util.AdamTools;
import static uniolunisaar.adam.modelchecker.lola.ModelCheckerLoLA.check;
import uniolunisaar.adam.modelchecker.transformers.FlowLTLTransformerLoLA;
import uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerLoLA;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingLoLA {

    @Test
    void testLoLa2() throws RenderException, InterruptedException, IOException {
        PetriGame game = new PetriGame("testing");
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

        AdamTools.savePG2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");

    }

    @Test
    void testSemantics() throws RenderException, InterruptedException, IOException {
        PetriGame game = new PetriGame("testSemantics");
        Place init = game.createPlace("inA");
        init.setInitialToken(1);

        Transition tstolen = game.createTransition("tB");
        game.createFlow(init, tstolen);
        Place out = game.createPlace("out");
        game.createFlow(tstolen, out);

        // test finite        
        AdamTools.savePG2PDF(game.getName() + "_finite", game, true);
        boolean ret = check(game, "A(inA > 0)", "./testing");
        Assert.assertEquals(ret, true);
        ret = check(game, "A(X(out > 0))", "./testing");
        Assert.assertEquals(ret, true);
        ret = check(game, "A(X(X(out > 0)))", "./testing");
        Assert.assertEquals(ret, false); // this means LoLA is not stuttering at the end

        Place init2 = game.createPlace("inB");
        init2.setInitialToken(1);
        Transition t = game.createTransition("tA");
        game.createFlow(init2, t);
        game.createFlow(t, init2);
        ret = check(game, "A(X(out > 0))", "./testing");
        Assert.assertEquals(ret, false); // this means LoLA considers the interleaving semantics also for the next

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
//        AdamTools.savePG2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
    }

    @Test
    public void testLoLa() throws ParseException, IOException, RenderException, InterruptedException, NotSupportedGameException {
        final String path = System.getProperty("examplesfolder") + "/safety/burglar/";
        PetriGame pn = new PetriGame(Tools.getPetriNet(path + "burglar.apt"));
        final String formula = "EF qbadA > 0 OR qbadB > 0";
        check(pn, formula, path + "burglar");
    }

    @Test
    public void checkFirstExample() throws RenderException, IOException, InterruptedException {
        PetriGame net = ToyExamples.createFirstExample(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertFalse(ret);

        net = ToyExamples.createFirstExample(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertTrue(ret);
    }

    @Test
    public void checkFirstExampleExtended() throws RenderException, IOException, InterruptedException {
        PetriGame net = ToyExamples.createFirstExampleExtended(true);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertFalse(ret);
    }

    @Test
    public void checkFirstExampleExtendedPositiv() throws RenderException, IOException, InterruptedException {
        PetriGame net = ToyExamples.createFirstExampleExtended(false);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F out > 0";
        boolean ret = check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
        Assert.assertTrue(ret);
    }

    @Test
    public void updatingNetworkExample() throws IOException, InterruptedException, RenderException {
        PetriGame net = UpdatingNetwork.create(3, 2);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F p3 > 0";
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
    }

    @Test
    public void redundantFlowExample() throws IOException, InterruptedException, RenderException {
        PetriGame net = RedundantNetwork.getBasis();
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);
        PetriGame mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        String formula = "F p3 > 0";
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());

        net = RedundantNetwork.getUpdatingFixedMutexNetwork();
        AdamTools.savePG2PDF(net.getName(), net, false);
        mc = PetriNetTransformerLoLA.createNet4ModelChecking4LoLA(net);
        AdamTools.savePG2PDF(net.getName() + "_mc", mc, true);
        check(mc, FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLA(net, formula), "./" + net.getName());
    }

}

package uniolunisaar.adam.modelchecker.libraries;

import java.io.IOException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.util.logics.transformers.logics.TransformerTools;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingAiger {

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    private void testAiger(PetriNet pn) throws IOException, InterruptedException {
//        final String outputFolder = AdamProperties.getInstance().getLibFolder();
        // save aiger file
        final String outputFolder = ".";
//        TransformerTools.save2Aiger(pn, outputFolder + "/" + pn.getName());
        TransformerTools.save2AigerAndPdf(pn, Circuit.getRenderer(Circuit.Renderer.INGOING), outputFolder + "/" + pn.getName());
    }

    @Test(enabled = true)
    void testAigerRenderer() throws ParseException, IOException, InterruptedException {
        final String path = System.getProperty("examplesfolder") + "/safety/firstExamplePaper/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
        PNWTTools.savePnwt2PDF("example", new PetriNetWithTransits(pn), false);
        testAiger(pn);
    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException {
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

        PNWTTools.savePnwt2PDF(game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
        testAiger(game);
    }

}

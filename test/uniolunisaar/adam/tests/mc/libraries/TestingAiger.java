package uniolunisaar.adam.tests.mc.libraries;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeStutterRegister;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.AigerTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingAiger {

    private static final String outputDir = System.getProperty("testoutputfolder");

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    private void testAiger(PetriNet pn) throws IOException, InterruptedException, ExternalToolException {
//        final String outputFolder = AdamProperties.getInstance().getLibFolder();
        // save aiger file
        final String outputFolder = outputDir;
//        TransformerTools.save2Aiger(pn, outputFolder + "/" + pn.getName());
        AigerTools.save2Aiger(new AigerRendererSafeStutterRegister(pn, true, CircuitRendererSettings.TransitionSemantics.INGOING), outputFolder + "/" + pn.getName());
        AigerTools.saveAiger2PDF(outputFolder + "/" + pn.getName() + ".aag", outputFolder + "/" + pn.getName(), PetriNetExtensionHandler.getProcessFamilyID(pn));
    }

    @Test(enabled = true)
    void testAigerRenderer() throws ParseException, IOException, InterruptedException, ExternalToolException {
        final String path = System.getProperty("examplesfolder") + "/synthesis/forallsafety/firstExamplePaper/";
        PetriNetWithTransits pn = new PetriNetWithTransits(Tools.getPetriNet(path + "firstExamplePaper.apt"));
//        PNWTTools.savePnwt2PDF(outputDir + "/example", new PetriNetWithTransits(pn), false);
        testAiger(pn);
    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ExternalToolException {
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

//        PNWTTools.savePnwt2PDF(outputDir + "/" + game.getName(), game, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
        testAiger(game);
    }

}

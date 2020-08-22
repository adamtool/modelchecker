package uniolunisaar.adam.modelchecker.transformers;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerOutgoing;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestPnwt2KripkeStructure {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/transformers/pnwt2kripke/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test
    public void testLateCreation() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);
        Transition tp = pnwt.getTransition("tp");
        Assert.assertTrue(pnwt.isInhibitor(pnwt.getFlow("r", "tp")));

        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerOutgoing.create(pnwt, true);
        Tools.save2DotAndPDF(outputDir + "initLate_ks", k);
        System.out.println(k.getEdges().toString());
    }
}

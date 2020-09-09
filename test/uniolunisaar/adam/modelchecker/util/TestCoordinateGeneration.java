package uniolunisaar.adam.modelchecker.util;

import java.io.File;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallelInhibitor;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNParInhibitorNoInit;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.MCTools;
import uniolunisaar.adam.util.PNTools;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author thewn
 */
@Test
public class TestCoordinateGeneration {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/coords/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @Test
    public void motivatingATVAExample() throws Exception {
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "ltl/ATVA19_motivatingExample.apt", false);
        PetriNetWithTransits pnmc = PnwtAndNbFlowFormulas2PNParInhibitorNoInit.createNet4ModelCheckingParallel(pnwt, 2);
        MCTools.addCoordinates(pnwt, pnmc);
        PetriNet unique = PNTools.createPetriNetWithIDsInLabel(pnmc);
        Tools.savePN(outputDir + pnmc.getName(), unique);
    }
}

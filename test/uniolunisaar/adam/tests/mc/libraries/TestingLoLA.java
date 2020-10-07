package uniolunisaar.adam.tests.mc.libraries;

import java.io.IOException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.LoLAPNParser;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingLoLA {

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test(enabled = false)
    void testLoLaParser() throws ParseException, IOException, InterruptedException {
        LoLAPNParser parser = new LoLAPNParser();
        PetriNet net = parser.parseFile("./example.lola");
        PNWTTools.savePnwt2PDF("example", new PetriNetWithTransits(net), false);
    }
}

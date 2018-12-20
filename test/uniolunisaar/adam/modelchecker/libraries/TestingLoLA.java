package uniolunisaar.adam.modelchecker.libraries;

import java.io.IOException;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.LoLAPNParser;
import uniolunisaar.adam.ds.exceptions.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingLoLA {

    @Test(enabled = false)
    void testLoLaParser() throws ParseException, IOException, InterruptedException, NotSupportedGameException {
        LoLAPNParser parser = new LoLAPNParser();
        PetriNet net = parser.parseFile("./example.lola");
        PNWTTools.savePnwt2PDF("example", new PetriGame(net), false);
    }
}

package uniolunisaar.adam.modelchecker.aba;

import java.io.File;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.modelchecking.aba.ABAEdge;
import uniolunisaar.adam.ds.modelchecking.aba.ABAState;
import uniolunisaar.adam.ds.modelchecking.aba.AlternatingBuchiAutomaton;
import uniolunisaar.adam.util.MCTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestAlternatingBuchiAutomata {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/aba/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
    }

    @Test
    public void firstTests() throws Exception {
        AlternatingBuchiAutomaton aba = new AlternatingBuchiAutomaton("test");
        ABAState i = aba.createAndAddState("i");
        aba.addInitialStates(i);
        ABAState a = aba.createAndAddState("phi,(s,tq)");
        ABAState b = aba.createAndAddState("phi,(s,tp)");
        aba.createAndAddEdge(i.getId(), ABAEdge.TYPE.EXISTS, "ts", a.getId(), b.getId());
        aba.addInitialStates(a,b);
        ABAState c = aba.createAndAddState("phi,q1");
        ABAState d = aba.createAndAddState("phi,q2");
        aba.createAndAddEdge(a.getId(), ABAEdge.TYPE.ALL, "tq", c.getId(), d.getId());
        ABAState e = aba.createAndAddState("phi,(p,tb)");
        aba.createAndAddEdge(b.getId(), ABAEdge.TYPE.ALL, "tp", e.getId());
        aba.createSpecialEdge(e.getId(), ABAEdge.Special.FALSE);
        aba.setBuchi(true, a, b, c, d, e);
        MCTools.save2DotAndPDF(outputDir + aba.getName(), aba);
    }

}

package uniolunisaar.adam.modelchecker.aba;

import org.testng.annotations.Test;
import uniolunisaar.adam.ds.modelchecking.aba.ABAEdge;
import uniolunisaar.adam.ds.modelchecking.aba.ABAState;
import uniolunisaar.adam.ds.modelchecking.aba.AlternatingBuchiAutomaton;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestAlternatingBuchiAutomata {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/aba/";

    @Test
    public void firstTests() {
        AlternatingBuchiAutomaton aba = new AlternatingBuchiAutomaton("test");
        ABAState i = aba.createAndAddState("i");
        ABAState a = aba.createAndAddState("phi,(s,tq)");
        ABAState b = aba.createAndAddState("phi,(s,tp)");
        aba.createAndAddEdge(i.getId(), ABAEdge.TYPE.EXISTS, "ts", a.getId(), b.getId());
        ABAState c = aba.createAndAddState("phi,q1");
        ABAState d = aba.createAndAddState("phi,q2");
        aba.createAndAddEdge(a.getId(), ABAEdge.TYPE.ALL, "tq", c.getId(), d.getId());
        ABAState e = aba.createAndAddState("phi,(p,tb)");
        aba.createAndAddEdge(b.getId(), ABAEdge.TYPE.ALL, "tp", e.getId());
        aba.createSpecialEdge(e.getId(), ABAEdge.Special.FALSE);
    }

}

package uniolunisaar.adam.modelchecker.aba;

import java.io.File;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.modelchecking.aba.ABAOperatorState;
import uniolunisaar.adam.ds.modelchecking.aba.UniversalExistentialEdge;
import uniolunisaar.adam.ds.modelchecking.aba.ABAState;
import uniolunisaar.adam.ds.modelchecking.aba.ABATrueFalseEdge;
import uniolunisaar.adam.ds.modelchecking.aba.AlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.modelchecking.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.modelchecking.aba.UniversalExistentialBuchiAutomaton;
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
        UniversalExistentialBuchiAutomaton aba = new UniversalExistentialBuchiAutomaton("test");
        ABAState i = aba.createAndAddState("i");
        aba.addInitialStates(i);
        ABAState a = aba.createAndAddState("phi,(s,tq)");
        ABAState b = aba.createAndAddState("phi,(s,tp)");
        aba.createAndAddEdge(i.getId(), UniversalExistentialEdge.TYPE.EXISTS, "ts", a.getId(), b.getId());
        aba.addInitialStates(a, b);
        ABAState c = aba.createAndAddState("phi,q1");
        ABAState d = aba.createAndAddState("phi,q2");
        aba.createAndAddEdge(a.getId(), UniversalExistentialEdge.TYPE.ALL, "tq", c.getId(), d.getId());
        ABAState e = aba.createAndAddState("phi,(p,tb)");
        aba.createAndAddEdge(b.getId(), UniversalExistentialEdge.TYPE.ALL, "tp", e.getId());
        aba.createSpecialEdge(e.getId(), UniversalExistentialEdge.Special.FALSE);
        aba.setBuchi(true, a, b, c, d, e);
        MCTools.save2DotAndPDF(outputDir + aba.getName(), aba);
    }

    @Test
    public void firstTestsGeneralABA() throws Exception {
        GeneralAlternatingBuchiAutomaton aba = new GeneralAlternatingBuchiAutomaton("test_general");
        ABAState i = aba.createAndAddState("i");
        aba.addInitialStates(i);
        ABAState a = aba.createAndAddState("phi,(s,tq)");
        ABAState b = aba.createAndAddState("phi,(s,tp)");
        aba.createAndAddDirectEdge(i.getId(), ABAOperatorState.TYPE.EXISTS, "ts", true, a.getId(), b.getId());
        aba.addInitialStates(a, b);
        ABAState c = aba.createAndAddState("phi,q1");
        ABAState d = aba.createAndAddState("phi,q2");
        aba.createAndAddDirectEdge(a.getId(), ABAOperatorState.TYPE.ALL, "tq", true, c.getId(), d.getId());
        ABAState e = aba.createAndAddState("phi,(p,tb)");
        aba.createAndAddDirectEdge(b.getId(), ABAOperatorState.TYPE.ALL, "tp", true, e.getId());
        aba.createSpecialEdge(e.getId(), ABATrueFalseEdge.Type.FALSE, true);
        aba.setBuchi(true, a, b, c, d, e);
        MCTools.save2DotAndPDF(outputDir + aba.getName(), aba);
    }

}

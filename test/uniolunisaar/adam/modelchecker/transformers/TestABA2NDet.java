package uniolunisaar.adam.modelchecker.transformers;

import java.io.File;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.automata.BuchiAutomaton;
import uniolunisaar.adam.ds.modelchecking.aba.ABAOperatorState;
import uniolunisaar.adam.ds.modelchecking.aba.UniversalExistentialEdge;
import uniolunisaar.adam.ds.modelchecking.aba.ABAState;
import uniolunisaar.adam.ds.modelchecking.aba.ABATrueFalseEdge;
import uniolunisaar.adam.ds.modelchecking.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.modelchecking.aba.UniversalExistentialBuchiAutomaton;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.ABA2NDetTransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformer;
import static uniolunisaar.adam.modelchecker.transformers.TestABTAxKripkeStructure2ABA.createExampleTree;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestABA2NDet {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/aba2ndet/";
    private static final String inputDir = System.getProperty("examplesfolder") + "/modelchecking/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
//        Logger.getInstance().setVerbose(true);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
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
        aba.createSpecialEdge(e.getId(), UniversalExistentialEdge.Special.FALSE, "tb");
        aba.setBuchi(true, a, b, c, d, e);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);

        BuchiAutomaton ndet = ABA2NDetTransformer.transform(aba, true);
        Tools.save2DotAndPDF(outputDir + ndet.getName(), ndet);
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
        aba.createAndAddSpecialEdge(e.getId(), ABATrueFalseEdge.Type.FALSE, "tb", true);
//        aba.setBuchi(true, a, b, c, d, e);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);

        BuchiAutomaton ndet = ABA2NDetTransformer.transform(aba, true);
        Tools.save2DotAndPDF(outputDir + ndet.getName(), ndet);
    }

    @Test
    public void testLateCreation() throws Exception {
        // Load Kripke structure
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);
        Transition tp = pnwt.getTransition("tp");
        Assert.assertTrue(pnwt.isInhibitor(pnwt.getFlow("r", "tp")));

        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformer.create(pnwt, true);
        Tools.save2DotAndPDF(outputDir + "initLate_ks", k);

        // create tree
        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = createExampleTree(pnwt);

        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformer.transform(abta, k);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);

        BuchiAutomaton ndet = ABA2NDetTransformer.transform(aba, true);
        Tools.save2DotAndPDF(outputDir + ndet.getName(), ndet);
    }

}

package uniolunisaar.adam.modelchecker.transformers;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.abta.TreeDirectionxState;
import uniolunisaar.adam.ds.abta.TreeState;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators;
import uniolunisaar.adam.ds.modelchecking.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerOutgoing;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestABTAxKripkeStructure2ABA {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/transformers/abtaxkripke2aba/";
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
        // Load Kripke structure
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "initLate.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);
        Transition tp = pnwt.getTransition("tp");
        Assert.assertTrue(pnwt.isInhibitor(pnwt.getFlow("r", "tp")));

        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerOutgoing.create(pnwt, true);
        Tools.save2DotAndPDF(outputDir + "initLate_ks", k);

        // create tree
        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = createExampleTree(pnwt);

        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformer.transform(abta, k);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);
    }

    public static AlternatingBuchiTreeAutomaton<Set<NodeLabel>> createExampleTree(PetriNetWithTransits pnwt) {
        // The tree for A(false U' not p)
        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = new AlternatingBuchiTreeAutomaton<>("A(false U' !p)", "A(false U' !p)");
        abta.createAndAddStates("p", "!p");
//        TreeState phi = abta.createAndAddState("A(false U' !p)");
        TreeState phi = abta.getInitialState();
        abta.setBuchi(phi, true);
        Set<NodeLabel> emptySet = new HashSet<>();
        Set<NodeLabel> p = new HashSet<>();
        p.add(new NodeLabel(pnwt.getNode("p")));
        abta.createAndAddEdge("p", emptySet, -1, PositiveBooleanFormulaFactory.createFalse());
        abta.createAndAddEdge("p", p, -1, PositiveBooleanFormulaFactory.createTrue());
        abta.createAndAddEdge("!p", emptySet, -1, PositiveBooleanFormulaFactory.createTrue());
        abta.createAndAddEdge("!p", p, -1, PositiveBooleanFormulaFactory.createFalse());
        abta.createAndAddEdge("A(false U' !p)", emptySet, -1, PositiveBooleanFormulaFactory.createParameterizedPositiveBooleanFormula(-1, PositiveBooleanFormulaOperators.Binary.AND, new TreeDirectionxState(phi, -1)));
        abta.createAndAddEdge("A(false U' !p)", p, -1, PositiveBooleanFormulaFactory.createFalse());
        return abta;
    }
}

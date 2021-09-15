package uniolunisaar.adam.tests.transformers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.automata.BuchiAutomaton;
import uniolunisaar.adam.ds.aba.ABAOperatorState;
import uniolunisaar.adam.ds.aba.UniversalExistentialEdge;
import uniolunisaar.adam.ds.aba.ABAState;
import uniolunisaar.adam.ds.aba.ABATrueFalseEdge;
import uniolunisaar.adam.ds.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.aba.UniversalExistentialBuchiAutomaton;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.ctl.CTL2AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.logic.transformers.modelchecking.ABA2NDetTransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformerFull;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerIngoingFull;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerOutgoing;
import static uniolunisaar.adam.tests.transformers.TestABTAxKripkeStructure2ABA.createExampleTree;
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
        Logger.getInstance().setVerbose(false);
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

        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerOutgoing.create(pnwt, true);
        Tools.save2DotAndPDF(outputDir + "initLate_ks", k);

        // create tree
        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = createExampleTree(pnwt);

        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformer.transform(abta, k);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);

        BuchiAutomaton ndet = ABA2NDetTransformer.transform(aba, true);
        Tools.save2DotAndPDF(outputDir + ndet.getName(), ndet);
    }

    @Test
    public void testLectureHall() throws Exception {
        // Load Kripke structure
//        String name = "lectureHallSmall";
        String name = "lectureHall";
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + name + ".apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        List<String> AP = new ArrayList<>();
        AP.add("emergency");
        AP.add("yard");
        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerIngoingFull.create(pnwt, AP);
        List<Transition> trAP = new ArrayList<>();
        trAP.add(pnwt.getTransition("emergency"));
//        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerIngoing.create(pnwt, trAP);
        Tools.save2DotAndPDF(outputDir + name + "_ks", k);

        // create tree
        Place yard = pnwt.getPlace("yard");
        Transition emergency = pnwt.getTransition("emergency");
        CTLAtomicProposition y = new CTLAtomicProposition(yard);
        CTLAtomicProposition em = new CTLAtomicProposition(emergency);
        // want to check "𝔸 AG(emergency -> EF yard)"
        // transformed is this "𝔸 E(false U' \neg emergency v E(true U yard))
//        ICTLFormula ftrans = new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.EUD, new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, em), CTLOperators.Binary.OR,
//                new CTLFormula(new CTLConstants.True(), CTLOperators.Binary.EU, y)));        
        // todo: don't I have to take the negation?
        // negation: "𝔸 \neg AG(emergency -> EF yard) = E(true U emergency \wedge E(false U' \neg yard))"
        ICTLFormula ftrans = new CTLFormula(new CTLConstants.True(), CTLOperators.Binary.EU, new CTLFormula(em, CTLOperators.Binary.AND,
                new CTLFormula(new CTLConstants.False(), CTLOperators.Binary.EUD, new CTLFormula(CTLOperators.Unary.NEG, y))));

        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = CTL2AlternatingBuchiTreeAutomaton.transform(ftrans, pnwt);
        System.out.println(abta.toString());

        System.out.println("created the tree");
//        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformer.transform(abta, k);
        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformerFull.transform(abta, k);
        Tools.save2DotAndPDF(outputDir + aba.getName(), aba);

        System.out.println("created the product");
        BuchiAutomaton ndet = ABA2NDetTransformer.transform(aba, true);
        System.out.println("created the NBA");
        Tools.save2DotAndPDF(outputDir + ndet.getName(), ndet);
    }

}

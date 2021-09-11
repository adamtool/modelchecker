package uniolunisaar.adam.tests.transformers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.abta.TreeDirectionxState;
import uniolunisaar.adam.ds.abta.TreeState;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators;
import uniolunisaar.adam.ds.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.ctl.CTL2AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerIngoing;
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

    @Test
    public void testLectureHall() throws Exception {
        // Load Kripke structure
        PetriNetWithTransits pnwt = PNWTTools.getPetriNetWithTransitsFromFile(inputDir + "lectureHall.apt", false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, false);

        List<String> AP = new ArrayList<>();
        AP.add("emergency");
        AP.add("yard");
//        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerIngoingFull.create(pnwt, AP);
        List<Transition> trAP = new ArrayList<>();
        trAP.add(pnwt.getTransition("emergency"));
        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformerIngoing.create(pnwt, trAP);
        Tools.save2DotAndPDF(outputDir + "lectureHall_ks", k);

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

//        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformerFull.transform(abta, k);
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

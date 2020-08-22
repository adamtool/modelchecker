package uniolunisaar.adam.modelchecker.util.mc.sdn;

import java.io.File;
import java.io.FileNotFoundException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.generators.pnwt.SDNCreator;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.SDNTools;
import uniolunisaar.adam.util.mc.sdn.SDNFormelCreator;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestSDNFormulaCreator {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    private static final String topologyA = ".name \"asdf\"\n"
            + ".switches\n"
            + "s1\n"
            + "s2\n"
            + "s3\n"
            + "s4\n"
            + "s5\n"
            + "\n"
            + ".connections\n" // idea two pipelines s1->s2->s4 (init)
            + "s1 s2\n"
            + "s1 s3\n"
            + "s1 s4\n" // jumping over s2
            + "s1 s5\n" // jumping over s3
            + "s2 s4\n"
            + "s2 s3\n" // switch from upper to lower pipeline
            + "s3 s5\n"
            + "\n"
            + ".ingress={s1}\n"
            + ".egress={s4,s5}\n"
            + "\n"
            + ".forwarding"
            + "s1.fwd(s2)\n"
            + "s2.fwd(s4)";

    private static final String updateChangePipeLineParallel = "[[upd(s1.fwd(s3/s2)) >> upd(s2.fwd(-/s4))] || upd(s3.fwd(s5))]";
//    private static final String updateChangePipeLineParallel = "[upd(s1.fwd(s3/s2)) || upd(s3.fwd(s5))]";
    private static final String updateChangePipeLineSequential = "[upd(s1.fwd(s3/s2)) >> upd(s3.fwd(s5))]";
    private static final String updateJumpOver = "[upd(s1.fwd(s4/s2)) >> upd(s2.fwd(-/s4))]";
    private static final String updateSwitchFromUpperToLower = "[upd(s2.fwd(s3/s4)) >> upd(s3.fwd(s5))]";

    @Test
    public void testConnectivity() throws ParseException {
        PetriNetWithTransits pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, true);
        RunLTLFormula f = SDNFormelCreator.createConnectivity(pnwt);
        String formula = f.toString();
        Assert.assertTrue(formula.startsWith("A F ("));
        Assert.assertTrue(formula.contains(" OR "));
        Assert.assertTrue(formula.contains("s4"));
        Assert.assertTrue(formula.contains("s5"));

        pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, false);
        f = SDNFormelCreator.createConnectivity(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A F ("));
        Assert.assertTrue(formula.contains(" OR "));
        Assert.assertTrue(formula.contains("s4"));
        Assert.assertTrue(formula.contains("s5"));
    }

    @Test
    public void loopFreedom() throws ParseException {
        // optimized
        PetriNetWithTransits pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, true);
        // strong
        RunLTLFormula f = SDNFormelCreator.createLoopFreedom(pnwt, false);
        String formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("(s1 IMP (s1 U G NEG s1)"));
        Assert.assertTrue(formula.contains("(s2 IMP (s2 U G NEG s2)"));
        Assert.assertTrue(formula.contains("(s3 IMP (s3 U G NEG s3)"));
        // weak
        f = SDNFormelCreator.createLoopFreedom(pnwt, true);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A F G ("));
        Assert.assertTrue(formula.contains("(s1 IMP (s1 U G NEG s1)"));
        Assert.assertTrue(formula.contains("(s2 IMP (s2 U G NEG s2)"));
        Assert.assertTrue(formula.contains("(s3 IMP (s3 U G NEG s3)"));

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, false);
        f = SDNFormelCreator.createLoopFreedom(pnwt, false);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("(s1 IMP (s1 U G NEG s1)"));
        Assert.assertTrue(formula.contains("(s2 IMP (s2 U G NEG s2)"));
        Assert.assertTrue(formula.contains("(s3 IMP (s3 U G NEG s3)"));
        // weak
        f = SDNFormelCreator.createLoopFreedom(pnwt, true);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A F G ("));
        Assert.assertTrue(formula.contains("(s1 IMP (s1 U G NEG s1)"));
        Assert.assertTrue(formula.contains("(s2 IMP (s2 U G NEG s2)"));
        Assert.assertTrue(formula.contains("(s3 IMP (s3 U G NEG s3)"));
    }

    @Test
    public void dropFreedom() throws ParseException {
        // optimized
        PetriNetWithTransits pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, true);
        RunLTLFormula f = SDNFormelCreator.createDropFreedom(pnwt);
        String formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("NEG s4"));
        Assert.assertTrue(formula.contains("NEG s5"));
        for (Transition transition : pnwt.getTransitions()) { // here all forwarding transitions should be there
            if (transition.getLabel().contains(SDNTools.infixTransitionLabel)) {
                Assert.assertTrue(formula.contains(transition.getId()));
            } else {
                Assert.assertFalse(formula.contains(transition.getId()));
            }
        }

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, false);
        f = SDNFormelCreator.createDropFreedom(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("NEG s4"));
        Assert.assertTrue(formula.contains("NEG s5"));
        for (Transition transition : pnwt.getTransitions()) { // here all forwarding transitions should be there
            if (transition.getLabel().contains(SDNTools.infixTransitionLabel)) {
                Assert.assertTrue(formula.contains(transition.getId()));
            } else {
                Assert.assertFalse(formula.contains(transition.getId()));
            }
        }

        // optimized
        pnwt = SDNCreator.parse(topologyA, updateJumpOver, true);
        f = SDNFormelCreator.createDropFreedom(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("NEG s4"));
        Assert.assertTrue(formula.contains("NEG s5"));
        for (Transition transition : pnwt.getTransitions()) { // here all forwarding transitions should be there
            if (transition.getLabel().contains(SDNTools.infixTransitionLabel)) {
                Assert.assertTrue(formula.contains(transition.getId()));
            } else {
                Assert.assertFalse(formula.contains(transition.getId()));
            }
        }

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateJumpOver, false);
        f = SDNFormelCreator.createDropFreedom(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A G ("));
        Assert.assertTrue(formula.contains("NEG s4"));
        Assert.assertTrue(formula.contains("NEG s5"));
        for (Transition transition : pnwt.getTransitions()) { // here all forwarding transitions should be there
            if (transition.getLabel().contains(SDNTools.infixTransitionLabel)) {
                Assert.assertTrue(formula.contains(transition.getId()));
            } else {
                Assert.assertFalse(formula.contains(transition.getId()));
            }
        }
    }

    @Test
    public void packageCoherence() throws ParseException, FileNotFoundException {
        // parallel
        // optimized
        PetriNetWithTransits pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, true);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        RunLTLFormula f = SDNFormelCreator.createPackageCoherence(pnwt);
        String formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        String pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        String pfad2 = formula.substring(formula.lastIndexOf("G"));
//        System.out.println(pfad2);
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s3"));
        Assert.assertTrue(pfad2.contains("s5"));
        Assert.assertFalse(pfad2.contains("s2"));
        Assert.assertFalse(pfad2.contains("s4"));

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateChangePipeLineParallel, false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        f = SDNFormelCreator.createPackageCoherence(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        pfad2 = formula.substring(formula.lastIndexOf("G"));
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s3"));
        Assert.assertTrue(pfad2.contains("s5"));
        Assert.assertFalse(pfad2.contains("s2"));
        Assert.assertFalse(pfad2.contains("s4"));

        // jump over
        // optimized
        pnwt = SDNCreator.parse(topologyA, updateJumpOver, true);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        f = SDNFormelCreator.createPackageCoherence(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        pfad2 = formula.substring(formula.lastIndexOf("G"));
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s4"));
        Assert.assertFalse(pfad2.contains("s2"));
        Assert.assertFalse(pfad2.contains("s3"));
        Assert.assertFalse(pfad2.contains("s5"));

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateJumpOver, false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        f = SDNFormelCreator.createPackageCoherence(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        pfad2 = formula.substring(formula.lastIndexOf("G"));
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s4"));
        Assert.assertFalse(pfad2.contains("s2"));
        Assert.assertFalse(pfad2.contains("s5"));

        // switch from upper to lower
        // optimized
        pnwt = SDNCreator.parse(topologyA, updateSwitchFromUpperToLower, true);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        f = SDNFormelCreator.createPackageCoherence(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        pfad2 = formula.substring(formula.lastIndexOf("G"));
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s2"));
        Assert.assertTrue(pfad2.contains("s3"));
        Assert.assertTrue(pfad2.contains("s5"));
        Assert.assertFalse(pfad2.contains("s4"));

        // unoptimized
        pnwt = SDNCreator.parse(topologyA, updateSwitchFromUpperToLower, false);
        PNWTTools.savePnwt2PDF(outputDir + pnwt.getName(), pnwt, true);
        f = SDNFormelCreator.createPackageCoherence(pnwt);
        formula = f.toString();
        Assert.assertTrue(formula.startsWith("A ("));
        pfad1 = formula.substring(0, formula.lastIndexOf("G"));
        Assert.assertTrue(pfad1.contains("s1"));
        Assert.assertTrue(pfad1.contains("s2"));
        Assert.assertTrue(pfad1.contains("s4"));
        Assert.assertFalse(pfad1.contains("s3"));
        Assert.assertFalse(pfad1.contains("s5"));
        pfad2 = formula.substring(formula.lastIndexOf("G"));
        Assert.assertTrue(pfad2.contains("s1"));
        Assert.assertTrue(pfad2.contains("s2"));
        Assert.assertTrue(pfad2.contains("s3"));
        Assert.assertTrue(pfad2.contains("s5"));
        Assert.assertFalse(pfad2.contains("s4"));
    }
}

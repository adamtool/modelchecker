package uniolunisaar.adam.modelchecker.transformers;

import java.io.File;
import uniolunisaar.adam.logic.transformers.flowltl.FlowLTLTransformerHyperLTL;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.modelchecking.circuits.PetriNetModelChecker;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl.FlowLTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequential;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingFlowLTLTransformer {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/transformers/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test
    public void transitionReplacement() throws RenderException, IOException, InterruptedException, NotConvertableException {
        PetriNetWithTransits net = new PetriNetWithTransits("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTransit(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTransit(b);
        Place c = net.createPlace("C");
        c.setInitialToken(1);
        Place d = net.createPlace("D");
        Place e = net.createPlace("E");
        Place f = net.createPlace("F");
        Transition t1 = net.createTransition();
        Transition t2 = net.createTransition();
        net.createFlow(a, t1);
        net.createFlow(b, t1);
        net.createFlow(t1, d);
        net.createFlow(c, t2);
        net.createFlow(d, t2);
        net.createFlow(t2, e);
        net.createFlow(t2, f);
        net.createFlow(t2, b);
        net.createTransit(a, t1, d);
        net.createTransit(b, t1, d);
        net.createTransit(d, t2, e, b);
        net.createInitialTransit(t2, f);
        PNWTTools.saveAPT(outputDir + net.getName(), net, false);
        PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, false);

//        RunFormula formula = new RunFormula(new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(t2)), RunOperators.Implication.IMP, new FlowFormula(new AtomicProposition(f)));
        RunFormula formula = new RunFormula(new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.G, new LTLAtomicProposition(t2))), RunOperators.Implication.IMP, new FlowFormula(new LTLAtomicProposition(f)));

        PetriNetWithTransits mc = PnwtAndFlowLTLtoPNSequential.createNet4ModelCheckingSequential(net, formula, true);
        PNWTTools.savePnwt2PDF(outputDir + mc.getName() + "mc", mc, true);
        ILTLFormula f_mc = new FlowLTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(net, mc, formula, new AdamCircuitFlowLTLMCSettings());
//        System.out.println(f_mc);

    }

    @Test
    public void testMCHyperTransformation() throws ParseException, InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("testing");
        Place init = net.createPlace("a");
        Place end = net.createPlace("b");
        Transition t = net.createTransition();
        net.createFlow(init, t);
//        String formula = "F (TRUE OR b)";
//        String formula = "F (b)";
        String formula = "F TRUE";
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(net, formula);
//        System.out.println(formula);
        ModelCheckingResult output = PetriNetModelChecker.check(VerificationAlgo.IC3, net, Circuit.getRenderer(Circuit.Renderer.INGOING, net), formula, outputDir + net.getName(), "");
        Assert.assertEquals(output.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ParseException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits game = new PetriNetWithTransits("testing");
        Place init = game.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = game.createTransition("tB");
        game.createFlow(init, tstolen);
        Place out = game.createPlace("out");
        game.createFlow(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = game.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = game.createTransition("tC");
        game.createFlow(init3, t2);
        game.createFlow(t2, init3);

        PNWTTools.savePnwt2PDF(outputDir + game.getName(), game, true);

        // test maximality
        // standard
        IRunFormula f = FormulaCreatorIngoingSemantics.getMaximalityInterleavingObject(game);
//        System.out.println("Maximality standard:");
//        System.out.println(f.toSymbolString());
        // reisig
        ILTLFormula f1 = FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(game);
//        System.out.println("Maximality Reisig:");
//        System.out.println(f1.toSymbolString());
        // reisig
        f = FormulaCreatorIngoingSemantics.getMaximalityConcurrentObject(game);
//        System.out.println("Maximality Reisig:");
//        System.out.println(f.toSymbolString());

        // test to hyper ltl
        String formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(game, f.toString());
//        String formula = FlowLTLTransformer.toMCHyperFormat(f); // working
        Assert.assertEquals(formula, "Forall (And (G (F (Or (Neg (AP \"#out#_inittfl\" 0)) (X (AP \"#out#_tB\" 0))))) (G (F (Or (Neg (AP \"#out#_inittflB\" 0)) (X (AP \"#out#_tC\" 0))))))");

        ModelCheckingResult output = PetriNetModelChecker.check(VerificationAlgo.IC3, game, Circuit.getRenderer(Circuit.Renderer.INGOING, game), formula, outputDir + game.getName(), "");
        Assert.assertEquals(output.getSatisfied(), ModelCheckingResult.Satisfied.FALSE);

        // new version
        String formula2 = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        Assert.assertEquals(formula.replace(" ", ""), formula2.replace(" ", ""));
    }
}

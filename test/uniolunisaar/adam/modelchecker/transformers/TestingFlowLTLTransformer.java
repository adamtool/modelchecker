package uniolunisaar.adam.modelchecker.transformers;

import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerHyperLTL;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.modelchecker.circuits.Circuit;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerMCHyper;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerSequential;
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTLSequential;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingFlowLTLTransformer {

    @Test
    public void transitionReplacement() throws RenderException, IOException, InterruptedException, NotConvertableException {
        PetriGame net = new PetriGame("introduction");
        Place a = net.createPlace("a");
        a.setInitialToken(1);
        net.setInitialTokenflow(a);
        Place b = net.createPlace("B");
        b.setInitialToken(1);
        net.setInitialTokenflow(b);
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
        net.createTokenFlow(a, t1, d);
        net.createTokenFlow(b, t1, d);
        net.createTokenFlow(d, t2, e, b);
        net.createInitialTokenFlow(t2, f);
        AdamTools.saveAPT(net.getName(), net, false);
        AdamTools.savePG2PDF(net.getName(), net, false);

//        RunFormula formula = new RunFormula(new LTLFormula(LTLOperators.Unary.F, new AtomicProposition(t2)), RunOperators.Implication.IMP, new FlowFormula(new AtomicProposition(f)));
        RunFormula formula = new RunFormula(new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.G, new AtomicProposition(t2))), RunOperators.Implication.IMP, new FlowFormula(new AtomicProposition(f)));

        PetriGame mc = PetriNetTransformerFlowLTLSequential.createNet4ModelCheckingSequential(net, formula, true);
        AdamTools.savePG2PDF(mc.getName() + "mc", mc, true);
        ILTLFormula f_mc = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(net, mc, formula, true);
        System.out.println(f_mc);

    }

    @Test
    public void testMCHyperTransformation() throws ParseException, InterruptedException, IOException, ProcessNotStartedException {
        PetriGame net = new PetriGame("testing");
        Place init = net.createPlace("a");
        Place end = net.createPlace("b");
        Transition t = net.createTransition();
        net.createFlow(init, t);
//        String formula = "F (TRUE OR b)";
//        String formula = "F (b)";
        String formula = "F TRUE";
        formula = FlowLTLTransformerHyperLTL.toMCHyperFormat(net, formula);
//        System.out.println(formula);
        CounterExample output = ModelCheckerMCHyper.check(net, Circuit.getRenderer(Circuit.Renderer.INGOING), formula, "./" + net.getName());
        Assert.assertNull(output);
    }

    @Test
    void testToyExample() throws RenderException, InterruptedException, IOException, ParseException, ProcessNotStartedException {
        PetriGame game = new PetriGame("testing");
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

        AdamTools.savePG2PDF(game.getName(), game, true);

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

        CounterExample output = ModelCheckerMCHyper.check(game, Circuit.getRenderer(Circuit.Renderer.INGOING), formula, "./" + game.getName());
        Assert.assertNotNull(output);

        // new version
        String formula2 = FlowLTLTransformerHyperLTL.toMCHyperFormat(f);
        Assert.assertEquals(formula.replace(" ", ""), formula2.replace(" ", ""));
    }
}

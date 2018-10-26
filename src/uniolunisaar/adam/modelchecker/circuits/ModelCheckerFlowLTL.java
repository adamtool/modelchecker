package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.IOException;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.flowltl.RunOperators;
import uniolunisaar.adam.logic.util.AdamTools;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL.Maximality;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL.TransitionSemantics;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformer;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerHyperLTL;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerParallel;
import uniolunisaar.adam.modelchecker.transformers.formula.FlowLTLTransformerSequential;
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTLParallel;
import uniolunisaar.adam.modelchecker.transformers.petrinet.PetriNetTransformerFlowLTLSequential;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowLTL {

    public enum Approach {
        PARALLEL,
        SEQUENTIAL
    }

    private TransitionSemantics semantics = TransitionSemantics.OUTGOING;
    private Approach approach = Approach.SEQUENTIAL;
    private Maximality maximality = Maximality.MAX_INTERLEAVING;
    private ModelCheckerLTL.Stuttering stuttering = ModelCheckerLTL.Stuttering.PREFIX_REGISTER;
    private boolean initFirst = true;

    public ModelCheckerFlowLTL() {
    }

    public ModelCheckerFlowLTL(TransitionSemantics semantics, Approach approach, Maximality maximality, ModelCheckerLTL.Stuttering stuttering, boolean initFirst) {
        this.semantics = semantics;
        this.approach = approach;
        this.maximality = maximality;
        this.stuttering = stuttering;
        this.initFirst = initFirst;
    }

    /**
     *
     * @param net
     * @param formula
     * @param path
     * @param verbose
     * @return null iff the formula holds, otherwise a counter example violating
     * the formula.
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.modelchecker.exceptions.NotConvertableException
     */
    public CounterExample check(PetriGame net, RunFormula formula, String path, boolean verbose) throws InterruptedException, IOException, ParseException, NotConvertableException {
        Logger.getInstance().addMessage("Checking the net '" + net.getName() + "' for the formula '" + formula.toSymbolString() + "'.\n"
                + " With maximality term: " + maximality
                + " approach: " + approach + " semantics: " + semantics + " stuttering: " + stuttering, true);

        ModelCheckerLTL mcLTL = new ModelCheckerLTL(semantics, maximality, stuttering);

        // If we have the LTL fragment just use the standard LTLModelchecker
        if (ModelCheckerTools.getFlowFormulas(formula).isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula.toSymbolString() + "'. Thus, we use the standard model checking algorithm for LTL.");
            return mcLTL.check(net, formula.toLTLFormula(), path, verbose);
        }

        // Add Fairness
        RunFormula f = FlowLTLTransformer.addFairness(net, formula);

        // Get the formula for the maximality (null if MAX_NONE)
        ILTLFormula max = ModelCheckerTools.getMaximality(maximality, semantics, net);
        if (max != null) {
            f = new RunFormula(max, RunOperators.Implication.IMP, f);
            mcLTL.setMaximality(Maximality.MAX_NONE); // already done the maximality here
        }
//        IRunFormula f = formula;

        if (approach == Approach.PARALLEL) {
            PetriGame gameMC = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
            if (verbose) {
                AdamTools.savePG2PDF(path + "_mc", gameMC, true);
            }
            ILTLFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(net, gameMC, f);
//            Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC.toSymbolString() + "'.", false);
            return mcLTL.check(gameMC, formulaMC, path + "_mc", verbose);
        } else {
            PetriGame gameMC = PetriNetTransformerFlowLTLSequential.createNet4ModelCheckingSequential(net, f, initFirst);
            if (verbose) {
                // color all original places
                for (Place p : gameMC.getPlaces()) {
                    if (!gameMC.hasPartition(p)) {
                        gameMC.setEnvironment(p);
                    }
                }
                AdamTools.savePG2PDF(path + "_mc", gameMC, true, ModelCheckerTools.getFlowFormulas(formula).size());
//                try {
//                    AdamTools.saveAPT(path + "_mc", gameMC, false);
//                } catch (RenderException ex) {
//                    java.util.logging.Logger.getLogger(ModelCheckerFlowLTL.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (FileNotFoundException ex) {
//                    java.util.logging.Logger.getLogger(ModelCheckerFlowLTL.class.getName()).log(Level.SEVERE, null, ex);
//                }
            }
            ILTLFormula formulaMC = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(net, gameMC, f, initFirst);
//            // Add Fairness but without the active places in the preset
//            Collection<ILTLFormula> elements = new ArrayList<>();
//            for (Transition t : net.getTransitions()) {
//                if (net.isStrongFair(t)) {
//                    elements.add(FormulaCreator.createStrongFairness(t));
//                }
//                if (net.isWeakFair(t)) {
//                    elements.add(FormulaCreator.createStrongFairness(t)); // everything is strong fair in the sequential approach
//                }
//            }
//            formulaMC = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, true), LTLOperators.Binary.IMP, formulaMC);
//            Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC.toSymbolString() + "'.", false);
            return mcLTL.check(gameMC, formulaMC, path + "_mc", verbose);
        }
    }

    public TransitionSemantics getSemantics() {
        return semantics;
    }

    public void setSemantics(TransitionSemantics semantics) {
        this.semantics = semantics;
    }

    public Approach getApproach() {
        return approach;
    }

    public void setApproach(Approach approach) {
        this.approach = approach;
    }

    public Maximality getMaximality() {
        return maximality;
    }

    public void setMaximality(Maximality maximality) {
        this.maximality = maximality;
    }

    public ModelCheckerLTL.Stuttering getStuttering() {
        return stuttering;
    }

    public void setStuttering(ModelCheckerLTL.Stuttering stuttering) {
        this.stuttering = stuttering;
    }

    /**
     * Returns null iff the formula holds.
     *
     * This only works for formulas with at most one flow formula.
     *
     * It uses the MAX_PARALLEL approach (the first idea), where the flow and
     * the orignal net are succeed simulaneously.
     *
     * @param game
     * @param formula
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static CounterExample checkWithParallelApproach(PetriGame game, IRunFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException, NotConvertableException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        PetriGame gameMC = PetriNetTransformerFlowLTLParallel.createNet4ModelCheckingParallelOneFlowFormula(game);
        ILTLFormula formulaMC = FlowLTLTransformerParallel.createFormula4ModelChecking4CircuitParallel(game, gameMC, formula);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        }
        return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

    /**
     * Returns null iff the formula holds.
     *
     *
     * It uses the SEQUENTIAL approach, where a token is passed to each sub net
     * representing a flow subformula
     *
     * @param game
     * @param formula
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
    public static CounterExample checkWithSequentialApproach(PetriGame game, RunFormula formula, String path, boolean previousSemantics) throws InterruptedException, IOException, NotConvertableException {
        Logger.getInstance().addMessage("Checking the net '" + game.getName() + "' for the formula '" + formula + "'.", true);
        if (ModelCheckerTools.getFlowFormulas(formula).isEmpty()) {
            Logger.getInstance().addMessage("There is no flow formula within '" + formula + "'. Thus, we use the standard model checking algorithm for LTL.");
            formula = FlowLTLTransformer.addFairness(game, formula);
            AigerRenderer renderer;
            if (previousSemantics) {
                renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
            } else {
                renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
            }
            return ModelCheckerMCHyper.check(game, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formula), "./" + game.getName());
        }
        PetriGame gameMC = PetriNetTransformerFlowLTLSequential.createNet4ModelCheckingSequential(game, formula);
        ILTLFormula formulaMC = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(game, gameMC, formula, false);
        Logger.getInstance().addMessage("Checking the net '" + gameMC.getName() + "' for the formula '" + formulaMC + "'.", false);
        AigerRenderer renderer;
        if (previousSemantics) {
            renderer = Circuit.getRenderer(Circuit.Renderer.INGOING);
        } else {
            renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER);
        }
        return ModelCheckerMCHyper.check(gameMC, renderer, FlowLTLTransformerHyperLTL.toMCHyperFormat(formulaMC), "./" + gameMC.getName());
    }

}

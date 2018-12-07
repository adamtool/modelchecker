package uniolunisaar.adam.modelchecker.transformers.formula;

import java.util.ArrayList;
import java.util.Collection;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.logics.AtomicProposition;
import uniolunisaar.adam.logic.logics.Constants;
import uniolunisaar.adam.logic.logics.FormulaBinary;
import uniolunisaar.adam.logic.logics.IAtomicProposition;
import uniolunisaar.adam.logic.logics.ltl.flowltl.IFlowFormula;
import uniolunisaar.adam.logic.logics.IFormula;
import uniolunisaar.adam.logic.logics.ltl.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.logics.IOperatorBinary;
import uniolunisaar.adam.logic.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.logic.logics.ltl.flowltl.LTLFormula;
import uniolunisaar.adam.logic.logics.ltl.flowltl.LTLOperators;
import uniolunisaar.adam.logic.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.logic.logics.ltl.flowltl.RunOperators;
import uniolunisaar.adam.logic.logics.ltl.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.FormulaCreator;
import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRendererSafeOutStutterRegister;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL;
import uniolunisaar.adam.modelchecker.exceptions.NotConvertableException;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformer {

    public static ILTLFormula getFairness(PetriGame net) {
        // Add Fairness to the formula
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            if (net.isStrongFair(t)) {
                elements.add(FormulaCreator.createStrongFairness(t));
            }
            if (net.isWeakFair(t)) {
                elements.add(FormulaCreator.createWeakFairness(t));
            }
        }
        return FormulaCreator.bigWedgeOrVeeObject(elements, true);
    }

    public static RunFormula addFairness(PetriGame net, RunFormula formula) {
        ILTLFormula fairness = getFairness(net);
        return (!fairness.toString().equals("TRUE")) ? new RunFormula(fairness, RunOperators.Implication.IMP, formula) : formula;
    }

    public static ILTLFormula addFairness(PetriGame net, ILTLFormula formula) {
        ILTLFormula fairness = getFairness(net);
        return (!fairness.toString().equals("TRUE")) ? new LTLFormula(fairness, LTLOperators.Binary.IMP, formula) : formula;
    }

    public static ILTLFormula convert(IFormula f) throws NotConvertableException {
        if (f instanceof IFlowFormula) {
            throw new NotConvertableException("The formula contains a flow formula '" + f.toSymbolString() + "'. Hence, we cannot transform it to an LTL formula.");
        } else if (f instanceof RunFormula) {
            return new LTLFormula(convert(((RunFormula) f).getPhi()));
        } else if (f instanceof IRunFormula && f instanceof FormulaBinary) {
            FormulaBinary form = ((FormulaBinary) f);
            IOperatorBinary op = form.getOp();
            ILTLFormula f1 = convert(form.getPhi1());
            ILTLFormula f2 = convert(form.getPhi2());
            if (op instanceof RunOperators.Binary) {
                if (op.equals(RunOperators.Binary.AND)) {
                    return new LTLFormula(f1, LTLOperators.Binary.AND, f2);
                } else if (op.equals(RunOperators.Binary.OR)) {
                    return new LTLFormula(f1, LTLOperators.Binary.OR, f2);
                }
            } else if (op instanceof RunOperators.Implication) {
                return new LTLFormula(f1, LTLOperators.Binary.IMP, f2);
            }
            throw new RuntimeException("Not every possible case matched.");
        } else {
            return (ILTLFormula) f;
        }
    }

    public static ILTLFormula handleStutteringOutGoingSemantics(PetriGame net, ILTLFormula formula, ModelCheckerLTL.Stuttering stutt, ModelCheckerLTL.Maximality max) throws ParseException {
        switch (stutt) {
            case REPLACEMENT: {
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    elements.add(new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(t)));
                }
                ILTLFormula allNotTrue = FormulaCreator.bigWedgeOrVeeObject(elements, true);
//        // does not work since we need simulatanous replacement
//        ILTLFormula f = formula;
//        // todo: expensive may go through the formula recursivly and only replace those which are existent
//        for (Transition t : net.getTransitions()) {
//            ILTLFormula tProp = new AtomicProposition(t);
//            f = (ILTLFormula) f.substitute(tProp, new LTLFormula(allNotTrue, LTLOperators.Binary.U, tProp));
//        }
                // todo: write method for simultanous replacements not on String replacement
                // replace all transitions with a buffer name
                String f = formula.toReplacableString();
                for (Transition t : net.getTransitions()) {
                    f = f.replace("'" + t.getId() + "'", "'~#@" + t.getId() + "@#~'");
                }
                // now replace all step by step
                for (Transition t : net.getTransitions()) {
                    ILTLFormula tProp = new AtomicProposition(t);
                    f = f.replace("'~#@" + t.getId() + "@#~'", new LTLFormula(allNotTrue, LTLOperators.Binary.U, tProp).toString());
                }
                // clean up
                for (Place p : net.getPlaces()) {
                    f = f.replace("'" + p.getId() + "'", p.getId());
                }
                return FlowLTLParser.parse(net, f).toLTLFormula();
            }
            case REPLACEMENT_REGISTER: {
                IAtomicProposition initReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.INIT_LATCH);
                IAtomicProposition stutterReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.STUTT_LATCH);
                String f = formula.toReplacableString();
                for (Transition t : net.getTransitions()) {
                    f = f.replace("'" + t.getId() + "'", "'~#@" + t.getId() + "@#~'");
                }
                // now replace all step by step
                for (Transition t : net.getTransitions()) {
                    ILTLFormula tProp = new AtomicProposition(t);
                    f = f.replace("'~#@" + t.getId() + "@#~'", new LTLFormula(stutterReg, LTLOperators.Binary.U, tProp).toString());
                }
                // clean up
                for (Place p : net.getPlaces()) {
                    f = f.replace("'" + p.getId() + "'", p.getId());
                }
                return new LTLFormula(initReg, LTLOperators.Binary.IMP, FlowLTLParser.parse(net, f).toLTLFormula());
            }
            case PREFIX: {
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    elements.add(new LTLFormula(LTLOperators.Unary.NEG, new AtomicProposition(t)));
                }
                ILTLFormula allNotTrue = FormulaCreator.bigWedgeOrVeeObject(elements, true);
//        return new LTLFormula(new LTLFormula(new LTLFormula(LTLOperators.Unary.F, allNotTrue),
//                LTLOperators.Binary.IMP,
//                new LTLFormula(LTLOperators.Unary.G, allNotTrue)),
//                LTLOperators.Binary.IMP, formula); // wrong since the globally starts from the first point
//        return new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(allNotTrue,
//                LTLOperators.Binary.IMP,
//                new LTLFormula(LTLOperators.Unary.G, allNotTrue))),
//                LTLOperators.Binary.IMP, formula);// also wrong since we have to skip the first allZero because of the initilization step
                return new LTLFormula(LTLOperators.Unary.X, new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(allNotTrue,
                        LTLOperators.Binary.IMP,
                        new LTLFormula(LTLOperators.Unary.G, allNotTrue))),
                        LTLOperators.Binary.IMP, formula));
            }
            case PREFIX_REGISTER: {
//                IAtomicProposition initReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.INIT_LATCH);
                IAtomicProposition stutterReg = new Constants.Container(AigerRendererSafeOutStutterRegister.OUTPUT_PREFIX + AigerRendererSafeOutStutterRegister.STUTT_LATCH);
//                return new LTLFormula(initReg, LTLOperators.Binary.IMP, new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(stutterReg,
//                        LTLOperators.Binary.IMP,
//                        new LTLFormula(LTLOperators.Unary.G, stutterReg))),
//                        LTLOperators.Binary.IMP, formula)); // this would make it true in every moment since initReg is not holding. Would have to be a globally around

                ILTLFormula f;
                if (max != ModelCheckerLTL.Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
                    f = new LTLFormula(
                            LTLOperators.Unary.G,
                            new LTLFormula(stutterReg,
                                    LTLOperators.Binary.IMP,
                                    new LTLFormula(LTLOperators.Unary.G, stutterReg))
                    );
                } else {
                    f = new LTLFormula(
                            new LTLFormula(
                                    LTLOperators.Unary.G,
                                    new LTLFormula(LTLOperators.Unary.NEG, stutterReg)
                            ));
                }
                return new LTLFormula(
                        LTLOperators.Unary.X,
                        new LTLFormula(f,
                                LTLOperators.Binary.IMP, formula)
                );
            }
        }
        throw new RuntimeException("Not for every possibility of stuttering approaches a case is handled. " + stutt + " is missing.");
    }
}

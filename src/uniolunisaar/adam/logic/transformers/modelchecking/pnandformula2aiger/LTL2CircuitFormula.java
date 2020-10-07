package uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger;

import java.util.ArrayList;
import java.util.Collection;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Maximality;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering;
import static uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering.PREFIX;
import static uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering.PREFIX_REGISTER;
import static uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering.REPLACEMENT;
import static uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings.Stuttering.REPLACEMENT_REGISTER;
import uniolunisaar.adam.logic.parser.logics.flowltl.FlowLTLParser;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeStutterRegister;
import uniolunisaar.adam.util.logics.FormulaCreator;

/**
 *
 * @author Manuel Gieseking
 */
public class LTL2CircuitFormula {

    /**
     * Here only the PREFIX_REGISTER AND THE ERROR_REGISTER are implemented. The
     * others are to slow.
     *
     * @param net
     * @param formula
     * @param stutt
     * @param max
     * @return
     * @throws ParseException
     */
    public static ILTLFormula handleStutteringInGoingSemantics(PetriNet net, ILTLFormula formula, Stuttering stutt, Maximality max) throws ParseException {
        switch (stutt) {
            case PREFIX_REGISTER: {
                ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
                ILTLFormula f;
                if (max != Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
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
                return new LTLFormula(f, LTLOperators.Binary.IMP, formula);
            }
            case ERROR_REGISTER: {
                ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
                ILTLFormula f = new LTLFormula(
                        new LTLFormula(
                                LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.NEG, stutterReg)
                        ));

                return new LTLFormula(f, LTLOperators.Binary.IMP, formula);
            }
        }
        throw new RuntimeException("Not for every possibility of stuttering approaches a case is handled. " + stutt + " is missing."
                + " They are not implemented because they are too expensive.");
    }

    /**
     * The replacement idea is way to expensive, since we replace each
     * transition with an until to jump over all stutter steps.
     *
     * The register idea should be also way faster, since we don't have to
     * consider all the transitions in the formula but only have one additional
     * register.
     *
     * The error register uses one globally less, but it still has to be tested
     * if this is really faster (currently there could still be a problem for
     * the logEncoding)
     *
     * @param net
     * @param formula
     * @param stutt
     * @param max
     * @return
     * @throws ParseException
     */
    public static ILTLFormula handleStutteringOutGoingSemantics(PetriNet net, ILTLFormula formula, Stuttering stutt, Maximality max) throws ParseException {
        switch (stutt) {
            case REPLACEMENT: {
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    elements.add(new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(t)));
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
                    ILTLFormula tProp = new LTLAtomicProposition(t);
                    f = f.replace("'~#@" + t.getId() + "@#~'", new LTLFormula(allNotTrue, LTLOperators.Binary.U, tProp).toString());
                }
                // clean up
                for (Place p : net.getPlaces()) {
                    f = f.replace("'" + p.getId() + "'", p.getId());
                }
                return FlowLTLParser.parse(net, f).toLTLFormula();
            }
            case REPLACEMENT_REGISTER: {
                ILTLFormula initReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.INIT_LATCH);
                ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
                String f = formula.toReplacableString();
                for (Transition t : net.getTransitions()) {
                    f = f.replace("'" + t.getId() + "'", "'~#@" + t.getId() + "@#~'");
                }
                // now replace all step by step
                for (Transition t : net.getTransitions()) {
                    ILTLFormula tProp = new LTLAtomicProposition(t);
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
                    elements.add(new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(t)));
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
                ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
//                return new LTLFormula(initReg, LTLOperators.Binary.IMP, new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(stutterReg,
//                        LTLOperators.Binary.IMP,
//                        new LTLFormula(LTLOperators.Unary.G, stutterReg))),
//                        LTLOperators.Binary.IMP, formula)); // this would make it true in every moment since initReg is not holding. Would have to be a globally around

                ILTLFormula f;
                if (max != Maximality.MAX_INTERLEAVING_IN_CIRCUIT) {
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
            case ERROR_REGISTER: {
                ILTLFormula stutterReg = new LTLConstants.Container(AigerRendererSafeStutterRegister.OUTPUT_PREFIX + AigerRendererSafeStutterRegister.STUTT_LATCH);
                ILTLFormula f = new LTLFormula(
                        new LTLFormula(
                                LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.NEG, stutterReg)
                        ));

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

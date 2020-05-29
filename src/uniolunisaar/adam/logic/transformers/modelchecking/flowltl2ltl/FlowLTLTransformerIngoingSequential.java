package uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.AtomicProposition;
import uniolunisaar.adam.ds.logics.FormulaBinary;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.IOperatorBinary;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings.Stucking;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNNoInit;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNNoInit.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNNoInit.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNSequentialNoInit;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerIngoingSequential extends FlowLTLTransformer {

    private boolean runInScopeOfTemporalOperator;

    private ILTLFormula anyRunTransition = null;
    private ILTLFormula initAndRunStuttering = null;

    /**
     * OR_(t\in T_i\setminus T_skip)
     *
     * @param mcNet
     * @param nb_ff
     * @return
     */
    private ILTLFormula getMyRealFlowTransitions(PetriNet mcNet, int nb_ff) {
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : mcNet.getTransitions()) {
            if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))// the transitions of my subnet
                    && !t.getId().endsWith(PnwtAndNbFlowFormulas2PNSequentialNoInit.NEXT_ID + "-" + nb_ff) // which are not the nxt transitions
                    ) {
                elements.add(new LTLAtomicProposition(t));
            }
        }
        return FormulaCreator.bigWedgeOrVeeObject(elements, false);
    }

    /**
     * OR_(t\inT_skip)
     *
     * @param mcNet
     * @param nb_ff
     * @return
     */
    private ILTLFormula getMySkippingFlowTransitions(PetriNet mcNet, int nb_ff) {
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : mcNet.getTransitions()) {
            if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))// the transitions of my subnet
                    && t.getId().endsWith(PnwtAndNbFlowFormulas2PNSequentialNoInit.NEXT_ID + "-" + nb_ff) // which are not the nxt transitions
                    ) {
                elements.add(new LTLAtomicProposition(t));
            }
        }
        return FormulaCreator.bigWedgeOrVeeObject(elements, false);
    }

    /**
     * G ( OR_(t in T_i) t -> OR_(t in T_skip)) AND (OR_(t in T_skip) t)
     *
     * @param mcNet
     * @param nb_ff
     * @return
     */
    private ILTLFormula getMyStutteringFlowTransitions(PetriNet mcNet, int nb_ff) {
        ILTLFormula global = new LTLFormula(LTLOperators.Unary.G,
                new LTLFormula(
                        new LTLFormula(getMyRealFlowTransitions(mcNet, nb_ff), LTLOperators.Binary.OR, getMySkippingFlowTransitions(mcNet, nb_ff)),
                        LTLOperators.Binary.IMP,
                        getMySkippingFlowTransitions(mcNet, nb_ff)));
        return new LTLFormula(global, LTLOperators.Binary.AND, getMySkippingFlowTransitions(mcNet, nb_ff));
    }

    /**
     * OR_(t\in\T_o)
     *
     * @param orig
     * @return
     */
    private ILTLFormula getRunTransitions(PetriNet orig) {
        if (anyRunTransition == null) {
            // all original transitions
            Collection<LTLAtomicProposition> elements = new ArrayList<>();
            for (Transition t : orig.getTransitions()) {
                elements.add(new LTLAtomicProposition(t));
            }
            anyRunTransition = FormulaCreator.bigWedgeOrVeeObject(elements, false);
        }
        return anyRunTransition;
    }

    /**
     * neg OR_(t in T) t
     *
     * @param mcNet
     * @return
     */
    private ILTLFormula getInitAndRunStuttering(PetriNet mcNet) {
        if (initAndRunStuttering == null) {
            // all original transitions
            Collection<LTLAtomicProposition> elements = new ArrayList<>();
            for (Transition t : mcNet.getTransitions()) {
                elements.add(new LTLAtomicProposition(t));
            }
            initAndRunStuttering = new LTLFormula(LTLOperators.Unary.NEG, FormulaCreator.bigWedgeOrVeeObject(elements, false));
        }
        return initAndRunStuttering;
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% FLOWFORMULA PART
    /**
     * Replaces the places and the transitions
     *
     * @param orig
     * @param net
     * @param phi
     * @param nb_ff
     * @param scopeEventually
     * @return
     */
    @Override
    ILTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, int nb_ff, boolean scopeEventually) {
        AtomicProposition atom = (AtomicProposition) phi;
        if (atom.isPlace()) { // replace the places with the corresponding of the subnet
            String id = atom.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
            if (!net.containsPlace(id)) {
                return new LTLConstants.False();
            }
            Place p = net.getPlace(id);
            return new LTLAtomicProposition(p);
        } else if (atom.isTransition()) { // replace the transitions with a disjunction of all equally labelled transitions of this subnet
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff)) // the transitions of my subnet
                        && !t.getId().endsWith(PnwtAndNbFlowFormulas2PNSequential.NEXT_ID + "-" + nb_ff) // which are not the nxt transitions
                        && t.getLabel().equals(atom.toString())) { // with the same label as the atom
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            return FormulaCreator.bigWedgeOrVeeObject(elements, false);
        }
        return phi;
    }

    /**
     * Replaces the next operator, globally, and finally operators
     *
     * @param orig
     * @param net
     * @param phi
     * @param nb_ff
     * @param scopeEventually
     * @return
     */
    @Override
    ILTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, int nb_ff, boolean scopeEventually) {
        if (phi.getOp() == LTLOperators.Unary.F) { // finally
            scopeEventually = true;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi()).isEmpty()) {
                return new LTLFormula(phi.getOp(), replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually)); // todo: could make it smarter and pass here that there is no transition in the subformulas
            }
            ILTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
            // F (( My OR stuttering) AND phi)
            return new LTLFormula(LTLOperators.Unary.F,
                    new LTLFormula(new LTLFormula(getMyRealFlowTransitions(net, nb_ff),
                            LTLOperators.Binary.OR,
                            getMyStutteringFlowTransitions(net, nb_ff)),
                            LTLOperators.Binary.AND,
                            substChildPhi));
        } else if (phi.getOp() == LTLOperators.Unary.G) { // globally
            scopeEventually = false;// if the last operator is a globally, then the previous eventually is not helping anymore
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi()).isEmpty()) {
                return new LTLFormula(phi.getOp(), replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually)); // todo: could make it smarter and pass here that there is no transition in the subformulas
            }
            ILTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
            // G(!(my or stuttering) or phi)
            return new LTLFormula(LTLOperators.Unary.G,
                    new LTLFormula(
                            new LTLFormula(LTLOperators.Unary.NEG,
                                    new LTLFormula(getMyRealFlowTransitions(net, nb_ff),
                                            LTLOperators.Binary.OR,
                                            getMyStutteringFlowTransitions(net, nb_ff))
                            ),
                            LTLOperators.Binary.OR,
                            substChildPhi));
        } else if (phi.getOp() == LTLOperators.Unary.X) { // next
            ILTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
            // all other transitions
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff)) // all transitions not belonging to my subnet
                        || t.getId().endsWith(PnwtAndNbFlowFormulas2PNSequentialNoInit.NEXT_ID + "-" + nb_ff)) // but skipping transitions are OK 
                {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            // others U (my or skipping) and phi
            return new LTLFormula(
                    FormulaCreator.bigWedgeOrVeeObject(elements, false),
                    LTLOperators.Binary.U,
                    new LTLFormula(
                            new LTLFormula(getMyRealFlowTransitions(net, nb_ff),
                                    LTLOperators.Binary.OR,
                                    getMyStutteringFlowTransitions(net, nb_ff)
                            ),
                            LTLOperators.Binary.AND,
                            substChildPhi
                    )
            );
        }
        return new LTLFormula(phi.getOp(), replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually));
    }

    @Override
    ILTLFormula replaceFormulaBinaryInFlowFormula(PetriNet orig, PetriNet net, FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> phi, int nb_ff, boolean scopeEventually) {
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%% UNTIL
        if (phi.getOp() == LTLOperators.Binary.U) {
            // when in both are transitions then
            // (mine or stuttering) -> phi1 U (mine or stuttering) AND phi2
            ILTLFormula subst1;
            ILTLFormula subst2;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                subst1 = replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst1 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.IMP,
                        replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually)
                );
            }
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                subst2 = replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst2 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.AND,
                        replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually)
                );
            }
            return new LTLFormula(subst1, LTLOperators.Binary.U, subst2);
        } // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% WEAK UNTIL
        else if (phi.getOp() == LTLOperators.Binary.W) {
            // when in both are transitions then
            // (mine or stuttering) -> phi1 W (mine or stuttering) AND phi2
            ILTLFormula subst1;
            ILTLFormula subst2;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                subst1 = replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst1 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.IMP,
                        replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually)
                );
            }
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                subst2 = replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst2 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.AND,
                        replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually)
                );
            }
            return new LTLFormula(subst1, LTLOperators.Binary.W, subst2);
        }// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% RELEASE
        else if (phi.getOp() == LTLOperators.Binary.R) {
            // when in both are transitions then
            // (mine or stuttering) AND phi1 R (mine or stuttering) -> phi2
            ILTLFormula subst1;
            ILTLFormula subst2;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                subst1 = replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst1 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.AND,
                        replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually)
                );
            }
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                subst2 = replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually);// todo: could make it smarter and pass here that there is no transition in the subformulas
            } else {
                subst2 = new LTLFormula(
                        new LTLFormula(
                                getMyRealFlowTransitions(net, nb_ff),
                                LTLOperators.Binary.OR,
                                getMyStutteringFlowTransitions(net, nb_ff)
                        ),
                        LTLOperators.Binary.IMP,
                        replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually)
                );
            }
            return new LTLFormula(subst1, LTLOperators.Binary.R, subst2);
        }
        return super.replaceFormulaBinaryInFlowFormula(orig, net, phi, nb_ff, scopeEventually);
    }

    /**
     * @param orig
     * @param net
     * @param phi
     * @param scopeEventually
     * @param nbFlowFormulas
     * @return
     */
    @Override
    ILTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, boolean scopeEventually, int nbFlowFormulas) {
        if (phi.getOp() == LTLOperators.Unary.F) { // finally 
            scopeEventually = true;  // check if it's in the scope of an eventually
            runInScopeOfTemporalOperator = true;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi()).isEmpty()) {
                return new LTLFormula(phi.getOp(), (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas)); // todo: could make it smarter and pass here that there is no transition in the subformulas
            }
            ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas);
            // F (( My OR stuttering) AND phi)
            return new LTLFormula(LTLOperators.Unary.F,
                    new LTLFormula(new LTLFormula(getRunTransitions(orig),
                            LTLOperators.Binary.OR,
                            getInitAndRunStuttering(net)),
                            LTLOperators.Binary.AND,
                            substChildPhi));
        } else if (phi.getOp() == LTLOperators.Unary.G) { // globally
            scopeEventually = false; // if the last operator is a globally, then the previous eventually is not helping anymore
            runInScopeOfTemporalOperator = true;
            if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi()).isEmpty()) {
                return new LTLFormula(phi.getOp(), (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas)); // todo: could make it smarter and pass here that there is no transition in the subformulas
            }
            ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas);
            // G(!(my or stuttering) or phi)
            return new LTLFormula(LTLOperators.Unary.G,
                    new LTLFormula(
                            new LTLFormula(LTLOperators.Unary.NEG,
                                    new LTLFormula(getRunTransitions(orig),
                                            LTLOperators.Binary.OR,
                                            getInitAndRunStuttering(net))
                            ),
                            LTLOperators.Binary.OR,
                            substChildPhi));
        } else if (phi.getOp() == LTLOperators.Unary.X) { // next
            ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
            LTLFormula substPhi = new LTLFormula(phi.getOp(), substChildPhi);
            // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
            // just delete the next, meaning just return the child
//            if (scopeEventually) {
//                return substChildPhi;
//            } this is not true F p -> X q, direct would work F X = F
            if (!runInScopeOfTemporalOperator) {
                runInScopeOfTemporalOperator = true;
                return substPhi;
            }
            if (nbFlowFormulas > 0) { // it is the case where we want to replace it by means of the next operator
                // we just do the replacement when it is in the scope of X, F, or G, not as stated in the paper
                // with skipping the initial part within the formula (should be faster)
                ILTLFormula retPhi = substChildPhi;
                for (int i = 0; i <= nbFlowFormulas; i++) {
                    retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
                }
                return retPhi;
            } else { // the special case chosed by giving -1, we don't want to replace it by means of the next operator (=0 should be checked and avoided before)
                throw new RuntimeException("The approach for replacing the next operator without n nexts is not implemented for the ingoing semantics.");
            }
        }
        // all others
        ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        LTLFormula substPhi = new LTLFormula(phi.getOp(), substChildPhi);
        return substPhi;
    }

    @Override
    IFormula replaceFormulaBinaryInRunFormula(PetriNet orig, PetriNet net, FormulaBinary<IFormula, IOperatorBinary<IFormula, IFormula>, IFormula> phi, boolean scopeEventually, int nbFlowFormulas) {
        IFormula gPhi1 = phi.getPhi1();
        IFormula gPhi2 = phi.getPhi2();
        IOperatorBinary<?, ?> gOp = phi.getOp();
        // only do s.th. for the LTL part and not for the real run binary formulas of the run part
        if (phi instanceof ILTLFormula) {
            ILTLFormula phi1 = (ILTLFormula) gPhi1;
            ILTLFormula phi2 = (ILTLFormula) gPhi2;
            LTLOperators.Binary op = (LTLOperators.Binary) gOp;
            // %%%%%%%%%%%%%%%%%%%%%%%%%%%% UNTIL
            if (op == LTLOperators.Binary.U) {
                // when in both are transitions then
                // (mine or stuttering) -> phi1 U (mine or stuttering) AND phi2
                ILTLFormula subst1;
                ILTLFormula subst2;
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                    subst1 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst1 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.IMP,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas)
                    );
                }
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                    subst2 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst2 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.AND,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas)
                    );
                }
                return new LTLFormula(subst1, LTLOperators.Binary.U, subst2);
            } // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% WEAK UNTIL
            else if (op == LTLOperators.Binary.W) {
                // when in both are transitions then
                // (mine or stuttering) -> phi1 W (mine or stuttering) AND phi2
                ILTLFormula subst1;
                ILTLFormula subst2;
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                    subst1 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst1 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.IMP,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas)
                    );
                }
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                    subst2 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst2 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.AND,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas)
                    );
                }
                return new LTLFormula(subst1, LTLOperators.Binary.W, subst2);
            }// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% RELEASE
            else if (op == LTLOperators.Binary.R) {
                // when in both are transitions then
                // (mine or stuttering) AND phi1 R (mine or stuttering) -> phi2
                ILTLFormula subst1;
                ILTLFormula subst2;
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi1()).isEmpty()) {
                    subst1 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst1 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.AND,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi1, scopeEventually, nbFlowFormulas)
                    );
                }
                if (LogicsTools.getTransitionAtomicPropositions(phi.getPhi2()).isEmpty()) {
                    subst2 = (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas);// todo: could make it smarter and pass here that there is no transition in the subformulas
                } else {
                    subst2 = new LTLFormula(
                            new LTLFormula(
                                    getRunTransitions(orig),
                                    LTLOperators.Binary.OR,
                                    getInitAndRunStuttering(net)
                            ),
                            LTLOperators.Binary.IMP,
                            (ILTLFormula) replaceInRunFormula(orig, net, gPhi2, scopeEventually, nbFlowFormulas)
                    );
                }
                return new LTLFormula(subst1, LTLOperators.Binary.R, subst2);
            }
        }
        return super.replaceFormulaBinaryInRunFormula(orig, net, phi, scopeEventually, nbFlowFormulas);
    }

    /**
     *
     * @param orig
     * @param net
     * @param formula
     * @param settings
     * @return
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     */
    public ILTLFormula createFormula4ModelChecking4CircuitSequential(PetriNet orig, PetriNet net, RunLTLFormula formula, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
        boolean useNext = settings.isUseNextToReplaceXandTransitionsInRunPart();

        int nbFlowFormulas = useNext ? LogicsTools.getFlowLTLFormulas(formula).size()
                : // todo: maybe expensive, could make this smarter
                -1;

        runInScopeOfTemporalOperator = false;

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        // don't do the flow formula replacement within the framework 
        // because then we don't know the id (todo: could think of counting)
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowLTLFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowLTLFormula flowFormula = flowFormulas.get(i);
            flowFormula = replaceInFlowFormula(orig, net, flowFormula, i);

            // %%%%%%%% NEWLY CREATED CHAINS CASE
            try {
                LTLAtomicProposition init = new LTLAtomicProposition(net.getPlace(PnwtAndNbFlowFormulas2PNNoInit.INIT_TOKENFLOW_ID + "-" + i));
                // is the first operator an eventually? Then do nothing.
                boolean eventually = false;
                if (flowFormulas.get(i).getPhi() instanceof LTLFormula) {
                    ILTLFormula phi = ((LTLFormula) flowFormulas.get(i).getPhi()).getPhi();
                    if (phi instanceof FormulaUnary && (((FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi).getOp() == LTLOperators.Unary.F)) {
                        eventually = true;
                    }
                }

                //%%% skip until the considered chain is active
                ILTLFormula skipTillActiveChain = flowFormula.getPhi();
                if (!eventually) { // the whole flowformula is not in the scope of an eventually, we have to skip as long as the new chain is created
                    // all transition starting a flow
                    skipTillActiveChain = new LTLFormula(
                            init,
                            LTLOperators.Binary.U,
                            new LTLFormula(
                                    new LTLFormula(LTLOperators.Unary.NEG, init),
                                    LTLOperators.Binary.AND,
                                    flowFormula.getPhi()
                            )
                    );

                }

                // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NO CHAIN, OR WRONGLY DECIDED FOR NEW CHAIN
                // it's OK to consider no chain
                LTLFormula omitNoChainConsidered = new LTLFormula(LTLOperators.Unary.G, init);

                // %%%%% PUT TOGETHER FLOW FORMULA                
                LTLFormula flowLTL = new LTLFormula(
                        omitNoChainConsidered,
                        LTLOperators.Binary.OR,
                        skipTillActiveChain);
                // %%%%% REPLACE THE FLOW FORMULA
                f = f.substitute(flowFormulas.get(i), new RunLTLFormula(flowLTL));

            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the flow formula. (Should not happen).", ex);
            }
        }

        // %%%%%%%%%% GENERAL FORMULA
        ILTLFormula ret = LogicsTools.convert2LTL(f);

        // %%%%%%%%%%%%%%%%%%%%%%%  NOT STUCKING IN SUBNET
        // since we don't want to stop within the subnets, omit these runs
        // this is not necessary when we have MAX_IN_CIRCUIT
        if (settings.getMaximality() != AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT
                || settings.isNotStuckingAlsoByMaxInCircuit()) {
            if (settings.getStucking() != Stucking.GFo) {
                // Suprisingly, this is the faster version.
                // We demand for every activation place of the subnets
                // that it has to be left
                List<LTLFormula> elements = new ArrayList<>();
                for (Place p : net.getPlaces()) {
                    String id = p.getId();
                    if (id.startsWith(ACTIVATION_PREFIX_ID)) {
                        // this is the version when every original transition has its own activation token
                        // if it's not a orignal one (meaning the rest of the string is not a transition of the original net
//                if (!orig.containsTransition(id.substring(ACTIVATION_PREFIX_ID.length()))) {
                        // this is the version where there is only one activation token for all original transitions together
                        if (!id.equals(ACTIVATION_PREFIX_ID + "orig")) { // not the activation place of the original transitions
                            LTLFormula inf;
                            if (settings.getStucking() == Stucking.ANDGFNpi) {
                                inf = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p))));
                            } else {
                                inf = new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p));
                            }
                            elements.add(inf);
                        }
                    }
                }
                if (settings.getStucking() == Stucking.ANDGFNpi) {
                    ret = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, true), LTLOperators.Binary.IMP, ret);
                } else {
                    ILTLFormula and = FormulaCreator.bigWedgeOrVeeObject(elements, true);
                    if (settings.getStucking() == Stucking.GFANDNpiAndo) {
                        and = new LTLFormula(and, LTLOperators.Binary.AND, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")));

                    }
                    ret = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, and)), LTLOperators.Binary.IMP, ret);

                }
            } else {
                // %%%%% NEW VERSION:
                // smaller is to just asked that again and again the activation token of the original net has to be occupied
                // Also in the final setting this works since then it is a globally
                // But suprisingly this is slower
                ret = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")))),
                        LTLOperators.Binary.IMP, ret);
            }
        }
        return ret;
    }

}

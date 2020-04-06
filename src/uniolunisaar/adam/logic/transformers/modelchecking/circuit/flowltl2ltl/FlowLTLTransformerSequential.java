package uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.AtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLConstants;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings.Stucking;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitLTLMCSettings;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequential;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerSequential extends FlowLTLTransformer {

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% FLOWFORMULA PART
    /**
     * Replaces only the next operator
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
        // check if it's in the scope of an eventually
        if (!scopeEventually && phi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && phi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }
        ILTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
        LTLFormula substPhi = new LTLFormula(phi.getOp(), substChildPhi);
        if (phi.getOp() == LTLOperators.Unary.X) {
//            // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
//            // just delete the next, meaning just return the child
//            if (scopeEventually) { // scope is still the old one because of phi.getOP == X
//                return substChildPhi;
//            } // don't do this, e.g. F( p -> X q)
            // if it's not in the last scope of an eventually, then replace it according to the document
            // All other transitions then those belonging to nb_ff, apart from the next transitions
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) { // all transitions
                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                        || // not of the subformula
                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
                        ) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }

            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

            // All of the transitions belonging to nb_ff, apart from the next transitions
            elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) { // all transitions
                if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff)) //  of the subformula
                        && !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // not the nxt transitions
                        ) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }

            ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
            LTLFormula untilSecond = new LTLFormula(myTransitions, LTLOperators.Binary.AND, substPhi);
            LTLFormula until = new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond);
            LTLFormula secondDisjunct = new LTLFormula(
                    new LTLFormula(LTLOperators.Unary.G,
                            new LTLFormula(LTLOperators.Unary.NEG, myTransitions)
                    ),
                    LTLOperators.Binary.AND,
                    substChildPhi
            );
            return new LTLFormula(until, LTLOperators.Binary.OR, secondDisjunct);
        }
        return substPhi;
    }

    /**
     * Replaces only the transitions
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
        if (atom.isTransition()) {
            // all of my transitions which have the same label as the atomic proposition and are not the next transition
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                        && // the transitions of my subnet
                        !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff)
                        &&// which are not the nxt transitions
                        t.getLabel().equals(atom.toString())) { // with the same label as the atom
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
            // if it's in the direct scope of an eventually we don't need the until
            if (scopeEventually) {
                return myTransitions;
            }
            // All other transitions then those belonging to nb_ff, apart from the next transitions
            elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) { // all transitions
                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
                        || // not of the subformula
                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
                        ) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);

            return new LTLFormula(untilFirst, LTLOperators.Binary.U, myTransitions);
        } else if (atom.isPlace()) {
            String id = atom.get() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
            if (!net.containsPlace(id)) {
                return new LTLConstants.False();
            }
            Place p = net.getPlace(id);
            return new LTLAtomicProposition(p);
        }
        return phi;
    }

    @Override
    ILTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, boolean scopeEventually, int nbFlowFormulas) {
        FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = phi;

        // check if it's in the scope of an eventually
        if (!scopeEventually && castPhi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && castPhi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }

        ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, castPhi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        LTLFormula substPhi = new LTLFormula(castPhi.getOp(), substChildPhi);
        if (castPhi.getOp() == LTLOperators.Unary.X) {
            // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
            // just delete the next, meaning just return the child
//            if (scopeEventually) {
//                return substChildPhi;
//            } this is not true F p -> X q, direct would work F X = F
            if (nbFlowFormulas > 0) {
                ILTLFormula retPhi = substChildPhi;
                for (int i = 0; i < nbFlowFormulas; i++) {
                    retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
                }
                return retPhi;
            } else {
                // if it's not in the last scope of an eventually, then replace it according to the document
                // all transitions of the subnet
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId())) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                // all original transitions
                elements = new ArrayList<>();
                for (Transition t : orig.getTransitions()) {
                    elements.add(new LTLAtomicProposition(t));
                }
                ILTLFormula origTrans = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                LTLFormula untilSecond = new LTLFormula(origTrans, LTLOperators.Binary.AND, substPhi);
                LTLFormula until = new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond);
                LTLFormula secondDisjunct = new LTLFormula(
                        new LTLFormula(LTLOperators.Unary.G,
                                new LTLFormula(LTLOperators.Unary.NEG, origTrans)
                        ),
                        LTLOperators.Binary.AND,
                        substChildPhi
                );
                return new LTLFormula(until, LTLOperators.Binary.OR, secondDisjunct);
            }
        }
        return substPhi;
    }

    @Override
    IFormula replaceLTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, boolean scopeEventually, int nbFlowFormulas) {
        LTLAtomicProposition atom = phi;
        if (atom.isTransition()) {
//             if it's in the direct scope of an eventually we don't need the until
            if (scopeEventually) {
                return phi;
            }
            if (nbFlowFormulas > 0) {
                ILTLFormula retPhi = atom;
                for (int i = 0; i < nbFlowFormulas; i++) {
                    retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
                }
                return retPhi;
            } else {
                // if it's not in the last scope of an eventually, then replace it according to the document
                // all transitions of the subnets
                Collection<ILTLFormula> elements = new ArrayList<>();
                for (Transition t : net.getTransitions()) {
                    if (!orig.containsTransition(t.getId())) {
                        elements.add(new LTLAtomicProposition(t));
                    }
                }
                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
                return new LTLFormula(untilFirst, LTLOperators.Binary.U, atom);
            }
        }
        return phi;
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
        boolean initFirst = settings.isInitFirst();
        boolean useNext = settings.isUseNextToReplaceXandTransitionsInRunPart();
        int nbFlowFormulas = useNext ? LogicsTools.getFlowFormulas(formula).size()
                : // todo: maybe expensive, could make this smarter
                -1;

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        // don't do the flow formula replacement within the framework 
        // because then we don't know the id (todo: could think of counting)
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowLTLFormula> flowFormulas = LogicsTools.getFlowFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowLTLFormula flowFormula = flowFormulas.get(i);
            flowFormula = replaceInFlowFormula(orig, net, flowFormula, i);

            // %%%%%%%% NEWLY CREATED CHAINS CASE
            try {
                LTLAtomicProposition init = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.INIT_TOKENFLOW_ID + "-" + i));
                LTLAtomicProposition newChains = new LTLAtomicProposition(net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i));
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
                    if (!net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i).getPostset().isEmpty()) { // only if new chains are created during the game
//                 // %%% OLD VERSION       (attention here I don't use the 'U not new chain and phi' to ensure that the first transition is not considered of a newly created chain, check if needed here)
                        if (settings.isNewChainsBySkippingTransitions()) {
                            // %%%%%%%%%%%%%%% OLD AND MORE EXPENSIVE VERSION: we skip all other transition than those which newly starts a chain, only in the next state the formula must hold
                            List<ILTLFormula> elements = new ArrayList<>();
                            Place p = net.getPlace(PnwtAndFlowLTLtoPN.NEW_TOKENFLOW_ID + "-" + i);
                            for (Transition t : p.getPostset()) {
                                elements.add(new LTLAtomicProposition(t));
                            }
                            List<ILTLFormula> others = new ArrayList<>();
                            // All other transitions then those belonging to i, apart from the next transitions
                            for (Transition t : net.getTransitions()) { // all transitions
                                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(i))
                                        || // not of the subformula
                                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + i) // or its one of the nxt transitions
                                        ) {
                                    others.add(new LTLAtomicProposition(t));
                                }
                            }
                            skipTillActiveChain = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(others, false), LTLOperators.Binary.U,
                                    new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, false), LTLOperators.Binary.AND,
                                            new LTLFormula(LTLOperators.Unary.X, flowFormula.getPhi())));
                        } else {
//               //  %%% NEW VERSION: We skip as long as no transition creating a new chain has been token by this subnet
                            skipTillActiveChain = new LTLFormula(
                                    new LTLFormula(newChains, LTLOperators.Binary.OR, init),
                                    LTLOperators.Binary.U,
                                    // the new chain starts with a transition which has to be skipped (the next)                                    
                                    new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, newChains), LTLOperators.Binary.AND, flowFormula.getPhi()));
//                                    flowFormula.getPhi());
                        }
                    }
                }

                // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NO CHAIN, OR WRONGLY DECIDED FOR NEW CHAIN
                LTLFormula omitNoChainConsidered;
                if (initFirst) {
                    // it's OK when there is no chain or the subformula decided to consider a newly created chain but this doesn't exists in this run.
                    omitNoChainConsidered = new LTLFormula(LTLOperators.Unary.G,
                            new LTLFormula(newChains, LTLOperators.Binary.OR, init));
                } else {
                    // it's OK to consider no chain
                    omitNoChainConsidered = new LTLFormula(LTLOperators.Unary.G, init);
                }

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
        ILTLFormula ret = LogicsTools.convert(f);
        // %%%%%%%%%%%%%%%% JUMP OVER INITIALIZATION
        // The formula does not have to hold in the initilisation phase
        if (initFirst) {
            // don't need the forcing of the firing since it is later done with the active places or the max interleaving conditions
            // so we can only jump over them
            // Old version:
//            // in the beginning all init transitions are allowed to fire until the original formula has to hold
//            // to expensive better use the next            
////            ILTLFormula init = new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(allInitPlaces, true));
////            return new LTLFormula(init, LTLOperators.Binary.IMP, new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(allInitTransitions, false), LTLOperators.Binary.U, ret));
//            return new LTLFormula(init, LTLOperators.Binary.IMP, next);
            for (int i = 0; i < flowFormulas.size(); i++) {
                ret = new LTLFormula(LTLOperators.Unary.X, ret);
            }
        }

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

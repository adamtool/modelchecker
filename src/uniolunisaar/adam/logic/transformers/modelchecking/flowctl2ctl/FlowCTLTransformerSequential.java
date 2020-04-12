package uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl;

import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.logics.AtomicProposition;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLConstants;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPN.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNNoInit;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLTransformerSequential extends FlowCTLTransformer {

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% FLOWFORMULA PART
    /**
     *
     * @param orig
     * @param net
     * @param phi
     * @param nb_ff
     * @return
     */
    @Override
    ICTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nb_ff) throws NotConvertableException {
        AtomicProposition atom = (AtomicProposition) phi;
        if (atom.isTransition()) {
            throw new NotConvertableException("FlowCTL formulas with transitions can only be transformed into CTL*.");
//            // all of my transitions which have the same label as the atomic proposition and are not the next transition
//            Collection<ILTLFormula> elements = new ArrayList<>();
//            for (Transition t : net.getTransitions()) {
//                if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
//                        && // the transitions of my subnet
//                        !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff)
//                        &&// which are not the nxt transitions
//                        t.getLabel().equals(atom.toString())) { // with the same label as the atom
//                    elements.add(new LTLAtomicProposition(t));
//                }
//            }
//            ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//            // if it's in the direct scope of an eventually we don't need the until
//            if (scopeEventually) {
//                return myTransitions;
//            }
//            // All other transitions then those belonging to nb_ff, apart from the next transitions
//            elements = new ArrayList<>();
//            for (Transition t : net.getTransitions()) { // all transitions
//                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
//                        || // not of the subformula
//                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
//                        ) {
//                    elements.add(new LTLAtomicProposition(t));
//                }
//            }
//            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//
//            return new LTLFormula(untilFirst, LTLOperators.Binary.U, myTransitions);
        } else if (atom.isPlace()) {
            String id = atom.get() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
            if (!net.containsPlace(id)) {
                return new CTLConstants.False();
            }
            Place p = net.getPlace(id);
            return new CTLAtomicProposition(p);
        }
        return phi;
    }

    /**
     *
     * @param orig
     * @param net
     * @param phi
     * @param nb_ff
     * @param scopeEventually
     * @return
     */
    @Override
    ICTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ICTLFormula, CTLOperators.Unary> phi, int nb_ff) throws NotConvertableException {
        ICTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff);
        CTLFormula substPhi = new CTLFormula(phi.getOp(), substChildPhi);
        if (phi.getOp() == CTLOperators.Unary.AX || phi.getOp() == CTLOperators.Unary.EX) {
            throw new NotConvertableException("FlowCTL formulas with a next operator can only be transformed into CTL*.");

////            // if it's under the scope of eventually, this means the last temporal operator Box or Diamond is a Diamond
////            // just delete the next, meaning just return the child
////            if (scopeEventually) { // scope is still the old one because of phi.getOP == X
////                return substChildPhi;
////            } // don't do this, e.g. F( p -> X q)
//            // if it's not in the last scope of an eventually, then replace it according to the document
//            // All other transitions then those belonging to nb_ff, apart from the next transitions
//            Collection<ILTLFormula> elements = new ArrayList<>();
//            for (Transition t : net.getTransitions()) { // all transitions
//                if (!(t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff))
//                        || // not of the subformula
//                        t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // or its one of the nxt transitions
//                        ) {
//                    elements.add(new LTLAtomicProposition(t));
//                }
//            }
//
//            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//
//            // All of the transitions belonging to nb_ff, apart from the next transitions
//            elements = new ArrayList<>();
//            for (Transition t : net.getTransitions()) { // all transitions
//                if ((t.hasExtension("subformula") && t.getExtension("subformula").equals(nb_ff)) //  of the subformula
//                        && !t.getId().endsWith(PnwtAndFlowLTLtoPNSequential.NEXT_ID + "-" + nb_ff) // not the nxt transitions
//                        ) {
//                    elements.add(new LTLAtomicProposition(t));
//                }
//            }
//
//            ILTLFormula myTransitions = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//            LTLFormula untilSecond = new LTLFormula(myTransitions, LTLOperators.Binary.AND, substPhi);
//            LTLFormula until = new LTLFormula(untilFirst, LTLOperators.Binary.U, untilSecond);
//            LTLFormula secondDisjunct = new LTLFormula(
//                    new LTLFormula(LTLOperators.Unary.G,
//                            new LTLFormula(LTLOperators.Unary.NEG, myTransitions)
//                    ),
//                    LTLOperators.Binary.AND,
//                    substChildPhi
//            );
//            return new LTLFormula(until, LTLOperators.Binary.OR, secondDisjunct);
        }
        return substPhi;
    }

    @Override
    IFormula replaceCTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nbFlowFormulas) throws NotConvertableException {
        CTLAtomicProposition atom = phi;
        if (atom.isTransition()) {
            //todo: could maybe think of replacing it with ((AX)^n t)
            throw new NotConvertableException("FlowCTL formulas with transitions can only be transformed into CTL*.");

//            if (nbFlowFormulas > 0) {
//                ILTLFormula retPhi = atom;
//                for (int i = 0; i < nbFlowFormulas; i++) {
//                    retPhi = new LTLFormula(LTLOperators.Unary.X, retPhi); // todo: could still be smarter and move the nexts to the next temporal operator
//                }
//                return retPhi;
//            } else {
//                // if it's not in the last scope of an eventually, then replace it according to the document
//                // all transitions of the subnets
//                Collection<ILTLFormula> elements = new ArrayList<>();
//                for (Transition t : net.getTransitions()) {
//                    if (!orig.containsTransition(t.getId())) {
//                        elements.add(new LTLAtomicProposition(t));
//                    }
//                }
//                ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
//                return new LTLFormula(untilFirst, LTLOperators.Binary.U, atom);
//            }
        }
        return phi;
    }

    @Override
    ICTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ICTLFormula, CTLOperators.Unary> phi, int nbFlowFormulas) throws NotConvertableException {
        FormulaUnary<ICTLFormula, CTLOperators.Unary> castPhi = phi;

        ICTLFormula substChildPhi = (ICTLFormula) replaceInRunFormula(orig, net, castPhi.getPhi(), nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        CTLFormula substPhi = new CTLFormula(castPhi.getOp(), substChildPhi);
        if (castPhi.getOp() == CTLOperators.Unary.AX) {
            for (int i = 0; i < nbFlowFormulas; i++) { //(AX^n)\phi
                substPhi = new CTLFormula(CTLOperators.Unary.AX, substPhi);
            }
            return substPhi;
        } else if (castPhi.getOp() == CTLOperators.Unary.EX) {
            for (int i = 0; i < nbFlowFormulas; i++) { //(EX^n)\phi
                substPhi = new CTLFormula(CTLOperators.Unary.EX, substPhi);
            }
            return substPhi;
        }
        return substPhi;
    }

    /**
     *
     * @param orig
     * @param net
     * @param formula
     * @return
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     */
    public ICTLFormula createFormula4ModelChecking4CircuitSequential(PetriNet orig, PetriNet net, RunCTLFormula formula) throws NotConvertableException {
        int nbFlowFormulas = LogicsTools.getFlowCTLFormulas(formula).size();

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        // don't do the flow formula replacement within the framework 
        // because then we don't know the id (todo: could think of counting)
        IFormula f = replaceInRunFormula(orig, net, formula, nbFlowFormulas);

        // %%%%%%%%%%%%%%%%%  REPLACE WITHIN FLOW FORMULA
        List<FlowCTLFormula> flowFormulas = LogicsTools.getFlowCTLFormulas(f);
        for (int i = 0; i < flowFormulas.size(); i++) {
            FlowCTLFormula flowFormula = flowFormulas.get(i);
            flowFormula = replaceInFlowFormula(orig, net, flowFormula, i);
            try { // todo: this only works for maximal runs (didn't considered stucking in subnet!)
                CTLAtomicProposition init = new CTLAtomicProposition(net.getPlace(PnwtAndFlowCTLStarToPNNoInit.INIT_TOKENFLOW_ID + "-" + i));
                CTLFormula phi2 = new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, init), CTLOperators.Binary.AND, flowFormula.getPhi());
                CTLFormula subs = null;
                if (flowFormula.getOp() == FlowCTLFormula.FlowCTLOperator.All) {
                    // not working, would need CTL* to have as second disjunct only G init
//                    subs = new CTLFormula(init, CTLOperators.Binary.AU, phi2);
//                    subs = new CTLFormula(subs, CTLOperators.Binary.OR, new CTLFormula(CTLOperators.Unary.AG, init));
                    subs = new CTLFormula(CTLOperators.Unary.NEG,
                            new CTLFormula(init, CTLOperators.Binary.EU, new CTLFormula(new CTLFormula(CTLOperators.Unary.NEG, init), CTLOperators.Binary.AND,
                                    new CTLFormula(CTLOperators.Unary.NEG, flowFormula.getPhi()))));
                } else if (flowFormula.getOp() == FlowCTLFormula.FlowCTLOperator.Exists) {
                    subs = new CTLFormula(init, CTLOperators.Binary.EU, phi2);
                }
                f = f.substitute(flowFormulas.get(i), new RunCTLFormula(subs));
            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute. (Should not happen).", ex);
            }
        }
        return LogicsTools.convert2CTL(f);
    }
}

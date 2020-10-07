package uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ltl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.exceptions.logics.NotSubstitutableException;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN.ACTIVATION_PREFIX_ID;
import uniolunisaar.adam.util.AdamExtensions;
import uniolunisaar.adam.util.logics.FormulaCreator;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLTransformerSequential extends FlowCTLTransformer {

    @Override
    ILTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, boolean scopeEventually, int nbFlowFormulas) {
        // recursively replace it in the child
        ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        LTLFormula substPhi = new LTLFormula(phi.getOp(), substChildPhi);
        if (phi.getOp() == LTLOperators.Unary.X) {
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
        return substPhi;
    }

    @Override
    IFormula replaceLTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, boolean scopeEventually, int nbFlowFormulas) {
        LTLAtomicProposition atom = phi;
        if (atom.isTransition()) {
            // allow all transitions of the subnet until this transition is fired
            Collection<ILTLFormula> elements = new ArrayList<>();
            for (Transition t : net.getTransitions()) {
                if (!orig.containsTransition(t.getId())) {
                    elements.add(new LTLAtomicProposition(t));
                }
            }
            ILTLFormula untilFirst = FormulaCreator.bigWedgeOrVeeObject(elements, false);
            return new LTLFormula(untilFirst, LTLOperators.Binary.U, atom);
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
    public ILTLFormula createFormula4ModelChecking4CircuitSequential(PetriNet orig, PetriNet net, RunCTLForAllFormula formula, AdamCircuitFlowLTLMCSettings settings) throws NotConvertableException {
        // get the flow formulas
        List<FlowCTLFormula> flowFormulas = LogicsTools.getFlowCTLFormulas(formula);

        // %%%%%%%%%%%%%%%%% REPLACE WITHIN RUN FORMULA
        // don't do the flow formula replacement within the framework 
        // because then we don't know the id (todo: could think of counting)
        IFormula f = replaceInRunFormula(orig, net, formula, flowFormulas.size());

        // %%%%%%%%%%%%%%%%%  REPLACE FLOW FORMULA
        for (int i = 0; i < flowFormulas.size(); i++) {
            // each flow formula should now check that no word is excepted
            // i.e. the emptyness check of the automaton by ! V_ GF buchi 
            FlowCTLFormula flowFormula = flowFormulas.get(i);
            // get the buchi places of this subnet
            List<LTLAtomicProposition> buchis = new ArrayList<>();
            for (Place place : net.getPlaces()) {
                if (PetriNetExtensionHandler.isBuchi(place) && i + 1 == (Integer) place.getExtension(AdamExtensions.token.name())) { // todo: quick hack put it properly
                    buchis.add(new LTLAtomicProposition(place));
                }
            }
            LTLFormula gf = new LTLFormula(LTLOperators.Unary.NEG, new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(buchis, false))));
            try {
                // %%%%% REPLACE THE FLOW FORMULA
                f = f.substitute(flowFormula, new RunLTLFormula(gf));

            } catch (NotSubstitutableException ex) {
                throw new RuntimeException("Cannot substitute the flow formula. (Should not happen).", ex);
            }
        }

//        // %%%%%%%%%% GENERAL FORMULA
        ILTLFormula ret = LogicsTools.convert2LTL(f);
                // GF act_o AND ! FG act_o <- don't want the last one since would prevent correct stuttering (for the mode stuttering has act preset place
//        LTLAtomicProposition actO = new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig"));
//        ret = new LTLFormula(
//                new LTLFormula(
//                        new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, actO))),
//                        LTLOperators.Binary.OR,
//                        new LTLFormula(LTLOperators.Unary.NEG, new LTLFormula(LTLOperators.Unary.G, actO))),
//                LTLOperators.Binary.IMP, ret);
        ///
        ret = new LTLFormula(
                new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")))),
                LTLOperators.Binary.IMP, ret);
        
        

        return ret;

//        // %%%%%%%%%%%%%%%% JUMP OVER INITIALIZATION
//        // The formula does not have to hold in the initilisation phase
//        if (initFirst) {
//            // don't need the forcing of the firing since it is later done with the active places or the max interleaving conditions
//            // so we can only jump over them
//            // Old version:
////            // in the beginning all init transitions are allowed to fire until the original formula has to hold
////            // to expensive better use the next            
//////            ILTLFormula init = new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(allInitPlaces, true));
//////            return new LTLFormula(init, LTLOperators.Binary.IMP, new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(allInitTransitions, false), LTLOperators.Binary.U, ret));
////            return new LTLFormula(init, LTLOperators.Binary.IMP, next);
//            for (int i = 0; i < flowFormulas.size(); i++) {
//                ret = new LTLFormula(LTLOperators.Unary.X, ret);
//            }
//        }
//
//        // %%%%%%%%%%%%%%%%%%%%%%%  NOT STUCKING IN SUBNET
//        // since we don't want to stop within the subnets, omit these runs
//        // this is not necessary when we have MAX_IN_CIRCUIT
//        if (settings.getMaximality() != AdamCircuitLTLMCSettings.Maximality.MAX_INTERLEAVING_IN_CIRCUIT
//                || settings.isNotStuckingAlsoByMaxInCircuit()) {
//            if (settings.getStucking() != Stucking.GFo) {
//                // Suprisingly, this is the faster version.
//                // We demand for every activation place of the subnets
//                // that it has to be left
//                List<LTLFormula> elements = new ArrayList<>();
//                for (Place p : net.getPlaces()) {
//                    String id = p.getId();
//                    if (id.startsWith(ACTIVATION_PREFIX_ID)) {
//                        // this is the version when every original transition has its own activation token
//                        // if it's not a orignal one (meaning the rest of the string is not a transition of the original net
////                if (!orig.containsTransition(id.substring(ACTIVATION_PREFIX_ID.length()))) {
//                        // this is the version where there is only one activation token for all original transitions together
//                        if (!id.equals(ACTIVATION_PREFIX_ID + "orig")) { // not the activation place of the original transitions
//                            LTLFormula inf;
//                            if (settings.getStucking() == Stucking.ANDGFNpi) {
//                                inf = new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p))));
//                            } else {
//                                inf = new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(p));
//                            }
//                            elements.add(inf);
//                        }
//                    }
//                }
//                if (settings.getStucking() == Stucking.ANDGFNpi) {
//                    ret = new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(elements, true), LTLOperators.Binary.IMP, ret);
//                } else {
//                    ILTLFormula and = FormulaCreator.bigWedgeOrVeeObject(elements, true);
//                    if (settings.getStucking() == Stucking.GFANDNpiAndo) {
//                        and = new LTLFormula(and, LTLOperators.Binary.AND, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")));
//
//                    }
//                    ret = new LTLFormula(new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, and)), LTLOperators.Binary.IMP, ret);
//
//                }
//            } else {
//                // %%%%% NEW VERSION:
//                // smaller is to just asked that again and again the activation token of the original net has to be occupied
//                // Also in the final setting this works since then it is a globally
//                // But suprisingly this is slower
//                ret = new LTLFormula(
//                        new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.F, new LTLAtomicProposition(net.getPlace(ACTIVATION_PREFIX_ID + "orig")))),
//                        LTLOperators.Binary.IMP, ret);
//            }
//        }
//        return ret;
    }

}

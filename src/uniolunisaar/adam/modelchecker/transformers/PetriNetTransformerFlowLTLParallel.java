package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.TokenFlow;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import static uniolunisaar.adam.modelchecker.transformers.PetriNetTransformerFlowLTL.createOriginalPartOfTheNet;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class PetriNetTransformerFlowLTLParallel extends PetriNetTransformerFlowLTL {

    /**
     * Fairness must be stated within the formula.
     *
     * Not yet finished, other algorithms are more urgent :$.
     *
     * @param orig
     * @param formula
     * @param initFirstStep
     * @return
     */
    public static PetriGame createNet4ModelCheckingParallel(PetriGame orig, IRunFormula formula, boolean initFirstStep) {
        PetriGame out = createOriginalPartOfTheNet(orig, initFirstStep);
        // for every original transition add a self dependency to the activation place
        for (Transition t : orig.getTransitions()) {
            Place act = out.getPlace(ACTIVATION_PREFIX_ID + t.getId());
            out.createFlow(act, t);
            out.createFlow(t, act);
        }

        // for all subformulas
        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(formula);
        if (flowFormulas.isEmpty()) {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "."
                    + " The standard LTL model checker should be used.", false);

            return out;
        }
        int[] indices = new int[flowFormulas.size()];
        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
            // adds the subnet which only creates places and copies of transitions for each flow
            addSubFlowFormulaNet(orig, out, nb_ff, initFirstStep);
            indices[nb_ff] = nb_ff;
        }
        // add the power set of for all transitions which move the token flow
        ArrayList<ArrayList<Integer>> powerSet = new ArrayList<>();
        powerSet(indices, 0, new ArrayList<>(), powerSet);
        for (Transition t : out.getTransitions()) {
            if (!orig.containsNode(t)) {
                for (ArrayList<Integer> set : powerSet) {
                    if (set.isEmpty()) { // skip the empty set
                        continue;
                    }
                    if (set.size() == 1) { // the case that the transition had already been created by addSubFlowFormulaNet
                        if (orig.containsTransition(t.getLabel())) { // it is not the additional init transitions
                            // add the original pre- and postset                        
                            Transition torig = orig.getTransition(t.getLabel());
                            for (Place place : torig.getPreset()) {
                                out.createFlow(place, t);
                            }
                            for (Place place : torig.getPostset()) {
                                out.createFlow(t, place);
                            }

                        } else {
                            // todo do the stuff for init
                        }
                    }
                }
            }
        }
        return out;
    }

    private static void powerSet(int[] flowFormulaIndices, int index, ArrayList<Integer> used, ArrayList<ArrayList<Integer>> powerSet) {
        if (index == flowFormulaIndices.length) {
            powerSet.add(used);
        } else {
            powerSet(flowFormulaIndices, index + 1, used, powerSet);
            ArrayList<Integer> set = new ArrayList<>(used);
            set.add(flowFormulaIndices[index]);
            powerSet(flowFormulaIndices, index + 1, set, powerSet);
        }
    }

    /**
     * Only allows for checking one FlowFormula.
     *
     * Fairness assumptions must be put into the formula. Here wir only check
     * one flow formula by checking all runs and a run considers one chain.
     *
     * Deprecated since there is a version which handels more then one
     * FlowFormula and handles the other initialization approach.
     *
     * This version is from 19.10.2018 and should be identically to the
     * semantics of createNet4ModelCheckingParallel concerning the special case.
     *
     * @param net
     * @return
     */
    public static PetriGame createNet4ModelCheckingParallelOneFlowFormula(PetriGame net) {
        PetriGame out = new PetriGame(net);
        out.setName(net.getName() + "_mc");
        // Add to each original transition a place such that we can disable these transitions
        // as soon as we started to check a token chain
        for (Transition t : net.getTransitions()) {
            Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
            act.setInitialToken(1);
            out.createFlow(act, t);
            out.createFlow(t, act);
        }
        List<Place> todo = new ArrayList<>();
        // add Place for the beginning of the guessed chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID);
        init.setInitialToken(1);
        // Add places which create a new token flow
        // via initial places
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTokenflow(place)) {
                Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID);
                todo.add(p);
                out.setOrigID(p, place.getId());
                Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId());
                out.createFlow(init, t);
                out.createFlow(t, p);
                // Deactivate all original postset transitions which continue the flow
                for (Transition tr : place.getPostset()) {
                    TokenFlow tfl = net.getTokenFlow(tr, place);
                    if (tfl != null && !tfl.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()), t);
                    }
                }
            }
        }
        // via transitions
        for (Transition t : net.getTransitions()) {
            TokenFlow tfl = net.getInitialTokenFlows(t);
            if (tfl == null) {
                continue;
            }
            for (Place post : tfl.getPostset()) { // for all token flows which are created during the game
                String id = post.getId() + TOKENFLOW_SUFFIX_ID;
                Place p;
                if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                    p = out.createPlace(id);
                    todo.add(p);
                    out.setOrigID(p, post.getId());
                } else {
                    p = out.getPlace(id);
                }
                Transition tout = out.createTransition();
                tout.setLabel(t.getId());
                out.createFlow(init, tout);
                out.createFlow(tout, p);
                for (Place place : t.getPreset()) {
                    out.createFlow(place, tout);
                }
                for (Place place : t.getPostset()) {
                    out.createFlow(tout, place);
                }
                // Deactivate all original postset transitions which continue the flow
                for (Transition tr : post.getPostset()) {
                    TokenFlow tfl_out = net.getTokenFlow(tr, post);
                    if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()), tout);
                    }
                }
            }
        }

        while (!todo.isEmpty()) {
            Place pl = todo.remove(todo.size() - 1); // do it for the next one
            Place pOrig = net.getPlace(out.getOrigID(pl));
            for (Transition t : pOrig.getPostset()) { // for all transitions of the place add for all token flows a new transition
                TokenFlow tfl = net.getTokenFlow(t, pOrig);
                if (tfl == null) {
                    continue;
                }
                String actID = ACTIVATION_PREFIX_ID + t.getId();

                for (Place post : tfl.getPostset()) {
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID;
                    Place pout;
                    if (!out.containsPlace(id)) { // create a new one if not already existent
                        pout = out.createPlace(id);
                        todo.add(pout);
                        out.setOrigID(pout, post.getId());
                    } else {
                        pout = out.getPlace(id);
                    }
                    Transition tout = out.createTransition(); // create the new transition
                    tout.setLabel(t.getId());
                    if (net.isStrongFair(t)) {
                        out.setStrongFair(tout);
                    }
                    if (net.isWeakFair(t)) {
                        out.setWeakFair(tout);
                    }
                    // move the token along the token flow
                    out.createFlow(pl, tout);

                    out.createFlow(tout, pout);
                    for (Place place : t.getPreset()) { // move the tokens in the original net
                        if (!place.getId().equals(actID)) {// don't add it to the added activation transition
                            out.createFlow(place, tout);
                        }
                    }

                    for (Place place : t.getPostset()) {
                        if (!place.getId().equals(actID)) { // don't add it to the added activation transition
                            out.createFlow(tout, place);
                        }
                    }

                    // reactivate the transitions of the former step
                    for (Transition tr : pOrig.getPostset()) {
                        TokenFlow tfl_out = net.getTokenFlow(tr, pOrig);
                        if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                            out.createFlow(tout, out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()));
                        }
                    }
                    // deactivate the succeeding the flow transitions of the original net
                    for (Transition tr : post.getPostset()) {
                        TokenFlow tfl_out = net.getTokenFlow(tr, post);
                        if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                            out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()), tout);
                        }
                    }
                    // if tout has a loop to an act place means, that the transition was deactivated before and should still be deactivated -> remove the loop
                    for (Place pre : tout.getPreset()) {
                        if (pre.getId().startsWith(ACTIVATION_PREFIX_ID) && tout.getPostset().contains(pre)) {
                            out.removeFlow(pre, tout);
                            out.removeFlow(tout, pre);
                        }
                    }
                }
            }
        }
        for (Transition t : net.getTransitions()) { // delete the fairness assumption of all original transitions
            out.removeStrongFair(out.getTransition(t.getId()));
            out.removeWeakFair(out.getTransition(t.getId()));
        }
        // delete all token flows
        for (Transition t : net.getTransitions()) {
            for (Place p : t.getPreset()) {
                out.removeTokenFlow(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTokenFlow(t, p);
            }
        }
        // and the initial token flow markers
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTokenflow(place)) {
                out.removeInitialTokenflow(out.getPlace(place.getId()));
            }
        }
        return out;
    }
}

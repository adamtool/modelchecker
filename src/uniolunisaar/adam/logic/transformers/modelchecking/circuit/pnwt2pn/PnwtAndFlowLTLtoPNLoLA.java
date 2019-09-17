package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndFlowLTLtoPNLoLA {

    /**
     * Uses the standard sequential method (without inhibitor arcs) and adds the
     * fairness assumptions of the original transitions
     * (not to the instances because the fairness is only stated to the transition 
     * not to the transits). Weak fairness results in strong fairness because of
     * the sequential passing.
     *
     * @param net
     * @param formula
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingSequential(PetriNetWithTransits net, IRunFormula formula) {
        PetriNetWithTransits out = PnwtAndFlowLTLtoPNSequential.createNet4ModelCheckingSequential(net, formula, true);
        // add the fairness assumptions to all instances of the corresponding transitions
        // Because of the sequential passing of the token is a weak fairness of a transition going to be a strong fairness assumption
        for (Transition transition : net.getTransitions()) {
            /// WEAK FAIR
            if (net.isWeakFair(transition)) {
                // original transition (strong because of the sequential passing)
                out.setStrongFair(out.getTransition(transition.getId()));
                // THE FAIRNESS ONLY SAYS S.TH. ABOUT THE TRANSITION NOT ABOUT THE TRANSITS
//                // search for all corresponding transitions in the subnet
//                // they have the form: t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff + "-" + (count++)
//                for (Transition tmc : out.getTransitions()) {
//                    if (tmc.getId().startsWith(transition.getId() + PnwtAndFlowLTLtoPNSequential.TOKENFLOW_SUFFIX_ID + "-")) {
//                        out.setWeakFair(tmc);
//                    }
//                }
            }
            // STRONG FAIR
            if (net.isStrongFair(transition)) {
                // original transition
                out.setStrongFair(out.getTransition(transition.getId()));                
                // THE FAIRNESS ONLY SAYS S.TH. ABOUT THE TRANSITION NOT ABOUT THE TRANSITS
//                // search for all corresponding transitions in the subnet
//                // they have the form: t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff + "-" + (count++)
//                for (Transition tmc : out.getTransitions()) {
//                    if (tmc.getId().startsWith(transition.getId() + PnwtAndFlowLTLtoPNSequential.TOKENFLOW_SUFFIX_ID + "-")) {
//                        out.setStrongFair(tmc);
//                    }
//                }
            }
        }
        return out;
    }

    /**
     * Attention: Only allows for checking one FlowFormula
     *
     * First version with transforming fairness assumptions given at the
     * original transitions to subnets with fairness annotations to the
     * transitions.
     *
     * This version deactivates the transitions in the original which could
     * would transit the current investigated flow. This is the parallel
     * approach.
     *
     * Currently, the handling of the fairness constraints let LoLA crash
     * (hopefully just because transitions are marked fair, which never fulfill
     * the if clause)
     *
     * For fairness (only strong so far) of transitions with more than one
     * outgoing transition a buffer transition is introduced which is strong
     * fair. Currently I don't see the point for this extra transition anymore.
     *
     * @param net
     * @return
     */
    @Deprecated
    public static PetriNetWithTransits createNet4ModelChecking4LoLA(PetriNetWithTransits net) {
        PetriNetWithTransits out = new PetriNetWithTransits(net);
        out.setName(net.getName() + "_mc");
        // Add to each original transition a place such that we can disable these transitions
        // as soon as we started to check a token chain
        for (Transition t : net.getTransitions()) {
            Place act = out.createPlace("act_" + t.getId());
            act.setInitialToken(1);
            out.createFlow(act, t);
            out.createFlow(t, act);
        }
        List<Place> todo = new ArrayList<>();
        // add Place for the beginning of the guessed chain
        Place init = out.createPlace("init_tfl");
        init.setInitialToken(1);
        // Add places which create a new token flow
        // via initial places
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                Place p = out.createPlace(place.getId() + "_tf");
                todo.add(p);
                out.setOrigID(p, place.getId());
                Transition t = out.createTransition(place.getId() + "_tf_new");
                out.createFlow(init, t);
                out.createFlow(t, p);
                // Deactivate all original postset transitions which continue the flow
                for (Transition tr : place.getPostset()) {
                    Transit tfl = net.getTransit(tr, place);
                    if (tfl != null && !tfl.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace("act_" + tr.getId()), t);
                    }
                }
            }
        }
        // via transitions
        for (Transition t : net.getTransitions()) {
            Transit tfl = net.getInitialTransit(t);
            if (tfl == null) {
                continue;
            }
            for (Place post : tfl.getPostset()) { // for all token flows which are created during the game
                String id = post.getId() + "_tf";
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
                    Transit tfl_out = net.getTransit(tr, post);
                    if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace("act_" + tr.getId()), tout);
                    }
                }
            }
        }

        while (!todo.isEmpty()) {
            Place pl = todo.remove(todo.size() - 1); // do it for the next one
            Place pOrig = net.getPlace(out.getOrigID(pl));
            for (Transition t : pOrig.getPostset()) { // for all transitions of the place add for all token flows a new transition
                Transit tfl = net.getTransit(t, pOrig);
                if (tfl == null) {
                    continue;
                }
                String actID = "act_" + t.getId();
                Place buffer = null;
                // %%%%%%%%%%%%%%%%% FAIR
                if (tfl.getPostset().size() > 1 && net.isStrongFair(t)) { // if the transition is fair and we have more then one token flow successor,
                    //add buffer transition such that the fairness is not concerning the choosing of the tokenflow
                    Transition tout = out.createTransition(); // create the new transition
                    tout.setLabel(t.getId() + "_fair");
                    out.setStrongFair(tout);
                    buffer = out.createPlace();
                    out.createFlow(pl, tout);
                    out.createFlow(tout, buffer);
                    for (Place place : t.getPreset()) { // remove the tokens in the original net
                        if (!place.getId().equals(actID)) {// don't do it for the added activation place
                            out.createFlow(place, tout);
                        }
                    }
                    // deactivate all transitions which are not already deactivated of the original net
                    for (Transition tact : net.getTransitions()) {
                        // only deactivated those which are not already deactivated, i.e., those which do not belong
                        // to a transition which succeeds a flow from pl
                        Transit tokenflow = net.getTransit(tact, pOrig);
                        if (tokenflow == null || tokenflow.isEmpty()) {
                            out.createFlow(out.getPlace("act_" + tact.getId()), tout);
                        }
                    }
                } // %%%%%%%%%%%%%%%%%%%%%%%%% END FAIR
                for (Place post : tfl.getPostset()) {
                    String id = post.getId() + "_tf";
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
                    if (net.isStrongFair(t) && tfl.getPostset().size() <= 1) { // a fair transition which has only one token flow successor, thus is not handeled above, set it here to be fair
                        out.setStrongFair(tout);
                        tout.setLabel(tout.getLabel() + "_fair");
                    }
                    if (buffer == null) { // move the token along the token flow
                        out.createFlow(pl, tout);
                    } else {
                        out.createFlow(buffer, tout);
                    }
                    out.createFlow(tout, pout);
                    if (!net.isStrongFair(t) || tfl.getPostset().size() <= 1) {
                        for (Place place : t.getPreset()) { // move the tokens in the original net
                            if (!place.getId().equals(actID)) {// don't add it to the added activation transition
                                out.createFlow(place, tout);
                            }
                        }
                    }
                    for (Place place : t.getPostset()) {
                        if (!place.getId().equals(actID)) { // don't add it to the added activation transition
                            out.createFlow(tout, place);
                        }
                    }
                    if (tfl.getPostset().size() > 1 && net.isStrongFair(t)) { // reactivate all but those transition which can succeed the flow in the next step
                        for (Transition tr : net.getTransitions()) {
                            Transit tfl_out = net.getTransit(tr, post);
                            if (tfl_out == null || tfl_out.getPostset().isEmpty()) {
                                out.createFlow(tout, out.getPlace("act_" + tr.getId()));
                            }
                        }
                    } else {
                        // reactivate the transitions of the former step
                        for (Transition tr : pOrig.getPostset()) {
                            Transit tfl_out = net.getTransit(tr, pOrig);
                            if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                                out.createFlow(tout, out.getPlace("act_" + tr.getId()));
                            }
                        }
                        // deactivate the succeeding the flow transitions of the original net
                        for (Transition tr : post.getPostset()) {
                            Transit tfl_out = net.getTransit(tr, post);
                            if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                                out.createFlow(out.getPlace("act_" + tr.getId()), tout);
                            }
                        }
                        // if tout has a loop to an act place means, that the transition was deactivated before and should still be deactivated -> remove the loop
                        for (Place pre : tout.getPreset()) {
                            if (pre.getId().startsWith("act_") && tout.getPostset().contains(pre)) {
                                out.removeFlow(pre, tout);
                                out.removeFlow(tout, pre);
                            }
                        }
                    }
                }
            }
        }
        for (Transition t : net.getTransitions()) { // delete the fairness assumption of all original transitions
            out.removeStrongFair(out.getTransition(t.getId()));
        }
        return out;
    }
}

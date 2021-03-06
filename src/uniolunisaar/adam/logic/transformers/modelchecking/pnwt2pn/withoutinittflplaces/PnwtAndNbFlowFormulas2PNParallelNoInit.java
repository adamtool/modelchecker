package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndNbFlowFormulas2PNParallelNoInit extends PnwtAndNbFlowFormulas2PNNoInit {

    /**
     * Only allows for checking one FlowFormula.
     *
     * Fairness assumptions must be put into the formula. Here we only check one
     * flow formula by checking all runs and a run considers one chain.
     *
     * Deprecated since there is a version which handles more then one
     * FlowFormula and handles the other initialization approach. (this is not
     * true, because I never finished that method)
     *
     * This version is from 19.10.2018 and should be identically to the
     * semantics of createNet4ModelCheckingParallel concerning the special case.
     *
     * @param net
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingParallelOneFlowFormula(PetriNetWithTransits net) {
        PetriNetWithTransits out = new PetriNetWithTransits(net);
        out.setName(net.getName() + "_mc");

        // Add to each original transition a place such that we can disable these transitions
        // as soon as we started to check a token chain
        for (Transition t : net.getTransitions()) {
            Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
            out.setPartition(act, 0);
            act.setInitialToken(1);
            out.createFlow(act, t);
            out.createFlow(t, act);
        }
        List<Place> todo = new ArrayList<>();
        // add Place for the beginning of the guessed chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "_0");
        init.setInitialToken(1);
        out.setPartition(init, 1);

        List<Integer> subNetid = new ArrayList<>();
        subNetid.add(0);
        // via transitions
        for (Transition t : net.getTransitions()) {
            Transit tfl = net.getInitialTransit(t);
            if (tfl == null) {
                continue;
            }
            for (Place post : tfl.getPostset()) { // for all token flows which are created during the game
                String id = post.getId() + TOKENFLOW_SUFFIX_ID + "_0";
                Place p;
                if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                    p = out.createPlace(id);
                    out.setPartition(p, 1);
                    todo.add(p);
                    out.setOrigID(p, post.getId());
                } else {
                    p = out.getPlace(id);
                }
                Transition tout = out.createTransition();
                tout.putExtension("subnets", subNetid); // remember which subnets are involved
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
                        out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()), tout);
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
                String actID = ACTIVATION_PREFIX_ID + t.getId();

                for (Place post : tfl.getPostset()) {
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID + "_0";
                    Place pout;
                    if (!out.containsPlace(id)) { // create a new one if not already existent
                        pout = out.createPlace(id);
                        out.setPartition(pout, 1);
                        todo.add(pout);
                        out.setOrigID(pout, post.getId());
                    } else {
                        pout = out.getPlace(id);
                    }
                    Transition tout = out.createTransition(); // create the new transition
                    tout.putExtension("subnets", subNetid); // remember which subnets are involved
                    tout.setLabel(t.getId());
//                    if (net.isStrongFair(t)) { // don't need this, fairness is done in the formula
//                        out.setStrongFair(tout);
//                    }
//                    if (net.isWeakFair(t)) {
//                        out.setWeakFair(tout);
//                    }
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
                        Transit tfl_out = net.getTransit(tr, pOrig);
                        if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                            out.createFlow(tout, out.getPlace(ACTIVATION_PREFIX_ID + tr.getId()));
                        }
                    }
                    // deactivate the succeeding the flow transitions of the original net
                    for (Transition tr : post.getPostset()) {
                        Transit tfl_out = net.getTransit(tr, post);
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
        // cleanup
        for (Transition t : net.getTransitions()) {
            // delete the fairness assumption of all original transitions  
            Transition tout = out.getTransition(t.getId());
            out.removeStrongFair(tout);
            out.removeWeakFair(tout);
            // delete all token flows
            for (Place p : tout.getPreset()) {
                out.removeTransit(p, tout);
            }
            for (Place p : tout.getPostset()) {
                out.removeTransit(tout, p);
            }
        }
        // and the initial token flow markers
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                out.removeInitialTransit(out.getPlace(place.getId()));
            }
        }
        return out;
    }
}

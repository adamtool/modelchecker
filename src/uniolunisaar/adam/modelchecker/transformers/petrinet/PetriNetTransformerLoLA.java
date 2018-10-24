package uniolunisaar.adam.modelchecker.transformers.petrinet;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.TokenFlow;

/**
 *
 * @author Manuel Gieseking
 */
public class PetriNetTransformerLoLA {

    /**
     * Only allows for checking one FlowFormula
     *
     * First version with transforming fairness assumptions given at the
     * original transitions to subnets with fairness annotations to the
     * transitions.
     *
     * @param game
     * @return
     */
    public static PetriGame createNet4ModelChecking4LoLA(PetriGame game) {
        PetriGame out = new PetriGame(game);
        out.setName(game.getName() + "_mc");
        // Add to each original transition a place such that we can disable these transitions
        // as soon as we started to check a token chain
        for (Transition t : game.getTransitions()) {
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
        for (Place place : game.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && game.isInitialTokenflow(place)) {
                Place p = out.createPlace(place.getId() + "_tf");
                todo.add(p);
                out.setOrigID(p, place.getId());
                Transition t = out.createTransition(place.getId() + "_tf_new");
                out.createFlow(init, t);
                out.createFlow(t, p);
                // Deactivate all original postset transitions which continue the flow
                for (Transition tr : place.getPostset()) {
                    TokenFlow tfl = game.getTokenFlow(tr, place);
                    if (tfl != null && !tfl.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace("act_" + tr.getId()), t);
                    }
                }
            }
        }
        // via transitions
        for (Transition t : game.getTransitions()) {
            TokenFlow tfl = game.getInitialTokenFlows(t);
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
                    TokenFlow tfl_out = game.getTokenFlow(tr, post);
                    if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                        out.createFlow(out.getPlace("act_" + tr.getId()), tout);
                    }
                }
            }
        }

        while (!todo.isEmpty()) {
            Place pl = todo.remove(todo.size() - 1); // do it for the next one
            Place pOrig = game.getPlace(out.getOrigID(pl));
            for (Transition t : pOrig.getPostset()) { // for all transitions of the place add for all token flows a new transition
                TokenFlow tfl = game.getTokenFlow(t, pOrig);
                if (tfl == null) {
                    continue;
                }
                String actID = "act_" + t.getId();
                Place buffer = null;
                // %%%%%%%%%%%%%%%%% FAIR
                if (tfl.getPostset().size() > 1 && game.isStrongFair(t)) { // if the transition is fair and we have more then one token flow successor,
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
                    for (Transition tact : game.getTransitions()) {
                        // only deactivated those which are not already deactivated, i.e., those which do not belong
                        // to a transition which succeeds a flow from pl
                        TokenFlow tokenflow = game.getTokenFlow(tact, pOrig);
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
                    if (game.isStrongFair(t) && tfl.getPostset().size() <= 1) { // a fair transition which has only one token flow successor, thus is not handeled above, set it here to be fair
                        out.setStrongFair(tout);
                        tout.setLabel(tout.getLabel() + "_fair");
                    }
                    if (buffer == null) { // move the token along the token flow
                        out.createFlow(pl, tout);
                    } else {
                        out.createFlow(buffer, tout);
                    }
                    out.createFlow(tout, pout);
                    if (!game.isStrongFair(t) || tfl.getPostset().size() <= 1) {
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
                    if (tfl.getPostset().size() > 1 && game.isStrongFair(t)) { // reactivate all but those transition which can succeed the flow in the next step
                        for (Transition tr : game.getTransitions()) {
                            TokenFlow tfl_out = game.getTokenFlow(tr, post);
                            if (tfl_out == null || tfl_out.getPostset().isEmpty()) {
                                out.createFlow(tout, out.getPlace("act_" + tr.getId()));
                            }
                        }
                    } else {
                        // reactivate the transitions of the former step
                        for (Transition tr : pOrig.getPostset()) {
                            TokenFlow tfl_out = game.getTokenFlow(tr, pOrig);
                            if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                                out.createFlow(tout, out.getPlace("act_" + tr.getId()));
                            }
                        }
                        // deactivate the succeeding the flow transitions of the original net
                        for (Transition tr : post.getPostset()) {
                            TokenFlow tfl_out = game.getTokenFlow(tr, post);
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
        for (Transition t : game.getTransitions()) { // delete the fairness assumption of all original transitions
            out.removeStrongFair(out.getTransition(t.getId()));
        }
        return out;
    }
}

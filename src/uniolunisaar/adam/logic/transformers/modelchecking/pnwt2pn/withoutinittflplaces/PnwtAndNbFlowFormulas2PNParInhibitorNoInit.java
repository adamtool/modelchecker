package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNParallelInhibitor.addInhibitorArcsToAllPresetsOfTransits;
import uniolunisaar.adam.tools.CartesianProduct;
import uniolunisaar.adam.tools.Logger;

/**
 ** The class is a copy of PnwtAndFlowLTLtoPNParallelInhibitor but simplified
 * such that it is not possible to have places starting flow chains.
 *
 * @author Manuel Gieseking
 */
public class PnwtAndNbFlowFormulas2PNParInhibitorNoInit extends PnwtAndNbFlowFormulas2PNNoInit {

    /**
     * Fairness must be stated within the formula.
     *
     *
     * @param net
     * @param nb_flowFormulas
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingParallel(PetriNetWithTransits net, int nb_flowFormulas) {
        PetriNetWithTransits out = new PetriNetWithTransits(net);
        out.setName(net.getName() + "_mc");

        // for all subformulas
        if (nb_flowFormulas == 0) {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] There is noflow subformula within."
                    + " The standard model checker should be used.", false);

            return net;
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% PLACES
        // %%% CREATE ALL PLACES FOR EACH SUBNET 
        for (int nb_ff = 0; nb_ff < nb_flowFormulas; nb_ff++) {
            List<Place> todo = new ArrayList<>();
            // add Place for the beginning of the guessed chain
            Place init = out.createPlace(INIT_TOKENFLOW_ID + "_" + nb_ff);
            init.setInitialToken(1);
            out.setPartition(init, nb_ff + 1);
            // %%%% Add places which create a new token flow         
            // add the place in which the transit starts
            for (Transition t : net.getTransitions()) {
                Transit tfl = net.getInitialTransit(t);
                if (tfl == null) {
                    continue;
                }
                for (Place post : tfl.getPostset()) { // for all token flows which are created during the game
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                    Place p;
                    if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                        p = out.createPlace(id);
                        out.setPartition(p, nb_ff + 1);
                        todo.add(p);
                        out.setOrigID(p, post.getId());
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
                    for (Place post : tfl.getPostset()) {
                        String id = post.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                        Place pout;
                        if (!out.containsPlace(id)) { // create a new one if not already existent
                            pout = out.createPlace(id);
                            out.setPartition(pout, nb_ff + 1);
                            todo.add(pout);
                            out.setOrigID(pout, post.getId());
                        }
                    }
                }
            }
        }

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% TRANSITIONS
        // ADD THE |T|*(|Y(t)|+1)^n NEW TRANSITIONS
        for (Transition tOrig : net.getTransitions()) {
            // calculate the list of all transits +1 for (|Y(t)|+1)
            List<Pair<Place, Place>> transits = new ArrayList<>();
            // only collect those transits which are really occurring
            for (Transit transit : net.getTransits(tOrig)) {
                if (!transit.isInitial()) {
                    // if this transit belongs to an unconnected area -> skip
                    if (!out.containsPlace(transit.getPresetPlace().getId() + TOKENFLOW_SUFFIX_ID + "_0")) {
                        continue;
                    }
                }
                for (Place postOrig : transit.getPostset()) { // so here each transit
                    transits.add(new Pair<>(transit.getPresetPlace(), postOrig));
                }
            }
            // add the special marker (the +1) for that no subnet should contain any of the transits
            transits.add(null);

            // create the cartesian product to obtain (|Y(t)|+1)^n
            List<List<Pair<Place, Place>>> combinations = new ArrayList<>();
            for (int nb_ff = 0; nb_ff < nb_flowFormulas; nb_ff++) {
                combinations.add(transits);
            }
            CartesianProduct<Pair<Place, Place>> product = new CartesianProduct<>(combinations);

            // now add for all of these tuples a new transition and connect it to the subnets accordingly
            for (List<Pair<Place, Place>> tuples : product) {
                // create a list of only the ids of the subnets which are involved for this combination
                // especially if the list is empty it was the (null,null,...,null) tuple, i.e., no subnet is involved
                List<Integer> involvedSubnets = new ArrayList<>();
                for (int i = 0; i < tuples.size(); i++) {
                    Pair<Place, Place> tuple = tuples.get(i);
                    if (tuple != null) {
                        involvedSubnets.add(i);
                    }
                }

                if (!involvedSubnets.isEmpty()) { // we have to connect subnets
                    // for each combination which is not belonging to the original net add a transition and connected them accordingly
                    Transition tOut = out.createTransition();
                    tOut.putExtension("subnets", involvedSubnets); // remember which subnets are involved
                    tOut.setLabel(tOrig.getId());
                    // add all arcs and inhibitor arcs to this transition
                    for (int nb_ff = 0; nb_ff < tuples.size(); nb_ff++) {
                        Pair<Place, Place> tuple = tuples.get(nb_ff);
                        if (tuple == null) { // this net is not involved, i.e., tuple = null, or in other words !involvedSubnets.contains(nb_ff)
                            // we need inhibitor arcs to every preset place of any transit
                            addInhibitorArcsToAllPresetsOfTransits(net, out, tOrig, nb_ff);
                        } else {
                            Place preOrig = tuple.getFirst();
                            // get either the new or the corresponding place as predecessor (if was connected with transits)
                            String id = preOrig == null ? INIT_TOKENFLOW_ID + "_" + nb_ff : preOrig.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                            Place preOut = out.getPlace(id);
                            out.createFlow(preOut, tOut);
                            out.createFlow(tOut, out.getPlace(tuple.getSecond().getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff));
                        }
                    }
                    // add the original pre and postsets
                    for (Place place : tOrig.getPreset()) { // works since it is done via the ids and we copied the net
                        out.createFlow(place.getId(), tOut.getId());
                    }
                    for (Place place : tOrig.getPostset()) {// works since it is done via the ids and we copied the net
                        out.createFlow(tOut.getId(), place.getId());
                    }
                } else {
                    // no subnet should be involved, i.e., this is only allowed when no place of any transit of this transition is occupied
                    // add the inhibitor arcs for the original transition (if not already existent)
                    // if it is not a newly created token flow
                    for (int nb_ff = 0; nb_ff < nb_flowFormulas; nb_ff++) {
                        addInhibitorArcsToAllPresetsOfTransits(net, out, tOrig, nb_ff);
                    }
                }
            }
        }

        // %%%% CLEANUP
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

    /**
     * Works, but not needed anymore
     *
     * @param flowFormulaIndices
     * @param index
     * @param used
     * @param powerSet
     * @deprecated
     */
    @Deprecated
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
     * Fairness assumptions must be put into the formula. Here we only check one
     * flow formula by checking all runs and a run considers one chain.
     *
     * This version creates no transition which initially only activates the
     * net. So either an initial chain is chosen or it is chosen that a new
     * chain should be created during the runs. But we don't force the original
     * transitions which creates new chains to take this token, so therewith
     * also all later creation of chains can be considered and especially the
     * case of no chain is also considered.
     *
     * @param net
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingParallelOneFlowFormula(PetriNetWithTransits net) {
        PetriNetWithTransits out = new PetriNetWithTransits(net);
        out.setName(net.getName() + "_mc");

        List<Place> todo = new ArrayList<>();
        // add Place for the beginning of the guessed chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "_0");
        out.setPartition(init, 1);
        init.setInitialToken(1);
        // Add places which create a new token flow
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
                    // add an inhibitor arc to all original transitions whichs succeed this chain
                    // to prevent them from firing without moving the chain
                    for (Transition transition : post.getPostset()) {
                        Transition orig = out.getTransition(transition.getId());
                        Transit tfl_out = net.getTransit(transition, post);
                        if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                            Flow f = out.createFlow(p, orig);
                            out.setInhibitor(f);
                        }
                    }
                } else {
                    p = out.getPlace(id);
                }
                Transition tout = out.createTransition();
                tout.putExtension("subnets", subNetid); // remember which subnets are involved
                tout.setLabel(t.getId());
                //INITPLACES:
                out.createFlow(init, tout);
                out.createFlow(tout, p);
                for (Place place : t.getPreset()) { // works since it is done via the ids and we copied the net
                    out.createFlow(place, tout);
                }
                for (Place place : t.getPostset()) {// works since it is done via the ids and we copied the net
                    out.createFlow(tout, place);
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
                for (Place post : tfl.getPostset()) {
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID + "_0";
                    Place pout;
                    if (!out.containsPlace(id)) { // create a new one if not already existent
                        pout = out.createPlace(id);
                        out.setPartition(pout, 1);
                        todo.add(pout);
                        out.setOrigID(pout, post.getId());
                        // add an inhibitor arc to all original transitions whichs succeeds this chain
                        // to prevent them from firing without moving the chain
                        for (Transition transition : post.getPostset()) {
                            Transition orig = out.getTransition(transition.getId());
                            Transit tfl_out = net.getTransit(transition, post);
                            if (tfl_out != null && !tfl_out.getPostset().isEmpty()) {
                                Flow f = out.createFlow(pout, orig);
                                out.setInhibitor(f);
                            }
                        }
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
                        out.createFlow(place, tout);
                    }
                    for (Place place : t.getPostset()) {
                        out.createFlow(tout, place);
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

package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Token;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndNbFlowFormulas2PNParallelInhibitor extends PnwtAndNbFlowFormulas2PN {

    /**
     * Fairness must be stated within the formula.
     *
     *
     * @param net
     * @param nbFlowFormulas
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingParallel(PetriNetWithTransits net, int nbFlowFormulas) {
        PetriNetWithTransits out = new PetriNetWithTransits(net);
        out.putExtension("parallel", true);// todo: just a quick hack to have the counter example properly printed
        out.setName(net.getName() + "_mc");
        //      INITIALISATION IS NOW DONE IN THE FIRST STEP:
//           otherwise here was a problem that one could chose 
//           to consider the chain, when the original net already terminated.
//           Also just taken the token while firing the original transition 
//           should be a problem, since then everytime passing this place
//           it could be chosen to consider it as creating a new chain.  
//          Changes are marekd by keyword INITPLACES:
        // INITPLACES:
        Set<Place> origInitMarking = new HashSet<>();
        for (Node node : out.getNodes()) {
            // mark all nodes as original 
            PetriNetExtensionHandler.setOriginal(node);
            // INITPLACES: Remove the original initial marking since the initialisation is done in the first step
            if (out.containsPlace(node.getId())) {
                Place p = (Place) node;
                if (p.getInitialToken().getValue() > 0) {
                    origInitMarking.add(p);
                    p.setInitialToken(Token.ZERO);
                }
            }
        }

        // for all subformulas
        if (nbFlowFormulas == 0) {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] There is no flow formula."
                    + " The standard LTL model checker should be used.", false);

            return net;
        }
        int[] indices = new int[nbFlowFormulas];
        // %%% CREATE ALL PLACES FOR EACH SUBNET 
        for (int nb_ff = 0; nb_ff < nbFlowFormulas; nb_ff++) {
            List<Place> todo = new ArrayList<>();
            // add Place for the beginning of the guessed chain
            Place init = out.createPlace(INIT_TOKENFLOW_ID + "_" + nb_ff);
            init.setInitialToken(1);
            out.setPartition(init, nb_ff + 1);
            // %%%% Add places which create a new token flow
            // %% via initial places
            for (Place place : net.getPlaces()) {
                if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                    Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff);
                    out.setPartition(p, nb_ff + 1);
                    todo.add(p);
                    out.setOrigID(p, place.getId());
                }
            }

            //%% via transitions
            // create the guessing place
            // INITPLACES: add a place and transition for the case that a newly created chain should be considered
            //          this is necesarry since otherwise only adding a transition activating the 
            //          original initial marking would yield that still the chosing of a transition
            //          for an initial transit marked place could be chosen after the first step
            Place newTransitByTransition = out.createPlace(NEW_TOKENFLOW_ID + "_" + nb_ff);
            out.setPartition(newTransitByTransition, nb_ff + 1);

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
            indices[nb_ff] = nb_ff;
        }

        // CREATE THE INIT AND NEW CHAIN GUESSING TRANSITION
        // this means we need a transition for every combination of guesses
        // that is m = |initPlaces|+1 (the one for the new chains)
        // then we have m ^ n where n is the number of subformulas transitions
        // Create the list of input places
        List<Place> initPlaces = new ArrayList<>();
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                initPlaces.add(place);
            }
        }
        // start recursion
        List<List<String>> ids = new ArrayList<>();
        createGuesses(initPlaces, 0, nbFlowFormulas, new ArrayList<>(), ids);
        // now create transitions and the connections
        for (List<String> id : ids) {
            Transition t = out.createTransition(); // don't have to name this one, since it's used in the first step
            t.putExtension("initSubnet", true);// todo: hack
            // take each init token
            for (int nb_ff = 0; nb_ff < nbFlowFormulas; nb_ff++) {
                out.createFlow(out.getPlace(INIT_TOKENFLOW_ID + "_" + nb_ff), t);
            }
            // activate the original initial marking
            for (Place place1 : origInitMarking) {
                out.createFlow(t, place1);
            }
            // add the guessing
            for (int i = 0; i < id.size(); i++) {
                String placeId = id.get(i);
                if (placeId.equals("new")) {
                    out.createFlow(t, out.getPlace(NEW_TOKENFLOW_ID + "_" + i));
                } else {
                    out.createFlow(t, out.getPlace(placeId + TOKENFLOW_SUFFIX_ID + "_" + i));
                }
            }
        }
//        for (Place place : net.getPlaces()) {
//            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
//                for (ArrayList<Integer> set : powerSet) {
//                    Transition t = out.createTransition();
//                    // INITPLACES: activate the original initial marking
//                    for (Place place1 : origInitMarking) {
//                        out.createFlow(t, place1);
//                    }
//                    for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) { // add all arcs and inhibitor arcs to this transition
//                        if (set.contains(nb_ff)) {
//                            out.createFlow(out.getPlace(INIT_TOKENFLOW_ID + "_" + nb_ff), t);
//                            out.createFlow(t, out.getPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff));
//                        }
//                    }
//                }
//            }
//        }
//        for (ArrayList<Integer> set : powerSet) {
//            // put a token into new tokenflow
//            if (!set.isEmpty()) {
//                Transition t = out.createTransition();
//                // INITPLACES: activate the original initial marking
//                for (Place place1 : origInitMarking) {
//                    out.createFlow(t, place1);
//                }
//                for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) { // add all arcs and inhibitor arcs to this transition
//                    if (set.contains(nb_ff)) {
//                        out.createFlow(out.getPlace(INIT_TOKENFLOW_ID + "_" + nb_ff), t);
//                        out.createFlow(t, out.getPlace(NEW_TOKENFLOW_ID + "_" + nb_ff));
//                    }
//                }
//            }
//        }
        ArrayList<ArrayList<Integer>> powerSet = new ArrayList<>();
        powerSet(indices, 0, new ArrayList<>(), powerSet);

        // ADD THE POWER SET OF ALL TRANSITIONS FOR EACH TRANSIT
        for (Transition tOrig : net.getTransitions()) {
            Collection<Transit> transits = net.getTransits(tOrig);
            for (Transit transit : transits) { // for each transition and each transit
                Place preOrig = null;
                if (!transit.isInitial()) {
                    preOrig = transit.getPresetPlace();
                    // if this transit belongs to an unconnected area -> skip
                    if (!out.containsPlace(preOrig.getId() + TOKENFLOW_SUFFIX_ID + "_0")) {
                        continue;
                    }
                }
                for (Place postOrig : transit.getPostset()) { // so here each transit
                    for (ArrayList<Integer> set : powerSet) {
                        // for each combination which is not belonging to the original net add a transition and connected them accordingly
                        if (!set.isEmpty()) {
                            Transition tOut = out.createTransition();
                            tOut.putExtension("subnets", set); // remember which subnets are involved
                            tOut.setLabel(tOrig.getId());
                            for (int nb_ff = 0; nb_ff < nbFlowFormulas; nb_ff++) { // add all arcs and inhibitor arcs to this transition
                                // get either the new or the corresponding place as predecessor (if was connected with transits)
                                String id = preOrig == null ? NEW_TOKENFLOW_ID + "_" + nb_ff : preOrig.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                                Place preOut = out.getPlace(id);
                                Flow f = out.createFlow(preOut, tOut);
                                if (!set.contains(nb_ff)) {
                                    out.setInhibitor(f);
                                } else {
                                    out.createFlow(tOut, out.getPlace(postOrig.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff));
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
                            // add the inhibitor arcs for the original transition (if not already existent)
                            // if it is not a newly created token flow
                            if (preOrig != null) {
                                for (int nb_ff = 0; nb_ff < nbFlowFormulas; nb_ff++) {
                                    String id = preOrig.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                                    if (out.containsPlace(id)) { // only iff the place really was connected
                                        Place preOut = out.getPlace(id);
                                        Transition tOut = out.getTransition(tOrig.getId());
                                        if (!preOut.getPostset().contains(tOut)) {
                                            Flow f = out.createFlow(preOut, tOut);
                                            out.setInhibitor(f);
                                        }
                                    }
                                }
                            }
                        }
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

    private static void createGuesses(List<Place> initPlaces, int idx, int nbSubnets, List<String> current, List<List<String>> result) {
        for (int i = 0; i < initPlaces.size(); i++) {
            Place get = initPlaces.get(i);
            ArrayList<String> newCur = new ArrayList<>(current);
            newCur.add(get.getId());
            if (idx < nbSubnets - 1) {
                createGuesses(initPlaces, idx + 1, nbSubnets, newCur, result);
            } else {
                result.add(newCur);
            }
        }
        ArrayList<String> newCur = new ArrayList<>(current);
        newCur.add("new");
        if (idx < nbSubnets - 1) {
            createGuesses(initPlaces, idx + 1, nbSubnets, newCur, result);
        } else {
            result.add(newCur);
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
        out.putExtension("parallel", true);// todo: just a quick hack to have the counter example properly printed
        out.setName(net.getName() + "_mc");
        //      INITIALISATION IS NOW DONE IN THE FIRST STEP:
//           otherwise here was a problem that one could chose 
//           to consider the chain, when the original net already terminated.
//           Also just taken the token while firing the original transition 
//           should be a problem, since then everytime passing this place
//           it could be chosen to consider it as creating a new chain.  
//          Changes are marekd by keyword INITPLACES:
        // INITPLACES:
        Set<Place> origInitMarking = new HashSet<>();
        for (Node node : out.getNodes()) {
            // mark all nodes as original 
            PetriNetExtensionHandler.setOriginal(node);
            // INITPLACES: Remove the original initial marking since the initialisation is done in the first step
            if (out.containsPlace(node.getId())) {
                Place p = (Place) node;
                if (p.getInitialToken().getValue() > 0) {
                    origInitMarking.add(p);
                    p.setInitialToken(Token.ZERO);
                }
            }
        }

        List<Place> todo = new ArrayList<>();
        // add Place for the beginning of the guessed chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "_0");
        init.setInitialToken(1);
        // Add places which create a new token flow
        // via initial places
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "_0");
                todo.add(p);
                out.setOrigID(p, place.getId());
                Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId() + "_0");
                t.putExtension("initSubnet", true);// todo: hack
                out.createFlow(init, t);
                out.createFlow(t, p);
                // add an inhibitor arc to all original transitions whichs succeeds this chain
                // to prevent them from firing without moving the chain
                for (Transition transition : place.getPostset()) {
                    Transition orig = out.getTransition(transition.getId());
                    Transit tfl = net.getTransit(transition, place);
                    if (tfl != null && !tfl.getPostset().isEmpty()) {
                        Flow f = out.createFlow(p, orig);
                        out.setInhibitor(f);
                    }
                }
                // INITPLACES: activate the original initial marking
                for (Place place1 : origInitMarking) {
                    out.createFlow(t, place1);
                }
            }
        }

        List<Integer> subNetid = new ArrayList<>();
        subNetid.add(0);
        // INITPLACES: add a place and transition for the case that a newly created chain should be considered
        //          this is necesarry since otherwise only adding a transition activating the 
        //          original initial marking would yield that still the chosing of a transition
        //          for an initial transit marked place could be chosen after the first step
        Place newTransitByTransition = out.createPlace(NEW_TOKENFLOW_ID + "_0");
        Transition initTransitByTransition = out.createTransition(INIT_TOKENFLOW_ID + "-new" + "_0");
        initTransitByTransition.putExtension("initSubnet", true);// todo: hack
        out.createFlow(init, initTransitByTransition);
        out.createFlow(initTransitByTransition, newTransitByTransition);
        for (Place place1 : origInitMarking) {
            out.createFlow(initTransitByTransition, place1);
        }
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
//                out.createFlow(init, tout);
                out.createFlow(newTransitByTransition, tout);
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

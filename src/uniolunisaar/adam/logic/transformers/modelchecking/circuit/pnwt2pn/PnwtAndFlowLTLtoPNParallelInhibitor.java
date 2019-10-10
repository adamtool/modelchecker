package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn;

import java.util.ArrayList;
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

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndFlowLTLtoPNParallelInhibitor extends PnwtAndFlowLTLtoPN {

//    /**
//     * Fairness must be stated within the formula.
//     *
//     * Not yet finished, other algorithms are more urgent :$.
//     *
//     * @param orig
//     * @param formula
//     * @param initFirstStep
//     * @return
//     */
//    public static PetriNetWithTransits createNet4ModelCheckingParallel(PetriNetWithTransits orig, IRunFormula formula, boolean initFirstStep) {
//        PetriNetWithTransits out = createOriginalPartOfTheNet(orig, initFirstStep);
//
//        if (initFirstStep) { // delete the initial marking if init
//            for (Place p : out.getPlaces()) {
//                if (p.getInitialToken().getValue() > 0) {
//                    p.setInitialToken(0);
//                }
//            }
//        }
//
//        // Add to each original transition a place such that we can disable the transitions
//        for (Transition t : orig.getTransitions()) {
//            if (!orig.getTransits(t).isEmpty()) { // only for those which have tokenflows
//                Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
//                act.setInitialToken(1);
//            }
//        }
//
//        // for every original transition add a self dependency to the activation place
//        for (Transition t : orig.getTransitions()) {
//            Place act = out.getPlace(ACTIVATION_PREFIX_ID + t.getId());
//            out.createFlow(act, t);
//            out.createFlow(t, act);
//        }
//
//        // for all subformulas
//        List<FlowFormula> flowFormulas = LogicsTools.getFlowFormulas(formula);
//        if (flowFormulas.isEmpty()) {
//            // should not really be used, since the normal model checking should be used in these cases
//            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "."
//                    + " The standard LTL model checker should be used.", false);
//
//            return out;
//        }
//        int[] indices = new int[flowFormulas.size()];
//        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
//            // adds the subnet which only creates places and copies of transitions for each flow
//            addSubFlowFormulaNet(orig, out, nb_ff, initFirstStep);
//            indices[nb_ff] = nb_ff;
//        }
//        // add the power set of for all transitions which move the token flow
//        ArrayList<ArrayList<Integer>> powerSet = new ArrayList<>();
//        powerSet(indices, 0, new ArrayList<>(), powerSet);
//        for (Transition t : out.getTransitions()) {
//            if (!orig.containsNode(t)) {
//                for (ArrayList<Integer> set : powerSet) {
//                    if (set.isEmpty()) { // skip the empty set
//                        continue;
//                    }
//                    if (set.size() == 1) { // the case that the transition had already been created by addSubFlowFormulaNet
//                        if (orig.containsTransition(t.getLabel())) { // it is not the additional init transitions
//                            // add the original pre- and postset                        
//                            Transition torig = orig.getTransition(t.getLabel());
//                            for (Place place : torig.getPreset()) {
//                                out.createFlow(place, t);
//                            }
//                            for (Place place : torig.getPostset()) {
//                                out.createFlow(t, place);
//                            }
//
//                        } else {
//                            // todo do the stuff for init
//                        }
//                    }
//                }
//            }
//        }
//        return out;
//    }
//
//    private static void powerSet(int[] flowFormulaIndices, int index, ArrayList<Integer> used, ArrayList<ArrayList<Integer>> powerSet) {
//        if (index == flowFormulaIndices.length) {
//            powerSet.add(used);
//        } else {
//            powerSet(flowFormulaIndices, index + 1, used, powerSet);
//            ArrayList<Integer> set = new ArrayList<>(used);
//            set.add(flowFormulaIndices[index]);
//            powerSet(flowFormulaIndices, index + 1, set, powerSet);
//        }
//    }
    /**
     * Only allows for checking one FlowFormula.
     *
     * Fairness assumptions must be put into the formula. Here we only check one
     * flow formula by checking all runs and a run considers one chain.
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
        Place init = out.createPlace(INIT_TOKENFLOW_ID);
        init.setInitialToken(1);
        // Add places which create a new token flow
        // via initial places
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTransit(place)) {
                Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID);
                todo.add(p);
                out.setOrigID(p, place.getId());
                Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId());
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
        // INITPLACES: add a place and transition for the case that a newly created chain should be considered
        //          this is necesarry since otherwise only adding a transition activating the 
        //          original initial marking would yield that still the chosing of a transition
        //          for an initial transit marked place could be chosen after the first step
        Place newTransitByTransition = out.createPlace(NEW_TOKENFLOW_ID);
        Transition initTransitByTransition = out.createTransition(INIT_TOKENFLOW_ID + "-new");
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
                String id = post.getId() + TOKENFLOW_SUFFIX_ID;
                Place p;
                if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                    p = out.createPlace(id);
                    todo.add(p);
                    out.setOrigID(p, post.getId());
                    // add an inhibitor arc to all original transitions whichs succeeds this chain
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
                tout.setLabel(t.getId());
                //INITPLACES:
//                out.createFlow(init, tout);
                out.createFlow(newTransitByTransition, tout);
                out.createFlow(tout, p);
                for (Place place : t.getPreset()) {
                    out.createFlow(place, tout);
                }
                for (Place place : t.getPostset()) {
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
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID;
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
            out.removeStrongFair(out.getTransition(t.getId()));
            out.removeWeakFair(out.getTransition(t.getId()));
            // delete all token flows
            for (Place p : t.getPreset()) {
                out.removeTransit(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTransit(t, p);
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

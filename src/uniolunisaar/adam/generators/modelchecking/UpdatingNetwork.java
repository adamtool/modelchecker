package uniolunisaar.adam.generators.modelchecking;

import java.util.Random;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 *
 * @author Manuel Gieseking
 */
public class UpdatingNetwork {

    /**
     * Creates a sequential network with 'nb_nodes' nodes where one random node
     * can be removed via a network update.
     *
     * @param nb_nodes
     * @return
     */
    public static PetriNetWithTransits create(int nb_nodes) {
        Random rand = new Random(1);
        return create(nb_nodes, rand.nextInt(nb_nodes - 2) + 1);
    }

    public static PetriNetWithTransits create(int nb_nodes, int failing_node) {
        if (failing_node > nb_nodes - 2 || failing_node < 1) {
            throw new RuntimeException("The failing node " + failing_node + "is not in a suitable range: [1,"+(nb_nodes-2)+"]");
        }
        if (nb_nodes < 3) {
            throw new RuntimeException("Not a meaningful example.");
        }
        PetriNetWithTransits net = new PetriNetWithTransits("network_" + nb_nodes);
//        PetriGameExtensionHandler.setWinningConditionAnnotation(net, Condition.Objective.LTL);
        Transition tin = net.createTransition("createFlows");
//        net.setWeakFair(tin); not necessary since it is tested on all runs
        Place init = net.createPlace("pIn");
        init.setInitialToken(1);
        net.createFlow(tin, init);
        net.createFlow(init, tin);
        net.createTransit(init, tin, init);
        net.createInitialTransit(tin, init);
        Place pre = init;
        for (int i = 1; i < nb_nodes; i++) {
            Transition t = net.createTransition();
            net.setWeakFair(t);
            Place p = net.createPlace("p" + ((i == nb_nodes - 1) ? "Out" : i));
            p.setInitialToken(1);
            net.createFlow(pre, t);
            net.createFlow(t, pre);
            net.createFlow(p, t);
            net.createFlow(t, p);
            net.createTransit(pre, t, p);
            net.createTransit(p, t, p);
            if (i == failing_node) {
                Transition tr = net.createTransition();
//                net.setWeakFair(tr); net.setWeakFair(tin); not necessary since it is tested on all runs
                Place update = net.createPlace("pup");
                net.createFlow(p, tr);
                net.createFlow(tr, update);
                net.createTransit(p, tr, update);
                Transition tup = net.createTransition("tup");
                net.setWeakFair(tup);
                net.createFlow(update, tup);
                net.createFlow(tup, update);
                net.createFlow(pre, tup);
                net.createFlow(tup, pre);
            }
            if (i == failing_node + 1) {
                Transition tup = net.getTransition("tup");
                net.createFlow(tup, p);
                net.createFlow(p, tup);
                net.createTransit(net.getPlace("pup"), tup, p);
                net.createTransit(p, tup, p);
                net.createTransit((failing_node == 1) ? init : net.getPlace("p" + (failing_node - 1)), tup, p);
            }
            pre = p;
        }
        net.setReach(pre);
        return net;
    }

}

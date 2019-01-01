package uniolunisaar.adam.generators.modelchecking;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 *
 * @author Manuel Gieseking
 */
public class RedundantNetwork {

    public static PetriNetWithTransits getBasis(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
//        for (Transition t : net.getTransitions()) {
//            net.setStrongFair(t);
//        }
        return net;
    }

    public static PetriNetWithTransits getUpdatingNetwork(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
        addUpdate(net, nb_nodeInBetweenU);
//        for (Transition t : net.getTransitions()) {
//            net.setStrongFair(t);
//        }
        return net;
    }

    public static PetriNetWithTransits getUpdatingMutexNetwork(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
        addUpdate(net, nb_nodeInBetweenU);
        addMutex(net);
//        for (Transition t : net.getTransitions()) {
//            net.setStrongFair(t);
//        }
        return net;
    }

    public static PetriNetWithTransits getUpdatingIncorrectFixedMutexNetwork(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
        addUpdate(net, nb_nodeInBetweenU);
        addMutex(net);
        incorrectFixMutex(net, nb_nodeInBetweenU);
//        for (Transition t : net.getTransitions()) {
//            net.setStrongFair(t);
//        }
        return net;
    }

    /**
     * This one has the token flows for the mutex transitions which enables 
     * back the pipeline. This ist OK when we don't have infinitly many updates.
     * @param nb_nodeInBetweenU
     * @param nb_nodeInBetweenD
     * @return 
     */
    public static PetriNetWithTransits getUpdatingStillNotFixedMutexNetwork(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
        addUpdate(net, nb_nodeInBetweenU);
        addMutex(net);
        incorrectFixMutex(net, nb_nodeInBetweenU);
        stillNofixMutex(net, nb_nodeInBetweenU);
        return net;
    }

    public static PetriNetWithTransits getFixedMutexNetwork(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        if (nb_nodeInBetweenU < 1 || nb_nodeInBetweenD < 1) {
            throw new RuntimeException("less than 1 node inbetween does not make sense.");
        }
        PetriNetWithTransits net = createBasis(nb_nodeInBetweenU, nb_nodeInBetweenD);
        addUpdate(net, nb_nodeInBetweenU);
        addMutex(net);
        incorrectFixMutex(net, nb_nodeInBetweenU);
        stillNofixMutex(net, nb_nodeInBetweenU);
        alsoNotTheFinalFixMutex(net, nb_nodeInBetweenU);
        return net;
    }

    private static PetriNetWithTransits createBasis(int nb_nodeInBetweenU, int nb_nodeInBetweenD) {
        PetriNetWithTransits net = new PetriNetWithTransits("redundantFlow_basis");
//        PetriGameExtensionHandler.setWinningConditionAnnotation(net, Condition.Objective.LTL);
        // input
        Place in = net.createPlace("in");
        in.setInitialToken(1);
        // output
        Place out = net.createPlace("out");
        out.setInitialToken(1);

        // create the initializing of flows
        Transition tin = net.createTransition("createFlows");
        net.createFlow(tin, in);
        net.createFlow(in, tin);
        net.createTransit(in, tin, in);
        net.createInitialTransit(tin, in);

        // create the upper way
        Place[] plsU = net.createPlaces(nb_nodeInBetweenU);
        for (int i = 0; i < plsU.length; i++) {
            Place pl = plsU[i];
            pl.setInitialToken(1);
            // preset 
            Transition preT = net.createTransition("tU" + i);
            Place plPre = (i == 0) ? in : plsU[i - 1];
            net.createFlow(plPre, preT);
            net.createFlow(preT, plPre);
            net.createFlow(pl, preT);
            net.createFlow(preT, pl);
            net.createTransit(plPre, preT, pl);
            net.createTransit(pl, preT, pl);
            net.setWeakFair(preT);
        }
        // add the last transition
        Transition postT = net.createTransition("tUout");
        Place plPre = plsU[plsU.length - 1];
        net.createFlow(plPre, postT);
        net.createFlow(postT, plPre);
        net.createFlow(out, postT);
        net.createFlow(postT, out);
        net.createTransit(plPre, postT, out);
        net.createTransit(out, postT, out);
        net.setWeakFair(postT);

        // create the lower way
        Place[] plsD = net.createPlaces(nb_nodeInBetweenD);
        for (int i = 0; i < plsD.length; i++) {
            Place pl = plsD[i];
            pl.setInitialToken(1);
            // preset 
            Transition preT = net.createTransition("tD" + i);
            Place plPreD = (i == 0) ? in : plsD[i - 1];
            net.createFlow(plPreD, preT);
            net.createFlow(preT, plPreD);
            net.createFlow(pl, preT);
            net.createFlow(preT, pl);
            net.createTransit(plPreD, preT, pl);
            net.createTransit(pl, preT, pl);
            net.setWeakFair(preT);
        }
        // add the last transition
        Transition postTD = net.createTransition("tDout");
        Place plPreD = plsD[plsD.length - 1];
        net.createFlow(plPreD, postTD);
        net.createFlow(postTD, plPreD);
        net.createFlow(out, postTD);
        net.createFlow(postTD, out);
        net.createTransit(plPreD, postTD, out);
        net.createTransit(out, postTD, out);
        net.setWeakFair(postTD);
        return net;
    }

    private static PetriNetWithTransits addUpdate(PetriNetWithTransits net, int nb_nodesInBetweenU) {
        net.setName(net.getName() + "_withUpdate");
        // update the upper way
        Place pU = net.createPlace("pupU");
//        pU.setInitialToken(1);
        Transition to1 = net.createTransition("tupU");
        Transition to2 = net.createTransition("resU");
        net.setWeakFair(to2);
        Place in = net.getPlace("in");
        Place pU0 = net.getPlace("p0"); // first place of the upper way
        net.createFlow(pU0, to1);
        net.createFlow(to1, pU);
//        net.createFlow(pU, to1);
        net.createFlow(pU, to2);
        net.createFlow(to2, pU);
        net.createFlow(in, to2);
        net.createFlow(to2, in);
        net.createTransit(pU0, to1, pU);
//        net.createTransit(pU, to1, pU);
        net.createTransit(pU, to2, in);
        net.createTransit(in, to2, in);

        Place pD = net.createPlace("pupD");
//        pD.setInitialToken(1);
        Transition tu1 = net.createTransition("tupD");
        Transition tu2 = net.createTransition("resD");
        net.setWeakFair(tu2);
        Place pD0 = net.getPlace("p" + nb_nodesInBetweenU); // first place of the down way
        net.createFlow(pD0, tu1);
        net.createFlow(tu1, pD);
//        net.createFlow(pD, tu1);
        net.createFlow(pD, tu2);
        net.createFlow(tu2, pD);
        net.createFlow(in, tu2);
        net.createFlow(tu2, in);
        net.createTransit(pD0, tu1, pD);
//        net.createTransit(pD, tu1, pD);
        net.createTransit(pD, tu2, in);
        net.createTransit(in, tu2, in);
        return net;
    }

    private static PetriNetWithTransits addMutex(PetriNetWithTransits net) {
        net.setName(net.getName() + "_withMutex");
        Place mutex = net.createPlace("mutex");
        mutex.setInitialToken(1);
        Place mU = net.createPlace("mU");
        Place mD = net.createPlace("mD");
        Transition mtU = net.createTransition("mtU");
        Transition mtD = net.createTransition("mtD");
        net.createFlow(mD, mtD);
        net.createFlow(mU, mtU);
        net.createFlow(mtD, mutex);
        net.createFlow(mtU, mutex);
        Transition tU = net.getTransition("tupU");
        Transition tD = net.getTransition("tupD");
        net.createFlow(mutex, tU);
        net.createFlow(tU, mU);
        net.createFlow(mutex, tD);
        net.createFlow(tD, mD);
        // change the weak fairness to strong since we can now disable and enable the transitions
        for (Transition t : net.getTransitions()) {
            if (net.isWeakFair(t)) {
                net.removeWeakFair(t);
                net.setStrongFair(t);
            }
        }
        return net;
    }

    private static PetriNetWithTransits incorrectFixMutex(PetriNetWithTransits net, int nb_nodesInBetweenU) {
        net.setName(net.getName() + "_not_fixed");
        Transition mtU = net.getTransition("mtU");
        Transition mtD = net.getTransition("mtD");
        Place pD = net.getPlace("pupD");
        Place pU = net.getPlace("pupU");
        Place p1 = net.getPlace("p0");
        Place p2 = net.getPlace("p" + nb_nodesInBetweenU);
        net.createFlow(pU, mtU);
        net.createFlow(mtU, p1);
        net.createFlow(pD, mtD);
        net.createFlow(mtD, p2);
        return net;
    }

    /**
     * Adds the token flow for the mutex giving back the token.
     * @param net
     * @param nb_nodesInBetweenU
     * @return 
     */
    private static PetriNetWithTransits stillNofixMutex(PetriNetWithTransits net, int nb_nodesInBetweenU) {
        net.setName(net.getName() + "_not_fixed");
        Transition mtU = net.getTransition("mtU");
        Transition mtD = net.getTransition("mtD");
        Place pD = net.getPlace("pupD");
        Place pU = net.getPlace("pupU");
        Place p1 = net.getPlace("p0");
        Place p2 = net.getPlace("p" + nb_nodesInBetweenU);
        net.createTransit(pU, mtU, p1);
        net.createTransit(pD, mtD, p2);
        return net;
    }

    /**
     * Creates a token flow from the input place, but that's still not enough
     * since also the place of the other line where an update could have happen
     * can have the tokenflows which will infinitly cycle.
     * One would need to take all chains from all nodes till the update date (included)
     * from the other pipeline to have a correct update
     * @param net
     * @param nb_nodesInBetweenU
     * @return 
     */
    private static PetriNetWithTransits alsoNotTheFinalFixMutex(PetriNetWithTransits net, int nb_nodesInBetweenU) {
        net.setName(net.getName() + "_FIXED");
        Place in = net.getPlace("in");
        Place pU0 = net.getPlace("p0"); // first place of the upper way
        Place pD0 = net.getPlace("p" + nb_nodesInBetweenU); // first place of the down way
        Transition mtU = net.getTransition("mtU");
        Transition mtD = net.getTransition("mtD");
        net.createFlow(in, mtU);
        net.createFlow(mtU, in);
        net.createTransit(in, mtU, pU0);
        net.createFlow(in, mtD);
        net.createFlow(mtD, in);
        net.createTransit(in, mtD, pD0);
        return net;
    }
}

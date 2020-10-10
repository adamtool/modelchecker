package uniolunisaar.adam.ds.modelchecking.cex;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class ReducedCounterExample {

    private final List<List<Place>> markingSequence;
    private final List<Transition> firingSequence;
    private int loopingID = -1;

    public ReducedCounterExample(PetriNet net, CounterExample cex, boolean isMCNetCEX) {
        markingSequence = new ArrayList<>();
        firingSequence = new ArrayList<>();
        // not the resistant way to changes, but since all of the different approaches
        // are already considered in the toString method, use this one and parse it
        String cexString = cex.represenationTimeSteps(!isMCNetCEX);
        if (cexString.contains("time step 1")) { // only if there are time steps
            String[] lines = cexString.split("\n");
            for (String line : lines) {
                if (line.contains("start loop") && loopingID == -1) { // take only the first point where the looping starts
                    loopingID = markingSequence.size();
                }
                if (line.startsWith("M=")) {
                    int start = line.indexOf("{");
                    int end = line.indexOf("}");
                    // parse transition
                    String transition = line.substring(end + 4, line.length() - 1);
                    transition = transition.trim();
                    if (net.containsTransition(transition)) { // only do it for those transitions belonging to the net (this possibly omits the step of the detailed cex)
                        firingSequence.add(net.getTransition(transition));
                        // parse marking
                        String marking = line.substring(start + 1, end);
                        String[] places = marking.split(",");
                        List<Place> m = new ArrayList<>();
                        for (String place : places) {
                            place = place.trim();
                            if (net.containsPlace(place)) { // only if it belongs to the net (omitting the steps of the detailed cex)
                                m.add(net.getPlace(place));
                            }
                        }
                        markingSequence.add(m);
                    }
                }
            }
        }
    }

    public List<List<Place>> getMarkingSequence() {
        return markingSequence;
    }

    public List<Transition> getFiringSequence() {
        return firingSequence;
    }

    public int getLoopingID() {
        return loopingID;
    }

    public boolean withLoop() {
        return loopingID != -1;
    }

}

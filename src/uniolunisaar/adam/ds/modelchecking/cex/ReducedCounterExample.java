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
                    String transitionID;
                    if (line.endsWith("-")) { // case transition is just '-' stating the example is finite
                        transitionID = "-";
                    } else {
                        transitionID = line.substring(end + 4, line.length() - 1).trim();
                    }
                    if (net.containsTransition(transitionID) || transitionID.equals("-")) { // only do it for those transitions belonging to the net (this possibly omits the step of the detailed cex)
                        if (!transitionID.equals("-")) {
                            firingSequence.add(net.getTransition(transitionID));
                        }
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < firingSequence.size(); i++) {
            Transition transition = firingSequence.get(i);
            List<Place> marking = markingSequence.get(i);
            sb.append("{");
            for (Place place : marking) {
                sb.append(place.getId()).append(",");
            }
            if (!marking.isEmpty()) {
                sb.replace(sb.length() - 1, sb.length(), "}");
            } else {
                sb.append("}");
            }

            sb.append(" [").append(transition.getId()).append("> ");
        }
        if (markingSequence.size() > firingSequence.size()) { // the sequence was finite
            List<Place> marking = markingSequence.get(markingSequence.size() - 1);
            sb.append("{");
            for (Place place : marking) {
                sb.append(place.getId()).append(",");
            }
            if (!marking.isEmpty()) {
                sb.replace(sb.length() - 1, sb.length(), "}");
            } else {
                sb.append("}");
            }
        }
        if (loopingID != -1 && markingSequence.size() == firingSequence.size() - 1) { // looping and not finite
            sb.append(" ... ");
        }

        return sb.toString();
    }

}

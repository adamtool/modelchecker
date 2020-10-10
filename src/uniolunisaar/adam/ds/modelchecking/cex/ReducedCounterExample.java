package uniolunisaar.adam.ds.modelchecking.cex;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Manuel Gieseking
 */
public class ReducedCounterExample {

    private final List<List<String>> markingSequence;
    private final List<String> firingSequence;
    private int loopingID = -1;

    public ReducedCounterExample(CounterExample cex) {
        markingSequence = new ArrayList<>();
        firingSequence = new ArrayList<>();
        // not the resistant way to changes, but since all of the different approaches
        // are already considered in the toString method, use this one and parse it
        String cexString = cex.toString();
        if (cexString.contains("time step 1")) { // only if there are time steps
            String[] lines = cexString.split("\n");
            for (String line : lines) {
                if (line.contains("start loop") && loopingID == -1) { // take only the first point where the looping starts
                    loopingID = markingSequence.size();
                }
                if (line.startsWith("M=")) {
                    int start = line.indexOf("{");
                    int end = line.indexOf("}");
                    // parse marking
                    String marking = line.substring(start + 1, end);
                    String[] places = marking.split(",");
                    List<String> m = new ArrayList<>();
                    for (String place : places) {
                        m.add(place);
                    }
                    markingSequence.add(m);
                    // parse transition
                    String transition = line.substring(end + 4, line.length() - 1);
                    firingSequence.add(transition);
                }
            }
        }
    }

    public List<List<String>> getMarkingSequence() {
        return markingSequence;
    }

    public List<String> getFiringSequence() {
        return firingSequence;
    }

    public int getLoopingID() {
        return loopingID;
    }

    public boolean withLoop() {
        return loopingID != -1;
    }

}

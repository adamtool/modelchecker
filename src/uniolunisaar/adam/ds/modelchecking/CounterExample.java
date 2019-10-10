package uniolunisaar.adam.ds.modelchecking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExample {

    private final List<CounterExampleElement> timestep = new ArrayList<>();
    private final boolean safety;
    private final boolean liveness;
    private boolean isDetailed;

    // Only for the logCod case
    private Map<String, Transition> codingMap;

    public CounterExample(boolean safety, boolean liveness) {
        this.safety = safety;
        this.liveness = liveness;
    }

    public boolean addTimeStep(CounterExampleElement elem) {
        return timestep.add(elem);
    }

    @Override
    public String toString() {
        String cause = (safety) ? "with safety violation." : (liveness) ? "with liveness." : "found but couldn't figure out the cause.";
        StringBuilder sb = new StringBuilder("Counter example " + cause + " \n");
        for (int i = 0; i < timestep.size(); i++) {
            CounterExampleElement elem = timestep.get(i);
            StringBuilder lastElem = new StringBuilder();
            boolean add = true;
            if (!elem.getTransitions().isEmpty() || elem.getBinID() != null) { // prunes the detailed steps in the not detailed case and the last one for looping
                lastElem.append("----------- time step ").append(i).append(" -----------------");
                if (elem.isStartsLoop()) {
                    lastElem.append(" start loop ->");
                }
//                if (elem.isInitLoop() && elem.isLooping()) { // todo: check if it is really meaningfully usable
//                    elemSB.append(" looping");
//                }
                lastElem.append("\n");
                String value = elem.getStringRepresenation(isDetailed);
                if (elem.isStutter()) {
                    lastElem.append("--- stuttering ...");
                } else {
                    if (elem.getBinID() != null) { // this for binary coded
                        String binString = new String(elem.getBinID());
                        Transition t = codingMap.get(binString);
                        String id = (t != null) ? t.getId() : "-"; // if the next step is a stuttering (could still be wrong cause could be any transition but prevents nullPointer)
                        value = value.replace(CounterExampleElement.PLACEHOLDER_FOR_BINTRAN, id);
                        if (t != null && !PetriNetExtensionHandler.isOriginal(t) && !isDetailed) { // for stuttering t could be null
                            add = false;
                        }
                    }
                    lastElem.append(value);
                }
                lastElem.append("\n");
            } else {
                // check if it is looping
                if (elem.isStartsLoop()) {
                    sb.append("%%%%%%% Loop started with this step.\n");
                }
            }
            if (add) {
                //if next step is a stuttering replace the transition by an -
                if (i + 1 < timestep.size()) {
                    CounterExampleElement nextElem = timestep.get(i + 1);
                    if (nextElem.isStutter() && !elem.isStutter()) {
                        int startIdx = lastElem.indexOf("[");
                        if (startIdx >= 0) {
                            lastElem.replace(startIdx, lastElem.indexOf("]") + 1, "-");
                        }
                    }
                }
                sb.append(lastElem);
            } else {
                if (elem.isStartsLoop()) {
                    sb.append("%%%%%%% Loop started with this step.\n");
                }
            }
        }
        return sb.toString();
    }

    public void setCodingMap(Map<String, Transition> codingMap) {
        this.codingMap = codingMap;
    }

    public void setIsDetailed(boolean isDetailed) {
        this.isDetailed = isDetailed;
    }

}

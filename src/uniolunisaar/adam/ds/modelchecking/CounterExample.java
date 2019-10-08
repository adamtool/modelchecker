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
            StringBuilder elemSB = new StringBuilder();
            boolean add = true;
            if (!elem.getTransitions().isEmpty() || elem.getBinID() != null) { // prunes the detailed steps in the not detailed case and the last one for looping
                elemSB.append("----------- time step ").append(i).append(" -----------------");
                if (elem.isStartsLoop()) {
                    elemSB.append(" start loop ->");
                }
//                if (elem.isInitLoop() && elem.isLooping()) { // todo: check if it is really meaningfully usable
//                    elemSB.append(" looping");
//                }
                elemSB.append("\n");
                String value = elem.toString();
                if (elem.getBinID() != null) { // this for binary coded
                    String binString = new String(elem.getBinID());
                    Transition t = codingMap.get(binString);
                    value = value.replace(CounterExampleElement.PLACEHOLDER_FOR_BINTRAN, t.getId());
                    if (!PetriNetExtensionHandler.isOriginal(t) && !isDetailed) {
                        add = false;
                    }
                }
                elemSB.append(value).append("\n");
            } else { // check if it is looping
                if (elem.isStartsLoop()) {
                    sb.append("%%%%%%% Loop started with this step.\n");
                }
            }
            if (add) {
                sb.append(elemSB.toString());
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

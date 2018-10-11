package uniolunisaar.adam.modelchecker.circuits;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExampleElement {

    private final List<Transition> transitions = new ArrayList<>();
    private final List<Place> marking = new ArrayList<>();
    private boolean init;
    private final int timestep;

    public CounterExampleElement(int timestep) {
        this.timestep = timestep;
    }

    public void add(Transition transition) {
        transitions.add(transition);
    }

    public void add(Place place) {
        marking.add(place);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public List<Place> getMarking() {
        return marking;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public boolean isEmpty() {
        return init && transitions.isEmpty() && marking.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(timestep);
        sb.append(": ").append(init).append(" - [");
        for (Transition t : transitions) {
            sb.append(t.getId()).append(", ");
        }
        if (!transitions.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("] -> M={");
        for (Place place : marking) {
            sb.append(place.getId()).append(", ");
        }
        if (!marking.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }
}

package uniolunisaar.adam.ds.modelchecking;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExampleElement {

    public static final String PLACEHOLDER_FOR_BINTRAN = "#binID#";

    private final List<Transition> transitions = new ArrayList<>();
    private final List<Place> marking = new ArrayList<>();
    private boolean init;
    private final int timestep;
    // only needed for SafeOutStutterRegisterRenderer
    private boolean withStuttering;
    private boolean stutter;
    private boolean startsLoop = false;
    private boolean initLoop = false;
    private boolean looping = false;

    // both only for the case that it is binary coded
    private char[] binID = null;
    private int digits;

    public CounterExampleElement(int timestep, boolean withStuttering) {
        this.timestep = timestep;
        this.withStuttering = withStuttering;
    }

    public CounterExampleElement(int timestep, boolean withStuttering, int digits) {
        this.timestep = timestep;
        this.withStuttering = withStuttering;
        this.digits = digits;
    }

    public void add(int binIDChar, char val) {
        if (binID == null) {
            binID = new char[digits];
        }
        binID[binIDChar] += val;
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

    public boolean isStutter() {
        return stutter;
    }

    public void setStutter(boolean stutter) {
        this.stutter = stutter;
    }

    public boolean isWithStuttering() {
        return withStuttering;
    }

    public void setWithStuttering(boolean withStuttering) {
        this.withStuttering = withStuttering;
    }

    public boolean isStartsLoop() {
        return startsLoop;
    }

    public void setStartsLoop(boolean startsLoop) {
        this.startsLoop = startsLoop;
        if (startsLoop) {
            initLoop = true;
        }
    }

    public boolean isInitLoop() {
        return initLoop;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public boolean isEmpty() {
        return init && transitions.isEmpty() && marking.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("init:").append(init);
        if (withStuttering) {
            sb.append(", stutt:").append(stutter);
        }
        sb.append("\n[");
        if (binID == null) {
            for (Transition t : transitions) {
                sb.append(t.getId()).append(", ");
            }
            if (!transitions.isEmpty()) {
                sb.setLength(sb.length() - 2);
            }
        } else {
            sb.append(PLACEHOLDER_FOR_BINTRAN);
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

    public char[] getBinID() {
        return binID;
    }

}

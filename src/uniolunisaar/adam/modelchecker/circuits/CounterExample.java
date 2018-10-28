package uniolunisaar.adam.modelchecker.circuits;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExample {

    private final List<CounterExampleElement> timestep = new ArrayList<>();
    private final boolean safety;
    private final boolean liveness;

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
            sb.append("----------- time step ").append(i).append(" -----------------");
            if (elem.isStartsLoop()) {
                sb.append(" start loop ->");
            }
            if (elem.isInitLoop() && elem.isLooping()) {
                sb.append(" looping");
            }
            sb.append("\n");
            sb.append(timestep.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

}

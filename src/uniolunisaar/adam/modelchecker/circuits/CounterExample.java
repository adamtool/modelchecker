package uniolunisaar.adam.modelchecker.circuits;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExample {

    private final List<CounterExampleElement> timestep = new ArrayList<>();

    public boolean addTimeStep(CounterExampleElement elem) {
        return timestep.add(elem);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Counter example: \n");
        for (int i = 0; i < timestep.size(); i++) {
            sb.append("----------- time step ").append(i).append(" -----------------\n");
            sb.append(timestep.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

}

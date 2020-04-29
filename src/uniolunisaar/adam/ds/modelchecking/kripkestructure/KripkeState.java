package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Manuel Gieseking
 * @param <L>
 */
public class KripkeState<L extends ILabel> {

    private final Set<L> labels = new HashSet<>();
    private final String id;

    KripkeState(String id, L[] labels) {
        this.id = id;
        this.labels.addAll(Arrays.asList(labels));
    }

    KripkeState(String id, L label) {
        this.id = id;
        this.labels.add(label);
    }

    public Set<L> getLabels() {
        return labels;
    }

    public String getId() {
        return id;
    }

}

package uniolunisaar.adam.ds.aba;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Manuel Gieseking
 */
public class Successors {

    private final List<List<ABAState>> successors;
    private final ABATrueFalseEdge.Type special;

    public Successors() {
        this.successors = new ArrayList<>();
        this.special = null;
    }

    public Successors(ABATrueFalseEdge.Type special) {
        this.successors = new ArrayList<>();
        this.special = special;
    }

    public void add(List<ABAState> succs, boolean universal) {
        if (universal) {
            successors.add(succs);
        } else {
            for (ABAState succ : succs) {
                List<ABAState> singleton = new ArrayList<>();
                singleton.add(succ);
                successors.add(singleton);
            }
        }
    }

    public List<List<ABAState>> getSuccessors() {
        return successors;
    }

    public ABATrueFalseEdge.Type getSpecial() {
        return special;
    }

}

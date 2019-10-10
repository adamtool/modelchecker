package uniolunisaar.adam.ds.modelchecking.settings;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckingSettings {

    public enum Solver {
        ADAM_CIRCUIT,
        LOLA
    }

    public enum Approach {
        PARALLEL,
        PARALLEL_INHIBITOR,
        SEQUENTIAL,
        SEQUENTIAL_INHIBITOR
    }

    private Solver solver = Solver.ADAM_CIRCUIT;

    ModelCheckingSettings(Solver solver) {
        this.solver = solver;
    }

    public Solver getSolver() {
        return solver;
    }

}

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

    private Solver solver = Solver.ADAM_CIRCUIT;

    ModelCheckingSettings(Solver solver) {
        this.solver = solver;
    }

    public Solver getSolver() {
        return solver;
    }

}

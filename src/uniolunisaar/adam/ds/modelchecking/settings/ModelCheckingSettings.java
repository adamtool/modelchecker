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

    public enum Logic {
        LTL,
        CTL
    }
    private Solver solver = Solver.ADAM_CIRCUIT;
    private Logic logic = Logic.LTL;

    protected ModelCheckingSettings(Solver solver) {
        this.solver = solver;
    }

    public ModelCheckingSettings(Solver solver, Logic logic) {
        this.solver = solver;
        this.logic = logic;
    }
    public Solver getSolver() {
        return solver;
    }

    public Logic getLogic() {
        return logic;
    }
}

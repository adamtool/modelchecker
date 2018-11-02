package uniolunisaar.adam.modelchecker.util;

/**
 *
 * @author Manuel Gieseking
 */
public class Statistics {

    // input tool
    private long in_nb_places;
    private long in_nb_transitions;
    private long in_size_formula;
    // input model checking
    private long mc_nb_places;
    private long mc_nb_transitions;
    private long mc_size_formula;
    // input circuit
    private long sys_nb_latches;
    private long sys_nb_gates;
    private long total_nb_latches;
    private long total_nb_gates;
    // data sub tools
    private long mcHyper_sec;
    private long mcHyper_mem;
    private long aiger_sec;
    private long aiger_mem;
    private long abc_sec;
    private long abc_mem;

    public long getIn_nb_places() {
        return in_nb_places;
    }

    public void setIn_nb_places(long in_nb_places) {
        this.in_nb_places = in_nb_places;
    }

    public long getIn_nb_transitions() {
        return in_nb_transitions;
    }

    public void setIn_nb_transitions(long in_nb_transitions) {
        this.in_nb_transitions = in_nb_transitions;
    }

    public long getIn_size_formula() {
        return in_size_formula;
    }

    public void setIn_size_formula(long in_size_formula) {
        this.in_size_formula = in_size_formula;
    }

    public long getMc_nb_places() {
        return mc_nb_places;
    }

    public void setMc_nb_places(long mc_nb_places) {
        this.mc_nb_places = mc_nb_places;
    }

    public long getMc_nb_transitions() {
        return mc_nb_transitions;
    }

    public void setMc_nb_transitions(long mc_nb_transitions) {
        this.mc_nb_transitions = mc_nb_transitions;
    }

    public long getMc_size_formula() {
        return mc_size_formula;
    }

    public void setMc_size_formula(long mc_size_formula) {
        this.mc_size_formula = mc_size_formula;
    }

    public long getSys_nb_latches() {
        return sys_nb_latches;
    }

    public void setSys_nb_latches(long sys_nb_latches) {
        this.sys_nb_latches = sys_nb_latches;
    }

    public long getSys_nb_gates() {
        return sys_nb_gates;
    }

    public void setSys_nb_gates(long sys_nb_gates) {
        this.sys_nb_gates = sys_nb_gates;
    }

    public long getTotal_nb_latches() {
        return total_nb_latches;
    }

    public void setTotal_nb_latches(long total_nb_latches) {
        this.total_nb_latches = total_nb_latches;
    }

    public long getTotal_nb_gates() {
        return total_nb_gates;
    }

    public void setTotal_nb_gates(long total_nb_gates) {
        this.total_nb_gates = total_nb_gates;
    }

    public long getMcHyper_sec() {
        return mcHyper_sec;
    }

    public void setMcHyper_sec(long mcHyper_sec) {
        this.mcHyper_sec = mcHyper_sec;
    }

    public long getMcHyper_mem() {
        return mcHyper_mem;
    }

    public void setMcHyper_mem(long mcHyper_mem) {
        this.mcHyper_mem = mcHyper_mem;
    }

    public long getAiger_sec() {
        return aiger_sec;
    }

    public void setAiger_sec(long aiger_sec) {
        this.aiger_sec = aiger_sec;
    }

    public long getAiger_mem() {
        return aiger_mem;
    }

    public void setAiger_mem(long aiger_mem) {
        this.aiger_mem = aiger_mem;
    }

    public long getAbc_sec() {
        return abc_sec;
    }

    public void setAbc_sec(long abc_sec) {
        this.abc_sec = abc_sec;
    }

    public long getAbc_mem() {
        return abc_mem;
    }

    public void setAbc_mem(long abc_mem) {
        this.abc_mem = abc_mem;
    }

}

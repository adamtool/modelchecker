package uniolunisaar.adam.modelchecker.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.tools.Tools;

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
    private PetriGame mc_net;
    private ILTLFormula mc_formula;
//    private long mc_nb_places;
//    private long mc_nb_transitions;
//    private long mc_size_formula;
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
    // output
    private int satisfied = 42;

    // Write the input sizes directly into a file before the checking starts
    // for time outs
    private String path = null;

    public Statistics() {

    }

    public Statistics(String path) {
        this.path = path;
    }

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
        return mc_net.getPlaces().size();
    }

//    public void setMc_nb_places(long mc_nb_places) {
//        this.mc_nb_places = mc_nb_places;
//    }
    public long getMc_nb_transitions() {
        return mc_net.getTransitions().size();
    }

//    public void setMc_nb_transitions(long mc_nb_transitions) {
//        this.mc_nb_transitions = mc_nb_transitions;
//    }
    public long getMc_size_formula() {
        return mc_formula.getSize();
    }

//    public void setMc_size_formula(long mc_size_formula) {
//        this.mc_size_formula = mc_size_formula;
//    }
    public PetriGame getMc_net() {
        return mc_net;
    }

    public void setMc_net(PetriGame mc_net) {
        this.mc_net = mc_net;
    }

    public ILTLFormula getMc_formula() {
        return mc_formula;
    }

    public void setMc_formula(ILTLFormula mc_formula) {
        this.mc_formula = mc_formula;
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

    public int isSatisfied() {
        return satisfied;
    }

    /**
     * 1 true 0 false all other don't know
     *
     * @param satisfied
     */
    public void setSatisfied(int satisfied) {
        this.satisfied = satisfied;
    }

    public void writeInputSizesToFile() throws FileNotFoundException {
        if (path != null) {
            Tools.saveFile(path, getInputSizes());
        }
    }

    public void addResultToFile() throws IOException {
        if (path != null) {
            try (BufferedWriter wr = new BufferedWriter(new FileWriter(path, true))) {
                wr.append("\nsatisfied:").append(satisfied == 1 ? "\\cmark" : satisfied == 0 ? "\\xmark" : "?");
            };
        }
    }

    public String getInputSizes() {
        StringBuilder sb = new StringBuilder();
        sb.append("#P, #T, #F, #Pmc, #Tmc, #Fmc, #L, #G, #Lt, #Gt, |=\n");
        sb.append("sizes:")
                .append(in_nb_places).append("  &  ")
                .append(in_nb_transitions).append("  &  ")
                .append(in_size_formula).append("  &  ")
                .append(getMc_nb_places()).append("  &  ")
                .append(getMc_nb_transitions()).append("  &  ")
                .append(getMc_size_formula()).append("  &  ")
                .append(sys_nb_latches).append("  &  ")
                .append(sys_nb_gates).append("  &  ")
                .append(total_nb_latches).append("  &  ")
                .append(total_nb_gates);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getInputSizes());
        sb.append("\nsatisfied:").append(satisfied == 1 ? "\\cmark" : satisfied == 0 ? "\\xmark" : "?");
        return sb.toString();
    }

}

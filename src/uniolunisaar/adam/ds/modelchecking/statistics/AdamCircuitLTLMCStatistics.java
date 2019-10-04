package uniolunisaar.adam.ds.modelchecking.statistics;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitLTLMCStatistics {

    // input tool
    private long in_nb_places;
    private long in_nb_transitions;
    private long in_size_formula;
    // input circuit
    private long sys_nb_latches;
    private long sys_nb_gates;
    private long total_nb_latches;
    private long total_nb_gates;
    // the final formula which is really checked
    private ILTLFormula formula = null;
    // data sub tools
    private long mcHyper_sec;
    private long mcHyper_mem;
    private long aiger_sec;
    private long aiger_mem;
    private boolean measure_abc = true;
    private double abc_sec;
    private long abc_mem;
    // output
    private int satisfied = 42;

    private boolean append = false;

    // Write the input sizes directly into a file before the checking starts
    // for time outs
    private String path = null;

    // flag to indicate whether the circuit sizes of the system should 
    // separately be printed
    private boolean printSysCircuitSizes = true;

    public AdamCircuitLTLMCStatistics() {

    }

    public AdamCircuitLTLMCStatistics(String path) {
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

    public boolean isMeasure_abc() {
        return measure_abc;
    }

    public void setMeasure_abc(boolean measure_abc) {
        this.measure_abc = measure_abc;
    }

    public double getAbc_sec() {
        return abc_sec;
    }

    public void setAbc_sec(double abc_sec) {
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

    public ILTLFormula getFormulaToCheck() {
        return formula;
    }

    public void setFormulaToCheck(ILTLFormula formula) {
        this.formula = formula;
    }

    public boolean isPrintSysCircuitSizes() {
        return printSysCircuitSizes;
    }

    public void setPrintSysCircuitSizes(boolean printSysCircuitSizes) {
        this.printSysCircuitSizes = printSysCircuitSizes;
    }

    public void writeInputSizesToFile() throws FileNotFoundException {
        if (path != null) {
            Tools.saveFile(path, getInputSizes(), append);
        }
    }

    public String getPath() {
        return path;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public void addResultToFile() throws IOException {
        if (getPath() != null) {
            try (BufferedWriter wr = new BufferedWriter(new FileWriter(getPath(), true))) {
                wr.append("\nsatisfied:").append(satisfied == 1 ? "\\cmark" : satisfied == 0 ? "\\xmark" : "?");
            }
        }
    }

    public String getInputSizes() { // todo: when it's only LTL?
        StringBuilder sb = new StringBuilder();
        sb.append("#P, #T, #F, #F',");
        if (printSysCircuitSizes) {
            sb.append(" #L, #G,");
        }
        sb.append(" #Lt, #Gt, |=\n");
        sb.append("sizes:")
                .append(in_nb_places).append("  &  ")
                .append(in_nb_transitions).append("  &  ")
                .append(in_size_formula).append("  &  ")
                .append(formula.getSize()).append("  &  ");
        if (printSysCircuitSizes) {
            sb.append(sys_nb_latches).append("  &  ")
                    .append(sys_nb_gates).append("  &  ");
        }
        sb.append(total_nb_latches).append("  &  ")
                .append(total_nb_gates).append("\n");
        sb.append("formula: ").append(formula.toString());
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

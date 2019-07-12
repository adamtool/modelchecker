package uniolunisaar.adam.ds.modelchecking;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import uniolunisaar.adam.util.logics.transformers.logics.PnAndLTLtoCircuitStatistics;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelcheckingStatistics extends PnAndLTLtoCircuitStatistics {

    // data sub tools
    private double abc_sec;
    private long abc_mem;
    // output
    private int satisfied = 42;

    public ModelcheckingStatistics() {
    }

    public ModelcheckingStatistics(String path) {
        super(path);
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

    public void addResultToFile() throws IOException {
        if (super.getPath() != null) {
            try (BufferedWriter wr = new BufferedWriter(new FileWriter(super.getPath(), true))) {
                wr.append("\nsatisfied:").append(satisfied == 1 ? "\\cmark" : satisfied == 0 ? "\\xmark" : "?");
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getInputSizes());
        sb.append("\nsatisfied:").append(satisfied == 1 ? "\\cmark" : satisfied == 0 ? "\\xmark" : "?");
        return sb.toString();
    }

}

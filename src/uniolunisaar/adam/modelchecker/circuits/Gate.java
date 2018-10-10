package uniolunisaar.adam.modelchecker.circuits;

/**
 *
 * @author Manuel Gieseking
 */
public class Gate {

    private final String in1;
    private final String in2;
    private final String out;

    public Gate(String out, String in1, String in2) {
        this.in1 = in1;
        this.in2 = in2;
        this.out = out;
    }

    public String getIn1() {
        return in1;
    }

    public String getIn2() {
        return in2;
    }

    public String getOut() {
        return out;
    }
}

package uniolunisaar.adam.ds.modelchecking.aba;

/**
 *
 * @author Manuel Gieseking
 */
public interface IABANode {

    public String getId();

    public String toDot();

    public int getDotIdentifier();
}

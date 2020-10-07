package uniolunisaar.adam.ds.aba;

/**
 *
 * @author Manuel Gieseking
 */
public interface IABASubEdge {

    public IABANode getPre();

    public IABANode getPost();

    public String toDot();

}

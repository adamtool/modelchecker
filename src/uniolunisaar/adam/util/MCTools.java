package uniolunisaar.adam.util;

import java.util.HashSet;
import java.util.Set;
import uniolunisaar.adam.ds.BoundingBox;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNNoInit;

/**
 *
 * @author Manuel Gieseking
 */
public class MCTools {

    /**
     * Sets the y-coordinates of the places of the mcnet corresponding to places
     * in orig to the same value and the x-coordinates of the original
     * transitions and places also the same values as in orig. For all places in
     * a subnet we add a padding an move them appropriate to the right.
     *
     * @param orig
     * @param mcnet
     */
    public static void addCoordinates(PetriNetWithTransits orig, PetriNet mcnet) {
        BoundingBox bb = PNTools.calculateBoundingBox(orig);
        final double padding = 50;
        final double topPadding = 10;
        for (Node node : mcnet.getNodes()) {
            String id = node.getId();
            if (orig.containsNode(id)) { // the copies of the original nodes
                PetriNetExtensionHandler.setXCoord(node, orig.getXCoord(orig.getNode(id)));
                PetriNetExtensionHandler.setYCoord(node, orig.getYCoord(orig.getNode(id)));
                continue;
            }
            // we don't have to parse it, easier version below
//            if (id.contains(PnwtAndNbFlowFormulas2PNNoInit.TOKENFLOW_SUFFIX_ID)) { // is a place in a subnet which corresponds to a place of the original net
//                // those places have the form origID_PnwtAndNbFlowFormulas2PNNoInit.TOKENFLOW_SUFFIX_ID_subnetID
//                int beginningOfSuffixID = id.lastIndexOf(PnwtAndNbFlowFormulas2PNNoInit.TOKENFLOW_SUFFIX_ID);                
//                int subnet = Integer.parseInt(id.substring(beginningOfSuffixID + PnwtAndNbFlowFormulas2PNNoInit.TOKENFLOW_SUFFIX_ID.length() + 1));
            if (mcnet.containsPlace(node.getId()) && PetriNetExtensionHandler.hasOrigID((Place) node)) { // it is a place in a subnet which corresponds to a place of the original net
                Place place = (Place) node;
                String origID = PetriNetExtensionHandler.getOrigID(place);
                int subnet = PetriNetExtensionHandler.getPartition(place);
                PetriNetExtensionHandler.setXCoord(node, orig.getXCoord(orig.getNode(origID)) + subnet * (padding + bb.getWidth()));
                PetriNetExtensionHandler.setYCoord(node, orig.getYCoord(orig.getNode(origID)));
                continue;
            }
            if (mcnet.containsPlace(node.getId()) && id.contains(PnwtAndNbFlowFormulas2PNNoInit.INIT_TOKENFLOW_ID)) { // the initial places of the subnet
                Place place = (Place) node;
                int subnet = PetriNetExtensionHandler.getPartition(place);
                PetriNetExtensionHandler.setXCoord(node, bb.getLeft() + bb.getWidth() / 2.0f + subnet * (padding + bb.getWidth()));
                PetriNetExtensionHandler.setYCoord(node, bb.getTop() + topPadding);
                continue;
            }
            if (mcnet.containsTransition(node.getId())) { // it's a subnet transition
                // everything is not really nice, but the clearst view could maybe achieved by putting them in the middle of the pre- and postsets
                // and move them to the buttom
                double xMean = 0, yMean = 0;
                int diffX = 0, diffY = 0;
                Transition t = (Transition) node;
                // to only consider places which are connected by more than one arc once
                // collect the places
                Set<Place> con = new HashSet<>();
                for (Place place : t.getPreset()) {
                    con.add(place);
                }
                for (Place place : t.getPostset()) {
                    con.add(place);
                }
                for (Place place : con) {
                    if (PetriNetExtensionHandler.hasXCoord(place)) {
                        xMean += PetriNetExtensionHandler.getXCoord(place);
                        diffX++;
                    }
                    if (PetriNetExtensionHandler.hasYCoord(place)) {
                        yMean += PetriNetExtensionHandler.getYCoord(place);
                        diffY++;
                    }
                }
                PetriNetExtensionHandler.setXCoord(node, xMean / diffX);
                PetriNetExtensionHandler.setYCoord(node, bb.getBottom() + yMean / diffY);
            }
        }
    }

}

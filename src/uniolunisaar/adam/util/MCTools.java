package uniolunisaar.adam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uniolunisaar.adam.ds.BoundingBox;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.DataFlowChain;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN;
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
     * The method is optimized for the parallel approach regarding the
     * transitions
     *
     * @param orig
     * @param mcnet
     */
    public static void addCoordinates(PetriNetWithTransits orig, PetriNet mcnet) {
        BoundingBox bb = PNTools.calculateBoundingBox(orig);
        if (bb == null) {
            return;
        }
        final double padding = 50;
        final double topPadding = 10;
        List<String> coords = new ArrayList<>();
        for (Node node : mcnet.getNodes()) {
            String id = node.getId();
            if (orig.containsNode(id)) { // the copies of the original nodes
                double x = orig.getXCoord(orig.getNode(id));
                double y = orig.getYCoord(orig.getNode(id));
                coords.add(x + "," + y);
                PetriNetExtensionHandler.setXCoord(node, x);
                PetriNetExtensionHandler.setYCoord(node, y);
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
                double x = orig.getXCoord(orig.getNode(origID)) + subnet * (padding + bb.getWidth());
                double y = orig.getYCoord(orig.getNode(origID));
                coords.add(x + "," + y);
                PetriNetExtensionHandler.setXCoord(node, x);
                PetriNetExtensionHandler.setYCoord(node, y);
                continue;
            }
            if (mcnet.containsPlace(node.getId()) && id.contains(PnwtAndNbFlowFormulas2PNNoInit.INIT_TOKENFLOW_ID)) { // the initial places of the subnet
                Place place = (Place) node;
                int subnet = PetriNetExtensionHandler.getPartition(place);
                double x = bb.getLeft() + bb.getWidth() / 2.0f + subnet * (padding + bb.getWidth());
                double y = bb.getTop() - topPadding;
                coords.add(x + "," + y);
                PetriNetExtensionHandler.setXCoord(node, x);
                PetriNetExtensionHandler.setYCoord(node, y);
                continue;
            }
            if (mcnet.containsPlace(node.getId()) && id.contains(PnwtAndNbFlowFormulas2PN.NEW_TOKENFLOW_ID)) { // the new token flow place of the net
                Place place = (Place) node;
                int subnet = PetriNetExtensionHandler.getPartition(place);
                double x = bb.getLeft() + bb.getWidth() / 2.0f + subnet * (padding + bb.getWidth());
                double y = bb.getTop() - topPadding + 50;
                coords.add(x + "," + y);
                PetriNetExtensionHandler.setXCoord(node, x);
                PetriNetExtensionHandler.setYCoord(node, y);
                continue;
            }
            if (mcnet.containsTransition(node.getId())) { // it's a subnet transition
                // everything is not really nice, but the clearst view could maybe achieved by putting them in the middle of the pre- and postsets
                // and move them to the bottom
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
                double x = xMean / diffX;
                double y = bb.getBottom() + yMean / diffY;
                while (coords.contains(x + "," + y)) { // when it already exists move it a little bit
                    x += 5;
                    y -= 5;
                }
                coords.add(x + "," + y);
                PetriNetExtensionHandler.setXCoord(node, x);
                PetriNetExtensionHandler.setYCoord(node, y);
            }
        }
    }

    public static Map<Integer, DataFlowChain> getWitnessDataFlowChains(PetriNetWithTransits pnwt, ReducedCounterExample detailedCEX) {
        Map<Integer, DataFlowChain> flowchains = new HashMap<>();
        for (int i = 0; i < detailedCEX.getMarkingSequence().size(); i++) {
            List<Place> m = detailedCEX.getMarkingSequence().get(i);
            for (Place place : m) {
                if (pnwt.hasPartition(place)) {
                    DataFlowChain chain = flowchains.get(pnwt.getPartition(place));
                    if (chain == null) {
                        chain = new DataFlowChain();
                        flowchains.put(pnwt.getPartition(place), chain);
                    }
                    chain.add(place);
                }
            }
        }
        return flowchains;
    }

}

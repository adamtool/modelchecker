package uniolunisaar.adam.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.pn.Flow;
import uniolunisaar.adam.ds.BoundingBox;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.DataFlowChain;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.externaltools.pn.Dot;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PN;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndNbFlowFormulas2PNNoInit;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;

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
                double y = bb.getTop() - topPadding + 100;
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

    /**
     * Creates for each subnet the witness data flow chain.
     *
     * @param pnwt - can be any PetriNetWithTransits since it is just use the
     * have the delegate methods for the partitions.
     * @param detailedCEX
     * @param detailed
     * @return
     */
    public static Map<Integer, DataFlowChain> getWitnessDataFlowChains(PetriNetWithTransits pnwt, ReducedCounterExample detailedCEX, boolean detailed) {
        Map<Integer, DataFlowChain> flowchains = new HashMap<>();
        for (int i = 0; i < detailedCEX.getMarkingSequence().size(); i++) {
            List<Place> m = detailedCEX.getMarkingSequence().get(i);
            for (Place place : m) {
                if (pnwt.hasPartition(place)) { // only for the data flows
                    DataFlowChain chain = flowchains.get(pnwt.getPartition(place));
                    if (chain == null) {
                        chain = new DataFlowChain();
                        flowchains.put(pnwt.getPartition(place), chain);
                    }
                    if (i == 0) { // this is for each chain the init place
                        if (detailed) {
                            chain.add(place);
                        }
                    } else {
                        // add it only if the previous transition occupied the place (not all the stutterings where the token just laying around)
                        Transition t = detailedCEX.getFiringSequence().get(i - 1);
                        if (t.getPostset().contains(place) && (detailed || !place.getId().startsWith(PnwtAndNbFlowFormulas2PN.NEW_TOKENFLOW_ID))) {
                            if (detailed) {
                                chain.add(t);
                                chain.add(place);
                            } else {
                                chain.add(pnwt.getTransition(t.getLabel()));
                                chain.add(pnwt.getPlace(pnwt.getOrigID(place)));
                            }
                        }
                    }
                }
            }
        }
        return flowchains;
    }

    /**
     * Only for the cases where we don't have places marked as having an initial
     * token flow
     *
     * @param pnwt
     * @param detailedCEX
     * @return
     */
    public static String dataFlowWitnessToDot(PetriNetWithTransits pnwt, ReducedCounterExample detailedCEX) {
        Map<Integer, DataFlowChain> witnessDataFlowChains = getWitnessDataFlowChains(pnwt, detailedCEX, true);
        StringBuilder sb = new StringBuilder();
        sb.append("digraph DataFlowWitnesses {\n");

        // for each step of the markings
        List<List<Place>> markingSequence = detailedCEX.getMarkingSequence();
        List<Transition> firingSequence = detailedCEX.getFiringSequence();
        Map<Integer, String> lastNodes = new HashMap<>();
        Map<Integer, Integer> positionNextTransitionInFlowChain = new HashMap<>();
        // we can initialize all position with id 3 to skip the '<init_tfl>', the initialising transition, and the '<new_tfl>' place
        // since we only consider transits created by a transition
        for (Integer subnetID : witnessDataFlowChains.keySet()) {
            positionNextTransitionInFlowChain.put(subnetID, 3);
        }
        Integer jumpBackMarkingIDForLooping = null;
        for (int i = 1; i < markingSequence.size(); i++) { // we jump over the first step, since we don't want to have the initial guessing of the subnets here
            boolean isFinite = false; // for skipping the end for finite traces
            List<Place> marking = markingSequence.get(i);
            Transition t = i < firingSequence.size() ? firingSequence.get(i) : null;
            // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% The control part
            // get the control marking
            boolean empty = true;
            StringBuilder ctrlMarking = new StringBuilder();
            ctrlMarking.append("{");
            for (Place place : marking) {
                if (pnwt.containsNode(place.getId())) {
                    empty = false;
                    ctrlMarking.append(place.getId()).append(",");
                }
            }
            if (!empty) {
                ctrlMarking.replace(ctrlMarking.length() - 1, ctrlMarking.length(), "}");
            } else {
                ctrlMarking.append("}");
            }
            // add the new marking as a node and the transition as successor
            int markingID = ctrlMarking.hashCode();
            if (detailedCEX.getLoopingID() == i) {
                jumpBackMarkingIDForLooping = markingID;
            }
            sb.append("\"").append(markingID).append("\"[label=\"").append(ctrlMarking).append("\", shape=box]\n");
            if (lastNodes.containsKey(-1)) {
                // add the edge to the predessor (if there is one)
                sb.append("\"").append(lastNodes.get(-1)).append("\"").append("->").append("\"").append(markingID).append("\"\n");
            }
            if (t != null) {
                // check if the example is finite, this means for the second last marking the transition is not firable
                boolean isFirable = true;
                for (Flow f : t.getPresetEdges()) {
                    if (!pnwt.isInhibitor(f) && !marking.contains(f.getPlace())) {
                        System.out.println("M = " + marking.toString());
                        System.out.println("t = " + t.toString());
                        System.out.println(t.getPreset());
                        isFirable = false;
                    }
                }
                // and only add it if it is firable
                if (isFirable) {
                    // add the node for the transition
                    String transitionID = t.getId() + "." + t.hashCode() + "." + i;
                    sb.append("\"").append(transitionID).append("\"[label=\"").append(t.getLabel()).append("\", shape=none]\n");
                    // add the edge to the marking
                    sb.append("\"").append(markingID).append("\"").append("->").append("\"").append(transitionID).append("\"[arrowhead=none]\n");
                    // and add it to the last nodes
                    lastNodes.put(-1, transitionID);
                } else {
                    isFinite = true;
                }
            }

            // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% The data part
            for (Map.Entry<Integer, DataFlowChain> witness : witnessDataFlowChains.entrySet()) {
                Integer subnetId = witness.getKey();
                DataFlowChain chain = witness.getValue();
                int nextPositionId = positionNextTransitionInFlowChain.get(subnetId);
                // only when it is not the last step in general (thus no transition is available anymore)
                // or the chain has no successor anymore
                if (t != null && nextPositionId < chain.size()) {
                    // check whether the current transition belongs to the chain
                    if (t.getId().equals(chain.get(nextPositionId).getId())) {
                        // add the node for the transition
                        String transitionID = subnetId + "." + t.getId() + "." + t.hashCode() + "." + i;
                        sb.append("\"").append(transitionID).append("\"[label=\"").append(t.getLabel()).append("\", shape=none]\n");
                        // add the edge to the previous place
                        if (lastNodes.containsKey(subnetId)) {
                            sb.append("\"").append(lastNodes.get(subnetId)).append("\"").append("->").append("\"").append(transitionID).append("\"[arrowhead=none]\n");
                        }
                        // and add the next place of the chain
                        Place place = (Place) chain.get(nextPositionId + 1);
                        String placeID = subnetId + "." + place.getId() + "." + place.hashCode() + "." + i;
                        sb.append("\"").append(placeID).append("\"[label=\"").append(pnwt.getPlace(pnwt.getOrigID(place)).getId()).append("\", shape=circle]\n");
                        // put this place as the last node                        
                        lastNodes.put(subnetId, placeID);
                        // add the connecting edge between transition and place
                        sb.append("\"").append(transitionID).append("\"").append("->").append("\"").append(placeID).append("\"\n");
                        // update the next position id
                        positionNextTransitionInFlowChain.put(subnetId, nextPositionId + 2);

                        // connect the transitions of the marking and the subnet to order the chains accorrding to the markings                        
                        String markingTransitionId = t.getId() + "." + t.hashCode() + "." + i;
                        sb.append("\"").append(markingTransitionId).append("\"").append("->").append("\"").append(transitionID).append("\"[style=dashed,arrowhead=none]\n");
                        sb.append("{rank = same; \"").append(markingTransitionId).append("\";\"").append(transitionID).append("\";}\n"); // put them on the same rank
                    }
                }
                if (isFinite) {
                    i++;
                }
            }
        }
        // draw the looping edge
        if (jumpBackMarkingIDForLooping != null) {
            sb.append("\"").append(lastNodes.get(-1)).append("\"").append("->").append("\"").append(jumpBackMarkingIDForLooping).append("\"[style=dashed]\n");
        }
        sb.append("\n\n");
        sb.append("overlap=false\n");
//        sb.append("label=\"").append(net.getName()).append("\"\n");
        sb.append("fontsize=12\n");
        sb.append("}");
        return sb.toString();
    }

    public static void saveDataFlowWitnessToPDF(String path, PetriNetWithTransits pnwt, ReducedCounterExample detailedCEX, String procID) throws FileNotFoundException, IOException {
        Tools.saveFile(path + ".dot", dataFlowWitnessToDot(pnwt, detailedCEX));
        try {
            Dot.call(path + ".dot", path, true, procID);
        } catch (IOException | InterruptedException | ExternalToolException ex) {
            File dotFile = new File(path + ".dot");
            if (dotFile.exists()) {
//                Files.delete(dotFile.toPath());
            }
            Logger.getInstance().addError("Saving pdf from dot failed.", ex);
        }
    }
}

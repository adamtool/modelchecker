package uniolunisaar.adam.sdnencoding;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AptLTSParser;
import uniolunisaar.adam.ds.petrigame.PetriGame;

public class TopologyToPN {
	private final Set<Arc> edges;
	private final Set<String> ingress; // subset of vertices
	private final Update update;

	
	public TopologyToPN(File file, Set<String> i, Update u) throws ParseException, IOException {
		TransitionSystem ts = new AptLTSParser().parseFile(file);
		edges = ts.getEdges();
		ingress = i;
		update = u;
	}
	
	public boolean checkUndirectedGraphForCorrectness () {
		// TODO edges are undirected (?) and do not use non-existent vertices
		return true;
	}
	
	public PetriGame generatePetriNet() {
		PetriGame pn = new PetriGame("");
		for (Arc arc : edges) {
			if (!pn.containsPlace(arc.getSourceId())) {
				Place sw = pn.createPlace(arc.getSourceId());
				sw.setInitialToken(1);
			}
			if (!pn.containsPlace(arc.getTargetId())) {
				Place sw = pn.createPlace(arc.getTargetId());
				sw.setInitialToken(1);
			}
			createTransition(pn, arc.getSourceId(), arc.getTargetId());
		}
		for (String start : ingress) {
			Transition transition = pn.createTransition("ingress" + start);
			String transitionID = transition.getId();
			pn.createFlow(start, transitionID);
			pn.createFlow(transitionID, start);
			pn.createInitialTokenFlow(transition, pn.getPlace(start));
		}
		Place updateStart = pn.createPlace("updateStart");
		updateStart.setInitialToken(1);
		update.addUpdate(pn, updateStart);
		return pn;
	}

	private void createTransition(PetriGame pn, String pre, String post) {
		Transition transition = pn.createTransition("fwd" + pre + "to" + post);
		String transitionID = transition.getId();
		pn.createFlow(pre, transitionID);
		pn.createFlow(transitionID, pre);
		pn.createFlow(post, transitionID);
		pn.createFlow(transitionID, post);
		Place place = pn.createPlace(pre + "fwdTo" + post);
		pn.createFlow(place, transition);
		pn.createFlow(transition, place);
		pn.createTokenFlow(pre, transitionID, post);
		pn.createTokenFlow(post, transitionID, post);
	}
}

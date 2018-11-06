package uniolunisaar.adam.sdnencoding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.parser.impl.AptLTSParser;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.PetriGameExtensionHandler;
import uniolunisaar.adam.ds.winningconditions.WinningCondition;

public class TopologyToPN {
	TransitionSystem ts;
	private final Set<Arc> edges;

	
	public TopologyToPN(File file) throws ParseException, IOException {
		ts = new AptLTSParser().parseFile(file);
		edges = ts.getEdges();
	}
	
	private Pair<String, String> chooseIngressAndEgress() {
		Set<Arc> edges = ts.getEdges();
		Random random = new Random(42);
		String ingress = "";
		String egress = "";
		// find ingress
		int size = edges.size();
		int item = random.nextInt(size);
		int i = 0;
		for(Arc arc : edges) {
			if (i == item) {
		        ingress = arc.getSourceId();
			}
		    i++;
		}
		
		// search for different egress
		while (egress.equals("") || ingress.equals(egress)) {
			item = random.nextInt(size);
			i = 0;
			for(Arc arc : edges) {
				if (i == item) {
			        egress = arc.getTargetId();
				}
			    i++;
			}
		}
		
		return new Pair<>(ingress, egress);
	}
	
	public String setUpdate(PetriGame pn) {
		Pair<String, String> ingressAndEgress = chooseIngressAndEgress();
		String ingress = ingressAndEgress.getFirst();
		String egress = ingressAndEgress.getSecond();
		
		System.out.println("FROM: " + ingress + " TO: " + egress);
		
		State start = ts.getNode(ingress);
		State end = ts.getNode(egress);
		Pair<State, List<String>> next = null;
		Queue<Pair<State, List<String>>> queue = new LinkedList<>();
		queue.add(new Pair<>(start, new ArrayList<>()));
		State currentState = null;
		List<String> configuration = null;
		Set<State> closed = new HashSet<>();
		Set<List<String>> allConfigurations = new HashSet<>();
	
		// search for all paths
		while ((next = queue.poll()) != null) {
			currentState = next.getFirst();
			closed.add(currentState);
			configuration = next.getSecond();
			if (currentState == end) {
				allConfigurations.add(configuration);
				closed.remove(currentState); // DO NOT STOP AFTER REACHING FINAL STATE ONCE
			}
			for (State nextState : currentState.getPostsetNodes()) {
				if (!closed.contains(nextState)) {
					List<String> newConfiguration = new ArrayList<>(configuration);
					newConfiguration.add(currentState.getId() + "fwdTo" + nextState.getId());
					queue.add(new Pair<>(nextState, newConfiguration));
				}
			}
		}
		
		System.out.println("CONSIDERED UPDATES: " + allConfigurations);
		
		// find initial and final configuration
		Random random = new Random(42);
		List<String> initialConfiguration = null;
		List<String> finalConfiguration = null;
		int size = allConfigurations.size();
		int item = random.nextInt(size);
		int c = 0;
		for(List<String> config : allConfigurations) {
			if (c == item) {
				initialConfiguration = config;
			}
		    c++;
		}
		
		// search for different final configuration
		while (finalConfiguration == null || initialConfiguration.equals(finalConfiguration)) {
			item = random.nextInt(size);
			c = 0;
			for(List<String> config : allConfigurations) {
				if (c == item) {
					finalConfiguration = config;
				}
			    c++;
			}
		}
		
		System.out.println("INITIAL CONFIG: " + initialConfiguration);
		System.out.println("FINAL CONFIG: " + finalConfiguration);
		
		// Set tokens for initial configuration
		for (String inital : initialConfiguration) {
			pn.getPlace(inital).setInitialToken(1);
		}
		
		// Calculate update
		List<Update> updateList = new ArrayList<>();
		for (int i = finalConfiguration.size() - 1; i >= 0; --i) {
			String update = finalConfiguration.get(i);
			boolean addConfig = true;
			for (String initial : initialConfiguration) {
				if (update.startsWith(initial.substring(0, 5))) {
					addConfig = false;
					updateList.add(new SwitchUpdate(initial.substring(0, 5), initial.substring(initial.length() - 5, initial.length()), update.substring(update.length() - 5, update.length())));
					break;
				}
			}
			if (addConfig) {
				String switchToUpdate = update.substring(0, 5);
				String original = "";
				String destination = update.substring(update.length() - 5, update.length());
				
				for (State bogus : ts.getNode(switchToUpdate).getPostsetNodes()) {
					if (!bogus.getId().equals(destination)) {
						original = bogus.getId();
						break;
					}
				}
				
				if (original.equals("")) {
					pn.getPlace(update).setInitialToken(1);
				} else {
					updateList.add(new SwitchUpdate(switchToUpdate, original, destination));
				}
			}
		}
		
		Place updateStart = pn.createPlace("updateStart");
		updateStart.setInitialToken(1);
		new SequentialUpdate(updateList).addUpdate(pn, updateStart);
		
		
		Transition transition = pn.createTransition("ingress" + ingress);
		String transitionID = transition.getId();
		pn.createFlow(ingress, transitionID);
		pn.createFlow(transitionID, ingress);
		pn.createInitialTokenFlow(transition, pn.getPlace(ingress));
		pn.setWeakFair(transition);
		
		pn.rename(pn.getPlace(egress), "pOut");
		if (egress.equals("sw000")) {
			pn.rename(pn.getPlace("sw001"), "sw000");
		}
		return egress;
	}
	
	
	
	
	public boolean checkUndirectedGraphForCorrectness () {
		// TODO edges are undirected (?) and do not use non-existent vertices
		return true;
	}
	
	public PetriGame generatePetriNet() {
		PetriGame pn = new PetriGame("");
		PetriGameExtensionHandler.setWinningConditionAnnotation(pn, WinningCondition.Objective.LTL);
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
		return pn;
	}

	private void createTransition(PetriGame pn, String pre, String post) {
		Transition transition = pn.createTransition("fwd" + pre + "to" + post);
		pn.setWeakFair(transition);
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

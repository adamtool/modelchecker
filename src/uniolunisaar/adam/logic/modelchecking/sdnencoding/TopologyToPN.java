package uniolunisaar.adam.logic.modelchecking.sdnencoding;

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
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class TopologyToPN {
	private TransitionSystem ts;
	
	static boolean simpleUpdate = false;
	static boolean shortenNets = false;
	static int numberOfSwitches = 7;
	
	// TODO bug ingress can be sw000
	
	public TopologyToPN(File file) throws ParseException, IOException {
		ts = new AptLTSParser().parseFile(file);
		if (shortenNets) {
			List<Arc> edgeCopy = new ArrayList<Arc>(ts.getEdges());
			for (int i = 0; i < edgeCopy.size(); ++i) {
				Arc edge = edgeCopy.get(i);
				if (Integer.parseInt(edge.getSourceId().substring(2, 5)) > numberOfSwitches || Integer.parseInt(edge.getTargetId().substring(2, 5)) > numberOfSwitches) {
					ts.removeArc(edge);
				} 
			}
			List<State> nodesCopy = new ArrayList<>(ts.getNodes());
			for (int i = 0; i < nodesCopy.size(); ++i) {
				State state = nodesCopy.get(i);
				if (Integer.parseInt(state.getId().substring(2, 5)) > numberOfSwitches) {
					ts.removeState(state);
				}	
			}
		}
	}
	
	private Pair<String, String> chooseIngressAndEgress(int seed) {
		Random random = new Random(seed);
		String ingress = "";
		String egress = "";
		// find ingress
		int size = ts.getEdges().size();
		int item = random.nextInt(size);
		int i = 0;
		for(Arc arc : ts.getEdges()) {
			if (i == item) {
		        ingress = arc.getSourceId();
			}
		    i++;
		}
		
		// search for different egress
		while (egress.equals("") || ingress.equals(egress)) {
			item = random.nextInt(size);
			i = 0;
			for(Arc arc : ts.getEdges()) {
				if (i == item) {
			        egress = arc.getTargetId();
				}
			    i++;
			}
		}
		
		return new Pair<>(ingress, egress);
	}
	
	private Pair<Pair<String,String>,Set<List<String>>> getAllConfigurations () {
		Set<List<String>> allConfigurations = new HashSet<>();
		int seed = 42;
		String ingress = "";
		String egress = "";
		while (allConfigurations.size() < 2 && seed < 100) {
			allConfigurations.clear();
			Pair<String, String> ingressAndEgress = chooseIngressAndEgress(seed++);
			ingress = ingressAndEgress.getFirst();
			egress = ingressAndEgress.getSecond();
			
			System.out.println("FROM: " + ingress + " TO: " + egress);
			
			State start = ts.getNode(ingress);
			State end = ts.getNode(egress);
			Set<State> initialClosed = new HashSet<>();
			initialClosed.add(start);
			Pair<State, Pair<List<String>, Set<State>>> next = null;
			Queue<Pair<State, Pair<List<String>, Set<State>>>> queue = new LinkedList<>();
			queue.add(new Pair<>(start, new Pair<>(new ArrayList<>(), initialClosed)));
			State currentState = null;
			List<String> configuration = null;
			Set<State> closed = null;
		
			// search for all paths
			while ((next = queue.poll()) != null) {
				currentState = next.getFirst();
				configuration = next.getSecond().getFirst();
				closed = next.getSecond().getSecond();
				if (currentState == end) {
					allConfigurations.add(configuration);
				} else {
					for (State nextState : currentState.getPostsetNodes()) {
						if (!closed.contains(nextState)) {
							List<String> newConfiguration = new ArrayList<>(configuration);
							newConfiguration.add(currentState.getId() + "fwdTo" + nextState.getId());
							Set<State> newClosed = new HashSet<>(closed);
							newClosed.add(nextState);
							queue.add(new Pair<>(nextState, new Pair<>(newConfiguration, newClosed)));
						}
					}
				}
			}
		}
		return new Pair<>(new Pair<>(ingress, egress), allConfigurations);
	}
	
	private List<Update> getPathCHANGEUpdate(PetriNetWithTransits pn, List<String> initialConfiguration, List<String> finalConfiguration) {
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
					pn.getPlace(switchToUpdate + "fwdTo" + original).setInitialToken(1);
					updateList.add(new SwitchUpdate(switchToUpdate, original, destination));
				}
			}
		}
		return updateList;
	}
	
	private List<Update> getPathADDUpdate(PetriNetWithTransits pn, List<String> initialConfiguration, List<String> finalConfiguration) {
		List<Update> updateList = new ArrayList<>();
		for (int i = finalConfiguration.size() - 1; i >= 0; --i) {
			String update = finalConfiguration.get(i);
			String sw = update.substring(0, 5);
			String destination = update.substring(update.length() - 5, update.length());
			if (pn.getPlace(sw + "fwdTo" + destination).getInitialToken().getValue() == 0) {
				updateList.add(new AddUpdate(sw, destination));
			} else {
				updateList.add(new SwitchUpdate(sw, destination, destination));
			}
		}
		return updateList;
	}
	
	public String setUpdate(PetriNetWithTransits pn) {
		Pair<Pair<String,String>,Set<List<String>>> result = getAllConfigurations();
		String ingress = result.getFirst().getFirst();
		String egress = result.getFirst().getSecond();
		Set<List<String>> allConfigurations = result.getSecond();
		
		System.out.println("CONSIDERED UPDATES: " + allConfigurations);
		
		if (allConfigurations.size() < 2) {
			throw new Error("Even after 50 seeds, no two points with more than one route could be found, this example is weird...");
		}
		
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
		
		List<Update> updateList;
		if (simpleUpdate) {
			updateList = getPathADDUpdate(pn, initialConfiguration, finalConfiguration);
		} else {
			updateList = getPathCHANGEUpdate(pn, initialConfiguration, finalConfiguration);
		}
		
		Place updateStart = pn.createPlace("updateStart");
		updateStart.setInitialToken(1);
		new SequentialUpdate(updateList).addUpdate(pn, updateStart);
		
		
		Transition transition = pn.createTransition("ingress" + ingress);
		String transitionID = transition.getId();
		pn.createFlow(ingress, transitionID);
		pn.createFlow(transitionID, ingress);
		pn.createInitialTransit(transition, pn.getPlace(ingress));
		pn.createTransit(ingress, transitionID, ingress);
		pn.setWeakFair(transition);
		
		pn.rename(pn.getPlace(egress), "pOut");
		pn.getPlace("pOut").setInitialToken(1);
		if (egress.equals("sw000")) {
			pn.rename(pn.getPlace("sw001"), "sw000");
			pn.getPlace("sw000").setInitialToken(1);
		}
		return egress;
	}	
	
	public PetriNetWithTransits generatePetriNet() {
		PetriNetWithTransits pn = new PetriNetWithTransits("");
//		PetriGameExtensionHandler.setWinningConditionAnnotation(pn, Condition.Objective.LTL);
		for (Arc arc : ts.getEdges()) {
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

	private void createTransition(PetriNetWithTransits pn, String pre, String post) {
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
		pn.createTransit(pre, transitionID, post);
		pn.createTransit(post, transitionID, post);
	}
}

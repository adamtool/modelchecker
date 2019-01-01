package uniolunisaar.adam.logic.modelchecking.sdnencoding;

import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class ConcurrentUpdate implements Update {
	
	Set<Update> concurrent;
	
	public ConcurrentUpdate(Set<Update> setOfUpdates) {
		concurrent = setOfUpdates;
	}

	@Override
	public Place addUpdate(PetriNetWithTransits pn, Place start) {
		// split
		Transition split = pn.createTransition();
		pn.setWeakFair(split);
		pn.createFlow(start, split);
		Set<Place> merge = new HashSet<>();
		for (Update update : concurrent) {
			Place p = pn.createPlace();
			pn.createFlow(split, p);
			merge.add(update.addUpdate(pn, p));	
		}
		Transition t = pn.createTransition();
		pn.setWeakFair(t);
		Place finish = pn.createPlace();
		for (Place p : merge) {
			pn.createFlow(p, t);
		}
		pn.createFlow(t, finish);
		return finish;
	}
}

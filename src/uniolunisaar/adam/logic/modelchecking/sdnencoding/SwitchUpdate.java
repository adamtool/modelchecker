package uniolunisaar.adam.logic.modelchecking.sdnencoding;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class SwitchUpdate implements Update {

	String sw;
	String before;
	String after;
	
	public SwitchUpdate (String switchToUpdate, String oldDestination, String newDestination) {
		sw = switchToUpdate;
		before = oldDestination;
		after = newDestination;
	}

	@Override
	public Place addUpdate(PetriNetWithTransits pn, Place start) {
		Place end = pn.createPlace();
		Transition t = pn.createTransition();
		pn.setWeakFair(t);
		pn.createFlow(start, t);
		pn.createFlow(t, end);
		pn.createFlow(sw + "fwdTo" + before, t.getId());
		pn.createFlow(t.getId(), sw + "fwdTo" + after);
		return end;
	}
}

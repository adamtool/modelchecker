package uniolunisaar.adam.logic.modelchecking.sdnencoding;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class AddUpdate implements Update {

	String sw;
	String after;
	
	public AddUpdate(String switchToUpdate, String addDestination) {
		sw = switchToUpdate;
		after = addDestination;
	}
	
	@Override
	public Place addUpdate(PetriNetWithTransits pn, Place start) {
		Place end = pn.createPlace();
		Transition t = pn.createTransition();
		pn.setWeakFair(t);
		pn.createFlow(start,t);
		pn.createFlow(t, end);
		pn.createFlow(t.getId(), sw + "fwdTo" + after);
		return end;
	}
}

package uniolunisaar.adam.logic.modelchecking.sdnencoding;

import java.util.List;

import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class SequentialUpdate implements Update {

	List<Update> sequential;
	
	public SequentialUpdate(List<Update> listOfUpdates) {
		sequential = listOfUpdates;
	}

	@Override
	public Place addUpdate(PetriNetWithTransits pn, Place start) {
		for (Update update : sequential) {
			start = update.addUpdate(pn, start);
		}
		
		return start;
	}
}

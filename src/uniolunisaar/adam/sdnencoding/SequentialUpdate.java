package uniolunisaar.adam.sdnencoding;

import java.util.List;

import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.petrigame.PetriGame;

public class SequentialUpdate implements Update {

	List<Update> sequential;
	
	public SequentialUpdate(List<Update> listOfUpdates) {
		sequential = listOfUpdates;
	}

	@Override
	public Place addUpdate(PetriGame pn, Place start) {
		for (Update update : sequential) {
			start = update.addUpdate(pn, start);
		}
		
		return start;
	}
}

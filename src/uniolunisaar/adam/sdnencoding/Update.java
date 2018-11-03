package uniolunisaar.adam.sdnencoding;

import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.petrigame.PetriGame;

public interface Update {
	
	public Place addUpdate(PetriGame pn, Place start);

}

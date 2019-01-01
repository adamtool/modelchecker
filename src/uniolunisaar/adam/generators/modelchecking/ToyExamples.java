package uniolunisaar.adam.generators.modelchecking;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 *
 * @author Manuel Gieseking
 */
public class ToyExamples {

    public static PetriNetWithTransits createFirstExample(boolean looping) {
        PetriNetWithTransits net = new PetriNetWithTransits("firstExample_" + looping);
        Place in = net.createPlace("in");
        in.setInitialToken(1);
        net.setInitialTokenflow(in);
        Place out = net.createPlace("out");
        out.setInitialToken(1);
        Transition t = net.createTransition("t");
        net.createFlow(t, in);
        net.createFlow(in, t);
        net.createFlow(t, out);
        net.createFlow(out, t);
        if (looping) {
            net.createTransit(in, t, in, out);
        } else {
            net.createTransit(in, t, out);
        }
        net.createTransit(out, t, out);
        return net;
    }

    public static PetriNetWithTransits createFirstExampleExtended(boolean looping) {
        PetriNetWithTransits net = new PetriNetWithTransits("firstExampleExtended_" + looping);
        Place in = net.createPlace("in");
        in.setInitialToken(1);
        net.setInitialTokenflow(in);
        Place mid = net.createPlace("mid");
        mid.setInitialToken(1);
        Transition t = net.createTransition("ta");
        net.setStrongFair(t);
        net.createFlow(in, t);
        net.createFlow(t, in);
        net.createFlow(t, mid);
        net.createFlow(mid, t);
        net.createTransit(mid, t, mid);

        Place out = net.createPlace("out");
        out.setInitialToken(1);
        Transition t1 = net.createTransition("tb");
        net.setStrongFair(t1);
        net.createFlow(t1, mid);
        net.createFlow(mid, t1);
        net.createFlow(t1, out);
        net.createFlow(out, t1);
        net.createTransit(out, t1, out);

        if (looping) {
            net.createTransit(mid, t1, mid, out);
            net.createTransit(in, t, in, mid);
        } else {
            net.createTransit(mid, t1, out);
            net.createTransit(in, t, mid);
        }
        return net;
//        
//        false code
// PetriGame net = createFirstExampleExtended();
//        net.setName(net.getName() + "_positiv");
//        net.removeTransit("ta", "in");
//        net.removeTransit("tb", "mid");
    }
}

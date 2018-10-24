package uniolunisaar.adam.modelchecker.circuits;

/**
 *
 * @author Manuel Gieseking
 */
public class Circuit {

    public enum Renderer {
        INGOING,
        INGOING_REGISTER,
        OUTGOING,
        OUTGOING_REGISTER,
        OUTGOING_REGISTER_MAX_INTERLEAVING;
    }

    public static AigerRenderer getRenderer(Renderer renderer) {
        switch (renderer) {
            case INGOING:
                return new AigerRendererSafeIn();
            case INGOING_REGISTER:
                throw new RuntimeException("Not yet implemented.");
            case OUTGOING:
                return new AigerRendererSafeOut();
            case OUTGOING_REGISTER:
                return new AigerRendererSafeOutStutterRegister();
            case OUTGOING_REGISTER_MAX_INTERLEAVING:
                return new AigerRendererSafeOutStutterRegisterMaxInterleaving();
        }
        throw new RuntimeException("The case " + renderer + " is not yet implemented.");
    }
}

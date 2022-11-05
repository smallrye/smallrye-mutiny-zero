package mutiny.zero.operators;

public interface Utils {

    static <T extends Throwable> void sneakyThrow(Throwable err) throws T {
        throw (T) err;
    }
}

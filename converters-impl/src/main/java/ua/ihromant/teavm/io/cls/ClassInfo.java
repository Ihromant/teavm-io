package ua.ihromant.teavm.io.cls;

public interface ClassInfo {
    boolean assignableTo(Class<?> constant);

    boolean isInterface();
}

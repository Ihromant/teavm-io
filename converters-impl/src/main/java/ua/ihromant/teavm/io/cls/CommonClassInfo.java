package ua.ihromant.teavm.io.cls;

public class CommonClassInfo implements ClassInfo {
    private final Class<?> cls;

    public CommonClassInfo(Class<?> cls) {
        this.cls = cls;
    }

    @Override
    public boolean assignableTo(Class<?> other) {
        return other.isAssignableFrom(cls);
    }

    @Override
    public boolean isInterface() {
        return cls.isInterface();
    }
}

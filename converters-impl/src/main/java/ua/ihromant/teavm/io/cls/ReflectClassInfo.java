package ua.ihromant.teavm.io.cls;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;

@CompileTime
public class ReflectClassInfo implements ClassInfo {
    private final ReflectClass<?> cls;

    public ReflectClassInfo(ReflectClass<?> cls) {
        this.cls = cls;
    }

    @Override
    public boolean assignableTo(Class<?> other) {
        return Metaprogramming.findClass(other).isAssignableFrom(cls);
    }

    @Override
    public boolean isInterface() {
        return cls.isInterface();
    }
}

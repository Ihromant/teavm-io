package ua.ihromant.teavm.io.cls;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;

import java.util.HashMap;
import java.util.Map;

@CompileTime
public class ClassCache {
    private static final Map<String, Class<?>> classes = new HashMap<>();

    static {
        classes.put(boolean.class.getName(), boolean.class);
        classes.put(int.class.getName(), int.class);
        classes.put(double.class.getName(), double.class);
    }

    private ClassCache() {

    }

    public static Class<?> find(String name) {
        if (!classes.containsKey(name)) {
            try {
                Class<?> cls = Class.forName(name, false, Metaprogramming.getClassLoader());
                classes.put(name, cls);
            } catch (Exception e) {
                Metaprogramming.getDiagnostics().error(Metaprogramming.getLocation(), "Was not able to find class " + name);
            }
        }
        return classes.get(name);
    }
}

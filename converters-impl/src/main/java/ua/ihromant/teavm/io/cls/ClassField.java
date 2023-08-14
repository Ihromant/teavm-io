package ua.ihromant.teavm.io.cls;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@CompileTime
public class ClassField {
    private final ReflectField refFd;
    private final Type fieldType;

    private ClassField(Class<?> cls, Field field) {
        ReflectClass<?> refCl = Metaprogramming.findClass(cls);
        this.refFd = refCl.getDeclaredField(field.getName());
        this.fieldType = field.getGenericType();
    }

    public static List<ClassField> readSerializableFields(Class<?> cls) {
        List<ClassField> result = new ArrayList<>();
        while (cls != null) {
            for (Field fd : cls.getDeclaredFields()) {
                int mod = fd.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                result.add(new ClassField(cls, fd));
            }
            cls = cls.getSuperclass();
        }
        return result;
    }

    public ReflectField getRefFd() {
        return refFd;
    }

    public Type getFieldType() {
        return fieldType;
    }
}

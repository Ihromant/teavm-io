package ua.ihromant.teavm.io.cls;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectMethod;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;

@CompileTime
public class RecordField {
    private final ReflectMethod accessor;
    private final Type fieldType;

    private RecordField(ReflectClass<?> refCl, Method method) {
        this.accessor = refCl.getDeclaredMethod(method.getName());
        this.fieldType = method.getGenericReturnType();
    }

    public static RecordField[] readAccessors(Class<?> cls) {
        ReflectClass<?> refCl = Metaprogramming.findClass(cls);
        return Arrays.stream(cls.getRecordComponents()).map(RecordComponent::getAccessor).map(m -> new RecordField(refCl, m)).toArray(RecordField[]::new);
    }

    public static ReflectMethod getConstructor(Class<?> cls) {
        RecordField[] fields = readAccessors(cls);
        ReflectClass<?> refCl = Metaprogramming.findClass(cls);
        return refCl.getDeclaredMethod("<init>", Arrays.stream(fields).map(rf -> rf.accessor.getReturnType()).toArray(ReflectClass[]::new));
    }

    public ReflectMethod getAccessor() {
        return accessor;
    }

    public Type getFieldType() {
        return fieldType;
    }
}

package ua.ihromant.teavm.io.deserializer;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSNumber;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.reflect.ReflectField;
import org.teavm.metaprogramming.reflect.ReflectMethod;
import ua.ihromant.teavm.io.cls.ClassField;
import ua.ihromant.teavm.io.cls.RecordField;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CompileTime
public class DeserializerGenerator {
    private static final Map<String, Value<Deserializer>> definedDeserializers = new HashMap<>();

    static {
        definedDeserializers.put(int.class.getName(), Metaprogramming.lazy(() -> Deserializer.INT));
        definedDeserializers.put(Integer.class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.INT)));
        definedDeserializers.put(boolean.class.getName(), Metaprogramming.lazy(() -> Deserializer.BOOLEAN));
        definedDeserializers.put(Boolean.class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.BOOLEAN)));
        definedDeserializers.put(double.class.getName(), Metaprogramming.lazy(() -> Deserializer.DOUBLE));
        definedDeserializers.put(Double.class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.DOUBLE)));
        definedDeserializers.put(String.class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.STRING)));
        definedDeserializers.put(int[].class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.INT_ARRAY)));
        definedDeserializers.put(boolean[].class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.BOOLEAN_ARRAY)));
        definedDeserializers.put(double[].class.getName(), Metaprogramming.lazy(() -> Deserializer.nullable(Deserializer.DOUBLE_ARRAY)));
    }

    private DeserializerGenerator() {
        
    }

    public static Value<Deserializer> getDeserializer(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type[] types = pt.getActualTypeArguments();
            Class<?> raw = (Class<?>) pt.getRawType();
            if (Map.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Deserializer> keyDeserializer = getDeserializer(types[0]);
                    Value<Deserializer> valueDeserializer = getDeserializer(types[1]);
                    Value<Deserializer> notNull = Metaprogramming.emit(() -> Deserializer.mapDeserializer(keyDeserializer.get(), valueDeserializer.get()));
                    return Metaprogramming.lazy(() -> Deserializer.nullable(notNull.get()));
                });
            }
            if (List.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Deserializer> elemDeserializer = getDeserializer(types[0]);
                    Value<Deserializer> notNull = Metaprogramming.emit(() -> Deserializer.listDeserializer(elemDeserializer.get()));
                    return Metaprogramming.lazy(() -> Deserializer.nullable(notNull.get()));
                });
            }
            if (Set.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Deserializer> elemDeserializer = getDeserializer(types[0]);
                    Value<Deserializer> notNull = Metaprogramming.emit(() -> Deserializer.setDeserializer(elemDeserializer.get()));
                    return Metaprogramming.lazy(() -> Deserializer.nullable(notNull.get()));
                });
            }
        }
        if (type instanceof Class<?> cls) {
            return getDeserializer(cls);
        }
        Metaprogramming.getDiagnostics().error(Metaprogramming.getLocation(), "Not supported type " + type);
        throw new IllegalArgumentException();
    }

    public static Value<Deserializer> getDeserializer(Class<?> cls) {
        Value<Deserializer> result = definedDeserializers.get(cls.getName());
        if (result != null) {
            return result;
        }
        Value<Deserializer> generated = Metaprogramming.lazyFragment(() -> {
            Value<Deserializer> notNull = buildDeserializer(cls);
            return Metaprogramming.emit(() -> Deserializer.nullable(notNull.get()));
        });
        definedDeserializers.put(cls.getName(), generated);
        return generated;
    }

    private static Value<Deserializer> buildDeserializer(Class<?> cls) {
        if (cls.isEnum()) {
            return buildEnumDeserializer(cls);
        }
        if (cls.isArray()) {
            return buildArrayDeserializer(cls.componentType());
        }
        if (cls.isRecord()) {
            return buildRecordDeserializer(cls);
        }
        return buildObjectDeserializer(cls);
    }

    private static Value<Deserializer> buildEnumDeserializer(Class<?> cls) {
        ReflectClass<?> refCl = Metaprogramming.findClass(cls);
        ReflectMethod values = refCl.getMethod("values");
        return Metaprogramming.proxy(Deserializer.class, (instance, method, args) -> {
            Value<JSNumber> val = Metaprogramming.emit(() -> (JSNumber) args[0]);
            Value<Object[]> arr = Metaprogramming.emit(() -> (Object[]) values.invoke(null));
            Metaprogramming.exit(() -> arr.get()[val.get().intValue()]);
        });
    }

    private static Value<Deserializer> buildArrayDeserializer(Class<?> elementInfo) {
        Value<Deserializer> childDeserializer = getDeserializer(elementInfo);
        if (childDeserializer == null) {
            Metaprogramming.getDiagnostics().error(Metaprogramming.getLocation(), "No deserializer for " + elementInfo.getName());
        }
        ReflectClass<?> refElem = Metaprogramming.findClass(elementInfo);
        return Metaprogramming.proxy(Deserializer.class, (instance, method, args) -> {
            @SuppressWarnings("unchecked") Value<JSArray<JSObject>> value = Metaprogramming.emit(
                    () -> (JSArray<JSObject>) args[0]);
            Metaprogramming.exit(() -> {
                JSArray<JSObject> jsArray = value.get();
                int length = jsArray.getLength();
                Object[] result = refElem.createArray(length);
                Deserializer itemDeserializer = childDeserializer.get();
                for (int i = 0; i < length; ++i) {
                    result[i] = itemDeserializer.read(jsArray.get(i));
                }
                return result;
            });
        });
    }

    private static Value<Deserializer> buildObjectDeserializer(Class<?> cls) {
        ReflectClass<?> refCl = Metaprogramming.findClass(cls);
        ReflectMethod defaultConstructor = refCl.getDeclaredMethod("<init>");
        List<ClassField> classFields = ClassField.readSerializableFields(cls);
        return Metaprogramming.proxy(Deserializer.class, (instance, method, args) -> {
            Value<Object> jo = Metaprogramming.emit(() -> defaultConstructor.construct());
            @SuppressWarnings("unchecked") Value<JSMapLike<JSObject>> jso = Metaprogramming.emit(() -> (JSMapLike<JSObject>) args[0]);
            for (ClassField cf : classFields) {
                ReflectField refFd = cf.getRefFd();
                String propName = refFd.getName();
                Value<Deserializer> fieldDeserializer = getDeserializer(cf.getFieldType());
                Value<JSObject> jsProp = Metaprogramming.emit(() -> jso.get().get(propName));
                Value<Object> javaProp = Metaprogramming.emit(() -> fieldDeserializer.get().read(jsProp.get()));
                Metaprogramming.emit(() -> refFd.set(jo.get(), javaProp.get()));
            }
            Metaprogramming.exit(() -> jo.get());
        });
    }

    private static Value<Deserializer> buildRecordDeserializer(Class<?> cls) {
        RecordField[] recFds = RecordField.readAccessors(cls);
        ReflectMethod ctr = RecordField.getConstructor(cls);
        return Metaprogramming.proxy(Deserializer.class, (instance, method, args) -> {
            @SuppressWarnings("unchecked") Value<JSMapLike<JSObject>> jso = Metaprogramming.emit(() -> (JSMapLike<JSObject>) args[0]);
            Value<List<Object>> list = Metaprogramming.emit(() -> new ArrayList<>());
            for (RecordField rf : recFds) {
                ReflectMethod rm = rf.getAccessor();
                String propName = rm.getName();
                Value<JSObject> jsProp = Metaprogramming.emit(() -> jso.get().get(propName));
                Value<Deserializer> fieldDeserializer = getDeserializer(rf.getFieldType());
                Value<Object> javaProp = Metaprogramming.emit(() -> fieldDeserializer.get().read(jsProp.get()));
                Metaprogramming.emit(() -> list.get().add(javaProp.get()));
            }
            Metaprogramming.exit(() -> ctr.construct(list.get().toArray()));
        });
    }
}

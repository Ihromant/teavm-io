package ua.ihromant.teavm.io.serializer;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CompileTime
public class SerializerGenerator {
    private static final Map<String, Value<Serializer>> definedSerializers = new HashMap<>();

    static {
        definedSerializers.put(int.class.getName(), Metaprogramming.lazy(() -> Serializer.INT));
        definedSerializers.put(Integer.class.getName(), Metaprogramming.lazy(() -> Serializer.nullable(Serializer.INT)));
        definedSerializers.put(boolean.class.getName(), Metaprogramming.lazy(() -> Serializer.BOOLEAN));
        definedSerializers.put(Boolean.class.getName(), Metaprogramming.lazy(() -> Serializer.nullable(Serializer.BOOLEAN)));
        definedSerializers.put(double.class.getName(), Metaprogramming.lazy(() -> Serializer.DOUBLE));
        definedSerializers.put(Double.class.getName(), Metaprogramming.lazy(() -> Serializer.nullable(Serializer.DOUBLE)));
        definedSerializers.put(String.class.getName(), Metaprogramming.lazy(() -> Serializer.nullable(Serializer.STRING)));
    }

    private SerializerGenerator() {

    }

    public static Value<Serializer> getSerializer(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type[] types = pt.getActualTypeArguments();
            Class<?> raw = (Class<?>) pt.getRawType();
            if (Map.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Serializer> valueSerializer = getSerializer(types[1]);
                    Value<Serializer> notNull = Metaprogramming.emit(() -> Serializer.mapSerializer(valueSerializer.get()));
                    return Metaprogramming.emit(() -> Serializer.nullable(notNull.get()));
                });
            }
            if (List.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Serializer> elemSerializer = getSerializer(types[0]);
                    Value<Serializer> notNull = Metaprogramming.emit(() -> Serializer.listSerializer(elemSerializer.get()));
                    return Metaprogramming.emit(() -> Serializer.nullable(notNull.get()));
                });
            }
            if (Set.class.equals(raw)) {
                return Metaprogramming.lazyFragment(() -> {
                    Value<Serializer> elemSerializer = getSerializer(types[0]);
                    Value<Serializer> notNull = Metaprogramming.emit(() -> Serializer.setSerializer(elemSerializer.get()));
                    return Metaprogramming.emit(() -> Serializer.nullable(notNull.get()));
                });
            }
        }
        if (type instanceof Class<?>) {
            return getSerializer((Class<?>) type);
        }
        Metaprogramming.getDiagnostics().error(Metaprogramming.getLocation(), "Not supported type " + type);
        throw new IllegalArgumentException();
    }

    public static Value<Serializer> getSerializer(Class<?> cls) {
        Value<Serializer> result = definedSerializers.get(cls.getName());
        if (result != null) {
            return result;
        }
        Value<Serializer> generated = Metaprogramming.lazyFragment(() -> {
            Value<Serializer> notNull = buildSerializer(cls);
            return Metaprogramming.emit(() -> Serializer.nullable(notNull.get()));
        });
        definedSerializers.put(cls.getName(), generated);
        return generated;
    }

    private static Value<Serializer> buildSerializer(Class<?> cls) {
        if (cls.isEnum()) {
            return Metaprogramming.emit(() -> Serializer.ENUM);
        }
        if (cls.isArray()) {
            return buildArraySerializer(cls);
        }
        if (cls.isRecord()) {
            return buildRecordSerializer(cls);
        }
        return buildObjectSerializer(cls);
    }

    private static Value<Serializer> buildArraySerializer(Class<?> cls) {
        Class<?> elementInfo = cls.componentType();
        Value<Serializer> elemSerializer = getSerializer(cls.componentType());
        if (elemSerializer == null) {
            Metaprogramming.getDiagnostics().error(Metaprogramming.getLocation(), "No serializer for " + elementInfo.getName());
        }
        Value<Serializer> notNull = Metaprogramming.emit(() -> Serializer.arraySerializer(elemSerializer.get()));
        return Metaprogramming.emit(() -> Serializer.nullable(notNull.get()));
    }

    private static Value<Serializer> buildObjectSerializer(Class<?> cls) {
        List<ClassField> classFields = ClassField.readSerializableFields(cls);
        return Metaprogramming.proxy(Serializer.class, (instance, method, args) -> {
            Value<JSMapLike<JSObject>> result = Metaprogramming.emit(() -> JSObjects.create());
            Value<Object> object = args[0];
            for (ClassField cf : classFields) {
                ReflectField refFd = cf.getRefFd();
                String propName = refFd.getName();
                Value<Serializer> fieldSerializer = getSerializer(cf.getFieldType());
                Value<Object> javaProp = Metaprogramming.emit(() -> refFd.get(object.get()));
                Value<JSObject> jsProp = Metaprogramming.emit(() -> fieldSerializer.get().write(javaProp.get()));
                Metaprogramming.emit(() -> result.get().set(propName, jsProp.get()));
            }
            Metaprogramming.exit(() -> result.get());
        });
    }

    private static Value<Serializer> buildRecordSerializer(Class<?> cls) {
        RecordField[] recFds = RecordField.readAccessors(cls);
        return Metaprogramming.proxy(Serializer.class, (instance, method, args) -> {
            Value<Object> object = args[0];
            Value<JSMapLike<JSObject>> result = Metaprogramming.emit(() -> JSObjects.create());
            for (RecordField rf : recFds) {
                ReflectMethod accessor = rf.getAccessor();
                String propName = accessor.getName();
                Value<Object> javaProp = Metaprogramming.emit(() -> accessor.invoke(object.get()));
                Value<Serializer> fieldSerializer = getSerializer(rf.getFieldType());
                Value<JSObject> jsProp = Metaprogramming.emit(() -> fieldSerializer.get().write(javaProp.get()));
                Metaprogramming.emit(() -> result.get().set(propName, jsProp.get()));
            }
            Metaprogramming.exit(() -> result.get());
        });
    }
}

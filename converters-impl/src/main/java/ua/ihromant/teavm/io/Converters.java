package ua.ihromant.teavm.io;

import org.teavm.jso.JSObject;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import ua.ihromant.teavm.io.cls.ClassCache;
import ua.ihromant.teavm.io.cls.ClassInfo;
import ua.ihromant.teavm.io.cls.CommonClassInfo;
import ua.ihromant.teavm.io.cls.ReflectClassInfo;
import ua.ihromant.teavm.io.deserializer.Deserializer;
import ua.ihromant.teavm.io.deserializer.DeserializerGenerator;
import ua.ihromant.teavm.io.serializer.Serializer;
import ua.ihromant.teavm.io.serializer.SerializerGenerator;

@CompileTime
public final class Converters {
    private Converters() {

    }

    public static JSObject javaToJs(Object jo) {
        return serializerFor(jo.getClass()).write(jo);
    }

    public static Object jsToJava(JSObject jso, Class<?> cls) {
        return deserializerFor(cls).read(jso);
    }

    private static Deserializer deserializerFor(Class<?> cls) {
        if (blackList(new CommonClassInfo(cls))) {
            throw new IllegalArgumentException("Not supported class " + cls.getName());
        }
        return deserializer(cls);
    }

    private static Serializer serializerFor(Class<?> cls) {
        if (blackList(new CommonClassInfo(cls))) {
            throw new IllegalArgumentException("Not supported class " + cls.getName());
        }
        return serializer(cls);
    }

    private static boolean blackList(ClassInfo cls) {
        if (cls.isInterface()) {
            return true;
        }
        return !cls.assignableTo(Message.class);
    }

    @Meta
    private static native Serializer serializer(Class<?> cls);
    private static void serializer(ReflectClass<?> cls) {
        if (blackList(new ReflectClassInfo(cls))) {
            Metaprogramming.unsupportedCase();
            return;
        }
        //Metaprogramming.getDiagnostics().warning(Metaprogramming.getLocation(), "Generating serializer for " + cls.getName());
        Value<Serializer> serializer = SerializerGenerator.getSerializer(ClassCache.find(cls.getName()));
        Metaprogramming.exit(() -> serializer.get());
    }

    @Meta
    private static native Deserializer deserializer(Class<?> cls);
    private static void deserializer(ReflectClass<?> cls) {
        if (blackList(new ReflectClassInfo(cls))) {
            Metaprogramming.unsupportedCase();
            return;
        }
        //Metaprogramming.getDiagnostics().warning(Metaprogramming.getLocation(), "Generating deserializer for " + cls.getName());
        Value<Deserializer> deserializer = DeserializerGenerator.getDeserializer(ClassCache.find(cls.getName()));
        Metaprogramming.exit(() -> deserializer.get());
    }
}

package ua.ihromant.teavm.io.serializer;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Serializer {
    Serializer BOOLEAN = jo -> Serializer.fromBoolean((boolean) jo);
    Serializer INT = jo -> Serializer.fromInt((int) jo);
    Serializer DOUBLE = jo -> Serializer.fromDouble((double) jo);
    Serializer ENUM = jo -> JSNumber.valueOf(((Enum<?>) jo).ordinal());
    Serializer STRING = jo -> JSString.valueOf((String) jo);

    static JSNumber fromDouble(double d) {
        return JSNumber.valueOf(d);
    }

    static JSNumber fromInt(int i) {
        return JSNumber.valueOf(i);
    }

    static JSBoolean fromBoolean(boolean b) {
        return JSBoolean.valueOf(b);
    }

    static Serializer nullable(Serializer base) {
        return jo -> jo == null ? JSObjects.undefined() : base.write(jo);
    }

    static Serializer mapSerializer(Serializer valueSerializer) {
        return jo -> {
            JSMapLike<JSObject> result = JSObjects.create();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) jo).entrySet()) {
                result.set(e.getKey().toString(), valueSerializer.write(e.getValue()));
            }
            return result;
        };
    }

    static Serializer listSerializer(Serializer elemSerializer) {
        return jo -> {
            JSArray<JSObject> result = new JSArray<>();
            for (Object o : (List<?>) jo) {
                result.push(elemSerializer.write(o));
            }
            return result;
        };
    }

    static Serializer arraySerializer(Serializer elemSerializer) {
        return jo -> {
            JSArray<JSObject> result = new JSArray<>();
            if (jo instanceof int[] ia) {
                for (int i : ia) {
                    result.push(fromInt(i));
                }
            }
            if (jo instanceof boolean[] ba) {
                for (boolean b : ba) {
                    result.push(fromBoolean(b));
                }
            }
            if (jo instanceof double[] da) {
                for (double d : da) {
                    result.push(fromDouble(d));
                }
            }
            if (jo instanceof Object[] oa) {
                for (Object o : oa) {
                    result.push(elemSerializer.write(o));
                }
            }
            return result;
        };
    }

    static Serializer setSerializer(Serializer elemSerializer) {
        return jo -> {
            JSArray<JSObject> result = new JSArray<>();
            for (Object o : (Set<?>) jo) {
                result.push(elemSerializer.write(o));
            }
            return result;
        };
    }

    JSObject write(Object jo);
}

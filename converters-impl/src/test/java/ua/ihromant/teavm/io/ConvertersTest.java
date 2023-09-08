package ua.ihromant.teavm.io;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.json.JSON;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.metaprogramming.CompileTime;

import java.util.Map;

import static org.junit.Assert.*;

@CompileTime
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ConvertersTest {
    private final String sample = "{\"a\":[1],\"b\":\"abc\",\"c\":5,\"d\":true,\"e\":{\"t\":\"def\"}}";
    private final String sampleNull = "{\"d\":true}";
    private final String sampleNested = "{\"d\":true,\"e\":{}}";
    private final String sampleRecord = "{\"a\":\"abc\",\"b\":3}";
    private final String sampleRecordNull = "{\"b\":1}";
    private final String sampleArrayRecord = "{\"moves\":[0,1,0]}";

    @Test
    public void commonJavaToJs() {
        TestClass obj = new TestClass();
        obj.a = new int[]{1};
        obj.b = "abc";
        obj.c = 5;
        obj.d = true;
        obj.e = new TestChildClass();
        obj.e.t = "def";
        obj.cache = Map.of("s", System.out);
        assertEquals(sample, JSON.stringify(Converters.javaToJs(obj)));
    }

    @Test
    public void recordJavaToJs() {
        TestRecord obj = new TestRecord("abc", 3);
        assertEquals(sampleRecord, JSON.stringify(Converters.javaToJs(obj)));
        obj = new TestRecord(null, 1);
        assertEquals(sampleRecordNull, JSON.stringify(Converters.javaToJs(obj)));
    }

    @Test
    public void nullableJavaToJs() {
        TestClass obj = new TestClass();
        obj.d = true;
        assertEquals(sampleNull, JSON.stringify(Converters.javaToJs(obj)));
        obj.e = new TestChildClass();
        assertEquals(sampleNested, JSON.stringify(Converters.javaToJs(obj)));
    }

    @Test
    public void commonJsToJava() {
        TestClass obj = (TestClass) Converters.jsToJava(JSON.parse(sample), TestClass.class);
        assertArrayEquals(new int[]{1}, obj.a);
        assertEquals("abc", obj.b);
        assertEquals(Integer.valueOf(5), obj.c);
        assertTrue(obj.d);
        assertNotNull(obj.e);
        assertEquals("def", obj.e.t);
        assertNull(obj.cache);
    }

    @Test
    public void recordJsToJava() {
        TestRecord obj = (TestRecord) Converters.jsToJava(JSON.parse(sampleRecord), TestRecord.class);
        assertEquals("abc", obj.a);
        assertEquals(3, obj.b);
        obj = (TestRecord) Converters.jsToJava(JSON.parse(sampleRecordNull), TestRecord.class);
        assertNull(obj.a);
        assertEquals(1, obj.b);
    }

    @Test
    public void nullableJsToJava() {
        TestClass obj = (TestClass) Converters.jsToJava(JSON.parse(sampleNull), TestClass.class);
        assertNull(obj.a);
        assertNull(obj.b);
        assertNull(obj.c);
        assertTrue(obj.d);
        assertNull(obj.e);
        assertNull(obj.cache);
        obj = (TestClass) Converters.jsToJava(JSON.parse(sampleNested), TestClass.class);
        assertNotNull(obj.e);
        assertNull(obj.e.t);
    }

    @Test
    public void testArrayRecord() {
        TestArrayRecord parsed = (TestArrayRecord) Converters.jsToJava(JSON.parse(sampleArrayRecord), TestArrayRecord.class);
        assertArrayEquals(new TestMove[]{TestMove.LEFT, TestMove.RIGHT, TestMove.LEFT}, parsed.moves());
        String s = JSON.stringify(Converters.javaToJs(parsed));
        assertEquals(sampleArrayRecord, s);
    }

    @Test
    public void writeArray() {
        JSArray<JSObject> arr = JSArray.create();
        for (int i = 0; i < 3; i++) {
            arr.push(JSNumber.valueOf(TestMove.values()[(i + 1) % 2].ordinal()));
        }
        assertEquals("[1,0,1]", JSON.stringify(arr));
    }

    private enum TestMove {
        LEFT, RIGHT
    }

    private static class TestClass implements Message {
        private int[] a;
        private String b;
        private Integer c;
        private boolean d;
        private TestChildClass e;
        private transient Map<Object, Object> cache;
    }

    private static class TestChildClass {
        private String t;
    }

    record TestRecord(String a, int b) implements Message {
    }

    record TestArrayRecord(TestMove[] moves, TestChildClass[] children) implements Message {
    }
}

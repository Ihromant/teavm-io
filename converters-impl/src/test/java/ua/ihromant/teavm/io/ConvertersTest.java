package ua.ihromant.teavm.io;

import org.junit.Test;
import org.junit.runner.RunWith;
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
    private final String sample = "{\"a\":1,\"b\":\"abc\",\"c\":5,\"d\":true,\"e\":{\"t\":\"def\"}}";
    private final String sampleNull = "{\"a\":0,\"d\":true}";
    private final String sampleNested = "{\"a\":0,\"d\":true,\"e\":{}}";
    private final String sampleRecord = "{\"a\":\"abc\",\"b\":3}";
    private final String sampleRecordNull = "{\"b\":1}";
    private final String sampleArrayRecord = "{\"moves\":[0,1,0],\"childs\":[{}]}";

    @Test
    public void commonJavaToJs() {
        TestClass obj = new TestClass();
        obj.a = 1;
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
        assertEquals(1, obj.a);
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
        assertEquals(0, obj.a);
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
        TestArrayRecord record = new TestArrayRecord(new Move[]{Move.LEFT, Move.RIGHT, Move.LEFT},
                new TestChildClass[]{new TestChildClass()});
        String s = JSON.stringify(Converters.javaToJs(record));
        assertEquals(sampleArrayRecord, s);
        TestArrayRecord reparsed = (TestArrayRecord) Converters.jsToJava(JSON.parse(s), TestArrayRecord.class);
        assertArrayEquals(new Move[]{Move.LEFT, Move.RIGHT, Move.LEFT}, reparsed.moves());
    }

    private enum Move {
        LEFT, RIGHT
    }

    private static class TestClass implements Message {
        private int a;
        private String b;
        private Integer c;
        private boolean d;
        private TestChildClass e;
        private transient Map<Object, Object> cache;

        @Override
        public MessageType getMsType() {
            return TestMessageType.TEST_CLASS;
        }
    }

    private static class TestChildClass {
        private String t;
    }

    record TestRecord(String a, int b) implements Message {
        @Override
        public MessageType getMsType() {
            return TestMessageType.TEST_RECORD;
        }
    }

    record TestArrayRecord(Move[] moves, TestChildClass[] childs) implements Message {
        @Override
        public MessageType getMsType() {
            return TestMessageType.TEST_ARRAY_RECORD;
        }
    }

    enum TestMessageType implements MessageType {
        TEST_CLASS(TestClass.class), TEST_RECORD(TestRecord.class),
        TEST_ARRAY_RECORD(TestArrayRecord.class);
        private final Class<? extends Message> cls;

        TestMessageType(Class<? extends Message> cls) {
            this.cls = cls;
        }

        @Override
        public Class<? extends Message> getCls() {
            return cls;
        }
    }
}

package ua.ihromant.teavm.io;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.json.JSON;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.metaprogramming.CompileTime;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@CompileTime
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ConvertersTest {
    private final String sample = ""; // TODO
    @Test
    public void jsToJava() {
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
    public void javaToJs() {
        TestClass obj = (TestClass) Converters.jsToJava(JSON.parse(sample), TestClass.class);
        assertEquals(1, obj.a); // TODO etc.
        assertTrue(obj.d);
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

    enum TestMessageType implements MessageType {
        TEST_CLASS(TestClass.class), TEST_RECORD(TestRecord.class);
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

package io.github.senthilganeshs.fj.codec;

import io.github.senthilganeshs.fj.ds.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.*;

public class CodecTest {

    @Test
    public void testPrimitiveRoundTrip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        Codec.intEncoder().encode(out, 123);
        Codec.doubleEncoder().encode(out, 45.67);
        Codec.booleanEncoder().encode(out, true);
        Codec.stringEncoder().encode(out, "hello world");
        Codec.stringEncoder().encode(out, null);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        Assert.assertEquals(Codec.intDecoder().decode(in).orElse(-1), Integer.valueOf(123));
        Assert.assertEquals(Codec.doubleDecoder().decode(in).orElse(-1.0), 45.67);
        Assert.assertEquals(Codec.booleanDecoder().decode(in).orElse(false), Boolean.TRUE);
        Assert.assertEquals(Codec.stringDecoder().decode(in).orElse(""), "hello world");
        Assert.assertNull(Codec.stringDecoder().decode(in).orElse("not null"));
    }

    @Test
    public void testCollectionRoundTrip() throws IOException {
        List<String> list = List.of("a", "b", "c");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        Codec.listEncoder(Codec.stringEncoder()).encode(out, list);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        List<String> decoded = Codec.listDecoder(Codec.stringDecoder()).decode(in).orElse(List.nil());

        Assert.assertEquals(decoded.length(), 3);
        Assert.assertEquals(decoded.drop(0).headMaybe().orElse(""), "a");
        Assert.assertEquals(decoded.drop(1).headMaybe().orElse(""), "b");
        Assert.assertEquals(decoded.drop(2).headMaybe().orElse(""), "c");
    }

    @Test
    public void testMapRoundTrip() throws IOException {
        HashMap<String, Integer> map = HashMap.<String, Integer>nil().put("one", 1).put("two", 2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        Codec.mapEncoder(Codec.stringEncoder(), Codec.intEncoder()).encode(out, map);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        HashMap<String, Integer> decoded = Codec.mapDecoder(Codec.stringDecoder(), Codec.intDecoder()).decode(in).orElse(HashMap.nil());

        Assert.assertEquals(decoded.size(), 2);
        Assert.assertEquals(decoded.get("one").orElse(-1), Integer.valueOf(1));
        Assert.assertEquals(decoded.get("two").orElse(-1), Integer.valueOf(2));
    }
}

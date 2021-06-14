package cn.ning.offheap;


import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 0h2o
 * @date 2021/6/14
 **/
public class OffHeapLinkedListTest {

    static final long _1G = 1024 * 1024 * 1024;
    static final ElementSerializer intSerializer = new IntegerSerializer();
    static final int ELEMENT_SIZE = 10000;
    static final Random random = new Random();

    @Test
    public void testAddAndGet() {
        OffHeapLinkedList<Integer> offHeapLinkedList = new OffHeapLinkedList<>(_1G, intSerializer);
        for (int i = 0; i < ELEMENT_SIZE; i++) {
            offHeapLinkedList.add(i);
        }
        assertEquals(ELEMENT_SIZE, offHeapLinkedList.size());

        for (int i = 0; i < ELEMENT_SIZE; i++) {
            assertEquals(i, offHeapLinkedList.get(i));
        }

        offHeapLinkedList.destroy();
    }

    @Test
    public void testRemove() {
        OffHeapLinkedList<Integer> offHeapLinkedList = new OffHeapLinkedList<>(_1G, intSerializer);
        ArrayList<Integer> arrayList = new ArrayList<>(ELEMENT_SIZE);
        for (int i = 0; i < ELEMENT_SIZE; i++) {
            offHeapLinkedList.add(i);
            arrayList.add(i);
        }
        assertEquals(ELEMENT_SIZE, offHeapLinkedList.size());

        for (int i = 0; i < ELEMENT_SIZE; i++) {
            if (random.nextBoolean()) {
               offHeapLinkedList.remove(Integer.valueOf(i));
               arrayList.remove(Integer.valueOf(i));
            }
        }
        assertEquals(arrayList.size(), offHeapLinkedList.size());

        for (int i = 0; i < arrayList.size(); i++) {
            assertEquals(arrayList.get(i), offHeapLinkedList.get(i));
        }

        offHeapLinkedList.destroy();
    }


    static class IntegerSerializer implements ElementSerializer<Integer> {

        @Override
        public void serialize(Integer element, ByteBuffer buffer) {
            buffer.putInt(element);
        }

        @Override
        public Integer deserialize(ByteBuffer buffer) {
            return buffer.getInt();
        }

        @Override
        public int serializeSize(Integer element) {
            return Integer.BYTES;
        }
    }
}

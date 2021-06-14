package cn.ning.offheap.allocator;

import cn.ning.offheap.utils.MemoryUtil;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * @author ningjie6
 * @date 2021/6/14
 **/
public class UnsafeAllocatorTest {

    private static final Unsafe unsafe;

    static final int CAPACITY = 65536;
    static final ByteBuffer directBuffer;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            directBuffer = ByteBuffer.allocateDirect(CAPACITY);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void putMemory() {
        NativeMemoryAllocator allocator = new JNAMemoryAllocator();
        long address = allocator.allocate(16 + 4 + 4);

        System.out.println(MemoryUtil.getInt(address, 0));
        System.out.println(MemoryUtil.getInt(address, 4));
    }
}

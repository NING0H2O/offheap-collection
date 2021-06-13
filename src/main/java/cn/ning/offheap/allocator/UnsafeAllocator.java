package cn.ning.offheap.allocator;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author 0h2o
 * @date 2021/6/13
 **/
public class UnsafeAllocator implements NativeMemoryAllocator {

    static final Unsafe unsafe;

    static {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long allocate(long size) {
        try {
            return unsafe.allocateMemory(size);
        } catch (OutOfMemoryError e) {
            return 0L;
        }
    }

    @Override
    public void free(long peer) {
        unsafe.freeMemory(peer);
    }

    @Override
    public long getTotalAllocated() {
        return -1L;
    }
}

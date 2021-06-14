package cn.ning.offheap.utils;

import cn.ning.offheap.allocator.JNAMemoryAllocator;
import cn.ning.offheap.allocator.NativeMemoryAllocator;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * copy from OHC
 *
 * @author 0h2o
 * @date 2021/6/14
 **/
public class MemoryUtil {

    private static final Unsafe unsafe;
    // TODO 配置选择
    private static NativeMemoryAllocator allocator = new JNAMemoryAllocator();

    static {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static long allocate(long size) {
        long address = allocator.allocate(size);
        if (address == 0L) {
            throw new OutOfMemoryError();
        }
        return address;
    }

    public static void free(long address) {
        if (address <= 0) {
            return;
        }
        allocator.free(address);
    }

    public static void setMemory(long address, long offset, long len, byte value) {
        unsafe.setMemory(address + offset, len, value);
    }

    public static void putLong(long address, long offset, long value) {
        unsafe.putLong(null, address + offset, value);
    }

    public static long getLong(long address, long offset) {
        return unsafe.getLong(null, address + offset);
    }

    public static void putInt(long address, long offset, int value) {
        unsafe.putInt(null, address + offset, value);
    }

    public static int getInt(long address, long offset) {
        return unsafe.getInt(null, address + offset);
    }

    public static void copyMemory(long src, long srcOffset, long dst, long dstOffset, long len) {
        unsafe.copyMemory(null, src + srcOffset, null, dst + dstOffset, len);
    }

    private static final Class<?> DIRECT_BYTE_BUFFER_CLASS;
    private static final Class<?> DIRECT_BYTE_BUFFER_CLASS_R;
    private static final long DIRECT_BYTE_BUFFER_ADDRESS_OFFSET;
    private static final long DIRECT_BYTE_BUFFER_CAPACITY_OFFSET;
    private static final long DIRECT_BYTE_BUFFER_LIMIT_OFFSET;

    static {
        try {
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(0);
            ByteBuffer directReadOnly = directBuffer.asReadOnlyBuffer();
            Class<?> clazz = directBuffer.getClass();
            Class<?> clazzReadOnly = directReadOnly.getClass();
            DIRECT_BYTE_BUFFER_ADDRESS_OFFSET = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("address"));
            DIRECT_BYTE_BUFFER_CAPACITY_OFFSET = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("capacity"));
            DIRECT_BYTE_BUFFER_LIMIT_OFFSET = unsafe.objectFieldOffset(Buffer.class.getDeclaredField("limit"));
            DIRECT_BYTE_BUFFER_CLASS = clazz;
            DIRECT_BYTE_BUFFER_CLASS_R = clazzReadOnly;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer directBufferFor(long address, long offset, long len, boolean readOnly) {
        if (len > Integer.MAX_VALUE || len < 0L) {
            throw new IllegalArgumentException();
        }
        try {
            ByteBuffer bb = (ByteBuffer) unsafe.allocateInstance(readOnly ? DIRECT_BYTE_BUFFER_CLASS_R : DIRECT_BYTE_BUFFER_CLASS);
            unsafe.putLong(bb, DIRECT_BYTE_BUFFER_ADDRESS_OFFSET, address + offset);
            unsafe.putInt(bb, DIRECT_BYTE_BUFFER_CAPACITY_OFFSET, (int) len);
            unsafe.putInt(bb, DIRECT_BYTE_BUFFER_LIMIT_OFFSET, (int) len);
            bb.order(ByteOrder.BIG_ENDIAN);
            return bb;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}

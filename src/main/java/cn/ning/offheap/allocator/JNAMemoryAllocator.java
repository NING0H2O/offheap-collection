package cn.ning.offheap.allocator;


import com.sun.jna.Native;

/**
 *
 * @author 0h2o
 * @date 2021/6/13
 **/
public class JNAMemoryAllocator implements NativeMemoryAllocator{

    @Override
    public long allocate(long size) {
        try {
            return Native.malloc(size);
        } catch (OutOfMemoryError e) {
            return 0L;
        }
    }

    @Override
    public void free(long peer) {
        Native.free(peer);
    }

    @Override
    public long getTotalAllocated() {
        return -1L;
    }

}

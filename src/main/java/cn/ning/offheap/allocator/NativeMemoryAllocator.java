package cn.ning.offheap.allocator;

/**
 * @author 0h2o
 * @date 2021/6/13
 **/
public interface NativeMemoryAllocator {

    long allocate(long size);

    void free(long peer);

    long getTotalAllocated();
}

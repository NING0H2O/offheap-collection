package cn.ning.offheap;

import cn.ning.offheap.utils.MemoryUtil;
import com.google.common.base.Preconditions;

/**
 *
 * @author 0h2o
 * @date 2021/6/13
 **/
public class OffHeapLinkedList<T> {

    private static final int HEADER_SIZE = 20;
    private static final int PREV_OFFSET = 0;
    private static final int NEXT_OFFSET = 8;
    private static final int SIZE_OFFSET = 16;
    private static final int DATA_OFFSET = 20;

    private long maxCapacity;
    private long currCapacity;
    private int size;
    private long first;
    private long last;
    private ElementSerializer<T> serializer;

    public OffHeapLinkedList(long maxCapacity, ElementSerializer serializer) {
        Preconditions.checkArgument(maxCapacity > 0);
        Preconditions.checkArgument(serializer != null);
        this.maxCapacity = maxCapacity;
        this.serializer = serializer;
    }

    private long putToMemory(T element) {
        int serializeSize = serializer.serializeSize(element);
        long addr = MemoryUtil.allocate(HEADER_SIZE + serializeSize);
        MemoryUtil.setMemory(addr, 0, HEADER_SIZE, (byte)0);
        MemoryUtil.putInt(addr, SIZE_OFFSET, serializeSize);
        serializer.serialize(element, MemoryUtil.directBufferFor(addr, HEADER_SIZE, serializeSize, false));
        return addr;
    }

    private void linkLast(T element) {
        final long l = last;
        final long elementAddr = putToMemory(element);
        last = elementAddr;
        if (l == 0L) {
            first = elementAddr;
        } else {
            MemoryUtil.putLong(l, NEXT_OFFSET, elementAddr);
            MemoryUtil.putLong(elementAddr, PREV_OFFSET, l);
        }
        size ++;
        currCapacity += MemoryUtil.getInt(elementAddr, SIZE_OFFSET);
    }

    public boolean add(T element) {
        Preconditions.checkArgument(element != null);
        if (checkCapacityOverMax(element)) {
            return false;
        }
        linkLast(element);
        return true;
    }

    public boolean remove(T element) {
        Preconditions.checkArgument(element != null);
        for (long addr = first; addr != 0L; addr = MemoryUtil.getLong(addr, NEXT_OFFSET)) {
            int serializerSize = MemoryUtil.getInt(addr, SIZE_OFFSET);
            T data = serializer.deserialize(MemoryUtil.directBufferFor(addr, DATA_OFFSET, serializerSize, true));
            if (element.equals(data)) {
                unlink(addr);
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean contains(T element) {
        Preconditions.checkArgument(element != null);
        return indexOf(element) != -1;
    }

    private int indexOf(T element) {
        int index = 0;
        for (long addr = first; addr != 0; addr = MemoryUtil.getLong(addr, NEXT_OFFSET)) {
            int serializerSize = MemoryUtil.getInt(addr, SIZE_OFFSET);
            T data = serializer.deserialize(MemoryUtil.directBufferFor(addr, DATA_OFFSET, serializerSize, true));
            if (element.equals(data)) {
                return index;
            }
            index ++;
        }
        return -1;
    }

    public T get(int index) {
        Preconditions.checkArgument(index >= 0 && index < size);
        long elementAddr = elementAddr(index);
        int serializerSize = MemoryUtil.getInt(elementAddr, SIZE_OFFSET);
        return serializer.deserialize(MemoryUtil.directBufferFor(elementAddr, DATA_OFFSET, serializerSize, true));
    }

    public T set(int index, T element) {
        Preconditions.checkArgument(index >= 0 && index < size);
        long oldAddr = elementAddr(index);

        int newSize = serializer.serializeSize(element);
        int oldSize = MemoryUtil.getInt(oldAddr, SIZE_OFFSET);
        if (newSize < oldSize) {
            // 快速替换
            serializer.serialize(element, MemoryUtil.directBufferFor(oldAddr, DATA_OFFSET, oldSize, false));
        } else {
            long newAddr = MemoryUtil.allocate(newSize);
            long prev = MemoryUtil.getLong(oldAddr, PREV_OFFSET);
            long next = MemoryUtil.getLong(oldAddr, NEXT_OFFSET);
            MemoryUtil.putLong(next, PREV_OFFSET, newAddr);
            MemoryUtil.putLong(prev, NEXT_OFFSET, newAddr);
            MemoryUtil.putLong(newAddr, PREV_OFFSET, prev);
            MemoryUtil.putLong(newAddr, NEXT_OFFSET, next);
            MemoryUtil.putInt(newAddr, SIZE_OFFSET, newSize);
            serializer.serialize(element, MemoryUtil.directBufferFor(newAddr, DATA_OFFSET, newSize, false));
        }

        T oldElement = serializer.deserialize(MemoryUtil.directBufferFor(oldAddr, DATA_OFFSET, oldSize, true));
        MemoryUtil.free(oldAddr);
        return oldElement;
    }

    public boolean add(int index, T element) {
        Preconditions.checkArgument(index >= 0 && index <= size);
        if (checkCapacityOverMax(element)) {
            return false;
        }

        if (index == size) {
            linkLast(element);
        } else {
            long nextAddr = elementAddr(index);
            long prevAddr = MemoryUtil.getLong(nextAddr, PREV_OFFSET);
            long elementAddr = putToMemory(element);
            if (prevAddr == 0) {
                first = elementAddr;
            } else {
                MemoryUtil.putLong(prevAddr, NEXT_OFFSET, elementAddr);
            }
            MemoryUtil.putLong(nextAddr, PREV_OFFSET, elementAddr);
            MemoryUtil.putLong(elementAddr, PREV_OFFSET, prevAddr);
            MemoryUtil.putLong(elementAddr, NEXT_OFFSET, nextAddr);
            size ++;
            currCapacity += MemoryUtil.getInt(elementAddr, SIZE_OFFSET);
        }
        return true;
    }

    public boolean remove(int index) {
        Preconditions.checkArgument(index >= 0 && index < size);
        unlink(elementAddr(index));
        return true;
    }

    public void destroy() {
        long addr = first, nextAddr;
        while (addr != 0L) {
            nextAddr = MemoryUtil.getLong(addr, NEXT_OFFSET);
            MemoryUtil.free(addr);
            addr = nextAddr;
        }
    }

    private long elementAddr(int index) {
        long addr;
        if (index < (size >> 1)) {
            addr = first;
            for (int i = 0; i < index; i++) {
                addr = MemoryUtil.getLong(addr, NEXT_OFFSET);
            }
        } else {
            addr = last;
            for (int i = size-1; i > index; i--) {
                addr = MemoryUtil.getLong(addr, PREV_OFFSET);
            }
        }
        return addr;
    }

    private void unlink(long addr) {
        final long prev = MemoryUtil.getLong(addr, PREV_OFFSET);
        final long next = MemoryUtil.getLong(addr, NEXT_OFFSET);

        if (prev == 0L) {
            first = next;
        } else {
            MemoryUtil.putLong(prev, NEXT_OFFSET, next);
        }

        if (next == 0L) {
            last = prev;
        } else {
            MemoryUtil.putLong(next, PREV_OFFSET, prev);
        }

        MemoryUtil.free(addr);
        size --;
        currCapacity -= MemoryUtil.getInt(addr, SIZE_OFFSET);
    }

    private boolean checkCapacityOverMax(T element) {
        return currCapacity + HEADER_SIZE + serializer.serializeSize(element) > maxCapacity;
    }



}

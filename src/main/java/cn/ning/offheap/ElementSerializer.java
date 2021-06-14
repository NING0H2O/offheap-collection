package cn.ning.offheap;

import java.nio.ByteBuffer;

/**
 * @author ningjie6
 * @date 2021/6/14
 **/
public interface ElementSerializer<T> {

    void serialize(T element, ByteBuffer buffer);

    T deserialize(ByteBuffer buffer);

    int serializeSize(T element);
}

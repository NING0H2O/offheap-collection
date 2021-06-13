package cn.ning.offheap;

/**
 *
 * @author 0h2o
 * @date 2021/6/13
 **/
public class OffHeapLinkedList {

    private long maxCapacity;
    private long first;
    private long last;



    void linkLast() {
        // TODO
    }

    public boolean add() {
        linkLast();
        return true;
    }



    private static class InMemoryNode {
        private long prev;
        private long next;

    }
}

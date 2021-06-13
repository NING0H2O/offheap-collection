package cn.ning.other;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

/**
 *
 * url: https://mechanical-sympathy.blogspot.com/2012/10/compact-off-heap-structurestuples-in.html
 * run with -x
 */
public class TestJavaMemoryLayout {

    private static final int NUM_RECORDS = 50 * 1000 * 1000;

    private static JavaMemoryTrade[] trades;

    // ==== MAC 2.2 GHz Intel Core i7-4770HQ（HT enable, 4 cores, 8 threads）、16 GB 1600 MHz DDR3
    //Memory 4,116,185,088 total, 1,092,121,680 free
    //0 - duration 13206ms
    //buyCost = 6958024115266225536 sellCost = 6959274115241225536
    //Memory 4,116,185,088 total, 1,114,436,376 free
    //1 - duration 11562ms
    //buyCost = 6958024115266225536 sellCost = 6959274115241225536
    //Memory 4,116,185,088 total, 1,096,807,312 free
    //2 - duration 11166ms
    //buyCost = 6958024115266225536 sellCost = 6959274115241225536
    //Memory 3,817,865,216 total, 815,359,480 free
    //3 - duration 25044ms
    //buyCost = 6958024115266225536 sellCost = 6959274115241225536
    //Memory 3,817,865,216 total, 809,236,824 free
    //4 - duration 21879ms
    //buyCost = 6958024115266225536 sellCost = 6959274115241225536
    public static void main(final String[] args) {
        for (int i = 0; i < 5; i++) {
            System.gc();
            perfRun(i);
        }

        // 计算JavaMemoryTrade内存占用 = 56 bytes
        // 计算公式如下：12（对象头，指针压缩）+ 42（数据结构）= 54byte
        // JVM 8位对齐 roundTo8(54) = 56 bytes
        final JavaMemoryTrade trade = get(0);
        System.out.println(ObjectSizeCalculator.getObjectSize(trade));
    }

    private static void perfRun(final int runNum) {
        long start = System.currentTimeMillis();

        init();

        System.out.format("Memory %,d total, %,d free\n",
                Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory());

        long buyCost = 0;
        long sellCost = 0;

        for (int i = 0; i < NUM_RECORDS; i++) {
            final JavaMemoryTrade trade = get(i);

            if (trade.getSide() == 'B') {
                buyCost += (trade.getPrice() * trade.getQuantity());
            } else {
                sellCost += (trade.getPrice() * trade.getQuantity());
            }

        }

        long duration = System.currentTimeMillis() - start;
        System.out.println(runNum + " - duration " + duration + "ms");
        System.out.println("buyCost = " + buyCost + " sellCost = " + sellCost);
    }

    private static JavaMemoryTrade get(final int index) {
        return trades[index];
    }

    public static void init() {
        trades = new JavaMemoryTrade[NUM_RECORDS];

        final byte[] londonStockExchange = {'X', 'L', 'O', 'N'};
        final int venueCode = pack(londonStockExchange);

        final byte[] billiton = {'B', 'H', 'P'};
        final int instrumentCode = pack(billiton);

        for (int i = 0; i < NUM_RECORDS; i++) {
            JavaMemoryTrade trade = new JavaMemoryTrade();
            trades[i] = trade;

            trade.setTradeId(i);
            trade.setClientId(1);
            trade.setVenueCode(venueCode);
            trade.setInstrumentCode(instrumentCode);

            trade.setPrice(i);
            trade.setQuantity(i);

            trade.setSide((i & 1) == 0 ? 'B' : 'S');
        }
    }

    private static int pack(final byte[] value) {
        int result = 0;
        switch (value.length) {
            case 4:
                result = (value[3]);
            case 3:
                result |= ((int) value[2] << 8);
            case 2:
                result |= ((int) value[1] << 16);
            case 1:
                result |= ((int) value[0] << 24);
                break;

            default:
                throw new IllegalArgumentException("Invalid array size");
        }

        return result;
    }

    private static class JavaMemoryTrade {
        private long tradeId;
        private long clientId;
        private int venueCode;
        private int instrumentCode;
        private long price;
        private long quantity;
        private char side;

        public long getTradeId() {
            return tradeId;
        }

        public void setTradeId(final long tradeId) {
            this.tradeId = tradeId;
        }

        public long getClientId() {
            return clientId;
        }

        public void setClientId(final long clientId) {
            this.clientId = clientId;
        }

        public int getVenueCode() {
            return venueCode;
        }

        public void setVenueCode(final int venueCode) {
            this.venueCode = venueCode;
        }

        public int getInstrumentCode() {
            return instrumentCode;
        }

        public void setInstrumentCode(final int instrumentCode) {
            this.instrumentCode = instrumentCode;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(final long price) {
            this.price = price;
        }

        public long getQuantity() {
            return quantity;
        }

        public void setQuantity(final long quantity) {
            this.quantity = quantity;
        }

        public char getSide() {
            return side;
        }

        public void setSide(final char side) {
            this.side = side;
        }
    }
}

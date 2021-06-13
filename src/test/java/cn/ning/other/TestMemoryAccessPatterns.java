package cn.ning.other;

/**
 * 内存访问模式（Memory Access Pattern）对访问延迟的影响
 * url: https://mechanical-sympathy.blogspot.com/2012/08/memory-access-patterns-are-important.html
 * run with -Xmx4g
 *
 * 内存访问具有时空局部性，此外内存访问可能遵循可预测的模式。这种行为被用来进行处理器内核性能优化（缓存、内存预读取、分支预测）
 * 总体来说，内存访问模式越可预测，访问延迟就越好
 */
public class TestMemoryAccessPatterns {

    private static final int LONG_SIZE = 8;
    // large page 一般为4k，大页面相比小页面同样TLB缓存可以覆盖更大的地址空间（减少缓存未命中的损失）
    private static final int PAGE_SIZE = 2 * 1024 * 1024;
    private static final int ONE_GIG = 1024 * 1024 * 1024;
    private static final long TWO_GIG = 2L * ONE_GIG;

    private static final int ARRAY_SIZE = (int) (TWO_GIG / LONG_SIZE);
    private static final int WORDS_PER_PAGE = PAGE_SIZE / LONG_SIZE;

    private static final int ARRAY_MASK = ARRAY_SIZE - 1;
    private static final int PAGE_MASK = WORDS_PER_PAGE - 1;

    private static final int PRIME_INC = 514229;
    private static final long LARGE_PRIME_INC = 70368760954879L;

    private static final long[] memory = new long[ARRAY_SIZE];

    static {
        for (int i = 0; i < ARRAY_SIZE; i++) {
            memory[i] = 777;
        }
    }

    public enum StrideType {
        // 线性访问内存
        LINEAR_WALK {
            public int next(final int pageOffset, final int wordOffset, final int pos) {
                return (pos + 1) & ARRAY_MASK;
            }
        },
        // 页随机访问内存（本机内存中随机访问）
        RANDOM_PAGE_WALK {
            public int next(final int pageOffset, final int wordOffset, final int pos) {
                return pageOffset + ((pos + PRIME_INC) & PAGE_MASK);
            }
        },
        // 堆内存随机访问
        RANDOM_HEAP_WALK {
            public int next(final int pageOffset, final int wordOffset, final int pos) {
                return (pos + PRIME_INC) & ARRAY_MASK;
            }
        };

        public abstract int next(int pageOffset, int wordOffset, int pos);
    }

    public static void main(final String[] args) {
        final StrideType strideType;
        switch (Integer.parseInt(args[0])) {
            case 1:
                strideType = StrideType.LINEAR_WALK;
                break;

            case 2:
                strideType = StrideType.RANDOM_PAGE_WALK;
                break;

            case 3:
                strideType = StrideType.RANDOM_HEAP_WALK;
                break;

            default:
                throw new IllegalArgumentException("Unknown StrideType");
        }

        for (int i = 0; i < 5; i++) {
            perfTest(i, strideType);
        }
    }

    private static void perfTest(final int runNumber, final StrideType strideType) {
        final long start = System.nanoTime();

        int pos = -1;
        long result = 0;
        for (int pageOffset = 0; pageOffset < ARRAY_SIZE; pageOffset += WORDS_PER_PAGE) {
            for (int wordOffset = pageOffset, limit = pageOffset + WORDS_PER_PAGE;
                 wordOffset < limit;
                 wordOffset++) {
                pos = strideType.next(pageOffset, wordOffset, pos);
                result += memory[pos];
            }
        }

        final long duration = System.nanoTime() - start;
        final double nsOp = duration / (double) ARRAY_SIZE;

        if (208574349312L != result) {
            throw new IllegalStateException();
        }

        System.out.format("%d - %.2fns %s\n", Integer.valueOf(runNumber), Double.valueOf(nsOp), strideType);
        // ==== MAC 2.2 GHz Intel Core i7-4770HQ（HT enable, 4 cores, 8 threads）、16 GB 1600 MHz DDR3
        //0 - 0.81ns LINEAR_WALK
        //1 - 0.79ns LINEAR_WALK
        //2 - 0.89ns LINEAR_WALK
        //3 - 0.88ns LINEAR_WALK
        //4 - 0.86ns LINEAR_WALK

        //0 - 3.54ns RANDOM_PAGE_WALK
        //1 - 3.65ns RANDOM_PAGE_WALK
        //2 - 3.49ns RANDOM_PAGE_WALK
        //3 - 3.51ns RANDOM_PAGE_WALK
        //4 - 3.51ns RANDOM_PAGE_WALK

        //0 - 29.90ns RANDOM_HEAP_WALK
        //1 - 29.96ns RANDOM_HEAP_WALK
        //2 - 34.02ns RANDOM_HEAP_WALK
        //3 - 33.73ns RANDOM_HEAP_WALK
        //4 - 32.43ns RANDOM_HEAP_WALK
    }
}
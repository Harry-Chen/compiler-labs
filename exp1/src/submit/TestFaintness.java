package submit;

class TestFaintness {
    /**
     * In this method all variables are faint because the final value is never used.
     * Sample out is at src/test/Faintness.out
     */
    void test1() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return;
    }

    /**
     * Write your test cases here. Create as many methods as you want.
     * Run the test from root dir using
     * ./run.sh flow.Flow submit.MySolver submit.Faintness submit.TestFaintness
     */

    int testReturn() {
        int x = Integer.valueOf(2);
        int y = x + 2;
        // here x (R3), y (R4) are not faint
        int z = x + y;
        // here z (R5) is not faint
        return z;
    }

    // test the side effect (function call) that prevents faintness
    int testSideEffect() {
        int x = Integer.valueOf(233);
        int y = Integer.valueOf(234); // make sure that y (R4) is not optimized
        // here x and y are not faint
        System.out.println(x);
        // here everything except y are faint
        return y;
    }

    // due to optimization, everything variable is fainted in this test
    int testOptimization() {
        int y = Integer.valueOf(2);
        // here y (R3) is faint
        int z = 1 + y;
        // this statement faints y
        z = 0;
        // here z is fainted (constant is folded)
        return z;
    }

    // test the divergence in control flow
    int testControlFlow() {
        int x = Integer.valueOf(1); // x is R3
        int y = Integer.valueOf(2); // y is R4
        // here both x and y are not fainted
        if (Integer.valueOf(2333) == 2334) {
            // here x and y are not faint
            x = y + 1;
            // here y is faint
        } else {
            // here y is faint
            x = 5;
        }
        // here only x is not faint
        return x;
    }

    // test more divergences
    int testComplexControlFlow() {
        int x = Integer.valueOf(1); // x is R3
        int y = Integer.valueOf(2); // y is R4
        // anywhere in this loop, x and y should not be faint
        while (x < 5) {
            x = x + y;
            if (y < 3) {
                y = y + 1;
            }
        }
        // here x is not faint
        return x;
    }
}

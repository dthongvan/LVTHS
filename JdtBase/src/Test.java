public class Test {
    public static int computeValue(int x) {
        int result = 0;
        if (x > 0) {
            result = x * 2;
        } else if (x == 0) {
            result = 1;
        }
        return result;
    }

    public static int computeValue2(int x) {
        int result = 0;
        if (x < 0) {
            result = x * 2;
        } else if (x == 0) {
            result = 1;
        }
        return result;
    }

}


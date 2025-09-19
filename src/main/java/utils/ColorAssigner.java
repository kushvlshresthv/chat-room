package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ColorAssigner {
    // Reds / Oranges / Yellows
    public static final int RED = 196;
    public static final int ORANGE = 208;
    public static final int YELLOW = 226;

    // Greens / Cyans
    public static final int GREEN = 46;
    public static final int LIGHT_GREEN = 82;
    public static final int CYAN = 51;
    public static final int TEAL = 43;

    // Blues / Purples
    public static final int BLUE = 27;
    public static final int LIGHT_BLUE = 39;
    public static final int PURPLE = 93;
    public static final int MAGENTA = 201;

    // Light neutrals / grays
    public static final int LIGHT_GRAY = 250;
    public static final int WHITE = 231;

    static List<Integer> colors = new ArrayList<>();

    static {
        colors.add(RED);
        colors.add(GREEN);
        colors.add(YELLOW);
        colors.add(MAGENTA);
        colors.add(TEAL);
        colors.add(BLUE);
        colors.add(PURPLE);
        colors.add(LIGHT_BLUE);
        colors.add(LIGHT_GREEN);
        colors.add(YELLOW);
    }


    static int count = 0;

    public static int getNextColor() {
        int retVal = colors.get(count);
        count = (count + 1) % colors.size();
        return retVal;
    }
}

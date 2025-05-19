import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 华容道棋盘布局管理器，提供多种初始布局方案
 */
public class BoardLayouts {
    private static final Random random = new Random();

    public static List<String> getLayoutNames() {
        return List.of(
                "横刀立马",
                "指挥若定",
                "将拥曹营",
                "齐头并进",
                "兵分三路",
                "雨声淅沥",
                "左右布兵",
                "桃花园中",
                "一路进军",
                "一路顺风",
                "随机"
        );
    }

    public static List<GameFrame.Block> getLayout(int index) {
        switch (index) {
            case 0: return getLayout1();
            case 1: return getLayout2();
            case 2: return getLayout3();
            case 3: return getLayout4();
            case 4: return getLayout5();
            case 5: return getLayout6();
            case 6: return getLayout7();
            case 7: return getLayout8();
            case 8: return getLayout9();
            case 9: return getLayout10();
            case 10: return getRandomLayout();
            default: return getLayout1();
        }
    }

    public static int getLayoutCount() {
        return getLayoutNames().size();
    }

    public static List<GameFrame.Block> getRandomLayout() {
        int layoutCount = getLayoutCount() - 1;
        int layoutIndex = random.nextInt(layoutCount);
        return getLayout(layoutIndex);
    }

    // 1. 横刀立马
    public static List<GameFrame.Block> getLayout1() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        // 曹操
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        // 竖将
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        //
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        // 士兵
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.PURPLE, "紫兵1"));
        return blocks;
    }

    // 2. 指挥若定
    public static List<GameFrame.Block> getLayout2() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.BLUE, "横将1"));
        blocks.add(new GameFrame.Block(2, 3, 2, 1, Color.BLUE, "横将2"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        return blocks;
    }

    // 3. 将拥曹营
    public static List<GameFrame.Block> getLayout3() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.BLUE, "横将1"));
        blocks.add(new GameFrame.Block(2, 3, 2, 1, Color.BLUE, "横将2"));
        blocks.add(new GameFrame.Block(1, 3, 1, 2, Color.BLUE, "竖将3"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        return blocks;
    }

    // 4. 齐头并进
    public static List<GameFrame.Block> getLayout4() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        blocks.add(new GameFrame.Block(1, 4, 2, 1, Color.PURPLE, "紫兵"));
        return blocks;
    }

    // 5. 兵分三路
    public static List<GameFrame.Block> getLayout5() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.BLUE, "横将1"));
        blocks.add(new GameFrame.Block(2, 3, 2, 1, Color.BLUE, "横将2"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        return blocks;
    }

    // 6. 雨声淅沥
    public static List<GameFrame.Block> getLayout6() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 0, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 2, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 2, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.BLUE, "横将1"));
        blocks.add(new GameFrame.Block(2, 3, 2, 1, Color.BLUE, "横将2"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        blocks.add(new GameFrame.Block(1, 3, 1, 1, Color.PURPLE, "紫兵"));
        return blocks;
    }

    // 7. 左右布兵
    public static List<GameFrame.Block> getLayout7() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        blocks.add(new GameFrame.Block(1, 4, 2, 1, Color.PURPLE, "紫兵"));
        return blocks;
    }

    // 8. 桃花园中
    public static List<GameFrame.Block> getLayout8() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        blocks.add(new GameFrame.Block(1, 4, 2, 1, Color.PURPLE, "紫兵"));
        return blocks;
    }

    // 9. 一路进军
    public static List<GameFrame.Block> getLayout9() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        blocks.add(new GameFrame.Block(2, 3, 1, 2, Color.BLUE, "竖将3"));
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        return blocks;
    }

    // 10. 一路顺风
    public static List<GameFrame.Block> getLayout10() {
        List<GameFrame.Block> blocks = new ArrayList<>();
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));
        blocks.add(new GameFrame.Block(0, 1, 1, 2, Color.BLUE, "竖将1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 2, Color.BLUE, "竖将2"));
        blocks.add(new GameFrame.Block(1, 3, 2, 1, Color.BLUE, "横将"));
        blocks.add(new GameFrame.Block(2, 3, 1, 2, Color.BLUE, "竖将3"));
        blocks.add(new GameFrame.Block(0, 3, 1, 1, Color.GREEN, "士兵1"));
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.GREEN, "士兵2"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.GREEN, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.GREEN, "士兵4"));
        return blocks;
    }
}
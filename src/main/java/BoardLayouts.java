import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 华容道棋盘布局管理器，提供多种初始布局方案
 */
public class BoardLayouts {
    private static final Random random = new Random();

    /**
     * 获取随机布局
     */
    public static List<GameFrame.Block> getRandomLayout() {
        int layoutCount = 8; // 总共有8种布局
        int layoutIndex = random.nextInt(layoutCount);

        return switch (layoutIndex) {
            case 0 -> getStandardLayout();
            case 1 -> getClassicLayout();
            case 2 -> getDiagonalLayout();
            case 3 -> getComplexLayout();
            case 4 -> getAdvancedLayout();
            case 5 -> getChallengeLayout();
            case 6 -> getExpertLayout();
            case 7 -> getStrategicLayout();
            default -> getStandardLayout();
        };
    }

    /**
     * 获取指定索引的布局
     */
    public static List<GameFrame.Block> getLayout(int index) {
        return switch (index) {
            case 0 -> getStandardLayout();
            case 1 -> getClassicLayout();
            case 2 -> getDiagonalLayout();
            case 3 -> getComplexLayout();
            case 4 -> getAdvancedLayout();
            case 5 -> getChallengeLayout();
            case 6 -> getExpertLayout();
            case 7 -> getStrategicLayout();
            default -> getStandardLayout();
        };
    }

    /**
     * 获取布局总数
     */
    public static int getLayoutCount() {
        return 8;
    }

    /**
     * 标准布局（原始布局）
     */
    public static List<GameFrame.Block> getStandardLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(0, 1, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(2, 1, 2, 1, Color.BLUE, "关羽"));

        // 其他普通方块 (1x2)
        blocks.add(new GameFrame.Block(0, 0, 1, 2, Color.GREEN, "普通方块1"));
        blocks.add(new GameFrame.Block(3, 0, 1, 2, Color.GREEN, "普通方块2"));
        blocks.add(new GameFrame.Block(0, 3, 1, 2, Color.GREEN, "普通方块3"));
        blocks.add(new GameFrame.Block(3, 3, 1, 2, Color.GREEN, "普通方块4"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(1, 0, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(2, 0, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(1, 3, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(2, 3, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 经典布局（常见的华容道布局）
     */
    public static List<GameFrame.Block> getClassicLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(0, 0, 2, 1, Color.BLUE, "关羽"));

        // 其他普通方块 (1x2)
        blocks.add(new GameFrame.Block(0, 2, 1, 2, Color.GREEN, "普通方块1"));
        blocks.add(new GameFrame.Block(3, 0, 1, 2, Color.GREEN, "普通方块2"));
        blocks.add(new GameFrame.Block(3, 2, 1, 2, Color.GREEN, "普通方块3"));
        blocks.add(new GameFrame.Block(2, 0, 1, 2, Color.GREEN, "普通方块4"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 对角线布局（曹操在左上角）
     */
    public static List<GameFrame.Block> getDiagonalLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(0, 0, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(2, 0, 2, 1, Color.BLUE, "关羽"));

        // 其他普通方块 (1x2)
        blocks.add(new GameFrame.Block(0, 2, 1, 2, Color.GREEN, "普通方块1"));
        blocks.add(new GameFrame.Block(2, 2, 1, 2, Color.GREEN, "普通方块2"));
        blocks.add(new GameFrame.Block(0, 4, 1, 2, Color.GREEN, "普通方块3"));
        blocks.add(new GameFrame.Block(3, 2, 1, 2, Color.GREEN, "普通方块4"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(2, 1, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(3, 1, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 复杂布局（需要更多步骤才能解决）
     */
    public static List<GameFrame.Block> getComplexLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.BLUE, "关羽"));

        // 其他普通方块 (1x2)
        blocks.add(new GameFrame.Block(0, 0, 1, 2, Color.GREEN, "普通方块1"));
        blocks.add(new GameFrame.Block(3, 0, 1, 2, Color.GREEN, "普通方块2"));
        blocks.add(new GameFrame.Block(2, 2, 1, 2, Color.GREEN, "普通方块3"));
        blocks.add(new GameFrame.Block(3, 3, 1, 2, Color.GREEN, "普通方块4"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(0, 2, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(1, 0, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(1, 3, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(2, 0, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 进阶布局（需要策略规划）
     */
    public static List<GameFrame.Block> getAdvancedLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(0, 0, 2, 1, Color.BLUE, "关羽"));

        // 张飞方块 (1x2)
        blocks.add(new GameFrame.Block(0, 2, 1, 2, Color.PURPLE, "张飞"));

        // 赵云方块 (1x2)
        blocks.add(new GameFrame.Block(3, 0, 1, 2, Color.ORANGE, "赵云"));

        // 马超方块 (1x2)
        blocks.add(new GameFrame.Block(3, 2, 1, 2, Color.BROWN, "马超"));

        // 黄忠方块 (1x2)
        blocks.add(new GameFrame.Block(2, 0, 1, 2, Color.DARKGREEN, "黄忠"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(0, 4, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(1, 4, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(2, 4, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(3, 4, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 挑战布局（紧凑结构）
     */
    public static List<GameFrame.Block> getChallengeLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(0, 1, 2, 2, Color.RED, "曹操"));

        // 横向大将 (2x1)
        blocks.add(new GameFrame.Block(2, 0, 2, 1, Color.BLUE, "横向将1"));
        blocks.add(new GameFrame.Block(2, 2, 2, 1, Color.PURPLE, "横向将2"));

        // 纵向大将 (1x2)
        blocks.add(new GameFrame.Block(0, 0, 1, 2, Color.GREEN, "纵向将1"));
        blocks.add(new GameFrame.Block(0, 3, 1, 2, Color.ORANGE, "纵向将2"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(1, 0, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(1, 3, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(3, 1, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(3, 3, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 专家布局（需要复杂策略）
     */
    public static List<GameFrame.Block> getExpertLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(1, 1, 2, 2, Color.RED, "曹操"));

        // 横向大将 (2x1)
        blocks.add(new GameFrame.Block(0, 0, 2, 1, Color.BLUE, "横向将1"));
        blocks.add(new GameFrame.Block(0, 3, 2, 1, Color.PURPLE, "横向将2"));

        // 纵向大将 (1x2)
        blocks.add(new GameFrame.Block(2, 0, 1, 2, Color.GREEN, "纵向将1"));
        blocks.add(new GameFrame.Block(3, 2, 1, 2, Color.ORANGE, "纵向将2"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(2, 2, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(3, 0, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(2, 3, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(3, 1, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }

    /**
     * 战略布局（需要多步规划）
     */
    public static List<GameFrame.Block> getStrategicLayout() {
        List<GameFrame.Block> blocks = new ArrayList<>();

        // 曹操方块 (2x2)
        blocks.add(new GameFrame.Block(1, 0, 2, 2, Color.RED, "曹操"));

        // 关羽方块 (2x1)
        blocks.add(new GameFrame.Block(0, 2, 2, 1, Color.BLUE, "关羽"));

        // 张飞方块 (2x1)
        blocks.add(new GameFrame.Block(2, 2, 2, 1, Color.PURPLE, "张飞"));

        // 赵云方块 (1x2)
        blocks.add(new GameFrame.Block(0, 3, 1, 2, Color.GREEN, "赵云"));

        // 马超方块 (1x2)
        blocks.add(new GameFrame.Block(3, 3, 1, 2, Color.ORANGE, "马超"));

        // 士兵方块 (1x1)
        blocks.add(new GameFrame.Block(0, 0, 1, 1, Color.YELLOW, "士兵1"));
        blocks.add(new GameFrame.Block(1, 2, 1, 1, Color.YELLOW, "士兵2"));
        blocks.add(new GameFrame.Block(2, 0, 1, 1, Color.YELLOW, "士兵3"));
        blocks.add(new GameFrame.Block(3, 0, 1, 1, Color.YELLOW, "士兵4"));

        return blocks;
    }
}
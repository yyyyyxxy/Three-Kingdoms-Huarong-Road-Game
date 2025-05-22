import java.util.*;
import javafx.scene.paint.Color;

public class AIHuarongSolver {
    private static final int BOARD_ROWS = 5;
    private static final int BOARD_COLS = 4;
    private static final int EXIT_ROW = 3;
    private static final int EXIT_COL = 1;

    // 状态唯一编码（用于判重）
    private static String encode(List<GameFrame.Block> blocks) {
        List<String> parts = new ArrayList<>();
        for (GameFrame.Block b : blocks) {
            parts.add(b.getName() + "(" + b.getRow() + "," + b.getCol() + ")");
        }
        Collections.sort(parts); // 保证顺序一致
        return String.join(";", parts);
    }

    // 判断是否为终局
    private static boolean isGoal(List<GameFrame.Block> blocks) {
        for (GameFrame.Block b : blocks) {
            if ("曹操".equals(b.getName()) && b.getRow() == EXIT_ROW && b.getCol() == EXIT_COL && b.getWidth() == 2 && b.getHeight() == 2) {
                return true;
            }
        }
        return false;
    }

    // 生成所有可能的下一步
    private static List<List<GameFrame.Block>> getNextStates(List<GameFrame.Block> blocks) {
        List<List<GameFrame.Block>> nextStates = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            GameFrame.Block block = blocks.get(i);
            for (int[] dir : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                int newRow = block.getRow() + dir[0];
                int newCol = block.getCol() + dir[1];
                if (isValidMove(blocks, i, newRow, newCol)) {
                    List<GameFrame.Block> newBlocks = deepCopy(blocks);
                    newBlocks.get(i).setRow(newRow);
                    newBlocks.get(i).setCol(newCol);
                    nextStates.add(newBlocks);
                }
            }
        }
        return nextStates;
    }

    // 判断第i个棋子能否移动到新位置
    private static boolean isValidMove(List<GameFrame.Block> blocks, int idx, int newRow, int newCol) {
        GameFrame.Block block = blocks.get(idx);
        if (newRow < 0 || newRow + block.getHeight() > BOARD_ROWS ||
                newCol < 0 || newCol + block.getWidth() > BOARD_COLS) {
            return false;
        }
        for (int j = 0; j < blocks.size(); j++) {
            if (j == idx) continue;
            GameFrame.Block other = blocks.get(j);
            if (isOverlapping(newRow, newCol, block.getWidth(), block.getHeight(), other)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOverlapping(int row1, int col1, int w1, int h1, GameFrame.Block b2) {
        int row2 = b2.getRow();
        int col2 = b2.getCol();
        int w2 = b2.getWidth();
        int h2 = b2.getHeight();
        return !(row1 + h1 <= row2 ||
                row2 + h2 <= row1 ||
                col1 + w1 <= col2 ||
                col2 + w2 <= col1);
    }

    // 深拷贝
    private static List<GameFrame.Block> deepCopy(List<GameFrame.Block> blocks) {
        List<GameFrame.Block> copy = new ArrayList<>();
        for (GameFrame.Block b : blocks) {
            copy.add(new GameFrame.Block(b.getRow(), b.getCol(), b.getWidth(), b.getHeight(), b.getColor(), b.getName()));
        }
        return copy;
    }

    // BFS主流程
    public static List<List<GameFrame.Block>> solve(List<GameFrame.Block> startBlocks, int layoutIndex) {
        String startCode = encode(startBlocks);
        Set<String> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(new Node(startBlocks, null));
        visited.add(startCode);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            List<GameFrame.Block> curr = node.state;
            if (isGoal(curr)) {
                // 回溯路径
                LinkedList<List<GameFrame.Block>> path = new LinkedList<>();
                Node p = node;
                while (p != null) {
                    path.addFirst(deepCopy(p.state));
                    p = p.prev;
                }
                return path;
            }
            for (List<GameFrame.Block> next : getNextStates(curr)) {
                String code = encode(next);
                if (!visited.contains(code)) {
                    visited.add(code);
                    queue.add(new Node(next, node));
                }
            }
        }
        return null; // 无解
    }

    private static class Node {
        List<GameFrame.Block> state;
        Node prev;
        Node(List<GameFrame.Block> state, Node prev) {
            this.state = state;
            this.prev = prev;
        }
    }
}
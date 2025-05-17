import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/**
 * 华容道游戏主窗口
 */
public class GameFrame extends Application {
    private static final int BOARD_ROWS = 4;
    private static final int BOARD_COLS = 5;
    private static final int CELL_SIZE = 100;
    private static final int EXIT_ROW = 1;
    private static final int EXIT_COL = 2;

    private GridPane gameBoard;
    private List<Block> blocks;
    private Block selectedBlock;
    private int moveCount;
    private Label moveCountLabel;
    private Stage primaryStage;
    private boolean gameWon;
    private List<Button> directionButtons;



    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("华容道游戏");
        primaryStage.setResizable(false);

        // 显示布局选择对话框
        showLayoutSelectionDialog();

        // 创建主界面
        BorderPane root = createMainLayout();

        // 初始化游戏数据
        initGameData();


        // 设置场景
        Scene scene = new Scene(root, BOARD_COLS * CELL_SIZE + 200, BOARD_ROWS * CELL_SIZE + 150);
        primaryStage.setScene(scene);

        // 添加键盘事件处理
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        primaryStage.show();
    }

    /**
     * 初始化游戏数据
     */
    private int currentLayoutIndex = 0;

    // 在start方法中添加布局选择对话框
    private void showLayoutSelectionDialog() {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(0, 0, 1, 2, 3);
        dialog.setTitle("选择布局");
        dialog.setHeaderText("请选择华容道布局");
        dialog.setContentText("布局:");

        // 设置布局名称
        dialog.getItems().set(0, Integer.valueOf("标准布局"));
        dialog.getItems().set(1, Integer.valueOf("经典布局"));
        dialog.getItems().set(2, Integer.valueOf("对角线布局"));
        dialog.getItems().set(3, Integer.valueOf("复杂布局"));

        // 默认选择
        dialog.setSelectedItem(0);

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(layoutIndex -> {
            currentLayoutIndex = layoutIndex;
            initGameData();
        });
    }
    private void initGameData() {
        blocks = BoardLayouts.getLayout(currentLayoutIndex);
        moveCount = 0;
        gameWon = false;
        selectedBlock = null;
        directionButtons = new ArrayList<>();
    }

    /**
     * 创建主界面布局
     */
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f0f0;");

        // 创建顶部面板
        HBox topPanel = createTopPanel();
        root.setTop(topPanel);

        // 创建游戏棋盘
        gameBoard = createGameBoard();
        root.setCenter(gameBoard);

        // 创建控制面板
        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        return root;
    }

    /**
     * 创建顶部面板
     */
    private HBox createTopPanel() {
        HBox topPanel = new HBox(20);
        topPanel.setPadding(new javafx.geometry.Insets(10));
        topPanel.setStyle("-fx-background-color: #4a6fa5;");

        Label titleLabel = new Label("华容道游戏");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

        moveCountLabel = new Label("步数: 0");
        moveCountLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        Button restartButton = new Button("重新开始");
        restartButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white;");
        restartButton.setOnAction(e -> restartGame());

        topPanel.getChildren().addAll(titleLabel, moveCountLabel, restartButton);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);

        return topPanel;
    }

    /**
     * 创建游戏棋盘
     */
    private GridPane createGameBoard() {
        GridPane board = new GridPane();
        board.setHgap(2);
        board.setVgap(2);
        board.setPadding(new javafx.geometry.Insets(10));

        // 创建棋盘背景
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setFill(Color.LIGHTGRAY);
                cell.setStroke(Color.GRAY);
                cell.setStrokeWidth(1);

                // 设置出口位置
                if (row == EXIT_ROW && col == EXIT_COL) {
                    cell.setFill(Color.LIGHTBLUE);
                }

                board.add(cell, col, row);
            }
        }

        // 保存对棋盘的引用
        this.gameBoard = board;

        // 添加方块到棋盘
        drawBlocks();

        return board;
    }

    /**
     * 在棋盘上绘制所有方块
     */
    private void drawBlocks() {
        // 先清除所有方块
        gameBoard.getChildren().removeIf(node -> node instanceof StackPane);

        // 绘制所有方块
        for (Block block : blocks) {
            StackPane blockPane = createBlockPane(block);
            gameBoard.add(blockPane, block.getCol(), block.getRow(), block.getWidth(), block.getHeight());

            // 添加鼠标点击事件
            blockPane.setOnMouseClicked(e -> {
                if (!gameWon) {
                    selectBlock(block);
                }
            });
        }
    }

    /**
     * 创建方块面板
     */
    private StackPane createBlockPane(Block block) {
        StackPane pane = new StackPane();

        // 创建方块背景
        Rectangle rect = new Rectangle(
                block.getWidth() * CELL_SIZE - 4,
                block.getHeight() * CELL_SIZE - 4
        );
        rect.setFill(block.getColor());
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(2);

        // 如果是选中的方块，添加高亮效果
        if (block == selectedBlock) {
            rect.setEffect(new javafx.scene.effect.DropShadow(10, Color.YELLOW));
        }

        // 添加方块标签
        Label label = new Label(block.getName());
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        pane.getChildren().addAll(rect, label);
        return pane;
    }

    /**
     * 创建控制面板
     */
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(20);
        controlPanel.setPadding(new javafx.geometry.Insets(20));
        controlPanel.setStyle("-fx-background-color: #c8d8e4;");

        Label controlLabel = new Label("控制");
        controlLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 创建方向按钮
        Button upButton = new Button("上");
        Button downButton = new Button("下");
        Button leftButton = new Button("左");
        Button rightButton = new Button("右");

        // 设置按钮样式
        for (Button btn : new Button[]{upButton, downButton, leftButton, rightButton}) {
            btn.setPrefSize(80, 40);
            btn.setStyle("-fx-font-size: 16px; -fx-background-color: #52ab98; -fx-text-fill: white;");
            btn.setDisable(true); // 初始禁用，直到选择方块
            directionButtons.add(btn);
        }

        // 设置按钮事件
        upButton.setOnAction(e -> moveSelectedBlock(Direction.UP));
        downButton.setOnAction(e -> moveSelectedBlock(Direction.DOWN));
        leftButton.setOnAction(e -> moveSelectedBlock(Direction.LEFT));
        rightButton.setOnAction(e -> moveSelectedBlock(Direction.RIGHT));

        // 创建按钮网格
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(10);
        buttonGrid.setVgap(10);
        buttonGrid.add(upButton, 1, 0);
        buttonGrid.add(leftButton, 0, 1);
        buttonGrid.add(rightButton, 2, 1);
        buttonGrid.add(downButton, 1, 2);

        controlPanel.getChildren().addAll(controlLabel, buttonGrid);
        VBox.setVgrow(buttonGrid, Priority.ALWAYS);

        return controlPanel;
    }

    /**
     * 处理键盘事件
     */
    private void handleKeyPress(KeyEvent event) {
        if (selectedBlock == null || gameWon) {
            return;
        }

        switch (event.getCode()) {
            case UP:
                moveSelectedBlock(Direction.UP);
                break;
            case DOWN:
                moveSelectedBlock(Direction.DOWN);
                break;
            case LEFT:
                moveSelectedBlock(Direction.LEFT);
                break;
            case RIGHT:
                moveSelectedBlock(Direction.RIGHT);
                break;
        }
    }

    /**
     * 选择方块
     */
    private void selectBlock(Block block) {
        selectedBlock = block;
        drawBlocks(); // 重绘棋盘以显示选中效果

        // 启用方向按钮
        for (Button btn : directionButtons) {
            btn.setDisable(false);
        }
    }

    /**
     * 移动选中的方块
     */
    private void moveSelectedBlock(Direction direction) {
        if (selectedBlock == null || gameWon) {
            return;
        }

        int newRow = selectedBlock.getRow();
        int newCol = selectedBlock.getCol();

        // 根据方向计算新位置
        switch (direction) {
            case UP:
                newRow--;
                break;
            case DOWN:
                newRow++;
                break;
            case LEFT:
                newCol--;
                break;
            case RIGHT:
                newCol++;
                break;
        }

        // 检查移动是否合法
        if (isValidMove(selectedBlock, newRow, newCol)) {
            // 更新方块位置
            selectedBlock.setRow(newRow);
            selectedBlock.setCol(newCol);

            // 更新移动计数
            moveCount++;
            moveCountLabel.setText("步数: " + moveCount);

            // 重绘棋盘
            drawBlocks();

            // 检查是否获胜
            checkWinCondition();
        }
    }

    /**
     * 检查移动是否合法
     */
    private boolean isValidMove(Block block, int newRow, int newCol) {
        // 检查边界
        if (newRow < 0 || newRow + block.getHeight() > BOARD_ROWS ||
                newCol < 0 || newCol + block.getWidth() > BOARD_COLS) {
            return false;
        }

        // 检查碰撞
        for (Block other : blocks) {
            if (other == block) {
                continue;
            }

            if (isOverlapping(block, newRow, newCol, other)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查两个方块是否重叠
     */
    private boolean isOverlapping(Block block1, int row1, int col1, Block block2) {
        int row2 = block2.getRow();
        int col2 = block2.getCol();

        // 检查是否重叠
        return !(row1 + block1.getHeight() <= row2 ||
                row2 + block2.getHeight() <= row1 ||
                col1 + block1.getWidth() <= col2 ||
                col2 + block2.getWidth() <= col1);
    }

    /**
     * 检查胜利条件
     */
    private void checkWinCondition() {
        // 检查曹操方块是否到达出口位置
        Block caoCao = blocks.get(0); // 曹操方块是第一个添加的
        if (caoCao.getRow() == EXIT_ROW && caoCao.getCol() == EXIT_COL &&
                caoCao.getWidth() == 2 && caoCao.getHeight() == 2) {
            gameWon = true;
            showWinDialog();
        }
    }

    /**
     * 显示胜利对话框
     */
    private void showWinDialog() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("游戏胜利！");
        alert.setHeaderText("恭喜你完成了华容道！");
        alert.setContentText("你用了 " + moveCount + " 步完成了游戏。");

        // 添加重新开始按钮
        javafx.scene.control.ButtonType restartButton = new javafx.scene.control.ButtonType("重新开始");
        javafx.scene.control.ButtonType closeButton = new javafx.scene.control.ButtonType("关闭");
        alert.getButtonTypes().setAll(restartButton, closeButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == restartButton) {
                restartGame();
            }
        });
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        // 重新初始化游戏数据
        initGameData();

        // 重新创建游戏棋盘
        gameBoard.getChildren().clear();
        gameBoard = createGameBoard();
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(gameBoard);

        // 更新移动计数
        moveCountLabel.setText("步数: " + moveCount);

        // 禁用方向按钮
        for (Button btn : directionButtons) {
            btn.setDisable(true);
        }
    }

    /**
     * 方块类
     */

    public static class Block {
        private int row;
        private int col;
        private final int width;
        private final int height;
        private final Color color;
        private final String name;

        public Block(int row, int col, int width, int height, Color color, String name) {
            this.row = row;
            this.col = col;
            this.width = width;
            this.height = height;
            this.color = color;
            this.name = name;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Color getColor() {
            return color;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 方向枚举
     */
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
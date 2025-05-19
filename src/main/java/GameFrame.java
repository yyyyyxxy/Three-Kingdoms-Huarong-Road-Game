import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
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
public class GameFrame {
    private static final int BOARD_ROWS = 5;
    private static final int BOARD_COLS = 4;
    private static final int CELL_SIZE = 100;
    private static final int EXIT_ROW = 3;
    private static final int EXIT_COL = 1;

    private GridPane gameBoard;
    private List<Block> blocks;
    private Block selectedBlock;
    private int moveCount;
    private Label moveCountLabel;
    private Stage primaryStage;
    private boolean gameWon;
    private List<Button> directionButtons = new ArrayList<>();
    private int currentLayoutIndex = 0;

    public void show(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("华容道游戏");
        primaryStage.setResizable(false);

        // 先创建主界面
        BorderPane root = createMainLayout();

        // 然后初始化游戏数据
        initGameData();

        // 设置场景
        double sceneWidth = BOARD_COLS * CELL_SIZE + 240;
        double sceneHeight = BOARD_ROWS * CELL_SIZE + 150;
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        primaryStage.setScene(scene);

        // 设置最小宽度
        primaryStage.setMinWidth(sceneWidth);

        // 添加键盘事件处理
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        // 显示布局选择对话框
        showLayoutSelectionDialog();

        primaryStage.show();
    }

    private void showLayoutSelectionDialog() {
        List<String> layoutNames = BoardLayouts.getLayoutNames();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(layoutNames.get(0), layoutNames);
        dialog.setTitle("选择布局");
        dialog.setHeaderText("请选择华容道布局");
        dialog.setContentText("布局:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(layoutName -> {
            currentLayoutIndex = layoutNames.indexOf(layoutName);
            initGameData();
            drawBlocks();
        });
    }

    private void initGameData() {
        blocks = BoardLayouts.getLayout(currentLayoutIndex);
        moveCount = 0;
        gameWon = false;
        selectedBlock = null;
        if (gameBoard != null) {
            drawBlocks();
        }
    }

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

        BorderPane.setMargin(controlPanel, new Insets(0, 0, 0, 10));
        return root;
    }

    private HBox createTopPanel() {
        HBox topPanel = new HBox(20);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: #4a6fa5;");

        Label titleLabel = new Label("华容道游戏");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-font-weight: bold;");

        moveCountLabel = new Label("步数: 0");
        moveCountLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        Button restartButton = new Button("重新开始");
        restartButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white;");
        restartButton.setOnAction(e -> restartGame());

        Button layoutButton = new Button("更换布局");
        layoutButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white;");
        layoutButton.setOnAction(e -> showLayoutSelectionDialog());

        topPanel.getChildren().addAll(titleLabel, moveCountLabel, restartButton, layoutButton);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setAlignment(Pos.CENTER);

        return topPanel;
    }

    private GridPane createGameBoard() {
        GridPane board = new GridPane();
        board.setHgap(2);
        board.setVgap(2);
        board.setPadding(new Insets(10));

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

        return board;
    }

    private void drawBlocks() {
        // 清除所有方块
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

        // 选中效果
        if (block == selectedBlock) {
            rect.setEffect(new DropShadow(10, Color.YELLOW));
        }

        // 添加方块标签
        Label label = new Label(block.getName());
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        pane.getChildren().addAll(rect, label);
        return pane;
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(20);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setStyle("-fx-background-color: #c8d8e4;");
        controlPanel.setMinWidth(240);

        Label controlLabel = new Label("控制");
        controlLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 创建方向按钮
        Button upButton = new Button("上");
        Button downButton = new Button("下");
        Button leftButton = new Button("左");
        Button rightButton = new Button("右");

        directionButtons.clear();

        // 设置按钮样式
        for (Button btn : new Button[]{upButton, downButton, leftButton, rightButton}) {
            btn.setPrefSize(60, 35);
            btn.setStyle("-fx-font-size: 16px; -fx-background-color: #52ab98; -fx-text-fill: white;");
            btn.setDisable(true);
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 16px; -fx-background-color: #3b8c7a; -fx-text-fill: white;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 16px; -fx-background-color: #52ab98; -fx-text-fill: white;"));
            directionButtons.add(btn);
        }

        // 设置按钮事件
        upButton.setOnAction(e -> moveSelectedBlock(Direction.UP));
        downButton.setOnAction(e -> moveSelectedBlock(Direction.DOWN));
        leftButton.setOnAction(e -> moveSelectedBlock(Direction.LEFT));
        rightButton.setOnAction(e -> moveSelectedBlock(Direction.RIGHT));

        // 使用 HBox 代替 GridPane
        HBox buttonRow1 = new HBox(10, upButton);
        buttonRow1.setAlignment(Pos.CENTER);
        HBox buttonRow2 = new HBox(10, leftButton, rightButton);
        buttonRow2.setAlignment(Pos.CENTER);
        HBox buttonRow3 = new HBox(10, downButton);
        buttonRow3.setAlignment(Pos.CENTER);

        VBox buttonLayout = new VBox(10, buttonRow1, buttonRow2, buttonRow3);
        buttonLayout.setAlignment(Pos.CENTER);

        // 添加操作说明
        Label instructionLabel = new Label("操作说明:\n\n1. 点击方块选择\n2. 使用方向键移动\n3. 或使用按钮移动");
        instructionLabel.setStyle("-fx-font-size: 14px;");

        controlPanel.getChildren().addAll(controlLabel, buttonLayout, instructionLabel);
        return controlPanel;
    }

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

    private void selectBlock(Block block) {
        selectedBlock = block;
        drawBlocks();

        // 启用方向按钮
        for (Button btn : directionButtons) {
            btn.setDisable(false);
        }
    }

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

    private boolean isOverlapping(Block block1, int row1, int col1, Block block2) {
        int row2 = block2.getRow();
        int col2 = block2.getCol();

        // 检查是否重叠
        return !(row1 + block1.getHeight() <= row2 ||
                row2 + block2.getHeight() <= row1 ||
                col1 + block1.getWidth() <= col2 ||
                col2 + block2.getWidth() <= col1);
    }

    private void checkWinCondition() {
        // 检查曹操方块是否到达出口位置
        Block caoCao = blocks.get(0); // 曹操方块是第一个添加的
        if (caoCao.getRow() == EXIT_ROW && caoCao.getCol() == EXIT_COL &&
                caoCao.getWidth() == 2 && caoCao.getHeight() == 2) {
            gameWon = true;
            showWinDialog();
        }
    }

    private void showWinDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("游戏胜利！");
        alert.setHeaderText("恭喜你完成了华容道！");
        alert.setContentText("你用了 " + moveCount + " 步完成了游戏。");

        // 添加重新开始按钮
        ButtonType restartButton = new ButtonType("重新开始");
        ButtonType closeButton = new ButtonType("关闭");
        alert.getButtonTypes().setAll(restartButton, closeButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == restartButton) {
                restartGame();
            }
        });
    }

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
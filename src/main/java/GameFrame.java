import com.mongodb.client.model.Filters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.*;

import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private Label layoutNameLabel;
    private Stage primaryStage;
    private boolean gameWon;
    private List<Button> directionButtons = new ArrayList<>();
    private int currentLayoutIndex = 0;
    private String time = null;
    private boolean watchable = false; // 是否允许观战
    private String roomId = null;      // 观战房间号

    private Stack<List<Block>> historyStack = new Stack<>();
    private Label timerLabel;
    private long startTime;
    private javafx.animation.Timeline timer;
    private String userName;
    private volatile boolean pendingSync = false;
    private javafx.animation.Timeline syncTimeline = null;

    private boolean aiSolving = false;
    private boolean aiPaused = false;
    private List<List<Block>> aiSolution = null;
    private int aiStepIndex = 0;
    private Thread aiThread = null;
    private VBox controlPanelRef;// 保存控制栏引用

    private List<Button> topPanelButtons = new ArrayList<>();

    private List<Block> aiBeforeBlocks = null;
    private int aiBeforeMoveCount = 0;

    public GameFrame() {
        moveCountLabel = new Label("步数: 0");
        moveCountLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        layoutNameLabel = new Label();
        layoutNameLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: gold; -fx-font-weight: bold;");
        layoutNameLabel.setAlignment(Pos.CENTER);
        this.time = null;

        timerLabel = new Label("用时：00：00");
        timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        gameBoard = new GridPane();
        // 启动定时同步（每300ms检查一次）
        syncTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), e -> {
                    if (pendingSync && watchable && roomId != null) {
                        pendingSync = false;
                        uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
                    }
                })
        );
        syncTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        syncTimeline.play();
    }

    // 新增 parentStageToClose 参数
    public void show(Stage primaryStage, String userName, boolean showLayoutDialog, Stage parentStageToClose) {
        this.primaryStage = primaryStage;
        this.userName = userName;
        primaryStage.setTitle("华容道游戏");
        primaryStage.setResizable(true);

        BorderPane root = createMainLayout();
        initGameData();

        double sceneWidth = BOARD_COLS * CELL_SIZE + 700;
        double sceneHeight = BOARD_ROWS * CELL_SIZE + 170;
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        primaryStage.setScene(scene);

        primaryStage.setMinWidth(BOARD_COLS * CELL_SIZE + 700);
        primaryStage.setMinHeight(BOARD_ROWS * CELL_SIZE + 180);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        if (showLayoutDialog) {
            showLayoutSelectionDialog(parentStageToClose);
        }

        // 关闭窗口时清理观战房间
        primaryStage.setOnCloseRequest(e -> cleanOnlineRoom());

        primaryStage.show();
    }

    private void handleKeyPress(KeyEvent event) {
        if (aiSolving) return; // AI演示时禁用手动操作
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

    // 支持关闭上一个窗口
    private void showLayoutSelectionDialog(Stage parentStageToClose) {
        List<String> layoutNames = BoardLayouts.getLayoutNames();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(layoutNames.get(currentLayoutIndex), layoutNames);
        dialog.setTitle("选择布局");
        dialog.setHeaderText("请选择华容道布局");
        dialog.setContentText("布局:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(layoutName -> {
            currentLayoutIndex = layoutNames.indexOf(layoutName);
            if (layoutNameLabel != null) {
                layoutNameLabel.setText(getCurrentLayoutName());
            }
            initGameData();
            drawBlocks();
            // 关闭上一个窗口
            if (parentStageToClose != null) {
                parentStageToClose.close();
            }
        });
    }

    private void initGameData() {
        blocks = BoardLayouts.getLayout(currentLayoutIndex);
        moveCount = 0;
        gameWon = false;
        selectedBlock = null;
        historyStack.clear();
        startTimer();
        if (gameBoard != null) {
            drawBlocks();
        }
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f0f0;");

        HBox topPanel = createTopPanel();
        root.setTop(topPanel);

        StackPane gameBoardPane = createGameBoard();
        this.gameBoard = (GridPane) gameBoardPane.getChildren().get(1);
        root.setCenter(gameBoardPane);

        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        BorderPane.setMargin(controlPanel, new Insets(0, 0, 0, 10));
        return root;
    }

    private HBox createTopPanel() {
        HBox topPanel = new HBox(20);
        topPanel.setPadding(new Insets(10));
        topPanel.setStyle("-fx-background-color: linear-gradient(to right, #4a6fa5, #235390); -fx-background-radius: 12px;");

        Label titleLabel = new Label("华容道游戏");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: white; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        layoutNameLabel.setText(getCurrentLayoutName());
        layoutNameLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: gold; -fx-font-weight: bold;");
        layoutNameLabel.setAlignment(Pos.CENTER);

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button restartButton = new Button("重新开始");
        restartButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, #888, 2, 0, 0, 1);");
        restartButton.setOnAction(e -> restartGame());

        Button layoutButton = new Button("更换布局");
        layoutButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, #888, 2, 0, 0, 1);");
        layoutButton.setOnAction(e -> {
            cleanOnlineRoom(); // 这里加上
            showLayoutSelectionDialog(null);
        });
        Button aiSolveBtn = new Button(aiSolving ? "演示中" : "AI帮解");
        aiSolveBtn.setDisable(aiSolving);
        aiSolveBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #f7b731; -fx-text-fill: white; -fx-background-radius: 8px;");
        aiSolveBtn.setOnAction(e -> solveByAI());
        topPanel.getChildren().add(aiSolveBtn);

        Button undoButton = new Button("撤销");
        undoButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, #888, 2, 0, 0, 1);");
        undoButton.setOnAction(e -> undoMove());

        Button watchableBtn = new Button("可观战");
        watchableBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #52ab98; -fx-text-fill: white; -fx-background-radius: 8px;");
        watchableBtn.setOnAction(e -> {
            if (!watchable) {
                watchable = true;
                roomId = userName + "_" + System.currentTimeMillis();
                watchableBtn.setText("结束观战");
                // 立即上传一次初始状态
                uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
                showAlert("提示", "观战已开启", "现在其他用户可以观战你的对局。", Alert.AlertType.INFORMATION);
            } else {
                // 结束观战
                cleanOnlineRoom();
                watchableBtn.setText("可观战");
                showAlert("提示", "观战已关闭", "你的对局已不再同步到观战列表。", Alert.AlertType.INFORMATION);
            }
        });
        topPanel.getChildren().add(watchableBtn);

        Button saveButton = new Button("存档");
        saveButton.setStyle("-fx-font-size: 14px; -fx-background-color: #52ab98; -fx-text-fill: white; -fx-background-radius: 8px;");
        saveButton.setOnAction(e -> {
            if ("离线用户".equals(userName)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "离线游玩，不支持存档");
                alert.setHeaderText(null);
                alert.setTitle("提示");
                alert.showAndWait();
                return;
            }
            if (this.time != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "是否覆盖之前的历史记录？", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("检测到本局为历史存档继续");
                confirm.setTitle("覆盖提示");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    try {
                        MongoDBUtil db = new MongoDBUtil();
                        db.getCollection("game_history").deleteOne(
                                new org.bson.Document("username", userName)
                                        .append("layout", getCurrentLayoutName())
                                        .append("saveTime", this.time)
                        );
                        db.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    this.time = null;
                } else {
                    this.time = null;
                    return;
                }
            }

            uploadGameResult(userName, getCurrentLayoutName(), moveCount, getElapsedTimeString(), serializeHistoryStack());
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "存档成功！");
            alert.setHeaderText(null);
            alert.setTitle("提示");
            alert.showAndWait();
            primaryStage.close();
            cleanOnlineRoom();//关闭观战
            new MainInterfaceFrame().show(new Stage(), userName);
        });

        Button backButton = new Button("返回主界面");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #e07a5f; -fx-text-fill: white; -fx-background-radius: 8px;");
        backButton.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "是否存档当前进度？", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            confirm.setHeaderText("返回主界面");
            confirm.setTitle("提示");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent()) {
                if (result.get() == ButtonType.YES) {
                    if ("离线用户".equals(userName)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "离线游玩，不支持存档");
                        alert.setHeaderText(null);
                        alert.setTitle("提示");
                        alert.showAndWait();
                        primaryStage.close();
                        cleanOnlineRoom();
                        new MainInterfaceFrame().show(new Stage(), userName);
                        return;
                    }
                    // 如果是历史存档继续，弹出是否覆盖提示
                    if (this.time != null) {
                        Alert coverConfirm = new Alert(Alert.AlertType.CONFIRMATION, "是否覆盖上一次存档？", ButtonType.YES, ButtonType.NO);
                        coverConfirm.setHeaderText("检测到本局为历史存档继续");
                        coverConfirm.setTitle("覆盖提示");
                        Optional<ButtonType> coverResult = coverConfirm.showAndWait();
                        if (coverResult.isPresent() && coverResult.get() == ButtonType.YES) {
                            try {
                                MongoDBUtil db = new MongoDBUtil();
                                db.getCollection("game_history").deleteOne(
                                        new org.bson.Document("username", userName)
                                                .append("layout", getCurrentLayoutName())
                                                .append("saveTime", this.time)
                                );
                                db.close();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            this.time = null;
                        } else {
                            this.time = null;
                            // 不覆盖则直接返回主界面，不存档
                            primaryStage.close();
                            cleanOnlineRoom();
                            new MainInterfaceFrame().show(new Stage(), userName);
                            return;
                        }
                    }
                    // 存档
                    uploadGameResult(userName, getCurrentLayoutName(), moveCount, getElapsedTimeString(), serializeHistoryStack());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "存档成功！");
                    alert.setHeaderText(null);
                    alert.setTitle("提示");
                    alert.showAndWait();
                    primaryStage.close();
                    cleanOnlineRoom();
                    new MainInterfaceFrame().show(new Stage(), userName);
                } else if (result.get() == ButtonType.NO) {
                    primaryStage.close();
                    cleanOnlineRoom();
                    new MainInterfaceFrame().show(new Stage(), userName);
                }
                // 取消则什么都不做
            }
        });

        topPanelButtons.clear();
        topPanelButtons.add(undoButton);
        topPanelButtons.add(saveButton);
        topPanelButtons.add(backButton);

        topPanel.getChildren().addAll(
                titleLabel, spacer1, layoutNameLabel, spacer2, moveCountLabel, timerLabel,
                restartButton, layoutButton, undoButton, saveButton, backButton
        );

        return topPanel;
    }

    // 新增：获取当前布局名称
    private String getCurrentLayoutName() {
        List<String> layoutNames = BoardLayouts.getLayoutNames();
        if (currentLayoutIndex >= 0 && currentLayoutIndex < layoutNames.size()) {
            return layoutNames.get(currentLayoutIndex);
        }
        return "棋局：未知";
    }

    /**
     * 创建棋盘区域，只保留最外圈边框，下方2-3列开口，出口文字
     */
    private StackPane createGameBoard() {
        double boardWidth = BOARD_COLS * CELL_SIZE;
        double boardHeight = BOARD_ROWS * CELL_SIZE;
        double lineWidth = 6;
        double offset = lineWidth / 2;

        StackPane boardPane = new StackPane();
        boardPane.setMinSize(boardWidth + lineWidth, boardHeight + 40 + lineWidth);
        boardPane.setMaxSize(boardWidth + lineWidth, boardHeight + 40 + lineWidth);

        Canvas borderCanvas = new Canvas(boardWidth + lineWidth, boardHeight + 40 + lineWidth);
        GraphicsContext gc = borderCanvas.getGraphicsContext2D();

        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(lineWidth);

        // 边框
        gc.strokeLine(offset, offset, offset, boardHeight + offset);
        gc.strokeLine(boardWidth + offset, offset, boardWidth + offset, boardHeight + offset);
        gc.strokeLine(offset, offset, boardWidth + offset, offset);
        double y = boardHeight + offset;
        double x1 = offset;
        double x2 = 1 * CELL_SIZE + offset;
        double x3 = 2 * CELL_SIZE + offset;
        double x4 = 3 * CELL_SIZE + offset;
        double x5 = 4 * CELL_SIZE + offset;
        gc.strokeLine(x1, y, x2, y);
        gc.strokeLine(x4, y, x5, y);

        // “出口”文字（用Text测量宽度居中）
        String exitStr = "出口";
        gc.setFill(Color.web("#d7263d"));
        gc.setFont(Font.font("微软雅黑", 22));
        Text text = new Text(exitStr);
        text.setFont(gc.getFont());
        double exitTextWidth = text.getLayoutBounds().getWidth();
        double exitCenter = (x2 + x3) / 2;
        gc.fillText(exitStr, exitCenter - exitTextWidth / 2, y + 30);

        // 棋盘格子
        GridPane board = new GridPane();
        board.setHgap(0);
        board.setVgap(0);
        board.setPadding(Insets.EMPTY);
        board.setStyle("-fx-background-color: transparent;");
        board.setTranslateX(offset);
        board.setTranslateY(offset);

        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                StackPane cellPane = new StackPane();
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setArcWidth(18);
                cell.setArcHeight(18);
                cell.setFill(Color.LIGHTGRAY);
                cell.setStroke(null);
                cellPane.getChildren().add(cell);
                board.add(cellPane, col, row);
            }
        }

        boardPane.getChildren().addAll(borderCanvas, board);
        return boardPane;
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(20);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setStyle("-fx-background-color: #c8d8e4; -fx-background-radius: 12px;");
        controlPanel.setMinWidth(240);

        Label controlLabel = new Label("控制");
        controlLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        controlPanel.getChildren().add(controlLabel);

        if (!aiSolving) {
            // 普通控制栏
            Button upButton = new Button("上");
            Button downButton = new Button("下");
            Button leftButton = new Button("左");
            Button rightButton = new Button("右");

            directionButtons.clear();
            for (Button btn : new Button[]{upButton, downButton, leftButton, rightButton}) {
                btn.setPrefSize(60, 35);
                btn.setStyle("-fx-font-size: 16px; -fx-background-color: #52ab98; -fx-text-fill: white; -fx-background-radius: 8px;");
                btn.setDisable(true);
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 16px; -fx-background-color: #3b8c7a; -fx-text-fill: white; -fx-background-radius: 8px;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 16px; -fx-background-color: #52ab98; -fx-text-fill: white; -fx-background-radius: 8px;"));
                directionButtons.add(btn);
            }
            upButton.setOnAction(e -> moveSelectedBlock(Direction.UP));
            downButton.setOnAction(e -> moveSelectedBlock(Direction.DOWN));
            leftButton.setOnAction(e -> moveSelectedBlock(Direction.LEFT));
            rightButton.setOnAction(e -> moveSelectedBlock(Direction.RIGHT));

            HBox buttonRow1 = new HBox(10, upButton);
            buttonRow1.setAlignment(Pos.CENTER);
            HBox buttonRow2 = new HBox(10, leftButton, rightButton);
            buttonRow2.setAlignment(Pos.CENTER);
            HBox buttonRow3 = new HBox(10, downButton);
            buttonRow3.setAlignment(Pos.CENTER);

            VBox buttonLayout = new VBox(10, buttonRow1, buttonRow2, buttonRow3);
            buttonLayout.setAlignment(Pos.CENTER);

            Label instructionLabel = new Label("操作说明:\n\n1. 点击方块选择\n2. 使用方向键移动\n3. 或使用按钮移动");
            instructionLabel.setStyle("-fx-font-size: 14px;");

            controlPanel.getChildren().addAll(buttonLayout, instructionLabel);
        } else {
            // AI演示控制栏
            Button pauseBtn = new Button(aiPaused ? "继续帮解" : "暂停帮解");
            Button stopBtn = new Button("结束帮解");
            Button prevBtn = new Button("上一步");
            Button nextBtn = new Button("下一步");

            pauseBtn.setPrefWidth(90);
            stopBtn.setPrefWidth(90);
            prevBtn.setPrefWidth(90);
            nextBtn.setPrefWidth(90);

            pauseBtn.setOnAction(e -> {
                aiPaused = !aiPaused;
                refreshControlPanel();
            });
            stopBtn.setOnAction(e -> stopAISolve());
            prevBtn.setOnAction(e -> aiStepMove(-1));
            nextBtn.setOnAction(e -> aiStepMove(1));

            HBox btnRow1 = new HBox(10, pauseBtn, stopBtn);
            btnRow1.setAlignment(Pos.CENTER);
            HBox btnRow2 = new HBox(10, prevBtn, nextBtn);
            btnRow2.setAlignment(Pos.CENTER);

            controlPanel.getChildren().addAll(btnRow1, btnRow2);
        }

        controlPanelRef = controlPanel;
        return controlPanel;
    }

    private void refreshControlPanel() {
        BorderPane root = (BorderPane) primaryStage.getScene().getRoot();
        VBox newPanel = createControlPanel();
        root.setRight(newPanel);
    }

    private void selectBlock(Block block) {
        selectedBlock = block;
        drawBlocks();

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

        switch (direction) {
            case UP:    newRow--; break;
            case DOWN:  newRow++; break;
            case LEFT:  newCol--; break;
            case RIGHT: newCol++; break;
        }

        if (isValidMove(selectedBlock, newRow, newCol)) {
            historyStack.push(deepCopyBlocks(blocks));
            selectedBlock.setRow(newRow);
            selectedBlock.setCol(newCol);
            moveCount++;
            moveCountLabel.setText("步数: " + moveCount);
            drawBlocks();
            checkWinCondition();
            // 仅在观战模式下同步
            if (watchable && roomId != null) {
                uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
            }
        }
    }

    private void undoMove() {
        if (!historyStack.isEmpty()) {
            blocks = deepCopyBlocks(historyStack.pop());
            moveCount = Math.max(0, moveCount - 1);
            moveCountLabel.setText("步数: " + moveCount);
            selectedBlock = null;
            drawBlocks();
            for (Button btn : directionButtons) {
                btn.setDisable(true);
            }
            // 仅在观战模式下同步
            if (watchable && roomId != null) {
                uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
            }
        }
    }

    private List<Block> deepCopyBlocks(List<Block> original) {
        List<Block> copy = new ArrayList<>();
        for (Block b : original) {
            copy.add(new Block(b.getRow(), b.getCol(), b.getWidth(), b.getHeight(), b.getColor(), b.getName()));
        }
        return copy;
    }

    private boolean isValidMove(Block block, int newRow, int newCol) {
        if (newRow < 0 || newRow + block.getHeight() > BOARD_ROWS ||
                newCol < 0 || newCol + block.getWidth() > BOARD_COLS) {
            return false;
        }

        for (Block other : blocks) {
            if (other == block) continue;
            if (isOverlapping(block, newRow, newCol, other)) {
                return false;
            }
        }
        return true;
    }

    private boolean isOverlapping(Block block1, int row1, int col1, Block block2) {
        int row2 = block2.getRow();
        int col2 = block2.getCol();
        return !(row1 + block1.getHeight() <= row2 ||
                row2 + block2.getHeight() <= row1 ||
                col1 + block1.getWidth() <= col2 ||
                col2 + block2.getWidth() <= col1);
    }

    private void checkWinCondition() {
        Block caoCao = blocks.get(0);
        if (caoCao.getRow() == EXIT_ROW && caoCao.getCol() == EXIT_COL &&
                caoCao.getWidth() == 2 && caoCao.getHeight() == 2) {
            gameWon = true;
            showWinDialog();
        }
    }

    private void showWinDialog() {
        if (timer != null) timer.stop();
        String elapsedTime = getElapsedTimeString();
        String layoutName = getCurrentLayoutName();

        // 上传数据到云端
        if(!Objects.equals(userName, "离线用户")) {
            uploadGameResult(userName, layoutName, moveCount, elapsedTime, serializeHistoryStack());
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("游戏胜利！");
        alert.setHeaderText("恭喜你完成了华容道！");
        alert.setContentText("你用了 " + moveCount + " 步完成了游戏。\n总用时：" + elapsedTime);

        ButtonType restartButton = new ButtonType("重新开始");
        ButtonType closeButton = new ButtonType("关闭");
        alert.getButtonTypes().setAll(restartButton, closeButton);

        alert.showAndWait().ifPresent(response -> {
            // 观战结束时清理房间
            cleanOnlineRoom();
            if (response == restartButton) {
                restartGame();
            } else if (response == closeButton) {
                // 关闭当前窗口并返回主界面
                if (primaryStage != null) {
                    primaryStage.close();
                }
                new MainInterfaceFrame().show(new Stage(), userName);
            }
        });
    }

    private void restartGame() {
        initGameData();
        gameBoard.getChildren().clear();
        StackPane gameBoardPane = createGameBoard();
        this.gameBoard = (GridPane) gameBoardPane.getChildren().get(1);
        ((BorderPane) primaryStage.getScene().getRoot()).setCenter(gameBoardPane);
        drawBlocks();
        moveCountLabel.setText("步数: " + moveCount);
        for (Button btn : directionButtons) {
            btn.setDisable(true);
        }
        startTimer();
        // 仅在观战模式下同步
        if (watchable && roomId != null) {
            uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
        }
    }

    /**
     * 从存档恢复游戏状态
     * @param savedBlocks 存档的方块列表
     * @param savedMoveCount 存档的步数
     * @param savedElapsedTime 存档的用时
     * @param savedHistoryStack 存档的历史记录
     */
    public void restoreGame(List<Block> savedBlocks, int savedMoveCount, String savedElapsedTime, List<String> savedHistoryStack,String time) {
        // 恢复方块状态
        if (savedBlocks != null && !savedBlocks.isEmpty()) {
            this.blocks = savedBlocks;
        } else {
            // 如果存档为空，恢复默认布局
            this.blocks = BoardLayouts.getLayout(currentLayoutIndex);

        }

        // 恢复步数
        this.moveCount = savedMoveCount;
        if (moveCountLabel != null) {
            moveCountLabel.setText("步数: " + savedMoveCount);
        }

        // 恢复用时
        restoreTimer(savedElapsedTime);

        // 恢复历史记录
        restoreHistoryStack(savedHistoryStack);

        // 重新绘制方块
        if (gameBoard != null) {
            drawBlocks();
        }

        // 清除选中方块
        selectedBlock = null;
        for (Button btn : directionButtons) {
            btn.setDisable(true);
        }
        //设置记录时间
        this.time = time;
    }

    private void restoreTimer(String savedElapsedTime) {
        String[] parts = savedElapsedTime.split(":");
        if (parts.length == 2) {
            long minutes = Long.parseLong(parts[0]);
            long seconds = Long.parseLong(parts[1]);
            long elapsed = minutes * 60 + seconds;
            startTime = System.currentTimeMillis() - elapsed * 1000;
            updateTimer(); // 立即更新一次显示
            timer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateTimer()));
            timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timer.play();
        }
    }

    private void restoreHistoryStack(List<String> savedHistoryStack) {
        historyStack.clear();
        for (String serializedState : savedHistoryStack) {
            List<Block> state = new ArrayList<>();
            String[] blockStrings = serializedState.split(";");
            for (String blockString : blockStrings) {
                if (!blockString.isEmpty()) {
                    String[] parts = blockString.split("[()]");
                    String name = parts[0];
                    String[] coords = parts[1].split(",");
                    int row = Integer.parseInt(coords[0]);
                    int col = Integer.parseInt(coords[1]);

                    // 根据名字找到对应的原始Block，然后创建新的Block对象
                    Block originalBlock = findBlockByName(name);
                    if (originalBlock != null) {
                        state.add(new Block(row, col, originalBlock.getWidth(), originalBlock.getHeight(), originalBlock.getColor(), originalBlock.getName()));
                    }
                }
            }
            historyStack.push(state);
        }
    }

    private Block findBlockByName(String name) {
        for (Block block : BoardLayouts.getLayout(currentLayoutIndex)) {
            if (block.getName().equals(name)) {
                return block;
            }
        }
        return null;
    }

    private void uploadGameResult(String username, String layoutName, int moveCount, String elapsedTime, List<String> historyStack) {
        try {
            MongoDBUtil db = new MongoDBUtil();
            // 新增：格式化当前时间
            String saveTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Document record = new Document("username", username)
                    .append("layout", layoutName)
                    .append("blocks", blocksToDocuments(blocks))
                    .append("moveCount", moveCount)
                    .append("elapsedTime", elapsedTime)
                    .append("historyStack", historyStack)
                    .append("saveTime", saveTime) // 新增：存储时间字符串
                    .append("timestamp", System.currentTimeMillis())
                    .append("gameWon",gameWon);
            db.getCollection("game_history").insertOne(record);
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTimer() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;
        timerLabel.setText(String.format("用时: %02d:%02d", minutes, seconds));
    }

    // 获取当前用时字符串
    private String getElapsedTimeString() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private List<String> serializeHistoryStack() {
        // 先存储当前状态到历史栈（确保序列化包含最新状态）
        historyStack.push(deepCopyBlocks(blocks));  // 新增此行

        List<String> serialized = new ArrayList<>();
        for (List<Block> state : historyStack) {
            StringBuilder sb = new StringBuilder();
            for (Block b : state) {
                sb.append(String.format("%s(%d,%d)", b.getName(), b.getRow(), b.getCol())).append(";");
            }
            serialized.add(sb.toString());
        }
        return serialized;
    }

    //新增
    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        startTime = System.currentTimeMillis();
        timerLabel.setText("用时: 00:00");
        timer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timer.play();
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
    //设置索引
    public void setCurrentLayoutIndex(int index) {
        this.currentLayoutIndex = index;
    }
    public static List<Document> blocksToDocuments(List<Block> blocks) {
        List<Document> documents = new ArrayList<>();
        for (Block block : blocks) {
            Document doc = new Document()
                    .append("name", block.getName())
                    .append("row", block.getRow())
                    .append("col", block.getCol())
                    .append("width", block.getWidth())
                    .append("height", block.getHeight())
                    .append("color", block.getColor().toString()); // Color转为字符串
            documents.add(doc);
        }
        return documents;
    }
    private void drawBlocks() {
        // 仅移除带有 "block" 样式的节点，避免移除背景格子
        gameBoard.getChildren().removeIf(node -> node.getStyleClass().contains("block"));

        // 绘制所有方块
        for (Block block : blocks) {
            StackPane blockPane = createBlockPane(block);
            blockPane.getStyleClass().add("block"); // 标记当前节点为棋子
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

        Rectangle rect = new Rectangle(
                block.getWidth() * CELL_SIZE - 4,
                block.getHeight() * CELL_SIZE - 4
        );
        rect.setFill(block.getColor());
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(2);
        rect.setArcWidth(18);
        rect.setArcHeight(18);

        if (block == selectedBlock) {
            rect.setEffect(new DropShadow(10, Color.YELLOW));
        }

        Label label = new Label(block.getName());
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        pane.getChildren().addAll(rect, label);
        return pane;
    }
    // 在GameFrame每次棋盘状态变化后调用
    private void uploadOnlineGameState(String roomId, String username, List<GameFrame.Block> blocks, int moveCount, String elapsedTime) {
        new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                List<String> friends = getFriendsOfUser(userName); // 你需要实现这个方法

                Document state = new Document("roomId", roomId)
                        .append("host", username)
                        .append("blocks", blocksToDocuments(blocks))
                        .append("moveCount", moveCount)
                        .append("elapsedTime", elapsedTime)
                        .append("timestamp", System.currentTimeMillis())
                        .append("friends", friends); // 新增
                db.getCollection("online_games").updateOne(
                        Filters.eq("roomId", roomId),
                        new Document("$set", state),
                        new com.mongodb.client.model.UpdateOptions().upsert(true)
                );
                db.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<String> getFriendsOfUser(String username) {
        List<String> friends = new ArrayList<>();
        try {
            MongoDBUtil db = new MongoDBUtil();
            Document userDoc = db.getUserByUsername(username);
            if (userDoc != null && userDoc.get("friends") instanceof List) {
                friends = new ArrayList<>((List<String>) userDoc.get("friends"));
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friends;
    }

    // 观战结束时清理房间
    private void cleanOnlineRoom() {
        if (watchable && roomId != null) {
            try {
                MongoDBUtil db = new MongoDBUtil();
                db.getCollection("online_games").deleteOne(Filters.eq("roomId", roomId));
                db.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            watchable = false;
            roomId = null;
        }
        if (syncTimeline != null) {
            syncTimeline.stop();
        }
    }

    private void solveByAI() {
        if (aiSolving) return;
        aiBeforeBlocks = deepCopyBlocks(blocks);
        aiBeforeMoveCount = moveCount;

        aiSolving = true;
        setAISolvingStatus(true); // ← AI帮解开始时上传状态
        aiPaused = false;
        aiSolution = AIHuarongSolver.solve(deepCopyBlocks(blocks), currentLayoutIndex);
        aiStepIndex = 0;
        refreshControlPanel();
        // 禁用存档、返回主界面、撤销按钮
        setTopPanelButtonsEnabled(false);

        if (aiSolution == null || aiSolution.size() <= 1) {
            aiSolving = false;
            javafx.application.Platform.runLater(() -> {
                showAlert("提示", "AI帮解", "未找到解法或已是终局", Alert.AlertType.INFORMATION);
                refreshControlPanel();
                setTopPanelButtonsEnabled(true);
            });
            return;
        }

        aiThread = new Thread(() -> {
            while (aiSolving && aiStepIndex < aiSolution.size() - 1) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ignored) {}
                if (!aiSolving) break;
                if (aiPaused) continue;
                aiStepMove(1);
            }
            if (aiSolving && aiStepIndex == aiSolution.size() - 1) {
                javafx.application.Platform.runLater(() -> showAlert("AI帮解", null, "AI已完成演示！", Alert.AlertType.INFORMATION));
            }
        });
        aiThread.start();
    }

    private void aiStepMove(int delta) {
        int newIndex = aiStepIndex + delta;
        if (newIndex < 0 || newIndex >= aiSolution.size()) return;
        aiStepIndex = newIndex;
        List<Block> state = aiSolution.get(aiStepIndex);
        javafx.application.Platform.runLater(() -> {
            blocks = deepCopyBlocks(state);
            drawBlocks();
            moveCountLabel.setText("步数: " + aiStepIndex);
        });
    }

    private void stopAISolve() {
        setAISolvingStatus(false); // ← AI帮解结束时上传状态
        aiSolving = false;
        aiPaused = false;
        aiSolution = null;
        aiStepIndex = 0;
        if (aiThread != null) aiThread.interrupt();
        // 恢复AI前的棋盘和步数
        if (aiBeforeBlocks != null) {
            blocks = deepCopyBlocks(aiBeforeBlocks);
            moveCount = aiBeforeMoveCount;
            moveCountLabel.setText("步数: " + moveCount);
            drawBlocks();
        }
        refreshControlPanel();
        setTopPanelButtonsEnabled(true);
    }

    private void setTopPanelButtonsEnabled(boolean enabled) {
        for (Button btn : topPanelButtons) {
            btn.setDisable(!enabled);
        }
    }

    private void setAISolvingStatus(boolean solving) {
        if (watchable && roomId != null) {
            new Thread(() -> {
                try {
                    MongoDBUtil db = new MongoDBUtil();
                    db.getCollection("online_games").updateOne(
                            Filters.eq("roomId", roomId),
                            new Document("$set", new Document("aiSolving", solving))
                    );
                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 通用弹窗
    private void showAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
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

    private boolean isTimed = false;
    private int timeLimitSeconds = 300; // 5分钟
    private int remainSeconds = 300;

    // 新增：保存主界面Stage
    private Stage parentStageToClose = null;

    // 在类的开头添加音乐管理器引用
    private MusicManager musicManager = MusicManager.getInstance();

    // 修改：show 方法 - 添加音乐播放
    public void show(Stage primaryStage, String userName, boolean showLayoutDialog, Stage parentStageToClose, boolean isTimed) {
        this.primaryStage = primaryStage;
        this.userName = userName;
        this.isTimed = isTimed;
        this.parentStageToClose = parentStageToClose;
        primaryStage.setTitle("华容道游戏");
        primaryStage.setResizable(true);

        BorderPane root = createMainLayout();
        initGameData();

        Scene scene = new Scene(root);
        loadCSS(scene);
        primaryStage.setScene(scene);

        if (parentStageToClose != null) {
            primaryStage.setX(parentStageToClose.getX());
            primaryStage.setY(parentStageToClose.getY());
            primaryStage.setWidth(parentStageToClose.getWidth());
            primaryStage.setHeight(parentStageToClose.getHeight());
        } else {
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            primaryStage.centerOnScreen();
        }

        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        if (showLayoutDialog) {
            showLayoutSelectionDialog(parentStageToClose);
        }

        primaryStage.setOnCloseRequest(e -> {
            cleanOnlineRoom();
            musicManager.stopMusic(); // 关闭时停止音乐
        });

        primaryStage.show();

        // 新增：根据模式播放对应音乐
        if (isTimed) {
            musicManager.playMusic(MusicManager.TIMED_MODE);
        } else {
            musicManager.playMusic(MusicManager.GAME_PLAY);
        }
    }

    // 修改：createTopPanel 方法 - 添加音乐控制按钮
    // 修改：createTopPanel 方法 - 所有按钮居中对称排列
    private HBox createTopPanel() {
        HBox topPanel = new HBox();
        topPanel.setPadding(new Insets(12, 20, 12, 20));
        topPanel.getStyleClass().add("game-top-panel");
        topPanel.setAlignment(Pos.CENTER); // 修改：设置为居中对齐

        // 按钮容器 - 居中排列所有按钮
        HBox buttonRow = new HBox(8); // 设置按钮间距
        buttonRow.setAlignment(Pos.CENTER);

        // 创建所有按钮
        Button restartButton = createTopButton("🔄", "重开", "game-button-restart");
        restartButton.setOnAction(e -> restartGame());

        Button layoutButton = createTopButton("🎯", "更换布局", "game-button-layout");
        layoutButton.setOnAction(e -> {
            cleanOnlineRoom();
            showLayoutSelectionDialog(null);
        });

        Button aiSolveBtn = createTopButton("🤖", aiSolving ? "演示中" : "AI帮解", "game-button-ai");
        aiSolveBtn.setDisable(aiSolving);
        aiSolveBtn.setOnAction(e -> solveByAI());

        Button undoButton = createTopButton("↶", "撤销", "game-button-undo");
        undoButton.setOnAction(e -> undoMove());

        Button watchableBtn = createTopButton("👁", "可观战", "game-button-watch");
        watchableBtn.setOnAction(e -> {
            if (!watchable) {
                watchable = true;
                roomId = userName + "_" + System.currentTimeMillis();
                watchableBtn.setText("👁 结束观战");
                uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
                showAlert("提示", "观战已开启", "现在其他用户可以观战你的对局。", Alert.AlertType.INFORMATION);
            } else {
                cleanOnlineRoom();
                watchableBtn.setText("👁 可观战");
                showAlert("提示", "观战已关闭", "你的对局已不再同步到观战列表。", Alert.AlertType.INFORMATION);
            }
        });

        // 音乐控制按钮
        Button musicButton = createMusicControlButton();

        // 音量控制按钮
        Button volumeButton = createVolumeControlButton();

        Button saveButton = createTopButton("💾", "存档", "game-button-save");
        saveButton.setOnAction(e -> {
            // 存档逻辑保持不变...
            if (isTimed) {
                Alert failAlert = new Alert(Alert.AlertType.CONFIRMATION, "限时模式下存档将视为挑战失败，是否继续存档？", ButtonType.YES, ButtonType.NO);
                failAlert.setHeaderText("限时模式存档提示");
                failAlert.setTitle("提示");
                Optional<ButtonType> failResult = failAlert.showAndWait();
                if (failResult.isEmpty() || failResult.get() == ButtonType.NO) {
                    return;
                }
            }
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
                    uploadGameResult(userName, getCurrentLayoutName(), moveCount, getElapsedTimeString(), serializeHistoryStack());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "存档成功！");
                    alert.setHeaderText(null);
                    alert.setTitle("提示");
                    alert.showAndWait();
                    cleanOnlineRoom();
                    musicManager.stopMusic(); // 停止音乐
                    if (parentStageToClose != null) {
                        parentStageToClose.show();
                        musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                    }
                    primaryStage.close();
                    return;
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
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止音乐
            if (parentStageToClose != null) {
                parentStageToClose.show();
                musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
            }
            primaryStage.close();
        });

        Button backButton = createTopButton("🏠", "返回主界面", "game-button-back");
        backButton.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "是否存档当前进度？", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            confirm.setHeaderText("返回主界面");
            confirm.setTitle("提示");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent()) {
                if (result.get() == ButtonType.YES) {
                    // 存档逻辑...
                    if (isTimed) {
                        Alert failAlert = new Alert(Alert.AlertType.CONFIRMATION, "限时模式下存档将视为挑战失败，是否继续存档？", ButtonType.YES, ButtonType.NO);
                        failAlert.setHeaderText("限时模式存档提示");
                        failAlert.setTitle("提示");
                        Optional<ButtonType> failResult = failAlert.showAndWait();
                        if (failResult.isEmpty() || failResult.get() == ButtonType.NO) {
                            return;
                        }
                    }
                    if ("离线用户".equals(userName)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "离线游玩，不支持存档");
                        alert.setHeaderText(null);
                        alert.setTitle("提示");
                        alert.showAndWait();
                        cleanOnlineRoom();
                        musicManager.stopMusic(); // 停止音乐
                        if (parentStageToClose != null) {
                            parentStageToClose.show();
                            musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                        }
                        primaryStage.close();
                        return;
                    }
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
                            uploadGameResult(userName, getCurrentLayoutName(), moveCount, getElapsedTimeString(), serializeHistoryStack());
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "存档成功！");
                            alert.setHeaderText(null);
                            alert.setTitle("提示");
                            alert.showAndWait();
                            cleanOnlineRoom();
                            musicManager.stopMusic(); // 停止音乐
                            if (parentStageToClose != null) {
                                parentStageToClose.show();
                                musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                            }
                            primaryStage.close();
                            return;
                        } else {
                            this.time = null;
                            cleanOnlineRoom();
                            musicManager.stopMusic(); // 停止音乐
                            if (parentStageToClose != null) {
                                parentStageToClose.show();
                                musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                            }
                            primaryStage.close();
                            return;
                        }
                    }
                    uploadGameResult(userName, getCurrentLayoutName(), moveCount, getElapsedTimeString(), serializeHistoryStack());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "存档成功！");
                    alert.setHeaderText(null);
                    alert.setTitle("提示");
                    alert.showAndWait();
                    cleanOnlineRoom();
                    musicManager.stopMusic(); // 停止音乐
                    if (parentStageToClose != null) {
                        parentStageToClose.show();
                        musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                    }
                    primaryStage.close();
                } else if (result.get() == ButtonType.NO) {
                    cleanOnlineRoom();
                    musicManager.stopMusic(); // 停止音乐
                    if (parentStageToClose != null) {
                        parentStageToClose.show();
                        musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
                    }
                    primaryStage.close();
                }
            }
        });

        // 将所有按钮按逻辑顺序添加到按钮行中
        buttonRow.getChildren().addAll(
                restartButton,      // 重新开始
                layoutButton,       // 更换布局
                aiSolveBtn,        // AI帮解
                undoButton,        // 撤销
                watchableBtn,      // 观战
                musicButton,       // 音乐控制
                volumeButton,      // 音量控制
                saveButton,        // 存档
                backButton         // 返回主界面
        );

        // 更新顶部按钮引用
        topPanelButtons.clear();
        topPanelButtons.add(undoButton);
        topPanelButtons.add(saveButton);
        topPanelButtons.add(backButton);

        // 修改：直接将按钮行添加到顶部面板，实现居中对称布局
        topPanel.getChildren().add(buttonRow);

        return topPanel;
    }

    // 同时调整按钮尺寸以确保一排能放下所有按钮
    private Button createTopButton(String icon, String text, String styleClass) {
        Button button = new Button(icon + " " + text);
        button.setPrefWidth(85);  // 适当减小宽度以适应更多按钮
        button.setPrefHeight(32);
        button.setFont(Font.font("微软雅黑", 10)); // 适当减小字体
        button.getStyleClass().add("game-top-button");
        button.getStyleClass().add(styleClass);
        return button;
    }

    // 同时调整音乐控制按钮的尺寸
    private Button createMusicControlButton() {
        Button musicButton = new Button(musicManager.isMusicEnabled() ? "🔊 音乐" : "🔇 音乐");
        musicButton.setPrefWidth(75);  // 减小宽度
        musicButton.setPrefHeight(32);
        musicButton.setFont(Font.font("微软雅黑", 10)); // 减小字体
        musicButton.getStyleClass().add("game-top-button");
        musicButton.getStyleClass().add("game-button-music");

        musicButton.setOnAction(e -> {
            musicManager.toggleMusic();
            musicButton.setText(musicManager.isMusicEnabled() ? "🔊 音乐" : "🔇 音乐");

            // 如果重新开启音乐，播放当前界面对应的音乐
            if (musicManager.isMusicEnabled()) {
                if (isTimed) {
                    musicManager.playMusic(MusicManager.TIMED_MODE);
                } else {
                    musicManager.playMusic(MusicManager.GAME_PLAY);
                }
            }
        });

        return musicButton;
    }

    // 同时调整音量控制按钮的尺寸
    private Button createVolumeControlButton() {
        Button volumeButton = new Button("🔉 音量");
        volumeButton.setPrefWidth(75);  // 减小宽度
        volumeButton.setPrefHeight(32);
        volumeButton.setFont(Font.font("微软雅黑", 10)); // 减小字体
        volumeButton.getStyleClass().add("game-top-button");
        volumeButton.getStyleClass().add("game-button-volume");

        volumeButton.setOnAction(e -> showVolumeControl());

        return volumeButton;
    }



    // 新增：显示音量控制对话框
    private void showVolumeControl() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("音量控制");
        dialog.setHeaderText("调整背景音乐音量");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label volumeLabel = new Label("当前音量: " + Math.round(musicManager.getVolume() * 100) + "%");
        volumeLabel.setFont(Font.font("微软雅黑", 14));

        Slider volumeSlider = new Slider(0, 1, musicManager.getVolume());
        volumeSlider.setPrefWidth(200);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(0.25);
        volumeSlider.setMinorTickCount(4);

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            musicManager.setVolume(newVal.doubleValue());
            volumeLabel.setText("当前音量: " + Math.round(newVal.doubleValue() * 100) + "%");
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button muteButton = new Button(musicManager.isMusicEnabled() ? "🔇 静音" : "🔊 取消静音");
        muteButton.setOnAction(e -> {
            musicManager.toggleMusic();
            muteButton.setText(musicManager.isMusicEnabled() ? "🔇 静音" : "🔊 取消静音");
            volumeSlider.setDisable(!musicManager.isMusicEnabled());
        });

        buttonBox.getChildren().add(muteButton);

        content.getChildren().addAll(volumeLabel, volumeSlider, buttonBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // 修改：showWinDialog 方法 - 添加胜利音乐
    private void showWinDialog() {
        if (timer != null) timer.stop();

        // 新增：播放胜利音乐
        musicManager.playMusic(MusicManager.VICTORY);

        String elapsedTime = getElapsedTimeString();
        String layoutName = getCurrentLayoutName();

        // 异步上传数据到云端
        if (!Objects.equals(userName, "离线用户")) {
            new Thread(() -> uploadGameResult(userName, layoutName, moveCount, elapsedTime, serializeHistoryStack())).start();
        }

        // 限时模式且在规定时间内通关，奖励金币（异步）
        final boolean[] reward = {false};
        Thread rewardThread = null;
        if (isTimed && remainSeconds > 0 && !"离线用户".equals(userName)) {
            rewardThread = new Thread(() -> {
                try {
                    MongoDBUtil db = new MongoDBUtil();
                    db.getCollection("users").updateOne(
                            new Document("username", userName),
                            new Document("$inc", new Document("coins", 50))
                    );
                    db.close();
                    reward[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            rewardThread.start();
        }

        // 创建胜利画面Stage
        Stage victoryStage = new Stage();
        victoryStage.setTitle("游戏胜利！");
        victoryStage.setResizable(true);
        victoryStage.initOwner(primaryStage);
        victoryStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        victoryStage.setX(primaryStage.getX());
        victoryStage.setY(primaryStage.getY());
        victoryStage.setWidth(primaryStage.getWidth());
        victoryStage.setHeight(primaryStage.getHeight());

        victoryStage.setMinWidth(1000);
        victoryStage.setMinHeight(700);

        // 创建主容器 - 添加音乐控制
        HBox root = new HBox(0);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("victory-background");

        // 创建胜利画面内容
        VBox mainContent = new VBox(10);
        mainContent.setAlignment(Pos.CENTER);

        // 添加音乐控制栏
        HBox musicControlBar = createMusicControlBar();

        HBox victoryContent = createVictoryContentResponsive(layoutName, moveCount, elapsedTime, reward[0], rewardThread);

        mainContent.getChildren().addAll(musicControlBar, victoryContent);
        root.getChildren().add(mainContent);

        Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
        loadCSS(scene);

        victoryStage.setScene(scene);

        // 添加关闭事件处理
        victoryStage.setOnCloseRequest(e -> {
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止胜利音乐
            if (parentStageToClose != null) {
                parentStageToClose.show();
                musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
            }
            primaryStage.close();
        });

        victoryStage.show();

        // 启动胜利动画
        startVictoryAnimationsResponsive(victoryContent);

        // 等待奖励线程结束
        if (rewardThread != null) {
            try {
                rewardThread.join(100);
            } catch (InterruptedException ignored) {}
        }
    }

    // 修改：showFailDialog 方法 - 添加失败音乐
    private void showFailDialog(String failReason, String failMessage) {
        if (timer != null) timer.stop();

        // 新增：播放失败音乐
        musicManager.playMusic(MusicManager.FAILURE);

        String elapsedTime = getElapsedTimeString();
        String layoutName = getCurrentLayoutName();

        // 创建失败画面Stage
        Stage failStage = new Stage();
        failStage.setTitle("挑战失败");
        failStage.setResizable(true);
        failStage.initOwner(primaryStage);
        failStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        failStage.setX(primaryStage.getX());
        failStage.setY(primaryStage.getY());
        failStage.setWidth(primaryStage.getWidth());
        failStage.setHeight(primaryStage.getHeight());

        failStage.setMinWidth(1000);
        failStage.setMinHeight(700);

        // 创建主容器 - 添加音乐控制
        HBox root = new HBox(0);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("failure-background");

        // 创建失败画面内容
        VBox mainContent = new VBox(10);
        mainContent.setAlignment(Pos.CENTER);

        // 添加音乐控制栏
        HBox musicControlBar = createMusicControlBar();

        HBox failContent = createFailureContent(failReason, failMessage, layoutName, moveCount, elapsedTime);

        mainContent.getChildren().addAll(musicControlBar, failContent);
        root.getChildren().add(mainContent);

        Scene scene = new Scene(root, primaryStage.getWidth(), primaryStage.getHeight());
        loadCSS(scene);

        failStage.setScene(scene);

        // 添加关闭事件处理
        failStage.setOnCloseRequest(e -> {
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止失败音乐
            if (parentStageToClose != null) {
                parentStageToClose.show();
                musicManager.playMusic(MusicManager.MAIN_MENU); // 返回主界面音乐
            }
            primaryStage.close();
        });

        failStage.show();

        // 启动失败动画
        startFailureAnimations(failContent);
    }

    // 新增：创建音乐控制栏
    private HBox createMusicControlBar() {
        HBox musicBar = new HBox(15);
        musicBar.setAlignment(Pos.CENTER_RIGHT);
        musicBar.setPadding(new Insets(10, 20, 5, 20));
        musicBar.getStyleClass().add("music-control-bar");

        Button musicToggleBtn = new Button(musicManager.isMusicEnabled() ? "🔊" : "🔇");
        musicToggleBtn.setPrefSize(40, 30);
        musicToggleBtn.setFont(Font.font("微软雅黑", 14));
        musicToggleBtn.getStyleClass().add("music-control-button");
        musicToggleBtn.setOnAction(e -> {
            musicManager.toggleMusic();
            musicToggleBtn.setText(musicManager.isMusicEnabled() ? "🔊" : "🔇");
        });

        Slider volumeSlider = new Slider(0, 1, musicManager.getVolume());
        volumeSlider.setPrefWidth(100);
        volumeSlider.setMaxWidth(100);
        volumeSlider.getStyleClass().add("music-volume-slider");

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            musicManager.setVolume(newVal.doubleValue());
        });

        Label volumeLabel = new Label("音量");
        volumeLabel.setFont(Font.font("微软雅黑", 12));
        volumeLabel.getStyleClass().add("music-volume-label");

        musicBar.getChildren().addAll(volumeLabel, volumeSlider, musicToggleBtn);

        return musicBar;
    }

    // 修改：createVictoryButtonAreaFixed 方法中的按钮事件
    private HBox createVictoryButtonAreaFixed() {
        HBox buttonArea = new HBox(15);
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(15, 0, 5, 0));

        Button restartButton = new Button("🔄 再来一局");
        restartButton.setPrefWidth(120);
        restartButton.setPrefHeight(45);
        restartButton.setFont(Font.font("微软雅黑", 14));
        restartButton.getStyleClass().add("victory-button-restart");
        restartButton.setOnAction(e -> {
            Stage victoryStage = (Stage) restartButton.getScene().getWindow();
            victoryStage.close();
            musicManager.stopMusic(); // 停止胜利音乐
            restartGame();
            // 重新播放游戏音乐
            if (isTimed) {
                musicManager.playMusic(MusicManager.TIMED_MODE);
            } else {
                musicManager.playMusic(MusicManager.GAME_PLAY);
            }
        });

        Button newLayoutButton = new Button("🎯 新布局");
        newLayoutButton.setPrefWidth(120);
        newLayoutButton.setPrefHeight(45);
        newLayoutButton.setFont(Font.font("微软雅黑", 14));
        newLayoutButton.getStyleClass().add("victory-button-layout");
        newLayoutButton.setOnAction(e -> {
            Stage victoryStage = (Stage) newLayoutButton.getScene().getWindow();
            victoryStage.close();
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止胜利音乐
            showLayoutSelectionDialog(null);
        });

        Button backToMainButton = new Button("🏠 返回主界面");
        backToMainButton.setPrefWidth(130);
        backToMainButton.setPrefHeight(45);
        backToMainButton.setFont(Font.font("微软雅黑", 14));
        backToMainButton.getStyleClass().add("victory-button-main");
        backToMainButton.setOnAction(e -> {
            Stage victoryStage = (Stage) backToMainButton.getScene().getWindow();
            victoryStage.close();
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止胜利音乐
            if (parentStageToClose != null) {
                parentStageToClose.show();
                parentStageToClose.toFront();
                musicManager.playMusic(MusicManager.MAIN_MENU); // 播放主界面音乐
            }
            primaryStage.close();
        });

        Button continueButton = new Button("⏭ 继续游戏");
        continueButton.setPrefWidth(120);
        continueButton.setPrefHeight(45);
        continueButton.setFont(Font.font("微软雅黑", 14));
        continueButton.getStyleClass().add("victory-button-continue");
        continueButton.setOnAction(e -> {
            Stage victoryStage = (Stage) continueButton.getScene().getWindow();
            victoryStage.close();
            musicManager.stopMusic(); // 停止胜利音乐
            // 恢复游戏音乐
            if (isTimed) {
                musicManager.playMusic(MusicManager.TIMED_MODE);
            } else {
                musicManager.playMusic(MusicManager.GAME_PLAY);
            }
        });

        buttonArea.getChildren().addAll(restartButton, newLayoutButton, backToMainButton, continueButton);

        return buttonArea;
    }

    // 修改：createFailureButtonArea 方法中的按钮事件
    private HBox createFailureButtonArea() {
        HBox buttonArea = new HBox(15);
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(15, 0, 5, 0));

        Button retryButton = new Button("🔄 重新挑战");
        retryButton.setPrefWidth(130);
        retryButton.setPrefHeight(45);
        retryButton.setFont(Font.font("微软雅黑", 14));
        retryButton.getStyleClass().add("failure-button-retry");
        retryButton.setOnAction(e -> {
            Stage failStage = (Stage) retryButton.getScene().getWindow();
            failStage.close();
            musicManager.stopMusic(); // 停止失败音乐
            restartGame();
            // 重新播放游戏音乐
            if (isTimed) {
                musicManager.playMusic(MusicManager.TIMED_MODE);
            } else {
                musicManager.playMusic(MusicManager.GAME_PLAY);
            }
        });

        Button newLayoutButton = new Button("🎯 换个布局");
        newLayoutButton.setPrefWidth(130);
        newLayoutButton.setPrefHeight(45);
        newLayoutButton.setFont(Font.font("微软雅黑", 14));
        newLayoutButton.getStyleClass().add("failure-button-layout");
        newLayoutButton.setOnAction(e -> {
            Stage failStage = (Stage) newLayoutButton.getScene().getWindow();
            failStage.close();
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止失败音乐
            showLayoutSelectionDialog(null);
        });

        Button aiHelpButton = new Button("🤖 AI帮助");
        aiHelpButton.setPrefWidth(130);
        aiHelpButton.setPrefHeight(45);
        aiHelpButton.setFont(Font.font("微软雅黑", 14));
        aiHelpButton.getStyleClass().add("failure-button-ai");
        aiHelpButton.setOnAction(e -> {
            Stage failStage = (Stage) aiHelpButton.getScene().getWindow();
            failStage.close();
            musicManager.stopMusic(); // 停止失败音乐
            solveByAI();
            // AI求解时播放游戏音乐
            if (isTimed) {
                musicManager.playMusic(MusicManager.TIMED_MODE);
            } else {
                musicManager.playMusic(MusicManager.GAME_PLAY);
            }
        });

        Button backButton = new Button("🏠 返回主界面");
        backButton.setPrefWidth(140);
        backButton.setPrefHeight(45);
        backButton.setFont(Font.font("微软雅黑", 14));
        backButton.getStyleClass().add("failure-button-back");
        backButton.setOnAction(e -> {
            Stage failStage = (Stage) backButton.getScene().getWindow();
            failStage.close();
            cleanOnlineRoom();
            musicManager.stopMusic(); // 停止失败音乐
            if (parentStageToClose != null) {
                parentStageToClose.show();
                parentStageToClose.toFront();
                musicManager.playMusic(MusicManager.MAIN_MENU); // 播放主界面音乐
            }
            primaryStage.close();
        });

        buttonArea.getChildren().addAll(retryButton, newLayoutButton, aiHelpButton, backButton);

        return buttonArea;
    }

    // 新增：加载CSS样式的方法
    private void loadCSS(Scene scene) {
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("无法加载CSS文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改：createGameControls 方法 - 添加选中方块信息更新
    private VBox createGameControls() {
        VBox gameControls = new VBox(20);
        gameControls.setAlignment(Pos.CENTER);

        // 方向控制区域
        VBox directionSection = new VBox(15);
        directionSection.setAlignment(Pos.CENTER);
        directionSection.getStyleClass().add("control-section");

        Label directionTitle = new Label("🕹️ 方向控制");
        directionTitle.setFont(Font.font("微软雅黑", 16));
        directionTitle.getStyleClass().add("control-section-title");

        // 方向按钮 - 重新设计布局
        Button upButton = createDirectionButton("⬆", "上");
        Button downButton = createDirectionButton("⬇", "下");
        Button leftButton = createDirectionButton("⬅", "左");
        Button rightButton = createDirectionButton("➡", "右");

        directionButtons.clear();
        Collections.addAll(directionButtons, upButton, downButton, leftButton, rightButton);

        // 设置按钮事件
        upButton.setOnAction(e -> moveSelectedBlock(Direction.UP));
        downButton.setOnAction(e -> moveSelectedBlock(Direction.DOWN));
        leftButton.setOnAction(e -> moveSelectedBlock(Direction.LEFT));
        rightButton.setOnAction(e -> moveSelectedBlock(Direction.RIGHT));

        // 方向按钮布局 - 十字形排列
        VBox directionLayout = new VBox(8);
        directionLayout.setAlignment(Pos.CENTER);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER);
        topRow.getChildren().add(upButton);

        HBox middleRow = new HBox(8);
        middleRow.setAlignment(Pos.CENTER);
        middleRow.getChildren().addAll(leftButton, rightButton);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER);
        bottomRow.getChildren().add(downButton);

        directionLayout.getChildren().addAll(topRow, middleRow, bottomRow);

        directionSection.getChildren().addAll(directionTitle, directionLayout);

        // 操作说明区域
        VBox instructionSection = new VBox(10);
        instructionSection.setAlignment(Pos.CENTER);
        instructionSection.getStyleClass().add("control-section");

        Label instructionTitle = new Label("📖 操作说明");
        instructionTitle.setFont(Font.font("微软雅黑", 16));
        instructionTitle.getStyleClass().add("control-section-title");

        VBox instructionContent = new VBox(8);
        instructionContent.setAlignment(Pos.CENTER_LEFT);
        instructionContent.getStyleClass().add("instruction-content");

        Label[] instructions = {
                new Label("• 点击方块进行选择"),
                new Label("• 使用方向键移动"),
                new Label("• 或点击方向按钮"),
                new Label("• 将曹操移到出口获胜")
        };

        for (Label instruction : instructions) {
            instruction.setFont(Font.font("微软雅黑", 13));
            instruction.getStyleClass().add("instruction-text");
            instructionContent.getChildren().add(instruction);
        }

        instructionSection.getChildren().addAll(instructionTitle, instructionContent);

        // 选中方块信息区域
        VBox selectionSection = new VBox(10);
        selectionSection.setAlignment(Pos.CENTER);
        selectionSection.getStyleClass().add("control-section");

        Label selectionTitle = new Label("🎯 当前选择");
        selectionTitle.setFont(Font.font("微软雅黑", 16));
        selectionTitle.getStyleClass().add("control-section-title");

        // 修改：创建可更新的选择信息标签
        Label selectionInfo = new Label("请先选择一个方块");
        selectionInfo.setFont(Font.font("微软雅黑", 14));
        selectionInfo.getStyleClass().add("selection-info");
        selectionInfo.setWrapText(true);
        selectionInfo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 保存引用以便后续更新
        this.selectionInfoLabel = selectionInfo;

        selectionSection.getChildren().addAll(selectionTitle, selectionInfo);

        gameControls.getChildren().addAll(directionSection, instructionSection, selectionSection);

        return gameControls;
    }

    // 新增：选择信息标签引用
    private Label selectionInfoLabel;

    // 修改：selectBlock 方法 - 更新选择信息显示
    private void selectBlock(Block block) {
        selectedBlock = block;
        drawBlocks();

        for (Button btn : directionButtons) {
            btn.setDisable(false);
        }

        // 更新选择信息显示
        if (selectionInfoLabel != null) {
            String blockInfo = String.format("已选择：%s\n位置：第%d行，第%d列",
                    block.getName(), block.getRow() + 1, block.getCol() + 1);
            selectionInfoLabel.setText(blockInfo);
        }
    }

    // 修改：移动后清除选择时也要更新信息
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

            // 更新选择信息
            if (selectionInfoLabel != null) {
                String blockInfo = String.format("已选择：%s\n位置：第%d行，第%d列",
                        selectedBlock.getName(), selectedBlock.getRow() + 1, selectedBlock.getCol() + 1);
                selectionInfoLabel.setText(blockInfo);
            }

            drawBlocks();
            checkWinCondition();
            // 仅在观战模式下同步
            if (watchable && roomId != null) {
                uploadOnlineGameState(roomId, userName, blocks, moveCount, getElapsedTimeString());
            }
        }
    }

    // 修改：undoMove 方法 - 撤销时清除选择信息
    private void undoMove() {
        if (!historyStack.isEmpty()) {
            blocks = deepCopyBlocks(historyStack.pop());
            moveCount = Math.max(0, moveCount - 1);
            moveCountLabel.setText("步数: " + moveCount);
            selectedBlock = null;

            // 清除选择信息
            if (selectionInfoLabel != null) {
                selectionInfoLabel.setText("请先选择一个方块");
            }

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

    // 修改：createMainLayout 方法 - 重新调整布局结构
    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("game-main-background");

        // 顶部面板 - 移除状态显示
        HBox topPanel = createTopPanel();
        root.setTop(topPanel);

        // 中心区域 - 使用BorderPane来固定控制栏位置
        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(10));

        // 中心内容区域 - 包含棋盘和两侧状态信息
        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);

        // 新增：棋盘区域包装器，包含左右状态信息
        VBox gameBoardWrapper = createGameBoardWithStatus();

        // 下方状态框（只显示布局名称）
        HBox statusInfoBox = createGameStatusBox();

        centerContent.getChildren().addAll(statusInfoBox, gameBoardWrapper);
        centerPane.setCenter(centerContent);

        // 控制面板 - 固定在右侧
        VBox controlPanel = createControlPanel();
        centerPane.setRight(controlPanel);

        root.setCenter(centerPane);

        return root;
    }

    // 修改：restartGame 方法 - 更新布局创建
    private void restartGame() {
        initGameData();

        // 重新创建中心区域
        BorderPane centerPane = new BorderPane();
        centerPane.setPadding(new Insets(10));

        VBox centerContent = new VBox(15);
        centerContent.setAlignment(Pos.CENTER);

        // 重新创建带状态信息的棋盘区域
        VBox gameBoardWrapper = createGameBoardWithStatus();
        HBox statusInfoBox = createGameStatusBox();

        centerContent.getChildren().addAll(statusInfoBox, gameBoardWrapper);
        centerPane.setCenter(centerContent);

        // 重新创建控制面板
        VBox controlPanel = createControlPanel();
        centerPane.setRight(controlPanel);

        // 更新主布局
        BorderPane root = (BorderPane) primaryStage.getScene().getRoot();
        root.setCenter(centerPane);

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

    // 新增：创建方向按钮
    private Button createDirectionButton(String symbol, String text) {
        Button button = new Button(symbol);
        button.setPrefSize(60, 50);
        button.setFont(Font.font("微软雅黑", 18));
        button.getStyleClass().add("direction-button");
        button.setDisable(true);

        // 添加提示文本
        Tooltip tooltip = new Tooltip("向" + text + "移动");
        tooltip.setFont(Font.font("微软雅黑", 12));
        Tooltip.install(button, tooltip);

        return button;
    }

    // 修改：createControlPanel 方法 - 优化控制面板设计
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(15);
        controlPanel.setPadding(new Insets(25, 20, 25, 20));
        controlPanel.getStyleClass().add("game-control-panel");
        controlPanel.setMinWidth(280);
        controlPanel.setMaxWidth(280);
        controlPanel.setPrefWidth(280);
        controlPanel.setAlignment(Pos.TOP_CENTER);

        // 控制面板标题
        Label controlTitle = new Label("🎮 游戏控制");
        controlTitle.setFont(Font.font("微软雅黑", 20));
        controlTitle.getStyleClass().add("control-panel-title");

        if (!aiSolving) {
            // 普通游戏控制
            VBox gameControls = createGameControls();
            controlPanel.getChildren().addAll(controlTitle, gameControls);
        } else {
            // AI演示控制
            VBox aiControls = createAIControls();
            controlPanel.getChildren().addAll(controlTitle, aiControls);
        }

        controlPanelRef = controlPanel;
        return controlPanel;
    }
    // 修改：createGameStatusBox 方法 - 正确添加所有状态显示元素
    private HBox createGameStatusBox() {
        HBox statusBox = new HBox(40);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(10, 20, 5, 20));
        statusBox.getStyleClass().add("game-status-box");

        // 左侧：步数显示
        HBox leftStatus = new HBox(8);
        leftStatus.setAlignment(Pos.CENTER_LEFT);
        leftStatus.setPrefWidth(200);
        leftStatus.setMaxWidth(200);

        Label moveIcon = new Label("👣");
        moveIcon.setFont(Font.font("微软雅黑", 18));
        moveIcon.setStyle("-fx-text-fill: #495057;");
        moveIcon.getStyleClass().add("status-emoji-icon");

        moveCountLabel.setFont(Font.font("微软雅黑", 18));
        moveCountLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        leftStatus.getChildren().addAll(moveIcon, moveCountLabel);

        // 中间：布局名称显示
        VBox layoutBox = new VBox(5);
        layoutBox.setAlignment(Pos.CENTER);

        Label currentLayoutLabel = new Label(getCurrentLayoutName());
        currentLayoutLabel.setFont(Font.font("微软雅黑", 16));
        currentLayoutLabel.getStyleClass().add("status-value");

        layoutBox.getChildren().addAll(currentLayoutLabel);

        // 右侧：时间显示
        HBox rightStatus = new HBox(8);
        rightStatus.setAlignment(Pos.CENTER_RIGHT);
        rightStatus.setPrefWidth(200);
        rightStatus.setMaxWidth(200);

        Label timerIcon = new Label("⏱");
        timerIcon.setFont(Font.font("微软雅黑", 18));
        timerIcon.setStyle("-fx-text-fill: #495057;");
        timerIcon.getStyleClass().add("status-emoji-icon");

        timerLabel.setFont(Font.font("微软雅黑", 18));
        timerLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        rightStatus.getChildren().addAll(timerIcon, timerLabel);

        // 修复：将所有三个部分都添加到状态框中
        statusBox.getChildren().addAll(leftStatus, layoutBox, rightStatus);

        return statusBox;
    }

    // 修改：构造函数中的标签样式 - 确保白色显示
    public GameFrame() {
        moveCountLabel = new Label("步数: 0");
        moveCountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

        layoutNameLabel = new Label();
        layoutNameLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: gold; -fx-font-weight: bold;");
        layoutNameLabel.setAlignment(Pos.CENTER);
        this.time = null;

        timerLabel = new Label("用时：00：00");
        timerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

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

    // 新增：创建AI控制区域
    private VBox createAIControls() {
        VBox aiControls = new VBox(20);
        aiControls.setAlignment(Pos.CENTER);

        // AI状态显示
        VBox statusSection = new VBox(10);
        statusSection.setAlignment(Pos.CENTER);
        statusSection.getStyleClass().add("control-section");

        Label statusTitle = new Label("🤖 AI演示中");
        statusTitle.setFont(Font.font("微软雅黑", 16));
        statusTitle.getStyleClass().add("ai-status-title");

        Label statusInfo = new Label("AI正在为您演示解法");
        statusInfo.setFont(Font.font("微软雅黑", 14));
        statusInfo.getStyleClass().add("ai-status-info");
        statusInfo.setWrapText(true);
        statusInfo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        statusSection.getChildren().addAll(statusTitle, statusInfo);

        // AI控制按钮区域
        VBox controlSection = new VBox(15);
        controlSection.setAlignment(Pos.CENTER);
        controlSection.getStyleClass().add("control-section");

        Label controlTitle = new Label("🎛️ AI控制");
        controlTitle.setFont(Font.font("微软雅黑", 16));
        controlTitle.getStyleClass().add("control-section-title");

        // 主要控制按钮
        VBox mainControls = new VBox(10);
        mainControls.setAlignment(Pos.CENTER);

        Button pauseBtn = new Button(aiPaused ? "▶ 继续演示" : "⏸ 暂停演示");
        pauseBtn.setPrefWidth(200);
        pauseBtn.setPrefHeight(45);
        pauseBtn.setFont(Font.font("微软雅黑", 14));
        pauseBtn.getStyleClass().add("ai-control-button-primary");
        pauseBtn.setOnAction(e -> {
            aiPaused = !aiPaused;
            refreshControlPanel();
        });

        Button stopBtn = new Button("⏹ 结束演示");
        stopBtn.setPrefWidth(200);
        stopBtn.setPrefHeight(45);
        stopBtn.setFont(Font.font("微软雅黑", 14));
        stopBtn.getStyleClass().add("ai-control-button-stop");
        stopBtn.setOnAction(e -> stopAISolve());

        mainControls.getChildren().addAll(pauseBtn, stopBtn);

        // 步进控制按钮
        VBox stepControls = new VBox(10);
        stepControls.setAlignment(Pos.CENTER);

        Label stepTitle = new Label("单步控制");
        stepTitle.setFont(Font.font("微软雅黑", 14));
        stepTitle.getStyleClass().add("step-control-title");

        HBox stepButtons = new HBox(10);
        stepButtons.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("⏮ 上一步");
        prevBtn.setPrefWidth(95);
        prevBtn.setPrefHeight(40);
        prevBtn.setFont(Font.font("微软雅黑", 12));
        prevBtn.getStyleClass().add("ai-control-button-step");
        prevBtn.setOnAction(e -> aiStepMove(-1));

        Button nextBtn = new Button("⏭ 下一步");
        nextBtn.setPrefWidth(95);
        nextBtn.setPrefHeight(40);
        nextBtn.setFont(Font.font("微软雅黑", 12));
        nextBtn.getStyleClass().add("ai-control-button-step");
        nextBtn.setOnAction(e -> aiStepMove(1));

        stepButtons.getChildren().addAll(prevBtn, nextBtn);
        stepControls.getChildren().addAll(stepTitle, stepButtons);

        controlSection.getChildren().addAll(controlTitle, mainControls, stepControls);

        aiControls.getChildren().addAll(statusSection, controlSection);

        return aiControls;
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

    // 新增：获取当前布局名称
    private String getCurrentLayoutName() {
        List<String> layoutNames = BoardLayouts.getLayoutNames();
        if (currentLayoutIndex >= 0 && currentLayoutIndex < layoutNames.size()) {
            return layoutNames.get(currentLayoutIndex);
        }
        return "棋局：未知";
    }

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

    private void refreshControlPanel() {
        BorderPane root = (BorderPane) primaryStage.getScene().getRoot();
        VBox newPanel = createControlPanel();
        root.setRight(newPanel);
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

    private HBox createVictoryContentResponsive(String layoutName, int moveCount, String elapsedTime, boolean hasReward, Thread rewardThread) {
        HBox content = new HBox(40); // 使用固定间距，避免复杂绑定
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30, 40, 30, 40));

        // 左侧区域：标题和成就
        VBox leftSection = new VBox(20);
        leftSection.setAlignment(Pos.CENTER);
        leftSection.setPrefWidth(350);
        leftSection.setMinWidth(300);
        leftSection.setMaxWidth(400);

        // 胜利标题区域
        VBox titleArea = createVictoryTitleAreaFixed();

        // 成就展示区域
        VBox achievementArea = createAchievementAreaFixed(moveCount, elapsedTime);

        leftSection.getChildren().addAll(titleArea, achievementArea);

        // 右侧区域：统计和按钮
        VBox rightSection = new VBox(25);
        rightSection.setAlignment(Pos.CENTER);
        rightSection.setPrefWidth(500);
        rightSection.setMinWidth(400);
        rightSection.setMaxWidth(600);

        // 游戏统计区域
        VBox statsArea = createVictoryStatsAreaFixed(layoutName, moveCount, elapsedTime, hasReward, rewardThread);

        // 操作按钮区域
        HBox buttonArea = createVictoryButtonAreaFixed();

        rightSection.getChildren().addAll(statsArea, buttonArea);

        content.getChildren().addAll(leftSection, rightSection);

        return content;
    }

    // 新增：创建固定尺寸的胜利标题区域
    private VBox createVictoryTitleAreaFixed() {
        VBox titleArea = new VBox(15);
        titleArea.setAlignment(Pos.CENTER);

        // 主标题 - 使用固定字体大小
        Label victoryIcon = new Label("🏆");
        victoryIcon.setFont(Font.font("微软雅黑", 64));
        victoryIcon.getStyleClass().add("victory-main-icon");
        victoryIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label victoryTitle = new Label("恭喜通关！");
        victoryTitle.setFont(Font.font("微软雅黑", 32));
        victoryTitle.getStyleClass().add("victory-main-title");

        Label victorySubtitle = new Label("华容道挑战成功");
        victorySubtitle.setFont(Font.font("微软雅黑", 16));
        victorySubtitle.getStyleClass().add("victory-subtitle");

        titleArea.getChildren().addAll(victoryIcon, victoryTitle, victorySubtitle);

        return titleArea;
    }

    // 新增：创建固定尺寸的成就展示区域
    private VBox createAchievementAreaFixed(int moveCount, String elapsedTime) {
        VBox achievementArea = new VBox(12);
        achievementArea.setAlignment(Pos.CENTER);

        Label achievementTitle = new Label("🎖️ 成就达成");
        achievementTitle.setFont(Font.font("微软雅黑", 18));
        achievementTitle.getStyleClass().add("victory-achievement-title");
        achievementTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 0, 1);");

        // 根据表现生成成就
        VBox achievementList = new VBox(8);
        achievementList.setAlignment(Pos.CENTER);

        // 基础完成成就
        HBox basicAchievement = createAchievementItemFixed("🏆", "华容道大师", "成功完成华容道挑战");
        achievementList.getChildren().add(basicAchievement);

        // 步数相关成就
        if (moveCount <= 100) {
            HBox stepAchievement = createAchievementItemFixed("⚡", "效率专家", "用极少步数完成挑战");
            achievementList.getChildren().add(stepAchievement);
        } else if (moveCount <= 150) {
            HBox stepAchievement = createAchievementItemFixed("🎯", "策略大师", "用较少步数完成挑战");
            achievementList.getChildren().add(stepAchievement);
        }

        // 时间相关成就（解析用时）
        String[] timeParts = elapsedTime.split(":");
        if (timeParts.length == 2) {
            int totalMinutes = Integer.parseInt(timeParts[0]);
            if (totalMinutes <= 5) {
                HBox timeAchievement = createAchievementItemFixed("🚀", "闪电通关", "在5分钟内完成挑战");
                achievementList.getChildren().add(timeAchievement);
            } else if (totalMinutes <= 10) {
                HBox timeAchievement = createAchievementItemFixed("⏰", "快速求解", "在10分钟内完成挑战");
                achievementList.getChildren().add(timeAchievement);
            }
        }

        // 限时模式成就
        if (isTimed && remainSeconds > 0) {
            HBox timedAchievement = createAchievementItemFixed("🔥", "限时挑战者", "在限时模式下成功通关");
            achievementList.getChildren().add(timedAchievement);
        }

        achievementArea.getChildren().addAll(achievementTitle, achievementList);

        return achievementArea;
    }

    // 新增：创建固定尺寸的成就项目
    private HBox createAchievementItemFixed(String icon, String title, String description) {
        HBox achievementItem = new HBox(10);
        achievementItem.setAlignment(Pos.CENTER_LEFT);
        achievementItem.setPadding(new Insets(6, 12, 6, 12));
        achievementItem.getStyleClass().add("victory-achievement-item");
        achievementItem.setPrefWidth(320);
        achievementItem.setMaxWidth(320);

        Label achievementIcon = new Label(icon);
        achievementIcon.setFont(Font.font("微软雅黑", 18));
        achievementIcon.getStyleClass().add("victory-achievement-icon");
        achievementIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        VBox achievementText = new VBox(2);
        achievementText.setAlignment(Pos.CENTER_LEFT);

        Label achievementTitle = new Label(title);
        achievementTitle.setFont(Font.font("微软雅黑", 14));
        achievementTitle.getStyleClass().add("victory-achievement-name");

        Label achievementDesc = new Label(description);
        achievementDesc.setFont(Font.font("微软雅黑", 12));
        achievementDesc.getStyleClass().add("victory-achievement-desc");

        achievementText.getChildren().addAll(achievementTitle, achievementDesc);
        achievementItem.getChildren().addAll(achievementIcon, achievementText);

        return achievementItem;
    }

    // 新增：创建固定尺寸的胜利统计区域
    private VBox createVictoryStatsAreaFixed(String layoutName, int moveCount, String elapsedTime, boolean hasReward, Thread rewardThread) {
        VBox statsArea = new VBox(20);
        statsArea.setAlignment(Pos.CENTER);
        statsArea.setPadding(new Insets(20, 25, 20, 25));
        statsArea.getStyleClass().add("victory-stats-container");

        // 统计标题
        Label statsTitle = new Label("📊 游戏统计");
        statsTitle.setFont(Font.font("微软雅黑", 20));
        statsTitle.getStyleClass().add("victory-stats-title");

        // 统计信息网格
        GridPane statsGrid = new GridPane();
        statsGrid.setAlignment(Pos.CENTER);
        statsGrid.setHgap(40);
        statsGrid.setVgap(15);
        statsGrid.setPadding(new Insets(12, 0, 12, 0));

        // 布局信息
        VBox layoutInfo = createStatItemFixed("🎯", "挑战布局", layoutName);

        // 步数信息
        VBox moveInfo = createStatItemFixed("👣", "移动步数", moveCount + " 步");

        // 用时信息
        VBox timeInfo = createStatItemFixed("⏱️", "游戏用时", elapsedTime);

        // 效率评价
        VBox efficiencyInfo = createEfficiencyRatingFixed(moveCount);

        statsGrid.add(layoutInfo, 0, 0);
        statsGrid.add(moveInfo, 1, 0);
        statsGrid.add(timeInfo, 0, 1);
        statsGrid.add(efficiencyInfo, 1, 1);

        statsArea.getChildren().addAll(statsTitle, statsGrid);

        // 如果有奖励，添加奖励信息
        if (isTimed && remainSeconds > 0 && !"离线用户".equals(userName)) {
            VBox rewardArea = createRewardAreaFixed(rewardThread);
            statsArea.getChildren().add(rewardArea);
        }

        return statsArea;
    }

    // 新增：创建固定尺寸的统计项目
    private VBox createStatItemFixed(String icon, String label, String value) {
        VBox statItem = new VBox(8);
        statItem.setAlignment(Pos.CENTER);
        statItem.setPrefWidth(120);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("微软雅黑", 24));
        iconLabel.getStyleClass().add("victory-stat-icon");
        iconLabel.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label labelText = new Label(label);
        labelText.setFont(Font.font("微软雅黑", 14));
        labelText.getStyleClass().add("victory-stat-label");

        Label valueText = new Label(value);
        valueText.setFont(Font.font("微软雅黑", 16));
        valueText.getStyleClass().add("victory-stat-value");

        statItem.getChildren().addAll(iconLabel, labelText, valueText);

        return statItem;
    }

    // 新增：创建固定尺寸的效率评价
    private VBox createEfficiencyRatingFixed(int moveCount) {
        VBox efficiencyItem = new VBox(8);
        efficiencyItem.setAlignment(Pos.CENTER);
        efficiencyItem.setPrefWidth(120);

        String rating;
        String ratingIcon;
        String ratingClass;

        if (moveCount <= 100) {
            rating = "完美";
            ratingIcon = "⭐⭐⭐";
            ratingClass = "victory-rating-perfect";
        } else if (moveCount <= 150) {
            rating = "优秀";
            ratingIcon = "⭐⭐";
            ratingClass = "victory-rating-excellent";
        } else if (moveCount <= 200) {
            rating = "良好";
            ratingIcon = "⭐";
            ratingClass = "victory-rating-good";
        } else {
            rating = "及格";
            ratingIcon = "🎯";
            ratingClass = "victory-rating-pass";
        }

        Label iconLabel = new Label(ratingIcon);
        iconLabel.setFont(Font.font("微软雅黑", 18));
        iconLabel.getStyleClass().add("victory-stat-icon");

        Label labelText = new Label("效率评价");
        labelText.setFont(Font.font("微软雅黑", 14));
        labelText.getStyleClass().add("victory-stat-label");

        Label valueText = new Label(rating);
        valueText.setFont(Font.font("微软雅黑", 16));
        valueText.getStyleClass().addAll("victory-stat-value", ratingClass);

        efficiencyItem.getChildren().addAll(iconLabel, labelText, valueText);

        return efficiencyItem;
    }

    // 新增：创建固定尺寸的奖励区域
    private VBox createRewardAreaFixed(Thread rewardThread) {
        VBox rewardArea = new VBox(12);
        rewardArea.setAlignment(Pos.CENTER);
        rewardArea.setPadding(new Insets(15, 20, 15, 20));
        rewardArea.getStyleClass().add("victory-reward-container");

        Label rewardIcon = new Label("💰");
        rewardIcon.setFont(Font.font("微软雅黑", 28));
        rewardIcon.getStyleClass().add("victory-reward-icon");
        rewardIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label rewardTitle = new Label("限时挑战奖励");
        rewardTitle.setFont(Font.font("微软雅黑", 16));
        rewardTitle.getStyleClass().add("victory-reward-title");

        Label rewardText = new Label("恭喜您在限时内完成挑战！");
        rewardText.setFont(Font.font("微软雅黑", 14));
        rewardText.getStyleClass().add("victory-reward-text");

        Label coinReward = new Label("💰 +50 金币");
        coinReward.setFont(Font.font("微软雅黑", 16));
        coinReward.getStyleClass().add("victory-coin-reward");
        coinReward.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 2, 0, 0, 1);");

        rewardArea.getChildren().addAll(rewardIcon, rewardTitle, rewardText, coinReward);

        // 如果奖励线程还在运行，显示等待状态
        if (rewardThread != null && rewardThread.isAlive()) {
            ProgressIndicator rewardProgress = new ProgressIndicator();
            rewardProgress.setPrefSize(20, 20);
            rewardProgress.getStyleClass().add("victory-reward-progress");

            Label processingText = new Label("正在发放奖励...");
            processingText.setFont(Font.font("微软雅黑", 12));
            processingText.getStyleClass().add("victory-processing-text");

            rewardArea.getChildren().addAll(rewardProgress, processingText);
        }

        return rewardArea;
    }

    // 新增：启动固定布局的胜利动画
    private void startVictoryAnimationsResponsive(HBox victoryContent) {
        // 为胜利内容添加淡入动画
        victoryContent.setOpacity(0);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), victoryContent);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // 为左侧区域添加从左滑入动画
        VBox leftSection = (VBox) victoryContent.getChildren().get(0);
        leftSection.setTranslateX(-100);
        leftSection.setOpacity(0);

        javafx.animation.TranslateTransition leftSlideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(600), leftSection);
        leftSlideIn.setFromX(-100);
        leftSlideIn.setToX(0);

        javafx.animation.FadeTransition leftFadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), leftSection);
        leftFadeIn.setFromValue(0);
        leftFadeIn.setToValue(1);

        javafx.animation.ParallelTransition leftAnimation = new javafx.animation.ParallelTransition(leftSlideIn, leftFadeIn);

        // 为右侧区域添加从右滑入动画
        VBox rightSection = (VBox) victoryContent.getChildren().get(1);
        rightSection.setTranslateX(100);
        rightSection.setOpacity(0);

        javafx.animation.TranslateTransition rightSlideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(600), rightSection);
        rightSlideIn.setFromX(100);
        rightSlideIn.setToX(0);

        javafx.animation.FadeTransition rightFadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), rightSection);
        rightFadeIn.setFromValue(0);
        rightFadeIn.setToValue(1);

        javafx.animation.ParallelTransition rightAnimation = new javafx.animation.ParallelTransition(rightSlideIn, rightFadeIn);

        // 延迟启动左右区域动画
        javafx.animation.Timeline leftDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> leftAnimation.play())
        );
        leftDelay.play();

        javafx.animation.Timeline rightDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400), e -> rightAnimation.play())
        );
        rightDelay.play();

        // 为主图标添加缩放动画
        VBox titleArea = (VBox) leftSection.getChildren().get(0);
        Label victoryIcon = (Label) titleArea.getChildren().get(0);

        javafx.animation.ScaleTransition scaleAnimation = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(600), victoryIcon);
        scaleAnimation.setFromX(0.3);
        scaleAnimation.setFromY(0.3);
        scaleAnimation.setToX(1.0);
        scaleAnimation.setToY(1.0);
        scaleAnimation.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        // 延迟启动图标动画
        javafx.animation.Timeline iconDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600), e -> scaleAnimation.play())
        );
        iconDelay.play();
    }


    // 修改：createGameBoardWithStatus 方法 - 修复时钟图标右边矩形方框问题
    private VBox createGameBoardWithStatus() {
        VBox wrapper = new VBox(10);
        wrapper.setAlignment(Pos.CENTER);

        // 棋盘上方的状态信息行
        HBox statusRow = new HBox();
        statusRow.setAlignment(Pos.CENTER);
        statusRow.setPadding(new Insets(10, 0, 10, 0));



        // 棋盘本体
        StackPane gameBoardPane = createGameBoard();
        this.gameBoard = (GridPane) gameBoardPane.getChildren().get(1);

        wrapper.getChildren().addAll(statusRow, gameBoardPane);

        return wrapper;
    }



    public void restoreGame(List<Block> savedBlocks, int savedMoveCount, String savedElapsedTime, List<String> savedHistoryStack, String time) {
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

    // 修改：updateTimer 方法 - 添加失败界面调用
    private void updateTimer() {
        if (isTimed) {
            remainSeconds--;
            int minutes = remainSeconds / 60;
            int seconds = remainSeconds % 60;
            timerLabel.setText(String.format("倒计时: %02d:%02d", minutes, seconds));
            if (remainSeconds <= 0) {
                timer.stop();
                // 修改：调用失败界面而不是简单的弹窗
                showFailDialog("时间耗尽", "很遗憾，挑战时间已用完！");
            }
        } else {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsed / 60;
            long seconds = elapsed % 60;
            timerLabel.setText(String.format("用时: %02d:%02d", minutes, seconds));
        }
    }

    // 新增：创建失败画面内容
    private HBox createFailureContent(String failReason, String failMessage, String layoutName, int moveCount, String elapsedTime) {
        HBox content = new HBox(40);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30, 40, 30, 40));

        // 左侧区域：失败信息和统计
        VBox leftSection = new VBox(20);
        leftSection.setAlignment(Pos.CENTER);
        leftSection.setPrefWidth(400);
        leftSection.setMinWidth(350);
        leftSection.setMaxWidth(450);

        // 失败标题区域
        VBox titleArea = createFailureTitleArea(failReason, failMessage);

        // 失败统计区域
        VBox statsArea = createFailureStatsArea(layoutName, moveCount, elapsedTime);

        leftSection.getChildren().addAll(titleArea, statsArea);

        // 右侧区域：建议和按钮
        VBox rightSection = new VBox(25);
        rightSection.setAlignment(Pos.CENTER);
        rightSection.setPrefWidth(450);
        rightSection.setMinWidth(400);
        rightSection.setMaxWidth(500);

        // 失败建议区域
        VBox suggestionArea = createFailureSuggestionArea(failReason, moveCount);

        // 操作按钮区域
        HBox buttonArea = createFailureButtonArea();

        rightSection.getChildren().addAll(suggestionArea, buttonArea);

        content.getChildren().addAll(leftSection, rightSection);

        return content;
    }

    // 新增：创建失败标题区域
    private VBox createFailureTitleArea(String failReason, String failMessage) {
        VBox titleArea = new VBox(15);
        titleArea.setAlignment(Pos.CENTER);

        // 主标题 - 使用失败图标
        Label failIcon = new Label("❌");
        failIcon.setFont(Font.font("微软雅黑", 64));
        failIcon.getStyleClass().add("failure-main-icon");
        failIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label failTitle = new Label("挑战失败");
        failTitle.setFont(Font.font("微软雅黑", 32));
        failTitle.getStyleClass().add("failure-main-title");

        Label failSubtitle = new Label(failReason);
        failSubtitle.setFont(Font.font("微软雅黑", 18));
        failSubtitle.getStyleClass().add("failure-subtitle");

        Label failDesc = new Label(failMessage);
        failDesc.setFont(Font.font("微软雅黑", 14));
        failDesc.getStyleClass().add("failure-description");
        failDesc.setWrapText(true);
        failDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        titleArea.getChildren().addAll(failIcon, failTitle, failSubtitle, failDesc);

        return titleArea;
    }

    // 新增：创建失败统计区域
    private VBox createFailureStatsArea(String layoutName, int moveCount, String elapsedTime) {
        VBox statsArea = new VBox(18);
        statsArea.setAlignment(Pos.CENTER);
        statsArea.setPadding(new Insets(20, 25, 20, 25));
        statsArea.getStyleClass().add("failure-stats-container");

        // 统计标题
        Label statsTitle = new Label("📊 本局统计");
        statsTitle.setFont(Font.font("微软雅黑", 18));
        statsTitle.getStyleClass().add("failure-stats-title");

        // 统计信息网格
        GridPane statsGrid = new GridPane();
        statsGrid.setAlignment(Pos.CENTER);
        statsGrid.setHgap(30);
        statsGrid.setVgap(12);
        statsGrid.setPadding(new Insets(12, 0, 12, 0));

        // 布局信息
        VBox layoutInfo = createFailureStatItem("🎯", "挑战布局", layoutName);

        // 步数信息
        VBox moveInfo = createFailureStatItem("👣", "已移动", moveCount + " 步");

        // 用时信息
        VBox timeInfo = createFailureStatItem("⏱️", "用时", elapsedTime);

        // 完成度评估
        VBox progressInfo = createProgressEvaluation(moveCount);

        statsGrid.add(layoutInfo, 0, 0);
        statsGrid.add(moveInfo, 1, 0);
        statsGrid.add(timeInfo, 0, 1);
        statsGrid.add(progressInfo, 1, 1);

        statsArea.getChildren().addAll(statsTitle, statsGrid);

        return statsArea;
    }

    // 新增：创建失败统计项目
    private VBox createFailureStatItem(String icon, String label, String value) {
        VBox statItem = new VBox(8);
        statItem.setAlignment(Pos.CENTER);
        statItem.setPrefWidth(120);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("微软雅黑", 20));
        iconLabel.getStyleClass().add("failure-stat-icon");
        iconLabel.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label labelText = new Label(label);
        labelText.setFont(Font.font("微软雅黑", 12));
        labelText.getStyleClass().add("failure-stat-label");

        Label valueText = new Label(value);
        valueText.setFont(Font.font("微软雅黑", 14));
        valueText.getStyleClass().add("failure-stat-value");

        statItem.getChildren().addAll(iconLabel, labelText, valueText);

        return statItem;
    }

    // 新增：创建完成度评估
    private VBox createProgressEvaluation(int moveCount) {
        VBox progressItem = new VBox(8);
        progressItem.setAlignment(Pos.CENTER);
        progressItem.setPrefWidth(120);

        String progress;
        String progressIcon;
        String progressClass;

        // 根据步数估算完成度
        if (moveCount <= 20) {
            progress = "起步阶段";
            progressIcon = "🌱";
            progressClass = "failure-progress-start";
        } else if (moveCount <= 50) {
            progress = "初有进展";
            progressIcon = "🌿";
            progressClass = "failure-progress-early";
        } else if (moveCount <= 80) {
            progress = "努力探索";
            progressIcon = "🌳";
            progressClass = "failure-progress-middle";
        } else {
            progress = "接近目标";
            progressIcon = "🎯";
            progressClass = "failure-progress-near";
        }

        Label iconLabel = new Label(progressIcon);
        iconLabel.setFont(Font.font("微软雅黑", 20));
        iconLabel.getStyleClass().add("failure-stat-icon");
        iconLabel.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label labelText = new Label("完成度");
        labelText.setFont(Font.font("微软雅黑", 12));
        labelText.getStyleClass().add("failure-stat-label");

        Label valueText = new Label(progress);
        valueText.setFont(Font.font("微软雅黑", 14));
        valueText.getStyleClass().addAll("failure-stat-value", progressClass);

        progressItem.getChildren().addAll(iconLabel, labelText, valueText);

        return progressItem;
    }

    // 新增：创建失败建议区域
    private VBox createFailureSuggestionArea(String failReason, int moveCount) {
        VBox suggestionArea = new VBox(15);
        suggestionArea.setAlignment(Pos.CENTER);
        suggestionArea.setPadding(new Insets(20, 25, 20, 25));
        suggestionArea.getStyleClass().add("failure-suggestion-container");

        Label suggestionTitle = new Label("💡 改进建议");
        suggestionTitle.setFont(Font.font("微软雅黑", 18));
        suggestionTitle.getStyleClass().add("failure-suggestion-title");

        VBox suggestionList = new VBox(12);
        suggestionList.setAlignment(Pos.CENTER_LEFT);

        // 根据失败原因和步数给出不同建议
        if (failReason.contains("时间")) {
            suggestionList.getChildren().addAll(
                    createSuggestionItem("⏰", "合理规划时间", "限时模式需要快速决策"),
                    createSuggestionItem("🎯", "先找关键路径", "优先移动关键方块"),
                    createSuggestionItem("🧠", "多练习布局", "熟悉各种布局套路")
            );
        } else {
            if (moveCount <= 30) {
                suggestionList.getChildren().addAll(
                        createSuggestionItem("🤔", "仔细观察布局", "分析方块移动规律"),
                        createSuggestionItem("📚", "学习基础技巧", "掌握华容道基本解法"),
                        createSuggestionItem("💪", "坚持练习", "多尝试不同移动方案")
                );
            } else {
                suggestionList.getChildren().addAll(
                        createSuggestionItem("🔄", "尝试其他路线", "当前路线可能有误"),
                        createSuggestionItem("↶", "善用撤销功能", "回退到关键节点重试"),
                        createSuggestionItem("🤖", "考虑AI帮助", "观察AI演示学习技巧")
                );
            }
        }

        // 鼓励信息
        VBox encouragementArea = new VBox(8);
        encouragementArea.setAlignment(Pos.CENTER);
        encouragementArea.setPadding(new Insets(15, 0, 0, 0));

        Label encouragementIcon = new Label("🌟");
        encouragementIcon.setFont(Font.font("微软雅黑", 24));
        encouragementIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        Label encouragementText = new Label("失败是成功之母，继续加油！");
        encouragementText.setFont(Font.font("微软雅黑", 16));
        encouragementText.getStyleClass().add("failure-encouragement");
        encouragementText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        encouragementArea.getChildren().addAll(encouragementIcon, encouragementText);

        suggestionArea.getChildren().addAll(suggestionTitle, suggestionList, encouragementArea);

        return suggestionArea;
    }

    // 新增：创建建议项目
    private HBox createSuggestionItem(String icon, String title, String description) {
        HBox suggestionItem = new HBox(12);
        suggestionItem.setAlignment(Pos.CENTER_LEFT);
        suggestionItem.setPadding(new Insets(8, 15, 8, 15));
        suggestionItem.getStyleClass().add("failure-suggestion-item");

        Label suggestionIcon = new Label(icon);
        suggestionIcon.setFont(Font.font("微软雅黑", 18));
        suggestionIcon.getStyleClass().add("failure-suggestion-icon");
        suggestionIcon.setStyle("-fx-text-fill: transparent; -fx-background-color: transparent;");

        VBox suggestionText = new VBox(2);
        suggestionText.setAlignment(Pos.CENTER_LEFT);

        Label suggestionTitle = new Label(title);
        suggestionTitle.setFont(Font.font("微软雅黑", 14));
        suggestionTitle.getStyleClass().add("failure-suggestion-name");

        Label suggestionDesc = new Label(description);
        suggestionDesc.setFont(Font.font("微软雅黑", 12));
        suggestionDesc.getStyleClass().add("failure-suggestion-desc");

        suggestionText.getChildren().addAll(suggestionTitle, suggestionDesc);

        suggestionItem.getChildren().addAll(suggestionIcon, suggestionText);

        return suggestionItem;
    }

    // 新增：启动失败动画
    private void startFailureAnimations(HBox failContent) {
        // 为失败内容添加淡入动画
        failContent.setOpacity(0);

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), failContent);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // 为左侧区域添加从左滑入动画
        VBox leftSection = (VBox) failContent.getChildren().get(0);
        leftSection.setTranslateX(-100);
        leftSection.setOpacity(0);

        javafx.animation.TranslateTransition leftSlideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(600), leftSection);
        leftSlideIn.setFromX(-100);
        leftSlideIn.setToX(0);

        javafx.animation.FadeTransition leftFadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), leftSection);
        leftFadeIn.setFromValue(0);
        leftFadeIn.setToValue(1);

        javafx.animation.ParallelTransition leftAnimation = new javafx.animation.ParallelTransition(leftSlideIn, leftFadeIn);

        // 为右侧区域添加从右滑入动画
        VBox rightSection = (VBox) failContent.getChildren().get(1);
        rightSection.setTranslateX(100);
        rightSection.setOpacity(0);

        javafx.animation.TranslateTransition rightSlideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(600), rightSection);
        rightSlideIn.setFromX(100);
        rightSlideIn.setToX(0);

        javafx.animation.FadeTransition rightFadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), rightSection);
        rightFadeIn.setFromValue(0);
        rightFadeIn.setToValue(1);

        javafx.animation.ParallelTransition rightAnimation = new javafx.animation.ParallelTransition(rightSlideIn, rightFadeIn);

        // 延迟启动左右区域动画
        javafx.animation.Timeline leftDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> leftAnimation.play())
        );
        leftDelay.play();

        javafx.animation.Timeline rightDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400), e -> rightAnimation.play())
        );
        rightDelay.play();

        // 为主图标添加震动动画
        VBox titleArea = (VBox) leftSection.getChildren().get(0);
        Label failIcon = (Label) titleArea.getChildren().get(0);

        javafx.animation.TranslateTransition shakeAnimation = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(100), failIcon);
        shakeAnimation.setFromX(-5);
        shakeAnimation.setToX(5);
        shakeAnimation.setCycleCount(6);
        shakeAnimation.setAutoReverse(true);

        // 延迟启动震动动画
        javafx.animation.Timeline shakeDelay = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600), e -> shakeAnimation.play())
        );
        shakeDelay.play();
    }

    // 新增：手动触发失败界面的方法（可在其他地方调用）
    public void triggerFailure(String reason, String message) {
        showFailDialog(reason, message);
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
        if (isTimed) {
            remainSeconds = timeLimitSeconds;
            timerLabel.setText("倒计时: 05:00");
            timer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateTimer()));
            timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timer.play();
        } else {
            timerLabel.setText("用时: 00:00");
            timer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateTimer()));
            timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timer.play();
        }
    }

    public static class Block {
        public int row;
        public int col;
        public final int width;
        public final int height;
        public final Color color;
        public final String name;

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
        // 新增：AI帮解前弹窗并判断金币
        int coins = getUserCoins(userName);
        if (coins < 300) {
            showAlert("金币不足", null, "金币余额不足，请充值", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "是否花费300金币使用AI帮解功能？", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("AI帮解");
        confirm.setHeaderText("AI帮解功能");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }
        // 扣除金币
        try {
            MongoDBUtil db = new MongoDBUtil();
            db.getCollection("users").updateOne(
                    new org.bson.Document("username", userName),
                    new org.bson.Document("$inc", new org.bson.Document("coins", -300))
            );
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", null, "扣除金币失败", Alert.AlertType.ERROR);
            return;
        }
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

    // 新增：获取用户金币数量
    private int getUserCoins(String username) {
        int coins = 0;
        try {
            MongoDBUtil db = new MongoDBUtil();
            org.bson.Document userDoc = db.getUserByUsername(username);
            if (userDoc != null && userDoc.containsKey("coins")) {
                coins = userDoc.getInteger("coins", 0);
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coins;
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
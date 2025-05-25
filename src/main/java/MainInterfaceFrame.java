import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import org.bson.Document;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;
import org.bson.conversions.Bson;



public class MainInterfaceFrame {
    private Stage mainStage;
    private Label coinLabel;
    private String currentUsername;
    // 在类的开头添加音乐管理器引用
    private MusicManager musicManager = MusicManager.getInstance();

    // 预加载数据缓存
    private java.util.Map<String, List<ChatListRecord>> preloadedChatData = new java.util.HashMap<>();
    private java.util.Map<String, List<MailRecord>> preloadedFriendRequestData = new java.util.HashMap<>();

    // 重新添加：刷新金币的 Runnable
    private final Runnable refreshCoins = () -> {
        if (!"离线用户".equals(currentUsername)) {
            // 在后台线程中获取金币数量
            Thread coinThread = new Thread(() -> {
                try {
                    int coins = getUserCoins(currentUsername);
                    // 在JavaFX应用线程中更新UI
                    Platform.runLater(() -> {
                        if (coinLabel != null) {
                            coinLabel.setText("💰 " + coins + " 金币");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        if (coinLabel != null) {
                            coinLabel.setText("💰 -- 金币");
                        }
                    });
                }
            });
            coinThread.setDaemon(true);
            coinThread.start();
        } else {
            // 离线用户显示0金币
            if (coinLabel != null) {
                coinLabel.setText("💰 0 金币");
            }
        }
    };

    // 聊天列表记录类 - 增强版
    public static class ChatListRecord {
        private String friendName;
        private String lastMessage;
        private int unreadCount;
        private long lastMessageTime;

        public ChatListRecord(String friendName, String lastMessage, int unreadCount) {
            this.friendName = friendName;
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
            this.lastMessageTime = System.currentTimeMillis();
        }

        public ChatListRecord(String friendName, String lastMessage, int unreadCount, long lastMessageTime) {
            this.friendName = friendName;
            this.lastMessage = lastMessage;
            this.unreadCount = unreadCount;
            this.lastMessageTime = lastMessageTime;
        }

        public String getFriendName() { return friendName; }
        public String getLastMessage() { return lastMessage; }
        public int getUnreadCount() { return unreadCount; }
        public long getLastMessageTime() { return lastMessageTime; }
    }

    public void show(Stage loginStage, String username) {
        this.currentUsername = username; // 保存用户名

        // 启动主界面音乐
        try {
            musicManager.playMusic(MusicManager.MAIN_MENU);
        } catch (Exception e) {
            System.err.println("音乐播放失败: " + e.getMessage());
        }

        // loginStage 是登录界面的 Stage
        // 1. 创建一个新的 Stage 用于主界面
        this.mainStage = new Stage();
        this.mainStage.setTitle("华容道主界面");

        // 使用 ScrollPane 来支持任意大小调整
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        // 使用横向布局 - 左右分栏
        HBox mainLayout = new HBox(40);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getStyleClass().add("main-background");
        mainLayout.setMaxWidth(Double.MAX_VALUE);
        mainLayout.setMaxHeight(Double.MAX_VALUE);

        // 左侧区域 - 标题和主要功能
        VBox leftSection = new VBox(25);
        leftSection.setAlignment(Pos.CENTER);
        leftSection.setPrefWidth(400);
        leftSection.setMaxWidth(450);

        // 顶部用户信息区域 - 新布局
        HBox topSection = new HBox(15);
        topSection.setAlignment(Pos.CENTER_LEFT);
        topSection.setPadding(new Insets(0, 0, 20, 0));

        // 用户信息区域
        VBox userInfoBox = new VBox(5);
        userInfoBox.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLabel = new Label("欢迎回来");
        welcomeLabel.setFont(Font.font("微软雅黑", 14));
        welcomeLabel.getStyleClass().add("welcome-label");

        Label usernameLabel = new Label(username);
        usernameLabel.setFont(Font.font("微软雅黑", 20));
        usernameLabel.getStyleClass().add("username-label");

        userInfoBox.getChildren().addAll(welcomeLabel, usernameLabel);

        // 退出登录按钮 - 放在用户名右边
        Button logoutBtn = new Button("🚪 退出登录");
        logoutBtn.setFont(Font.font("微软雅黑", 10));
        logoutBtn.setPrefWidth(70);
        logoutBtn.setPrefHeight(25);
        logoutBtn.getStyleClass().add("logout-button");
        logoutBtn.setOnAction(e -> {
            // 显示高级确认对话框
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认退出");
            confirmAlert.setHeaderText("退出登录确认");
            confirmAlert.setContentText("您确定要退出当前登录状态吗？\n退出后将返回到登录界面。");

            // 修复：为对话框应用高级样式
            DialogPane dialogPane = confirmAlert.getDialogPane();
            dialogPane.getStyleClass().add("dialog-pane");
            dialogPane.getStyleClass().add("confirmation-dialog");

            // 设置对话框的最小尺寸
            dialogPane.setMinWidth(400);
            dialogPane.setPrefWidth(400);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 停止当前音乐
                try {
                    musicManager.stopMusic();
                } catch (Exception ex) {
                    System.err.println("停止音乐失败: " + ex.getMessage());
                }
                // 精确记录当前主界面的所有窗口状态
                double currentX = this.mainStage.getX();
                double currentY = this.mainStage.getY();
                double currentWidth = this.mainStage.getWidth();
                double currentHeight = this.mainStage.getHeight();
                boolean isMaximized = this.mainStage.isMaximized();

                // 关闭主界面
                this.mainStage.close();

                // 创建新的登录窗口，并设置完全相同的窗口状态
                Platform.runLater(() -> {
                    try {
                        LogInFrame loginFrame = new LogInFrame();
                        Stage newLoginStage = new Stage();

                        // 完全恢复主界面的窗口状态
                        newLoginStage.setX(currentX);
                        newLoginStage.setY(currentY);
                        newLoginStage.setWidth(currentWidth);
                        newLoginStage.setHeight(currentHeight);

                        // 设置最小尺寸限制
                        newLoginStage.setMinWidth(600);
                        newLoginStage.setMinHeight(400);

                        // 如果主界面是最大化的，登录界面也要最大化
                        if (isMaximized) {
                            newLoginStage.setMaximized(true);
                        }

                        // 显示登录界面，并传入登录成功后的回调
                        loginFrame.show(newLoginStage, loggedInUser -> {
                            // 登录成功后创建新的主界面
                            MainInterfaceFrame newMainFrame = new MainInterfaceFrame();
                            newMainFrame.show(newLoginStage, loggedInUser);
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        // 如果创建登录界面失败，显示高级错误信息
                        showAdvancedAlert("启动失败", "登录界面启动错误",
                                "无法创建登录界面，程序将退出。\n错误信息：" + ex.getMessage(),
                                Alert.AlertType.ERROR);
                        Platform.exit();
                    }
                });
                // 在新建登录界面后立即播放主界面音乐
                Platform.runLater(() -> {
                    try {
                        // 新登录界面也应该播放主界面音乐
                        musicManager.playMusic(MusicManager.MAIN_MENU);
                    } catch (Exception ex) {
                        System.err.println("启动登录界面音乐失败: " + ex.getMessage());
                    }
                });
            }
        });

        // 用弹性空间分隔用户信息和右边内容
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // 金币区域
        HBox coinArea = new HBox(8);
        coinArea.setAlignment(Pos.CENTER_RIGHT);

        this.coinLabel = new Label();
        coinLabel.setFont(Font.font("微软雅黑", 16));
        coinLabel.getStyleClass().add("coin-label");

        coinArea.getChildren().addAll(coinLabel);

        // 新的顶部布局 - 用户信息、退出按钮、弹性空间、金币区域
        topSection.getChildren().addAll(userInfoBox, logoutBtn, spacer1, coinArea);

        // 标题区域
        VBox titleSection = new VBox(10);
        titleSection.setAlignment(Pos.CENTER);
        titleSection.setPadding(new Insets(10, 0, 20, 0));

        Label titleIcon = new Label("🏯");
        titleIcon.setFont(Font.font("微软雅黑", 42));
        titleIcon.getStyleClass().add("title-icon"); // 添加样式类让emoji显示原色

        Label title = new Label("华容道");
        title.setFont(Font.font("微软雅黑", 36));
        title.getStyleClass().add("main-title");

        Label subtitle = new Label("经典益智解谜游戏");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("main-subtitle");

        titleSection.getChildren().addAll(titleIcon, title, subtitle);

        // 主要功能按钮区域
        VBox mainButtonSection = new VBox(15);
        mainButtonSection.setAlignment(Pos.CENTER);
        mainButtonSection.setPadding(new Insets(15, 0, 10, 0));

        // 开始游戏按钮（突出显示）
        Button startBtn = createPrimaryButton("🎮 开始游戏");
        startBtn.setOnAction(e -> startGame(this.mainStage, username));

        // 历史记录和排行榜按钮 - 横向排列
        HBox gameButtonsRow = new HBox(15);
        gameButtonsRow.setAlignment(Pos.CENTER);

        Button historyBtn = createSecondaryButton("📊 历史记录");
        historyBtn.setPrefWidth(180);
        historyBtn.setOnAction(e -> {
            if (!"离线用户".equals(username)) {
                showHistory(username);
            } else {
                showAlert("提示", "离线模式", "离线模式下无法查看历史记录", Alert.AlertType.INFORMATION);
            }
        });

        Button rankBtn = createSecondaryButton("🏆 游戏排行榜");
        rankBtn.setPrefWidth(180);
        rankBtn.setOnAction(e -> {
            if (!"离线用户".equals(username)) {
                showRank();
            } else {
                showAlert("提示", "离线模式", "离线模式下无法查看排行榜", Alert.AlertType.INFORMATION);
            }
        });

        gameButtonsRow.getChildren().addAll(historyBtn, rankBtn);

        mainButtonSection.getChildren().addAll(startBtn, gameButtonsRow);

        leftSection.getChildren().addAll(topSection, titleSection, mainButtonSection);

        // 右侧区域 - 社交功能
        VBox rightSection = new VBox(20);
        rightSection.setAlignment(Pos.CENTER);
        rightSection.setPrefWidth(350);
        rightSection.setMaxWidth(400);
        rightSection.setPadding(new Insets(20, 0, 0, 0));

        // 社交功能标题
        VBox socialTitleSection = new VBox(8);
        socialTitleSection.setAlignment(Pos.CENTER);

        Label socialIcon = new Label("👥");
        socialIcon.setFont(Font.font("微软雅黑", 32));
        socialIcon.getStyleClass().add("feature-icon"); // 添加样式类让emoji显示原色

        Label socialTitle = new Label("社交功能");
        socialTitle.setFont(Font.font("微软雅黑", 20));
        socialTitle.getStyleClass().add("section-title-large");

        socialTitleSection.getChildren().addAll(socialIcon, socialTitle);

        // 社交按钮网格 - 2x2布局
        GridPane socialGrid = new GridPane();
        socialGrid.setAlignment(Pos.CENTER);
        socialGrid.setHgap(15);
        socialGrid.setVgap(15);
        socialGrid.setPadding(new Insets(20, 0, 0, 0));

        // 创建社交功能卡片
        VBox mailboxCard = createSocialCard("📬", "信箱", e -> {
            if (!"离线用户".equals(username)) {
                showMailbox(username);
            } else {
                showAlert("提示", "离线模式", "离线模式下无法使用信箱功能", Alert.AlertType.INFORMATION);
            }
        });

        VBox friendsCard = createSocialCard("👥", "好友列表", e -> {
            if (!"离线用户".equals(username)) {
                showFriends(username);
            } else {
                showAlert("提示", "离线模式", "离线模式下无法查看好友列表", Alert.AlertType.INFORMATION);
            }
        });

        VBox addFriendCard = createSocialCard("➕", "加好友", e -> {
            if (!"离线用户".equals(username)) {
                addFriend(username);
            } else {
                showAlert("提示", "离线模式", "离线模式下无法添加好友", Alert.AlertType.INFORMATION);
            }
        });

        VBox watchCard = createSocialCard("👀", "在线观战", e -> {
            if (!"离线用户".equals(username)) {
                watchOnline(username);
            } else {
                showAlert("提示", "离线模式", "离线模式下无法观战", Alert.AlertType.INFORMATION);
            }
        });

        // 将卡片添加到网格中
        socialGrid.add(mailboxCard, 0, 0);
        socialGrid.add(friendsCard, 1, 0);
        socialGrid.add(addFriendCard, 0, 1);
        socialGrid.add(watchCard, 1, 1);

        // 音乐控制卡片
        VBox musicCard = createMusicControlCard();
        socialGrid.add(musicCard, 0, 2); // 添加到第三行第一列

        rightSection.getChildren().addAll(socialTitleSection, socialGrid);

        // 将左右两栏添加到主布局 - 移除底部区域
        mainLayout.getChildren().addAll(leftSection, rightSection);

        // 初始加载金币
        refreshCoins.run();

        scrollPane.setContent(mainLayout);

        // 2. 为新的主界面 Stage 设置 Scene
        Scene scene = new Scene(scrollPane);
        // 加载CSS样式
        loadCSS(scene);
        setupKeyboardShortcuts(scene);

        this.mainStage.setScene(scene);
        this.mainStage.setResizable(true);

        // 移除固定比例限制，设置与登录界面相同的最小尺寸
        this.mainStage.setMinWidth(600);   // 与登录界面保持一致的最小宽度
        this.mainStage.setMinHeight(400);  // 与登录界面保持一致的最小高度

        // 在主界面窗口设置中添加关闭事件处理
        this.mainStage.setOnCloseRequest(e -> {
            // 程序关闭时释放音乐资源
            try {
                musicManager.dispose();
            } catch (Exception ex) {
                System.err.println("音乐资源释放失败: " + ex.getMessage());
            }
            Platform.exit();
        });

        // 完全继承登录窗口的位置和大小，不进行任何比例调整
        if (loginStage.isShowing()) {
            // 精确复制登录窗口的所有尺寸属性
            this.mainStage.setX(loginStage.getX());
            this.mainStage.setY(loginStage.getY());
            this.mainStage.setWidth(loginStage.getWidth());
            this.mainStage.setHeight(loginStage.getHeight());

            // 如果登录窗口是最大化的，主界面也要最大化
            if (loginStage.isMaximized()) {
                this.mainStage.setMaximized(true);
            }
        } else {
            // 如果登录窗口已经关闭，使用默认大小
            this.mainStage.setWidth(900);
            this.mainStage.setHeight(600);
        }

        // 4. 显示新的主界面 Stage
        this.mainStage.show();

        // 5. 关闭登录界面的 Stage
        loginStage.close();

        // 当主界面窗口获得焦点，如果是在线模式就刷新金币
        this.mainStage.iconifiedProperty().addListener((obs, oldVal, newVal) -> {
            // 当窗口从最小化状态恢复时刷新金币（更安全的时机）
            if (!newVal && !"离线用户".equals(username)) {
                // 使用延迟执行避免影响UI布局
                Platform.runLater(() -> {
                    refreshCoins.run();
                });
            }
        });

        // 可选：添加窗口显示监听器作为补充
        this.mainStage.showingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !"离线用户".equals(username)) {
                // 延迟执行，避免在UI初始化时影响布局
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        refreshCoins.run();
                    });
                });
            }
        });

        // 可选：添加窗口显示监听器作为补充
        this.mainStage.showingProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && !"离线用户".equals(username)) {
                // 延迟执行，避免在UI初始化时影响布局
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        refreshCoins.run();
                    });
                });
            }
        });
    }

    // 修复：创建高级提示框方法
    private void showAdvancedAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // 应用高级样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");

        // 根据类型添加特殊样式
        switch (type) {
            case ERROR:
                dialogPane.getStyleClass().add("error-dialog");
                break;
            case WARNING:
                dialogPane.getStyleClass().add("warning-dialog");
                break;
            case INFORMATION:
                dialogPane.getStyleClass().add("info-dialog");
                break;
            case CONFIRMATION:
                dialogPane.getStyleClass().add("confirmation-dialog");
                break;
        }

        // 设置对话框的最小尺寸和字体
        dialogPane.setMinWidth(400);
        dialogPane.setPrefWidth(400);

        alert.showAndWait();
    }

    // 修改按钮创建方法 - 移除单独的退出登录按钮创建方法，因为现在直接在布局中创建
    private Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(380);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(55);
        btn.setFont(Font.font("微软雅黑", 18));
        btn.getStyleClass().add("primary-button");
        return btn;
    }

    private Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setPrefWidth(180);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(45);
        btn.setFont(Font.font("微软雅黑", 16));
        btn.getStyleClass().add("secondary-button");
        return btn;
    }

    // 创建社交功能卡片
    private VBox createSocialCard(String icon, String title, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15, 12, 15, 12));
        card.setPrefWidth(150);
        card.setPrefHeight(100);
        card.getStyleClass().add("social-card");

        // 图标
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("微软雅黑", 24));
        iconLabel.getStyleClass().add("feature-icon"); // 添加样式类让emoji显示原色

        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("微软雅黑", 14));
        titleLabel.getStyleClass().add("social-card-title");

        card.getChildren().addAll(iconLabel, titleLabel);

        // 添加点击事件
        card.setOnMouseClicked(e -> {
            if (action != null) {
                action.handle(new javafx.event.ActionEvent());
            }
        });

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("social-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("social-card-hover"));

        return card;
    }

    // 在 createSocialCard 方法后添加音乐控制卡片创建方法
    private VBox createMusicControlCard() {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15, 12, 15, 12));
        card.setPrefWidth(150);
        card.setPrefHeight(100);
        card.getStyleClass().add("social-card");

        // 音乐图标
        Label iconLabel = new Label("🎵");
        iconLabel.setFont(Font.font("微软雅黑", 24));
        iconLabel.getStyleClass().add("feature-icon");

        // 标题
        Label titleLabel = new Label("音乐控制");
        titleLabel.setFont(Font.font("微软雅黑", 14));
        titleLabel.getStyleClass().add("social-card-title");

        card.getChildren().addAll(iconLabel, titleLabel);

        // 添加点击事件
        card.setOnMouseClicked(e -> showMusicControlDialog());

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("social-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("social-card-hover"));

        return card;
    }

    // 音乐控制对话框
    private void showMusicControlDialog() {
        Stage musicStage = new Stage();
        musicStage.setTitle("音乐控制");
        musicStage.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 标题区域
        VBox titleArea = new VBox(10);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("🎵");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("音乐控制");
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("调整背景音乐设置");
        subtitle.setFont(Font.font("微软雅黑", 14));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 音乐控制面板
        VBox controlPanel = createMusicControlPanel();

        root.getChildren().addAll(titleArea, controlPanel);

        Scene scene = new Scene(root, 400, 350);
        loadCSS(scene);
        musicStage.setScene(scene);
        musicStage.show();
    }

    // 创建音乐控制面板
    private VBox createMusicControlPanel() {
        VBox musicPanel = new VBox(20);
        musicPanel.setAlignment(Pos.CENTER);
        musicPanel.setPadding(new Insets(20));
        musicPanel.getStyleClass().add("music-control-panel");

        // 音乐开关
        HBox musicToggleBox = new HBox(15);
        musicToggleBox.setAlignment(Pos.CENTER);

        Label musicToggleLabel = new Label("背景音乐:");
        musicToggleLabel.setFont(Font.font("微软雅黑", 16));

        Button musicToggleButton = new Button(musicManager.isMusicEnabled() ? "🔊 开启" : "🔇 关闭");
        musicToggleButton.setPrefWidth(100);
        musicToggleButton.setPrefHeight(35);
        musicToggleButton.setFont(Font.font("微软雅黑", 14));
        musicToggleButton.getStyleClass().add("music-toggle-button");
        musicToggleButton.setOnAction(e -> {
            musicManager.toggleMusic();
            musicToggleButton.setText(musicManager.isMusicEnabled() ? "🔊 开启" : "🔇 关闭");

            if (musicManager.isMusicEnabled()) {
                musicManager.playMusic(MusicManager.MAIN_MENU);
            }
        });

        musicToggleBox.getChildren().addAll(musicToggleLabel, musicToggleButton);

        // 音量控制
        HBox volumeBox = new HBox(15);
        volumeBox.setAlignment(Pos.CENTER);

        Label volumeLabel = new Label("音量:");
        volumeLabel.setFont(Font.font("微软雅黑", 16));

        Slider volumeSlider = new Slider(0, 1, musicManager.getVolume());
        volumeSlider.setPrefWidth(200);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setShowTickMarks(false);

        Label volumeValueLabel = new Label(Math.round(musicManager.getVolume() * 100) + "%");
        volumeValueLabel.setFont(Font.font("微软雅黑", 14));
        volumeValueLabel.setPrefWidth(50);

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            musicManager.setVolume(newVal.doubleValue());
            volumeValueLabel.setText(Math.round(newVal.doubleValue() * 100) + "%");
        });

        volumeBox.getChildren().addAll(volumeLabel, volumeSlider, volumeValueLabel);

        // 快捷操作按钮
        HBox quickButtonsBox = new HBox(10);
        quickButtonsBox.setAlignment(Pos.CENTER);

        Button volumeUpBtn = new Button("🔊 +");
        volumeUpBtn.setPrefWidth(60);
        volumeUpBtn.setPrefHeight(30);
        volumeUpBtn.getStyleClass().add("volume-button");
        volumeUpBtn.setOnAction(e -> {
            double newVolume = Math.min(1.0, musicManager.getVolume() + 0.1);
            musicManager.setVolume(newVolume);
            volumeSlider.setValue(newVolume);
            volumeValueLabel.setText(Math.round(newVolume * 100) + "%");
        });

        Button volumeDownBtn = new Button("🔉 -");
        volumeDownBtn.setPrefWidth(60);
        volumeDownBtn.setPrefHeight(30);
        volumeDownBtn.getStyleClass().add("volume-button");
        volumeDownBtn.setOnAction(e -> {
            double newVolume = Math.max(0.0, musicManager.getVolume() - 0.1);
            musicManager.setVolume(newVolume);
            volumeSlider.setValue(newVolume);
            volumeValueLabel.setText(Math.round(newVolume * 100) + "%");
        });

        quickButtonsBox.getChildren().addAll(volumeDownBtn, volumeUpBtn);

        musicPanel.getChildren().addAll(musicToggleBox, volumeBox, quickButtonsBox);

        return musicPanel;
    }

    // startGame 方法 - 添加返回主界面按钮
    private void startGame(Stage parent, String username) {
        // 停止主界面音乐
        try {
            musicManager.stopMusic();
        } catch (Exception e) {
            System.err.println("停止音乐失败: " + e.getMessage());
        }

        Stage layoutStage = new Stage();
        layoutStage.setTitle("选择布局");
        layoutStage.setResizable(true);

        // 窗口同步绑定（保持原有逻辑）
        layoutStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - parent.getWidth()) > 2) {
                parent.setWidth(newVal.doubleValue());
            }
        });
        parent.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getWidth()) > 2) {
                layoutStage.setWidth(newVal.doubleValue());
            }
        });
        layoutStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - parent.getHeight()) > 2) {
                parent.setHeight(newVal.doubleValue());
            }
        });
        parent.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getHeight()) > 2) {
                layoutStage.setHeight(newVal.doubleValue());
            }
        });
        layoutStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - parent.getX()) > 2) {
                parent.setX(newVal.doubleValue());
            }
        });
        parent.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getX()) > 2) {
                layoutStage.setX(newVal.doubleValue());
            }
        });
        layoutStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - parent.getY()) > 2) {
                parent.setY(newVal.doubleValue());
            }
        });
        parent.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getY()) > 2) {
                layoutStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        layoutStage.setX(parent.getX());
        layoutStage.setY(parent.getY());
        layoutStage.setWidth(parent.getWidth());
        layoutStage.setHeight(parent.getHeight());

        // 修复：创建主容器，不设置固定高度限制
        VBox root = new VBox(20); // 稍微减少间距
        root.setPadding(new Insets(30, 48, 30, 48)); // 调整边距
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");
        // 修复：移除任何高度限制，让内容自然展开
        root.setMaxHeight(Region.USE_COMPUTED_SIZE);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 返回按钮区域
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            layoutStage.close();
            // 返回到主界面
            if (!parent.isShowing()) {
                parent.show();
            }
            parent.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域 - 增加图标和描述
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("🎮");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("请选择华容道布局");
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("选择您想要挑战的布局难度");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        List<String> layoutNames = BoardLayouts.getLayoutNames();

        // 修复：使用GridPane实现两列布局，确保正确的尺寸计算
        GridPane layoutGrid = new GridPane();
        layoutGrid.setAlignment(Pos.CENTER);
        layoutGrid.setHgap(30); // 列间距
        layoutGrid.setVgap(20); // 行间距
        layoutGrid.setPadding(new Insets(20, 0, 20, 0)); // 增加底部边距
        // 修复：确保GridPane能够自适应内容大小
        layoutGrid.setMaxHeight(Region.USE_COMPUTED_SIZE);
        layoutGrid.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 修复：按两列排列布局选项
        for (int i = 0; i < layoutNames.size(); i++) {
            String layout = layoutNames.get(i);
            int layoutIndex = i;

            // 创建包含预览和按钮的容器
            VBox layoutContainer = new VBox(15);
            layoutContainer.setAlignment(Pos.CENTER);
            layoutContainer.getStyleClass().add("layout-container");
            layoutContainer.setPrefWidth(350); // 设置固定宽度，适应两列显示
            layoutContainer.setMaxWidth(350);
            // 修复：设置合适的高度，确保预览图和按钮都能显示
            layoutContainer.setPrefHeight(280);
            layoutContainer.setMaxHeight(280);

            // 创建小型棋盘预览
            GridPane previewBoard = createPreviewBoard(layoutIndex);

            // 创建布局信息容器
            VBox infoBox = new VBox(10);
            infoBox.setAlignment(Pos.CENTER);

            Label layoutNameLabel = new Label(layout);
            layoutNameLabel.setFont(Font.font("微软雅黑", 18));
            layoutNameLabel.getStyleClass().add("sub-title");

            Button selectBtn = new Button("选择此布局");
            selectBtn.setPrefWidth(150);
            selectBtn.setPrefHeight(35);
            selectBtn.setFont(Font.font("微软雅黑", 14));
            selectBtn.getStyleClass().add("layout-select-button");

            selectBtn.setOnAction(e -> {
                // 立即关闭布局选择窗口
                layoutStage.close();

                // 使用Platform.runLater确保UI更新在JavaFX应用线程中执行
                javafx.application.Platform.runLater(() -> {
                    // 只显示新的模式选择界面，移除旧的Alert对话框
                    showGameModeSelection(layoutIndex, username, layoutStage);
                });
            });

            infoBox.getChildren().addAll(layoutNameLabel, selectBtn);
            layoutContainer.getChildren().addAll(previewBoard, infoBox);

            // 计算行列位置，实现两列布局
            int row = i / 2; // 行号
            int col = i % 2; // 列号（0或1）

            layoutGrid.add(layoutContainer, col, row);
        }

        // 将返回按钮添加到根容器的最前面
        root.getChildren().addAll(headerBox, titleArea, layoutGrid);

        // 修复：使用 ScrollPane 包装内容，确保正确的滚动配置
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false); // 修复：设置为false，允许垂直滚动
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // 修复：确保垂直滚动条可用
        scrollPane.getStyleClass().add("main-scroll-pane");

        // 修复：设置ScrollPane的最小和首选尺寸
        scrollPane.setMinHeight(400);
        scrollPane.setPrefHeight(600);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = new Scene(scrollPane);
        // 加载CSS样式
        loadCSS(scene);

        layoutStage.setScene(scene);
        layoutStage.show();
    }

    private void showGameModeSelection(int layoutIndex, String username, Stage previousStage) {
        Stage modeStage = new Stage();
        modeStage.setTitle("选择游戏模式");
        modeStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        modeStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - modeStage.getWidth()) > 2) {
                modeStage.setWidth(newVal.doubleValue());
            }
        });
        modeStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - modeStage.getHeight()) > 2) {
                modeStage.setHeight(newVal.doubleValue());
            }
        });
        modeStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - modeStage.getX()) > 2) {
                modeStage.setX(newVal.doubleValue());
            }
        });
        modeStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - modeStage.getY()) > 2) {
                modeStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步窗口位置和大小
        modeStage.setX(mainStage.getX());
        modeStage.setY(mainStage.getY());
        modeStage.setWidth(mainStage.getWidth());
        modeStage.setHeight(mainStage.getHeight());

        // 创建主容器 - 与布局选择界面完全一致
        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 48, 30, 48));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");
        root.setMaxHeight(Region.USE_COMPUTED_SIZE);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 返回按钮区域 - 与布局选择界面一致
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            modeStage.close();
            // 返回到布局选择界面时恢复主界面音乐
            try {
                musicManager.playMusic(MusicManager.MAIN_MENU);
            } catch (Exception ex) {
                System.err.println("恢复主界面音乐失败: " + ex.getMessage());
            }
            startGame(mainStage, username);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域 - 与布局选择界面一致
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("🎮");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("请选择游戏模式");
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("选择适合您的游戏模式");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 模式选择区域 - 使用与布局选择相同的两列设计
        HBox modeOptionsBox = new HBox(50);
        modeOptionsBox.setAlignment(Pos.CENTER);
        modeOptionsBox.setPadding(new Insets(30, 0, 30, 0));

        // 普通模式选项 - 左侧
        VBox normalModeContainer = new VBox(20);
        normalModeContainer.setAlignment(Pos.CENTER);
        normalModeContainer.getStyleClass().add("layout-container"); // 复用布局容器样式
        normalModeContainer.setPrefWidth(350);
        normalModeContainer.setMaxWidth(350);
        normalModeContainer.setPrefHeight(280);
        normalModeContainer.setMaxHeight(280);

        // 普通模式图标
        Label normalIcon = new Label("🎯");
        normalIcon.setFont(Font.font("微软雅黑", 48));
        normalIcon.getStyleClass().add("feature-icon");

        Label normalModeLabel = new Label("普通模式");
        normalModeLabel.setFont(Font.font("微软雅黑", 24));
        normalModeLabel.getStyleClass().add("sub-title");

        Label normalModeDesc = new Label("经典华容道游戏体验\n没有时间限制，专注于策略思考\n适合初学者和休闲玩家");
        normalModeDesc.setFont(Font.font("微软雅黑", 14));
        normalModeDesc.getStyleClass().add("mailbox-subtitle");
        normalModeDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        normalModeDesc.setWrapText(true);

        Button normalModeBtn = new Button("🎯 选择普通模式");
        normalModeBtn.setPrefWidth(200);
        normalModeBtn.setPrefHeight(45);
        normalModeBtn.setFont(Font.font("微软雅黑", 16));
        normalModeBtn.getStyleClass().add("layout-select-button"); // 复用布局选择按钮样式

        normalModeBtn.setOnAction(e -> {
            // 先打开游戏，再关闭模式选择窗口，减少卡顿
            Platform.runLater(() -> {
                // 打开普通模式游戏
                GameFrame gameFrame = new GameFrame();
                gameFrame.setCurrentLayoutIndex(layoutIndex);
                Stage gameStage = new Stage();
                gameFrame.show(gameStage, username, false, mainStage, false);
                mainStage.hide();

                // 延迟关闭模式选择窗口，确保游戏窗口完全打开
                Platform.runLater(() -> {
                    modeStage.close();
                });
            });
        });

        normalModeContainer.getChildren().addAll(normalIcon, normalModeLabel, normalModeDesc, normalModeBtn);

        // 限时模式选项 - 右侧
        VBox timedModeContainer = new VBox(20);
        timedModeContainer.setAlignment(Pos.CENTER);
        timedModeContainer.getStyleClass().add("layout-container"); // 复用布局容器样式
        timedModeContainer.setPrefWidth(350);
        timedModeContainer.setMaxWidth(350);
        timedModeContainer.setPrefHeight(280);
        timedModeContainer.setMaxHeight(280);

        // 限时模式图标
        Label timedIcon = new Label("⚡");
        timedIcon.setFont(Font.font("微软雅黑", 48));
        timedIcon.getStyleClass().add("feature-icon");

        Label timedModeLabel = new Label("限时模式");
        timedModeLabel.setFont(Font.font("微软雅黑", 24));
        timedModeLabel.getStyleClass().add("sub-title");

        // 将限时模式提示直接集成到描述中，不再弹窗
        Label timedModeDesc = new Label("挑战时间限制，体验刺激感\n⏰ 在规定时间内通关可获得金币奖励\n⚡ 适合有经验的玩家挑战");
        timedModeDesc.setFont(Font.font("微软雅黑", 14));
        timedModeDesc.getStyleClass().add("mailbox-subtitle");
        timedModeDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        timedModeDesc.setWrapText(true);

        Button timedModeBtn = new Button("⚡ 选择限时模式");
        timedModeBtn.setPrefWidth(200);
        timedModeBtn.setPrefHeight(45);
        timedModeBtn.setFont(Font.font("微软雅黑", 16));
        timedModeBtn.getStyleClass().add("layout-select-button"); // 复用布局选择按钮样式

        timedModeBtn.setOnAction(e -> {
            // 移除弹窗提示，直接打开游戏，减少卡顿
            Platform.runLater(() -> {
                // 打开限时模式游戏
                GameFrame gameFrame = new GameFrame();
                gameFrame.setCurrentLayoutIndex(layoutIndex);
                Stage gameStage = new Stage();
                gameFrame.show(gameStage, username, false, mainStage, true);
                mainStage.hide();

                // 延迟关闭模式选择窗口，确保游戏窗口完全打开
                Platform.runLater(() -> {
                    modeStage.close();
                });
            });
        });

        timedModeContainer.getChildren().addAll(timedIcon, timedModeLabel, timedModeDesc, timedModeBtn);



        // 将两个选项添加到水平布局中
        modeOptionsBox.getChildren().addAll(normalModeContainer, timedModeContainer);

        // 将所有组件添加到根容器
        root.getChildren().addAll(headerBox, titleArea, modeOptionsBox);

        // 使用 ScrollPane 包装内容 - 与布局选择界面一致
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        scrollPane.setMinHeight(400);
        scrollPane.setPrefHeight(600);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);

        modeStage.setScene(scene);
        modeStage.show();
    }

    // 创建小型棋盘预览的方法
    private GridPane createPreviewBoard(int layoutIndex) {
        GridPane previewGrid = new GridPane();

        // 精确计算尺寸：4列×32像素 + 2像素边框 = 130像素宽
        // 5行×32像素 + 2像素边框 = 162像素高
        previewGrid.setPrefSize(130, 162);
        previewGrid.setMaxSize(130, 162);
        previewGrid.setMinSize(130, 162);

        previewGrid.setPadding(new Insets(0)); // 移除内边距
        previewGrid.setHgap(0); // 设置水平间距为0
        previewGrid.setVgap(0); // 设置垂直间距为0
        previewGrid.setAlignment(Pos.TOP_LEFT); // 左上角对齐
        previewGrid.setStyle("-fx-background-color: white; -fx-border-color: #654321; -fx-border-width: 1;");

        // 创建5x4的网格背景
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 4; col++) {
                Rectangle cell = new Rectangle(32, 32);
                cell.setFill(Color.TRANSPARENT);
                cell.setStroke(Color.web("#654321"));
                cell.setStrokeWidth(0.5);
                previewGrid.add(cell, col, row);
            }
        }

        // 获取布局并绘制方块
        List<GameFrame.Block> blocks = BoardLayouts.getLayout(layoutIndex);
        for (GameFrame.Block block : blocks) {
            Rectangle blockRect = new Rectangle(block.getWidth() * 32, block.getHeight() * 32);
            blockRect.setFill(block.getColor());
            blockRect.setStroke(Color.BLACK);
            blockRect.setStrokeWidth(1);
            blockRect.setArcWidth(6);
            blockRect.setArcHeight(6);

            // 添加方块名称标签（如果是曹操则显示）
            if ("曹操".equals(block.getName())) {
                StackPane blockContainer = new StackPane();
                blockContainer.getChildren().add(blockRect);

                Label nameLabel = new Label(block.getName());
                nameLabel.setFont(Font.font("微软雅黑", 8));
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                blockContainer.getChildren().add(nameLabel);

                previewGrid.add(blockContainer, block.getCol(), block.getRow(), block.getWidth(), block.getHeight());
            } else {
                previewGrid.add(blockRect, block.getCol(), block.getRow(), block.getWidth(), block.getHeight());
            }
        }

        return previewGrid;
    }

    private void showHistory(String username) {
        Stage historyStage = new Stage();
        historyStage.setTitle("历史记录 - 选择布局");
        historyStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        historyStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - historyStage.getWidth()) > 2) {
                historyStage.setWidth(newVal.doubleValue());
            }
        });
        historyStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - historyStage.getHeight()) > 2) {
                historyStage.setHeight(newVal.doubleValue());
            }
        });
        historyStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - historyStage.getX()) > 2) {
                historyStage.setX(newVal.doubleValue());
            }
        });
        historyStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - historyStage.getY()) > 2) {
                historyStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        historyStage.setX(mainStage.getX());
        historyStage.setY(mainStage.getY());
        historyStage.setWidth(mainStage.getWidth());
        historyStage.setHeight(mainStage.getHeight());

        // 修复：创建主容器，确保正确的尺寸设置
        VBox root = new VBox(24);
        root.setPadding(new Insets(36, 48, 36, 48));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");
        // 修复：移除高度限制
        root.setMaxHeight(Region.USE_COMPUTED_SIZE);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);

        Label title = new Label("请选择要查看历史的布局");
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("section-title");

        List<String> layoutNames = BoardLayouts.getLayoutNames();

        // 修复：使用GridPane实现两列布局，确保正确尺寸
        GridPane layoutGrid = new GridPane();
        layoutGrid.setAlignment(Pos.CENTER);
        layoutGrid.setHgap(30); // 列间距
        layoutGrid.setVgap(20); // 行间距
        layoutGrid.setPadding(new Insets(20, 0, 20, 0));
        // 修复：确保GridPane自适应内容
        layoutGrid.setMaxHeight(Region.USE_COMPUTED_SIZE);
        layoutGrid.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 修复：按两列排列布局选项
        for (int i = 0; i < layoutNames.size(); i++) {
            String layout = layoutNames.get(i);
            int layoutIndex = i;

            // 创建包含预览和按钮的容器
            VBox layoutContainer = new VBox(15);
            layoutContainer.setAlignment(Pos.CENTER);
            layoutContainer.getStyleClass().add("layout-container");
            layoutContainer.setPrefWidth(350); // 设置固定宽度，适应两列显示
            layoutContainer.setMaxWidth(350);
            // 修复：设置合适的高度
            layoutContainer.setPrefHeight(280);
            layoutContainer.setMaxHeight(280);

            // 创建小型棋盘预览
            GridPane previewBoard = createPreviewBoard(layoutIndex);

            // 创建布局信息容器
            VBox infoBox = new VBox(10);
            infoBox.setAlignment(Pos.CENTER);

            Label layoutNameLabel = new Label(layout);
            layoutNameLabel.setFont(Font.font("微软雅黑", 18));
            layoutNameLabel.getStyleClass().add("sub-title");

            Button selectBtn = new Button("查看历史");
            selectBtn.setPrefWidth(150);
            selectBtn.setPrefHeight(35);
            selectBtn.setFont(Font.font("微软雅黑", 14));
            selectBtn.getStyleClass().add("layout-select-button");

            selectBtn.setOnAction(e -> {
                historyStage.close();
                showHistoryList(username, layout, historyStage);
            });

            infoBox.getChildren().addAll(layoutNameLabel, selectBtn);
            layoutContainer.getChildren().addAll(previewBoard, infoBox);

            // 计算行列位置，实现两列布局
            int row = i / 2; // 行号
            int col = i % 2; // 列号（0或1）

            layoutGrid.add(layoutContainer, col, row);
        }

        root.getChildren().addAll(title, layoutGrid);

        // 修复：使用 ScrollPane 包装内容，确保正确滚动
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false); // 修复：允许垂直滚动
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        // 修复：设置合适的滚动面板尺寸
        scrollPane.setMinHeight(400);
        scrollPane.setPrefHeight(600);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = new Scene(scrollPane);
        // 加载CSS样式
        loadCSS(scene);

        historyStage.setScene(scene);
        historyStage.show();
    }

    // showRankLayoutSelectionAndClose 方法 - 添加返回按钮
    private void showRankLayoutSelectionAndClose(String sortType, Stage previousStage) {
        Stage layoutStage = new Stage();
        layoutStage.setTitle("排行榜 - 选择布局");
        layoutStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        layoutStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getWidth()) > 2) {
                layoutStage.setWidth(newVal.doubleValue());
            }
        });
        layoutStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getHeight()) > 2) {
                layoutStage.setHeight(newVal.doubleValue());
            }
        });
        layoutStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getX()) > 2) {
                layoutStage.setX(newVal.doubleValue());
            }
        });
        layoutStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - layoutStage.getY()) > 2) {
                layoutStage.setY(newVal.doubleValue());
            }
        });

        // 继承前一个窗口的位置和大小
        if (previousStage != null) {
            layoutStage.setX(previousStage.getX());
            layoutStage.setY(previousStage.getY());
            layoutStage.setWidth(previousStage.getWidth());
            layoutStage.setHeight(previousStage.getHeight());
        } else {
            layoutStage.setX(mainStage.getX());
            layoutStage.setY(mainStage.getY());
            layoutStage.setWidth(mainStage.getWidth());
            layoutStage.setHeight(mainStage.getHeight());
        }

        // 修复：创建主容器，确保正确尺寸
        VBox root = new VBox(24);
        root.setPadding(new Insets(36, 48, 36, 48));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");
        // 修复：移除高度限制
        root.setMaxHeight(Region.USE_COMPUTED_SIZE);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 返回按钮区域
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            layoutStage.close();
            // 返回到排序方式选择界面
            showRank();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题 - 包含排序类型信息
        Label title = new Label("请选择棋盘样式 - " + sortType);
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("section-title");

        // 增强：添加说明文字
        Label subtitle = new Label("选择您想要查看" + sortType + "的布局");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        VBox titleArea = new VBox(8);
        titleArea.setAlignment(Pos.CENTER);
        titleArea.getChildren().addAll(title, subtitle);

        List<String> layoutNames = BoardLayouts.getLayoutNames();

        // 修复：使用GridPane实现两列布局
        GridPane layoutGrid = new GridPane();
        layoutGrid.setAlignment(Pos.CENTER);
        layoutGrid.setHgap(30); // 列间距
        layoutGrid.setVgap(20); // 行间距
        layoutGrid.setPadding(new Insets(20, 0, 20, 0));
        // 修复：确保GridPane自适应
        layoutGrid.setMaxHeight(Region.USE_COMPUTED_SIZE);
        layoutGrid.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 修复：按两列排列布局选项
        for (int i = 0; i < layoutNames.size(); i++) {
            String layout = layoutNames.get(i);
            int layoutIndex = i;

            // 创建包含预览和按钮的容器
            VBox layoutContainer = new VBox(15);
            layoutContainer.setAlignment(Pos.CENTER);
            layoutContainer.getStyleClass().add("layout-container");
            layoutContainer.setPrefWidth(350); // 设置固定宽度，适应两列显示
            layoutContainer.setMaxWidth(350);
            // 修复：设置合适的高度
            layoutContainer.setPrefHeight(280);
            layoutContainer.setMaxHeight(280);

            // 创建小型棋盘预览
            GridPane previewBoard = createPreviewBoard(layoutIndex);

            // 创建布局信息容器
            VBox infoBox = new VBox(10);
            infoBox.setAlignment(Pos.CENTER);

            Label layoutNameLabel = new Label(layout);
            layoutNameLabel.setFont(Font.font("微软雅黑", 18));
            layoutNameLabel.getStyleClass().add("sub-title");

            Button selectBtn = new Button("查看排行榜");
            selectBtn.setPrefWidth(150);
            selectBtn.setPrefHeight(35);
            selectBtn.setFont(Font.font("微软雅黑", 14));
            selectBtn.getStyleClass().add("layout-select-button");

            selectBtn.setOnAction(e -> {
                showRankTable(sortType, layout, layoutStage);
            });

            infoBox.getChildren().addAll(layoutNameLabel, selectBtn);
            layoutContainer.getChildren().addAll(previewBoard, infoBox);

            // 计算行列位置，实现两列布局
            int row = i / 2; // 行号
            int col = i % 2; // 列号（0或1）

            layoutGrid.add(layoutContainer, col, row);
        }

        // 将返回按钮添加到根容器的最前面
        root.getChildren().addAll(headerBox, titleArea, layoutGrid);

        // 修复：使用 ScrollPane 包装内容，确保正确滚动
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false); // 修复：允许垂直滚动
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        // 修复：设置合适的滚动面板尺寸
        scrollPane.setMinHeight(400);
        scrollPane.setPrefHeight(600);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        Scene scene = new Scene(scrollPane);
        // 加载CSS样式
        loadCSS(scene);

        layoutStage.setScene(scene);

        // 先显示新窗口，再关闭旧窗口，确保无缝切换
        layoutStage.show();

        // 使用Platform.runLater确保新窗口完全显示后再关闭旧窗口
        Platform.runLater(() -> {
            if (previousStage != null) {
                previousStage.close();
            }
        });
    }

    private void showHistoryList(String username, String layoutName, Stage parentStage) {
        Stage stage = new Stage();
        stage.setTitle(layoutName + " 历史记录");
        stage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - stage.getWidth()) > 2) {
                stage.setWidth(newVal.doubleValue());
            }
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - stage.getHeight()) > 2) {
                stage.setHeight(newVal.doubleValue());
            }
        });
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - stage.getX()) > 2) {
                stage.setX(newVal.doubleValue());
            }
        });
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - stage.getY()) > 2) {
                stage.setY(newVal.doubleValue());
            }
        });

        // 继承父窗口的位置和大小
        if (parentStage != null) {
            stage.setX(parentStage.getX());
            stage.setY(parentStage.getY());
            stage.setWidth(parentStage.getWidth());
            stage.setHeight(parentStage.getHeight());
        } else {
            stage.setX(mainStage.getX());
            stage.setY(mainStage.getY());
            stage.setWidth(mainStage.getWidth());
            stage.setHeight(mainStage.getHeight());
        }

        // 使用现代化的布局设计
        VBox root = new VBox(25);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 返回按钮区域
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            stage.close();
            // 返回到历史记录布局选择界面
            showHistory(username);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 现代化的标题区域
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("📊");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label(layoutName + " 历史记录");
        title.setFont(Font.font("微软雅黑", 28));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("查看您在此布局中的游戏记录");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 加载状态显示
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.getStyleClass().add("mailbox-progress");

        Label loadingLabel = new Label("正在加载历史记录...");
        loadingLabel.setFont(Font.font("微软雅黑", 16));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

        root.getChildren().addAll(headerBox, titleArea, loadingBox);

        // 异步加载历史数据
        loadHistoryDataAsync(username, layoutName, root, loadingBox, stage, parentStage);

        // 使用ScrollPane包装，保持与其他界面一致
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        stage.setScene(scene);
        stage.show();
    }

    // 创建历史记录空状态
    private VBox createHistoryEmptyState(String layoutName) {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));

        Label emptyIcon = new Label("📊");
        emptyIcon.setFont(Font.font("微软雅黑", 48));
        emptyIcon.getStyleClass().add("feature-icon");

        Label emptyTitle = new Label("暂无历史记录");
        emptyTitle.setFont(Font.font("微软雅黑", 20));
        emptyTitle.getStyleClass().add("empty-state-title");

        Label emptyMessage = new Label("您还没有在 " + layoutName + " 布局中游戏\n快去挑战一下吧！");
        emptyMessage.setFont(Font.font("微软雅黑", 14));
        emptyMessage.getStyleClass().add("empty-state-message");
        emptyMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        return emptyBox;
    }

    // 创建历史记录卡片列表
    private VBox createHistoryCards(String username, String layoutName, List<HistoryRecord> data, Stage currentStage, Stage parentStage) {
        VBox cardsContainer = new VBox(12);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        for (HistoryRecord record : data) {
            // 传递容器引用
            HBox historyCard = createHistoryCard(username, layoutName, record, currentStage, parentStage, cardsContainer);
            cardsContainer.getChildren().add(historyCard);
        }

        return cardsContainer;
    }

    // 创建单个历史记录卡片
    private HBox createHistoryCard(String username, String layoutName, HistoryRecord record, Stage currentStage, Stage parentStage, VBox cardsContainer) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setPrefWidth(700);
        card.setMaxWidth(700);

        // 根据游戏状态设置不同的样式
        if (record.isGameWon()) {
            card.getStyleClass().add("history-card-won");
        } else {
            card.getStyleClass().add("history-card-saved");
        }

        // 状态图标区域
        StackPane statusIcon = new StackPane();
        statusIcon.setPrefSize(60, 60);
        statusIcon.setMaxSize(60, 60);
        statusIcon.getStyleClass().add("history-status-icon");

        Label iconLabel = new Label(record.isGameWon() ? "🏆" : "💾");
        iconLabel.setFont(Font.font("微软雅黑", 28));
        iconLabel.getStyleClass().add("feature-icon");

        statusIcon.getChildren().add(iconLabel);

        // 游戏信息区域
        VBox gameInfo = new VBox(8);
        gameInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(gameInfo, Priority.ALWAYS);

        // 状态标题
        Label statusLabel = new Label(record.isGameWon() ? "已通关" : "游戏存档");
        statusLabel.setFont(Font.font("微软雅黑", 18));
        statusLabel.getStyleClass().add(record.isGameWon() ? "history-status-won" : "history-status-saved");

        // 游戏数据
        HBox gameDataRow = new HBox(25);
        gameDataRow.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label("⏰ " + record.getElapsedTime());
        timeLabel.setFont(Font.font("微软雅黑", 14));
        timeLabel.getStyleClass().add("history-data-label");

        Label stepLabel = new Label("👣 " + record.getMoveCount() + " 步");
        stepLabel.setFont(Font.font("微软雅黑", 14));
        stepLabel.getStyleClass().add("history-data-label");

        Label saveTimeLabel = new Label("📅 " + record.getSaveTime());
        saveTimeLabel.setFont(Font.font("微软雅黑", 14));
        saveTimeLabel.getStyleClass().add("history-data-label");

        gameDataRow.getChildren().addAll(timeLabel, stepLabel, saveTimeLabel);

        gameInfo.getChildren().addAll(statusLabel, gameDataRow);

        // 操作按钮区域
        HBox buttonArea = new HBox(12);
        buttonArea.setAlignment(Pos.CENTER_RIGHT);

        // 回放按钮（仅通关记录显示）
        if (record.isGameWon()) {
            Button replayBtn = new Button("🎬 回放");
            replayBtn.setPrefWidth(100);
            replayBtn.setPrefHeight(40);
            replayBtn.setFont(Font.font("微软雅黑", 14));
            replayBtn.getStyleClass().add("history-replay-button");
            replayBtn.setOnAction(e -> {
                playReplay(username, record, currentStage);
            });
            buttonArea.getChildren().add(replayBtn);
        }

        // 恢复游戏按钮（仅未通关记录显示）
        if (!record.isGameWon()) {
            Button restoreBtn = new Button("🎮 恢复");
            restoreBtn.setPrefWidth(100);
            restoreBtn.setPrefHeight(40);
            restoreBtn.setFont(Font.font("微软雅黑", 14));
            restoreBtn.getStyleClass().add("history-restore-button");
            restoreBtn.setOnAction(e -> {
                restoreGame(username, record, currentStage, parentStage);
            });
            buttonArea.getChildren().add(restoreBtn);
        }

        Button deleteBtn = new Button("🗑 删除");
        deleteBtn.setPrefWidth(100);
        deleteBtn.setPrefHeight(40);
        deleteBtn.setFont(Font.font("微软雅黑", 14));
        deleteBtn.getStyleClass().add("history-delete-button");
        deleteBtn.setOnAction(e -> {
            // 显示确认对话框
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认删除");
            confirmAlert.setHeaderText("删除存档");
            confirmAlert.setContentText("确定要删除这个存档吗？此操作不可撤销。");

            DialogPane dialogPane = confirmAlert.getDialogPane();
            dialogPane.getStyleClass().add("dialog-pane");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 修复：直接使用传入的容器引用
                deleteGameRecordWithUI(username, layoutName, record, card, cardsContainer);
            }
        });

        buttonArea.getChildren().add(deleteBtn);

        card.getChildren().addAll(statusIcon, gameInfo, buttonArea);

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("history-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("history-card-hover"));

        return card;
    }

    // 带UI更新的删除游戏记录方法
    private void deleteGameRecordWithUI(String username, String layoutName, HistoryRecord record, HBox card, VBox container) {
        Thread deleteThread = new Thread(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                dbManager.getCollection("game_history").deleteOne(
                        Filters.and(
                                Filters.eq("username", username),
                                Filters.eq("layout", layoutName),
                                Filters.eq("saveTime", record.getSaveTime())
                        )
                );

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    container.getChildren().remove(card);

                    // 如果删除后没有记录了，显示空状态
                    if (container.getChildren().isEmpty()) {
                        VBox emptyState = createHistoryEmptyState(layoutName);
                        // 找到包含container的父容器并替换内容
                        if (container.getParent() instanceof ScrollPane) {
                            ScrollPane scrollPane = (ScrollPane) container.getParent();
                            if (scrollPane.getParent() instanceof VBox) {
                                VBox parentContainer = (VBox) scrollPane.getParent();
                                int index = parentContainer.getChildren().indexOf(scrollPane);
                                parentContainer.getChildren().set(index, emptyState);
                            }
                        }
                    }

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("删除成功");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("存档已删除");
                    successAlert.getDialogPane().getStyleClass().add("dialog-pane");
                    successAlert.showAndWait();
                });

            } catch (Exception e) {
                ExceptionHandler.handleDatabaseException(e, "删除存档");
            }
        });

        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    public static class HistoryRecord {
        private String saveTime;
        private int moveCount;
        private String elapsedTime;
        private boolean gameWon;

        public HistoryRecord(String saveTime, int moveCount, String elapsedTime, boolean gameWon) {
            this.saveTime = saveTime;
            this.moveCount = moveCount;
            this.elapsedTime = elapsedTime;
            this.gameWon = gameWon;
        }
        public String getSaveTime() { return saveTime; }
        public int getMoveCount() { return moveCount; }
        public String getElapsedTime() { return elapsedTime; }
        public boolean isGameWon() { return gameWon; }
    }

    // 完整的showRank方法修改版本 - 添加返回主界面按钮
    private void showRank() {
        Stage rankStage = new Stage();
        rankStage.setTitle("排行榜 - 选择排序方式");
        rankStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        rankStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getWidth()) > 2) {
                rankStage.setWidth(newVal.doubleValue());
            }
        });
        rankStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getHeight()) > 2) {
                rankStage.setHeight(newVal.doubleValue());
            }
        });
        rankStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getX()) > 2) {
                rankStage.setX(newVal.doubleValue());
            }
        });
        rankStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getY()) > 2) {
                rankStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        rankStage.setX(mainStage.getX());
        rankStage.setY(mainStage.getY());
        rankStage.setWidth(mainStage.getWidth());
        rankStage.setHeight(mainStage.getHeight());

        VBox root = new VBox(35); // 稍微增加间距
        root.setPadding(new Insets(50, 80, 50, 80)); // 调整边距
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 返回按钮区域
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0)); // 增加底部边距

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(150);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            // 关闭当前窗口
            rankStage.close(); // 使用 rankStage 而不是 currentStage

            // 恢复主界面音乐
            try {
                musicManager.playMusic(MusicManager.MAIN_MENU);
            } catch (Exception ex) {
                System.err.println("恢复主界面音乐失败: " + ex.getMessage());
            }

            // 返回主界面
            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("🏆");
        titleIcon.setFont(Font.font("微软雅黑", 42));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("请选择排行榜排序方式");
        title.setFont(Font.font("微软雅黑", 28));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("选择您想要查看的排行榜类型");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 改为水平布局 - 左右对称分布
        HBox rankOptionsBox = new HBox(50); // 增加间距到50
        rankOptionsBox.setAlignment(Pos.CENTER);
        rankOptionsBox.setPadding(new Insets(30, 0, 30, 0));

        // 按步数排名选项 - 左侧
        VBox stepRankContainer = new VBox(20);
        stepRankContainer.setAlignment(Pos.CENTER);
        stepRankContainer.getStyleClass().add("rank-option-container");
        stepRankContainer.setPrefWidth(350); // 设置固定宽度确保对称
        stepRankContainer.setMaxWidth(350);
        stepRankContainer.setPrefHeight(280); // 设置固定高度
        stepRankContainer.setMaxHeight(280);

        // 步数排名图标
        Label stepIcon = new Label("👣");
        stepIcon.setFont(Font.font("微软雅黑", 48));
        stepIcon.getStyleClass().add("feature-icon");

        Label stepRankLabel = new Label("按步数排名");
        stepRankLabel.setFont(Font.font("微软雅黑", 24));
        stepRankLabel.getStyleClass().add("rank-option-title");

        Label stepRankDesc = new Label("显示用最少步数通关的玩家排行\n挑战最优解，看谁能用最少的步数完成游戏");
        stepRankDesc.setFont(Font.font("微软雅黑", 14));
        stepRankDesc.getStyleClass().add("rank-option-description");
        stepRankDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        stepRankDesc.setWrapText(true);

        Button stepRankBtn = new Button("🎯 查看步数排行榜");
        stepRankBtn.setPrefWidth(250);
        stepRankBtn.setPrefHeight(45);
        stepRankBtn.setFont(Font.font("微软雅黑", 16));
        stepRankBtn.getStyleClass().add("rank-option-button");

        stepRankBtn.setOnAction(e -> {
            showRankLayoutSelectionAndClose("按步数排名", rankStage);
        });

        stepRankContainer.getChildren().addAll(stepIcon, stepRankLabel, stepRankDesc, stepRankBtn);

        // 按用时排名选项 - 右侧
        VBox timeRankContainer = new VBox(20);
        timeRankContainer.setAlignment(Pos.CENTER);
        timeRankContainer.getStyleClass().add("rank-option-container");
        timeRankContainer.setPrefWidth(350); // 设置固定宽度确保对称
        timeRankContainer.setMaxWidth(350);
        timeRankContainer.setPrefHeight(280); // 设置固定高度
        timeRankContainer.setMaxHeight(280);

        // 用时排名图标
        Label timeIcon = new Label("⏱️");
        timeIcon.setFont(Font.font("微软雅黑", 48));
        timeIcon.getStyleClass().add("feature-icon");

        Label timeRankLabel = new Label("按用时排名");
        timeRankLabel.setFont(Font.font("微软雅黑", 24));
        timeRankLabel.getStyleClass().add("rank-option-title");

        Label timeRankDesc = new Label("显示用最短时间通关的玩家排行\n比拼速度，看谁能最快完成挑战");
        timeRankDesc.setFont(Font.font("微软雅黑", 14));
        timeRankDesc.getStyleClass().add("rank-option-description");
        timeRankDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        timeRankDesc.setWrapText(true);

        Button timeRankBtn = new Button("⚡ 查看用时排行榜");
        timeRankBtn.setPrefWidth(250);
        timeRankBtn.setPrefHeight(45);
        timeRankBtn.setFont(Font.font("微软雅黑", 16));
        timeRankBtn.getStyleClass().add("rank-option-button");

        timeRankBtn.setOnAction(e -> {
            showRankLayoutSelectionAndClose("按用时排名", rankStage);
        });

        timeRankContainer.getChildren().addAll(timeIcon, timeRankLabel, timeRankDesc, timeRankBtn);

        // 将两个选项添加到水平布局中
        rankOptionsBox.getChildren().addAll(stepRankContainer, timeRankContainer);

        // 将返回按钮添加到根容器的最前面
        root.getChildren().addAll(headerBox, titleArea, rankOptionsBox);

        // 使用 ScrollPane 包装内容
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);

        rankStage.setScene(scene);
        rankStage.show();
    }

    // showRankTable 方法 - 添加返回按钮
    private void showRankTable(String sortType, String layoutName, Stage previousStage) {
        Stage rankStage = new Stage();
        rankStage.setTitle("排行榜 - " + layoutName + "（" + sortType + "）");
        rankStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        rankStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getWidth()) > 2) {
                rankStage.setWidth(newVal.doubleValue());
            }
        });
        rankStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getHeight()) > 2) {
                rankStage.setHeight(newVal.doubleValue());
            }
        });
        rankStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getX()) > 2) {
                rankStage.setX(newVal.doubleValue());
            }
        });
        rankStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - rankStage.getY()) > 2) {
                rankStage.setY(newVal.doubleValue());
            }
        });

        // 继承前一个窗口的位置和大小
        if (previousStage != null) {
            rankStage.setX(previousStage.getX());
            rankStage.setY(previousStage.getY());
            rankStage.setWidth(previousStage.getWidth());
            rankStage.setHeight(previousStage.getHeight());
        } else {
            rankStage.setX(mainStage.getX());
            rankStage.setY(mainStage.getY());
            rankStage.setWidth(mainStage.getWidth());
            rankStage.setHeight(mainStage.getHeight());
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 返回按钮区域
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(140);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            rankStage.close();
            // 返回到布局选择界面
            showRankLayoutSelectionAndClose(sortType, rankStage);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域 - 增加图标和描述
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("🏆");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label titleLabel = new Label("排行榜");
        titleLabel.setFont(Font.font("微软雅黑", 28));
        titleLabel.getStyleClass().add("rank-main-title");

        Label subtitleLabel = new Label(layoutName + " - " + sortType);
        subtitleLabel.setFont(Font.font("微软雅黑", 18));
        subtitleLabel.getStyleClass().add("rank-subtitle");

        Label descLabel = new Label("查看玩家在此布局中的最佳成绩排名");
        descLabel.setFont(Font.font("微软雅黑", 14));
        descLabel.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, titleLabel, subtitleLabel, descLabel);

        // 添加进度指示器
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(60, 60);
        progressIndicator.getStyleClass().add("rank-progress-indicator");

        Label loadingLabel = new Label("正在加载排行榜数据...");
        loadingLabel.setFont(Font.font("微软雅黑", 16));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

        // 初始显示返回按钮、标题和加载指示器
        root.getChildren().addAll(headerBox, titleArea, loadingBox);

        // 使用 ScrollPane 包装内容
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        rankStage.setScene(scene);

        // 先显示新窗口
        rankStage.show();

        // 然后关闭旧窗口（如果有的话）
        if (previousStage != null) {
            Platform.runLater(() -> previousStage.close());
        }

        // 异步加载排行榜数据
        loadRankDataWithCustomDesign(sortType, layoutName, root, loadingBox, new VBox());
    }

    // loadRankDataWithCustomDesign 方法 - 更新UI时保留返回按钮
    private void loadRankDataWithCustomDesign(String sortType, String layoutName, VBox root, VBox loadingBox, VBox rankContentArea) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> col = db.getCollection("game_history");

                // 只查通关数据
                Bson filter = Filters.and(
                        Filters.eq("layout", layoutName),
                        Filters.eq("gameWon", true)
                );
                // 排序
                Bson sort;
                if ("按步数排名".equals(sortType)) {
                    sort = Sorts.ascending("moveCount");
                } else {
                    sort = Sorts.ascending("elapsedTime");
                }

                FindIterable<Document> docs = col.find(filter).sort(sort).limit(15);

                List<Document> rankList = new ArrayList<>();
                docs.into(rankList);

                db.close();

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    if (rankList.isEmpty()) {
                        // 显示无数据状态
                        VBox emptyStateBox = createEmptyStateBox(layoutName, sortType);
                        root.getChildren().add(emptyStateBox);
                    } else {
                        // 创建排行榜卡片
                        VBox rankCards = createRankCards(rankList, sortType);

                        // 将排行榜卡片放在ScrollPane中
                        ScrollPane cardsScrollPane = new ScrollPane(rankCards);
                        cardsScrollPane.setFitToWidth(true);
                        cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                        cardsScrollPane.getStyleClass().add("rank-cards-scroll");
                        cardsScrollPane.setPrefHeight(400);

                        root.getChildren().add(cardsScrollPane);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    // 显示错误状态
                    VBox errorStateBox = createErrorStateBox(e.getMessage(), () -> {
                        // 重试时保留返回按钮和标题
                        int headerIndex = -1;
                        int titleIndex = -1;
                        for (int i = 0; i < root.getChildren().size(); i++) {
                            if (root.getChildren().get(i) instanceof HBox) {
                                headerIndex = i;
                            } else if (root.getChildren().get(i) instanceof VBox &&
                                    ((VBox)root.getChildren().get(i)).getChildren().get(0) instanceof Label) {
                                titleIndex = i;
                                break;
                            }
                        }

                        // 保存返回按钮和标题
                        HBox savedHeader = null;
                        VBox savedTitle = null;
                        if (headerIndex >= 0) {
                            savedHeader = (HBox) root.getChildren().get(headerIndex);
                        }
                        if (titleIndex >= 0) {
                            savedTitle = (VBox) root.getChildren().get(titleIndex);
                        }

                        // 清空并重新添加
                        root.getChildren().clear();
                        if (savedHeader != null) root.getChildren().add(savedHeader);
                        if (savedTitle != null) root.getChildren().add(savedTitle);
                        root.getChildren().add(loadingBox);

                        loadRankDataWithCustomDesign(sortType, layoutName, root, loadingBox, rankContentArea);
                    });
                    root.getChildren().add(errorStateBox);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 创建排行榜卡片
    // 创建排行榜卡片列表
    private VBox createRankCards(List<Document> rankList, String sortType) {
        VBox cardsContainer = new VBox(12);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        for (int i = 0; i < rankList.size(); i++) {
            Document rankDoc = rankList.get(i);
            int rank = i + 1; // 排名从1开始
            String username = rankDoc.getString("username");
            int moveCount = rankDoc.getInteger("moveCount", 0);
            String elapsedTime = rankDoc.getString("elapsedTime");

            HBox rankCard = createRankCard(rank, username, moveCount, elapsedTime, sortType);
            cardsContainer.getChildren().add(rankCard);
        }

        return cardsContainer;
    }

    // 创建单个排行榜卡片 - 修复奖牌显示问题
    private HBox createRankCard(int rank, String username, int moveCount, String elapsedTime, String sortType) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setPrefWidth(600);
        card.setMaxWidth(600);

        // 根据排名设置不同的样式
        if (rank == 1) {
            card.getStyleClass().add("rank-card-gold");
        } else if (rank == 2) {
            card.getStyleClass().add("rank-card-silver");
        } else if (rank == 3) {
            card.getStyleClass().add("rank-card-bronze");
        } else {
            card.getStyleClass().add("rank-card-normal");
        }

        // 排名徽章
        StackPane rankBadge = new StackPane();
        rankBadge.setPrefSize(50, 50);
        rankBadge.getStyleClass().add("rank-badge");

        // 排名图标 - 区分emoji和数字的样式
        Label rankIcon = new Label();
        rankIcon.setFont(Font.font("微软雅黑", 20));

        if (rank == 1) {
            rankIcon.setText("🥇");
            rankIcon.getStyleClass().add("rank-medal"); // 使用专门的奖牌样式
        } else if (rank == 2) {
            rankIcon.setText("🥈");
            rankIcon.getStyleClass().add("rank-medal"); // 使用专门的奖牌样式
        } else if (rank == 3) {
            rankIcon.setText("🥉");
            rankIcon.getStyleClass().add("rank-medal"); // 使用专门的奖牌样式
        } else {
            rankIcon.setText(String.valueOf(rank));
            rankIcon.getStyleClass().add("rank-number"); // 保持：数字排名的样式
        }

        rankBadge.getChildren().add(rankIcon);

        // 用户信息区域
        VBox userInfo = new VBox(5);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        // 用户名
        Label usernameLabel = new Label(username);
        usernameLabel.setFont(Font.font("微软雅黑", 18));
        usernameLabel.getStyleClass().add("rank-username");

        // 成绩信息
        String performanceText;
        if ("按步数排名".equals(sortType)) {
            performanceText = "最少步数：" + moveCount + " 步";
        } else {
            performanceText = "最短用时：" + elapsedTime;
        }

        Label performanceLabel = new Label(performanceText);
        performanceLabel.setFont(Font.font("微软雅黑", 14));
        performanceLabel.getStyleClass().add("rank-performance");

        userInfo.getChildren().addAll(usernameLabel, performanceLabel);

        // 排名数字（大号显示）
        Label rankNumber = new Label("#" + rank);
        rankNumber.setFont(Font.font("微软雅黑", 24));
        rankNumber.getStyleClass().add("rank-number-large");

        card.getChildren().addAll(rankBadge, userInfo, rankNumber);

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("rank-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("rank-card-hover"));

        return card;
    }

    // 创建空状态框
    private VBox createEmptyStateBox(String layoutName, String sortType) {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));

        Label emptyIcon = new Label("📊");
        emptyIcon.setFont(Font.font("微软雅黑", 48));

        Label emptyTitle = new Label("暂无排行数据");
        emptyTitle.setFont(Font.font("微软雅黑", 24));
        emptyTitle.getStyleClass().add("empty-state-title");

        Label emptyMessage = new Label("还没有玩家在" + layoutName + "布局中取得" + sortType + "的成绩\n快来成为第一个挑战者吧！");
        emptyMessage.setFont(Font.font("微软雅黑", 16));
        emptyMessage.getStyleClass().add("empty-state-message");
        emptyMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        return emptyBox;
    }

    // 创建错误状态框
    private VBox createErrorStateBox(String errorMessage, Runnable retryAction) {
        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(60));

        Label errorIcon = new Label("⚠️");
        errorIcon.setFont(Font.font("微软雅黑", 48));

        Label errorTitle = new Label("加载失败");
        errorTitle.setFont(Font.font("微软雅黑", 24));
        errorTitle.getStyleClass().add("error-state-title");

        Label errorDetail = new Label("网络连接异常，请检查网络后重试");
        errorDetail.setFont(Font.font("微软雅黑", 16));
        errorDetail.getStyleClass().add("error-state-message");
        errorDetail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button retryBtn = new Button("🔄 重新加载");
        retryBtn.setFont(Font.font("微软雅黑", 16));
        retryBtn.getStyleClass().add("retry-button");
        retryBtn.setOnAction(e -> retryAction.run());

        errorBox.getChildren().addAll(errorIcon, errorTitle, errorDetail, retryBtn);
        return errorBox;
    }

    private void loadCSS(Scene scene) {
        try {
            java.net.URL cssResource = getClass().getResource("/styles.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.err.println("警告：未找到 styles.css 文件，将使用默认样式");
            }
        } catch (Exception e) {
            System.err.println("加载CSS文件时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 好友列表 - 添加聊天功能
    private void showFriends(String username) {
        try {
            MongoDBUtil db = new MongoDBUtil();
            Document userDoc = db.getUserByUsername(username);
            if (userDoc == null) {
                showAlert("错误", "读取好友列表失败", "找不到用户：" + username, Alert.AlertType.ERROR);
                return;
            }

            List<String> friendUsernames = (List<String>) userDoc.get("friends");
            if (friendUsernames == null) {
                friendUsernames = new ArrayList<>();
            }

            Stage friendsStage = new Stage();
            friendsStage.setTitle("好友列表");
            friendsStage.setResizable(true);

            // 窗口同步绑定（与主界面保持一致）
            friendsStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                    mainStage.setWidth(newVal.doubleValue());
                }
            });
            mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - friendsStage.getWidth()) > 2) {
                    friendsStage.setWidth(newVal.doubleValue());
                }
            });
            friendsStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                    mainStage.setHeight(newVal.doubleValue());
                }
            });
            mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - friendsStage.getHeight()) > 2) {
                    friendsStage.setHeight(newVal.doubleValue());
                }
            });
            friendsStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                    mainStage.setX(newVal.doubleValue());
                }
            });
            mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - friendsStage.getX()) > 2) {
                    friendsStage.setX(newVal.doubleValue());
                }
            });
            friendsStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                    mainStage.setY(newVal.doubleValue());
                }
            });
            mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (Math.abs(newVal.doubleValue() - friendsStage.getY()) > 2) {
                    friendsStage.setY(newVal.doubleValue());
                }
            });

            // 初始同步
            friendsStage.setX(mainStage.getX());
            friendsStage.setY(mainStage.getY());
            friendsStage.setWidth(mainStage.getWidth());
            friendsStage.setHeight(mainStage.getHeight());

            VBox root = new VBox(18);
            root.setPadding(new Insets(24, 32, 24, 32));
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("main-background");

            Label titleLabel = new Label("好友列表");
            titleLabel.setFont(Font.font("微软雅黑", 22));
            titleLabel.getStyleClass().add("section-title");

            if (friendUsernames.isEmpty()) {
                // 显示空状态
                VBox emptyBox = new VBox(20);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(60));

                Label emptyIcon = new Label("👥");
                emptyIcon.setFont(Font.font("微软雅黑", 48));

                Label emptyTitle = new Label("暂无好友");
                emptyTitle.setFont(Font.font("微软雅黑", 20));
                emptyTitle.getStyleClass().add("empty-state-title");

                Label emptyMessage = new Label("快去添加一些好友吧！");
                emptyMessage.setFont(Font.font("微软雅黑", 14));
                emptyMessage.getStyleClass().add("empty-state-message");

                emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
                root.getChildren().addAll(titleLabel, emptyBox);
            } else {
                // 使用卡片样式而不是表格
                VBox friendCards = createFriendCards(username, friendUsernames, friendsStage);

                ScrollPane scrollPane = new ScrollPane(friendCards);
                scrollPane.setFitToWidth(true);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.getStyleClass().add("friend-list-scroll");
                scrollPane.setPrefHeight(400);

                root.getChildren().addAll(titleLabel, scrollPane);
            }

            ScrollPane mainScrollPane = new ScrollPane(root);
            mainScrollPane.setFitToWidth(true);
            mainScrollPane.setFitToHeight(true);
            mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            mainScrollPane.getStyleClass().add("main-scroll-pane");

            Scene scene = new Scene(mainScrollPane);
            loadCSS(scene);
            friendsStage.setScene(scene);
            friendsStage.show();

            db.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "读取好友列表失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // 创建好友卡片列表
    private VBox createFriendCards(String username, List<String> friendUsernames, Stage parentStage) {
        VBox cardsContainer = new VBox(8);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        for (String friendUsername : friendUsernames) {
            HBox friendCard = createFriendCard(username, friendUsername, parentStage, cardsContainer);
            cardsContainer.getChildren().add(friendCard);
        }

        return cardsContainer;
    }

    private void deleteFriendBothSides(String username, String friendUsername) {
        MongoDBUtil db = new MongoDBUtil();
        Document userDoc = db.getUserByUsername(username);
        if (userDoc != null && userDoc.get("friends") instanceof List) {
            List<String> friends = new ArrayList<>((List<String>) userDoc.get("friends"));
            friends.remove(friendUsername);
            userDoc.put("friends", friends);
            db.getCollection("users").updateOne(
                    Filters.eq("username", username),
                    new Document("$set", new Document("friends", friends))
            );
        }
        Document friendDoc = db.getUserByUsername(friendUsername);
        if (friendDoc != null && friendDoc.get("friends") instanceof List) {
            List<String> friends = new ArrayList<>((List<String>) friendDoc.get("friends"));
            friends.remove(username);
            friendDoc.put("friends", friends);
            db.getCollection("users").updateOne(
                    Filters.eq("username", friendUsername),
                    new Document("$set", new Document("friends", friends))
            );
        }
        db.close();
    }

    private void addFriend(String username) {
        Stage addFriendStage = new Stage();
        addFriendStage.setTitle("添加好友");
        addFriendStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        addFriendStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - addFriendStage.getWidth()) > 2) {
                addFriendStage.setWidth(newVal.doubleValue());
            }
        });
        addFriendStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - addFriendStage.getHeight()) > 2) {
                addFriendStage.setHeight(newVal.doubleValue());
            }
        });
        addFriendStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - addFriendStage.getX()) > 2) {
                addFriendStage.setX(newVal.doubleValue());
            }
        });
        addFriendStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - addFriendStage.getY()) > 2) {
                addFriendStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        addFriendStage.setX(mainStage.getX());
        addFriendStage.setY(mainStage.getY());
        addFriendStage.setWidth(mainStage.getWidth());
        addFriendStage.setHeight(mainStage.getHeight());

        VBox root = new VBox(30);
        root.setPadding(new Insets(40, 60, 40, 60));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 标题区域
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("👤");
        titleIcon.setFont(Font.font("微软雅黑", 48));
        titleIcon.getStyleClass().add("feature-icon");

        Label titleLabel = new Label("添加好友");
        titleLabel.setFont(Font.font("微软雅黑", 28));
        titleLabel.getStyleClass().add("section-title");

        Label subtitleLabel = new Label("输入好友的用户名来发送好友申请");
        subtitleLabel.setFont(Font.font("微软雅黑", 16));
        subtitleLabel.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, titleLabel, subtitleLabel);

        // 输入区域卡片
        VBox inputCard = new VBox(20);
        inputCard.setAlignment(Pos.CENTER);
        inputCard.setPadding(new Insets(30, 40, 30, 40));
        inputCard.getStyleClass().add("add-friend-card");
        inputCard.setPrefWidth(400);
        inputCard.setMaxWidth(450);

        Label inputLabel = new Label("好友用户名");
        inputLabel.setFont(Font.font("微软雅黑", 16));
        inputLabel.getStyleClass().add("input-label");

        TextField usernameField = new TextField();
        usernameField.setPromptText("请输入要添加的好友用户名");
        usernameField.setPrefHeight(45);
        usernameField.setFont(Font.font("微软雅黑", 16));
        usernameField.getStyleClass().add("add-friend-input");

        // 按钮区域
        HBox buttonArea = new HBox(15);
        buttonArea.setAlignment(Pos.CENTER);

        Button addBtn = new Button("✓ 发送申请");
        addBtn.setPrefWidth(140);
        addBtn.setPrefHeight(45);
        addBtn.setFont(Font.font("微软雅黑", 16));
        addBtn.getStyleClass().add("primary-button");

        Button cancelBtn = new Button("✗ 取消");
        cancelBtn.setPrefWidth(100);
        cancelBtn.setPrefHeight(45);
        cancelBtn.setFont(Font.font("微软雅黑", 16));
        cancelBtn.getStyleClass().add("cancel-button");

        buttonArea.getChildren().addAll(addBtn, cancelBtn);

        inputCard.getChildren().addAll(inputLabel, usernameField, buttonArea);

        // 状态显示区域
        VBox statusArea = new VBox(10);
        statusArea.setAlignment(Pos.CENTER);
        statusArea.setVisible(false);
        statusArea.setManaged(false);

        root.getChildren().addAll(titleArea, inputCard, statusArea);

        // 事件处理
        addBtn.setOnAction(e -> {
            String friendUsername = usernameField.getText().trim();
            if (friendUsername.isEmpty()) {
                showStatusMessage(statusArea, "⚠️", "请输入用户名", "用户名不能为空", "warning");
                return;
            }

            if (friendUsername.equals(username)) {
                showStatusMessage(statusArea, "⚠️", "无法添加", "不能添加自己为好友", "warning");
                return;
            }

            // 显示加载状态
            showStatusMessage(statusArea, "⏳", "正在处理", "正在发送好友申请...", "loading");
            addBtn.setDisable(true);

            // 异步处理
            addFriendAsync(username, friendUsername, addFriendStage, statusArea, addBtn, usernameField);
        });

        cancelBtn.setOnAction(e -> addFriendStage.close());

        // Enter键提交
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                addBtn.fire();
            }
        });

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        addFriendStage.setScene(scene);
        addFriendStage.show();

        // 自动聚焦到输入框
        Platform.runLater(() -> usernameField.requestFocus());
    }

    private void showStatusMessage(VBox statusArea, String icon, String title, String message, String type) {
        statusArea.getChildren().clear();

        Label statusIcon = new Label(icon);
        statusIcon.setFont(Font.font("微软雅黑", 24));

        Label statusTitle = new Label(title);
        statusTitle.setFont(Font.font("微软雅黑", 16));
        statusTitle.getStyleClass().add("status-title-" + type);

        Label statusMessage = new Label(message);
        statusMessage.setFont(Font.font("微软雅黑", 14));
        statusMessage.getStyleClass().add("status-message-" + type);

        statusArea.getChildren().addAll(statusIcon, statusTitle, statusMessage);
        statusArea.setVisible(true);
        statusArea.setManaged(true);
    }

    // 异步添加好友的方法
    private void addFriendAsync(String username, String friendUsername, Stage stage, VBox statusArea, Button addBtn, TextField usernameField) {
        Thread addThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                Document userDoc = db.getUserByUsername(username);
                Document friendDoc = db.getUserByUsername(friendUsername);

                if (userDoc == null || friendDoc == null) {
                    Platform.runLater(() -> {
                        showStatusMessage(statusArea, "❌", "用户不存在", "找不到用户名为 \"" + friendUsername + "\" 的用户", "error");
                        addBtn.setDisable(false);
                    });
                    db.close();
                    return;
                }

                List<String> friendList = (List<String>) userDoc.get("friends");
                if (friendList == null) friendList = new ArrayList<>();
                if (friendList.contains(friendUsername)) {
                    Platform.runLater(() -> {
                        showStatusMessage(statusArea, "ℹ️", "已是好友", "该用户已经是您的好友了", "info");
                        addBtn.setDisable(false);
                    });
                    db.close();
                    return;
                }

                // 检查是否已发送请求
                MongoCollection<Document> mailbox = db.getCollection("mailbox");
                Document exist = mailbox.find(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("from", username),
                                Filters.eq("to", friendUsername),
                                Filters.eq("status", "pending")
                        )
                ).first();
                if (exist != null) {
                    Platform.runLater(() -> {
                        showStatusMessage(statusArea, "ℹ️", "请求已发送", "您已经发送过好友申请，请等待对方回复", "info");
                        addBtn.setDisable(false);
                    });
                    db.close();
                    return;
                }

                // 插入好友请求
                Document request = new Document("type", "friend_request")
                        .append("from", username)
                        .append("to", friendUsername)
                        .append("status", "pending")
                        .append("time", System.currentTimeMillis());
                mailbox.insertOne(request);

                db.close();

                Platform.runLater(() -> {
                    showStatusMessage(statusArea, "✅", "发送成功", "好友申请已发送给 \"" + friendUsername + "\"", "success");
                    usernameField.clear();
                    addBtn.setDisable(false);

                    // 3秒后自动关闭窗口
                    javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> stage.close())
                    );
                    timeline.play();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showStatusMessage(statusArea, "❌", "发送失败", "网络错误，请稍后重试", "error");
                    addBtn.setDisable(false);
                });
            }
        });

        addThread.setDaemon(true);
        addThread.start();
    }

    private void watchOnline(String username) {
        Stage watchStage = new Stage();
        watchStage.setTitle("在线观战");
        watchStage.setResizable(true);

        // 窗口同步绑定代码保持不变...
        watchStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - watchStage.getWidth()) > 2) {
                watchStage.setWidth(newVal.doubleValue());
            }
        });
        watchStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - watchStage.getHeight()) > 2) {
                watchStage.setHeight(newVal.doubleValue());
            }
        });
        watchStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - watchStage.getX()) > 2) {
                watchStage.setX(newVal.doubleValue());
            }
        });
        watchStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - watchStage.getY()) > 2) {
                watchStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        watchStage.setX(mainStage.getX());
        watchStage.setY(mainStage.getY());
        watchStage.setWidth(mainStage.getWidth());
        watchStage.setHeight(mainStage.getHeight());

        VBox root = new VBox(25);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：将手动刷新按钮改为返回按钮
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(35);
        backBtn.getStyleClass().add("back-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("👀");
        titleIcon.setFont(Font.font("微软雅黑", 42));
        titleIcon.getStyleClass().add("feature-icon");

        Label titleLabel = new Label("在线观战");
        titleLabel.setFont(Font.font("微软雅黑", 28));
        titleLabel.getStyleClass().add("section-title");

        Label subtitleLabel = new Label("观看好友的精彩对局");
        subtitleLabel.setFont(Font.font("微软雅黑", 16));
        subtitleLabel.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, titleLabel, subtitleLabel);

        // 修复：更新自动刷新提示为1分钟
        Label autoRefreshLabel = new Label("🔄 每1分钟自动刷新一次");
        autoRefreshLabel.setFont(Font.font("微软雅黑", 12));
        autoRefreshLabel.getStyleClass().add("auto-refresh-hint");
        // 修复：确保自动刷新提示文字颜色正确显示
        autoRefreshLabel.setStyle("-fx-text-fill: #666666; -fx-opacity: 0.8;");

        // 加载指示器
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.getStyleClass().add("mailbox-progress");

        Label loadingLabel = new Label("正在搜索可观战的对局...");
        loadingLabel.setFont(Font.font("微软雅黑", 16));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(40));
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

        root.getChildren().addAll(headerBox, titleArea, autoRefreshLabel, loadingBox);

        // 修复：优化数据加载逻辑，移除手动刷新按钮的依赖
        final VBox[] gameCardsContainer = {null};
        final javafx.animation.Timeline[] autoRefreshTimeline = {null}; // 修复：只保留一个声明

        // 数据加载方法（移除refreshBtn参数）
        Runnable loadData = () -> {
            // 显示加载状态
            Platform.runLater(() -> {
                if (gameCardsContainer[0] != null) {
                    root.getChildren().remove(gameCardsContainer[0]);
                    gameCardsContainer[0] = null;
                }
                if (!root.getChildren().contains(loadingBox)) {
                    root.getChildren().add(loadingBox);
                }
            });

            loadOnlineGamesAsync(username, root, loadingBox, gameCardsContainer);
        };

        // 修复：返回按钮事件处理
        backBtn.setOnAction(e -> {
            // 停止自动刷新
            if (autoRefreshTimeline[0] != null) {
                autoRefreshTimeline[0].stop();
            }

            // 关闭观战窗口
            watchStage.close();

            // 返回主界面
            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();

            // 恢复主界面音乐
            try {
                musicManager.playMusic(MusicManager.MAIN_MENU);
            } catch (Exception ex) {
                System.err.println("恢复主界面音乐失败: " + ex.getMessage());
            }
        });

        // 修复：启动自动刷新（改为每1分钟，即60秒）
        autoRefreshTimeline[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(60), e -> {
                    loadData.run();
                })
        );
        autoRefreshTimeline[0].setCycleCount(javafx.animation.Animation.INDEFINITE);

        // 首次加载
        loadData.run();

        // 窗口显示后启动自动刷新
        watchStage.setOnShown(e -> {
            if (autoRefreshTimeline[0] != null) {
                autoRefreshTimeline[0].play();
            }
        });

        // 窗口关闭时停止自动刷新
        watchStage.setOnCloseRequest(e -> {
            if (autoRefreshTimeline[0] != null) {
                autoRefreshTimeline[0].stop();
            }

            // 返回主界面
            mainStage.setX(watchStage.getX());
            mainStage.setY(watchStage.getY());
            mainStage.setWidth(watchStage.getWidth());
            mainStage.setHeight(watchStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();

            // 恢复主界面音乐
            try {
                musicManager.playMusic(MusicManager.MAIN_MENU);
            } catch (Exception ex) {
                System.err.println("恢复主界面音乐失败: " + ex.getMessage());
            }
        });

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        watchStage.setScene(scene);
        watchStage.show();
    }

    private void loadOnlineGamesAsync(String username, VBox root, VBox loadingBox, VBox[] gameCardsContainer) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> col = db.getCollection("online_games");

                // 修复：首先清理过期的在线游戏记录（超过10分钟的记录）
                long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
                long deletedCount = col.deleteMany(Filters.lt("timestamp", tenMinutesAgo)).getDeletedCount();
                System.out.println("清理了 " + deletedCount + " 条过期的在线游戏记录");

                // 修复：获取当前用户的好友列表用于调试
                MongoCollection<Document> usersCol = db.getCollection("users");
                Document currentUserDoc = usersCol.find(Filters.eq("username", username)).first();
                List<String> myFriends = new ArrayList<>();
                if (currentUserDoc != null && currentUserDoc.get("friends") instanceof List) {
                    myFriends = (List<String>) currentUserDoc.get("friends");
                }
                System.out.println("当前用户 " + username + " 的好友列表: " + myFriends);

                // 修复：获取所有在线游戏记录并进行详细分析
                FindIterable<Document> allDocs = col.find().sort(Sorts.descending("timestamp"));
                List<Document> allGamesList = new ArrayList<>();
                allDocs.into(allGamesList);
                System.out.println("总共找到 " + allGamesList.size() + " 条在线游戏记录");

                List<OnlineGameRecord> availableGames = new ArrayList<>();
                List<OnlineGameRecord> debugAllGames = new ArrayList<>(); // 用于调试的所有游戏列表

                for (Document doc : allGamesList) {
                    String roomId = doc.getString("roomId");
                    String host = doc.getString("host");
                    String layoutName = doc.getString("layoutName");
                    String elapsedTime = doc.getString("elapsedTime");
                    Integer moveCount = doc.getInteger("moveCount");
                    Long timestamp = doc.getLong("timestamp");
                    List<String> roomFriends = (List<String>) doc.get("friends");

                    // 修复：创建游戏记录用于调试
                    OnlineGameRecord gameRecord = new OnlineGameRecord(
                            roomId != null ? roomId : "unknown",
                            host != null ? host : "unknown",
                            layoutName != null ? layoutName : "未知布局",
                            elapsedTime != null ? elapsedTime : "00:00",
                            moveCount != null ? moveCount : 0
                    );
                    debugAllGames.add(gameRecord);

                    // 修复：详细的调试信息
                    System.out.println("检查游戏记录:");
                    System.out.println("  房间ID: " + roomId);
                    System.out.println("  房主: " + host);
                    System.out.println("  布局: " + layoutName);
                    System.out.println("  房主好友列表: " + roomFriends);
                    System.out.println("  时间戳: " + timestamp + " (距离现在: " + (System.currentTimeMillis() - (timestamp != null ? timestamp : 0)) / 1000 + "秒)");

                    // 修复：多重检查条件
                    boolean isValidGame = true;
                    String skipReason = "";

                    // 检查基本字段完整性
                    if (roomId == null || host == null || layoutName == null) {
                        isValidGame = false;
                        skipReason = "基本字段缺失";
                    }

                    // 检查是否是自己的游戏
                    if (isValidGame && host.equals(username)) {
                        isValidGame = false;
                        skipReason = "是自己的游戏";
                    }

                    // 修复：检查好友关系（双向检查）
                    if (isValidGame) {
                        boolean isFriend = false;

                        // 方法1：检查我是否在房主的好友列表中
                        if (roomFriends != null && roomFriends.contains(username)) {
                            isFriend = true;
                            System.out.println("  ✓ 通过房主好友列表验证");
                        }

                        // 方法2：检查房主是否在我的好友列表中
                        if (!isFriend && myFriends.contains(host)) {
                            isFriend = true;
                            System.out.println("  ✓ 通过我的好友列表验证");
                        }

                        // 方法3：从数据库重新验证好友关系
                        if (!isFriend) {
                            Document hostDoc = usersCol.find(Filters.eq("username", host)).first();
                            if (hostDoc != null && hostDoc.get("friends") instanceof List) {
                                List<String> hostFriendsList = (List<String>) hostDoc.get("friends");
                                if (hostFriendsList.contains(username)) {
                                    isFriend = true;
                                    System.out.println("  ✓ 通过数据库重新验证好友关系");
                                }
                            }
                        }

                        if (!isFriend) {
                            isValidGame = false;
                            skipReason = "不是好友关系";
                        }
                    }

                    if (isValidGame) {
                        availableGames.add(gameRecord);
                        System.out.println("  ✓ 游戏记录有效，已添加到可观战列表");
                    } else {
                        System.out.println("  ✗ 跳过游戏记录，原因: " + skipReason);
                    }
                    System.out.println();
                }

                db.close();

                System.out.println("最终结果:");
                System.out.println("  总游戏数: " + debugAllGames.size());
                System.out.println("  可观战游戏数: " + availableGames.size());
                System.out.println("  当前用户好友数: " + myFriends.size());

                // 在JavaFX应用线程中更新UI
                List<String> finalMyFriends = myFriends;
                Platform.runLater(() -> {
                    // 移除加载指示器
                    if (root.getChildren().contains(loadingBox)) {
                        root.getChildren().remove(loadingBox);
                    }

                    if (availableGames.isEmpty()) {
                        // 修复：显示更详细的空状态信息
                        VBox emptyStateBox = createDetailedWatchEmptyState(debugAllGames.size(), finalMyFriends.size());
                        gameCardsContainer[0] = emptyStateBox;
                        root.getChildren().add(emptyStateBox);
                    } else {
                        // 显示可观战的游戏
                        VBox gameCards = createOnlineGameCards(availableGames);
                        gameCardsContainer[0] = gameCards;
                        root.getChildren().add(gameCards);

                        System.out.println("UI更新完成，显示了 " + availableGames.size() + " 个可观战游戏");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("加载在线游戏数据失败: " + e.getMessage());
                Platform.runLater(() -> {
                    // 移除加载指示器
                    if (root.getChildren().contains(loadingBox)) {
                        root.getChildren().remove(loadingBox);
                    }

                    // 显示错误状态
                    VBox errorStateBox = createErrorStateBox("加载在线对局失败: " + e.getMessage(), () -> {
                        Platform.runLater(() -> {
                            if (gameCardsContainer[0] != null) {
                                root.getChildren().remove(gameCardsContainer[0]);
                                gameCardsContainer[0] = null;
                            }
                            if (!root.getChildren().contains(loadingBox)) {
                                root.getChildren().add(loadingBox);
                            }
                        });
                        loadOnlineGamesAsync(username, root, loadingBox, gameCardsContainer);
                    });
                    gameCardsContainer[0] = errorStateBox;
                    root.getChildren().add(errorStateBox);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 修复：创建更详细的观战空状态，显示调试信息
    private VBox createDetailedWatchEmptyState(int totalGames, int friendCount) {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));

        Label emptyIcon = new Label("🎮");
        emptyIcon.setFont(Font.font("微软雅黑", 48));
        emptyIcon.getStyleClass().add("feature-icon");

        Label emptyTitle = new Label("暂无可观战的对局");
        emptyTitle.setFont(Font.font("微软雅黑", 20));
        emptyTitle.getStyleClass().add("empty-state-title");

        // 修复：添加详细的状态信息
        String detailMessage;
        if (friendCount == 0) {
            detailMessage = "您还没有添加好友\n先去添加一些好友吧！";
        } else if (totalGames == 0) {
            detailMessage = "当前没有人在线游戏\n有 " + friendCount + " 个好友，等待他们开始游戏";
        } else {
            detailMessage = "发现 " + totalGames + " 个在线游戏，但都不是好友的游戏\n" +
                    "您有 " + friendCount + " 个好友，邀请他们一起玩吧！";
        }

        Label emptyMessage = new Label(detailMessage);
        emptyMessage.setFont(Font.font("微软雅黑", 14));
        emptyMessage.getStyleClass().add("empty-state-message");
        emptyMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 修复：添加调试按钮（可选）
        Button debugBtn = new Button("🔍 查看详细信息");
        debugBtn.setFont(Font.font("微软雅黑", 12));
        debugBtn.getStyleClass().add("debug-button");
        debugBtn.setOnAction(e -> {
            Alert debugAlert = new Alert(Alert.AlertType.INFORMATION);
            debugAlert.setTitle("调试信息");
            debugAlert.setHeaderText("在线观战调试信息");
            debugAlert.setContentText(
                    "总在线游戏数: " + totalGames + "\n" +
                            "您的好友数: " + friendCount + "\n" +
                            "可观战游戏数: 0\n\n" +
                            "建议：\n" +
                            "1. 检查好友是否正在游戏中\n" +
                            "2. 确认好友关系是否正常\n" +
                            "3. 尝试刷新页面"
            );
            debugAlert.showAndWait();
        });

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage, debugBtn);
        return emptyBox;
    }

    // 创建在线对局卡片列表
    private VBox createOnlineGameCards(List<OnlineGameRecord> games) {
        VBox cardsContainer = new VBox(12);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        for (OnlineGameRecord game : games) {
            HBox gameCard = createOnlineGameCard(game);
            cardsContainer.getChildren().add(gameCard);
        }

        return cardsContainer;
    }

    // 创建单个在线对局卡片
    private HBox createOnlineGameCard(OnlineGameRecord game) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setPrefWidth(600);
        card.setMaxWidth(600);
        card.getStyleClass().add("online-game-card");

        // 房主信息区域
        VBox hostInfo = new VBox(8);
        hostInfo.setAlignment(Pos.CENTER_LEFT);

        // 房主头像
        StackPane hostAvatar = new StackPane();
        hostAvatar.setPrefSize(50, 50);
        hostAvatar.setMaxSize(50, 50);
        hostAvatar.getStyleClass().add("host-avatar");

        // 根据房主名的哈希值选择头像颜色
        String[] avatarColors = {"host-avatar-red", "host-avatar-green", "host-avatar-orange", "host-avatar-purple", "host-avatar-blue"};
        int colorIndex = Math.abs(game.getHost().hashCode()) % avatarColors.length;
        hostAvatar.getStyleClass().add(avatarColors[colorIndex]);

        Label hostAvatarLabel = new Label(game.getHost().substring(0, 1).toUpperCase());
        hostAvatarLabel.setFont(Font.font("微软雅黑", 18));
        hostAvatarLabel.getStyleClass().add("host-avatar-text");

        hostAvatar.getChildren().add(hostAvatarLabel);

        Label hostNameLabel = new Label(game.getHost());
        hostNameLabel.setFont(Font.font("微软雅黑", 16));
        hostNameLabel.getStyleClass().add("host-name");

        HBox hostContainer = new HBox(12);
        hostContainer.setAlignment(Pos.CENTER_LEFT);
        hostContainer.getChildren().addAll(hostAvatar, hostNameLabel);

        hostInfo.getChildren().add(hostContainer);

        // 游戏信息区域
        VBox gameInfo = new VBox(5);
        gameInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(gameInfo, Priority.ALWAYS);

        Label layoutLabel = new Label("🏯 " + game.getLayoutName());
        layoutLabel.setFont(Font.font("微软雅黑", 14));
        layoutLabel.getStyleClass().add("game-layout");

        Label timeLabel = new Label("⏰ " + (game.getElapsedTime() != null ? game.getElapsedTime() : "00:00"));
        timeLabel.setFont(Font.font("微软雅黑", 14));
        timeLabel.getStyleClass().add("game-time");

        Label moveLabel = new Label("👣 " + game.getMoveCount() + " 步");
        moveLabel.setFont(Font.font("微软雅黑", 14));
        moveLabel.getStyleClass().add("game-moves");

        gameInfo.getChildren().addAll(layoutLabel, timeLabel, moveLabel);

        // 观战按钮
        Button watchBtn = new Button("👀 观战");
        watchBtn.setPrefWidth(100);
        watchBtn.setPrefHeight(45);
        watchBtn.setFont(Font.font("微软雅黑", 16));
        watchBtn.getStyleClass().add("watch-button");
        watchBtn.setOnAction(e -> openWatchWindow(game.roomId));

        card.getChildren().addAll(hostInfo, gameInfo, watchBtn);

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("online-game-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("online-game-card-hover"));

        return card;
    }

    private void openWatchWindow(String roomId) {
        Stage stage = new Stage();
        stage.setTitle("观战 - " + roomId);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-background");

        // 顶部信息区域保持不变...
        VBox topArea = new VBox(12);
        topArea.setPadding(new Insets(20, 25, 15, 25));
        topArea.setAlignment(Pos.CENTER);

        Label title = new Label("观战中...");
        title.setFont(Font.font("微软雅黑", 22));
        title.getStyleClass().add("section-title");

        Label connectionStatus = new Label("🔄 连接中...");
        connectionStatus.setFont(Font.font("微软雅黑", 14));
        connectionStatus.getStyleClass().add("connection-status-label");

        Label aiStatusLabel = new Label("");
        aiStatusLabel.setFont(Font.font("微软雅黑", 16));
        aiStatusLabel.getStyleClass().add("ai-status-label");
        aiStatusLabel.setVisible(false);

        topArea.getChildren().addAll(title, connectionStatus, aiStatusLabel);
        root.setTop(topArea);

        // 中央棋盘区域
        HBox centerArea = new HBox(20);
        centerArea.setPadding(new Insets(10, 25, 20, 25));
        centerArea.setAlignment(Pos.CENTER);

        // 左侧信息面板保持不变...
        VBox gameInfoPanel = new VBox(15);
        gameInfoPanel.setPrefWidth(180);
        gameInfoPanel.setMaxWidth(180);
        gameInfoPanel.setMinWidth(180);
        gameInfoPanel.setPadding(new Insets(20, 15, 20, 15));
        gameInfoPanel.getStyleClass().add("game-info-panel");
        gameInfoPanel.setAlignment(Pos.TOP_CENTER);

        VBox timeCard = createInfoCard("⏰", "用时", "--:--");
        VBox moveCard = createInfoCard("👣", "步数", "--");
        VBox statusCard = createInfoCard("🎮", "状态", "观战中");

        gameInfoPanel.getChildren().addAll(timeCard, moveCard, statusCard);

        // 修复：中央棋盘 - 确保正确的5行4列尺寸
        ReplayBoardPane boardPane = new ReplayBoardPane(new ArrayList<>(), new ArrayList<>(), null);
        boardPane.setPrefSize(320, 400); // 修复：4列×80=320，5行×80=400
        boardPane.setMaxSize(320, 400);
        boardPane.setMinSize(320, 400);

        // 右侧操作面板保持不变...
        VBox operationPanel = new VBox(15);
        operationPanel.setPrefWidth(180);
        operationPanel.setMaxWidth(180);
        operationPanel.setMinWidth(180);
        operationPanel.setPadding(new Insets(20, 15, 20, 15));
        operationPanel.getStyleClass().add("operation-panel");
        operationPanel.setAlignment(Pos.TOP_CENTER);

        VBox controlCard = createControlCard();
        VBox infoCard = createWatchInfoCard();

        operationPanel.getChildren().addAll(controlCard, infoCard);

        centerArea.getChildren().addAll(gameInfoPanel, boardPane, operationPanel);
        root.setCenter(centerArea);

        // 修复：调整窗口大小以适应新的棋盘尺寸
        Scene scene = new Scene(root, 720, 600); // 减少宽度：180+320+180+40=720
        loadCSS(scene);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // 修复：优化刷新逻辑 - 改为2秒刷新一次（观战窗口内保持较快刷新）
        final javafx.animation.Timeline[] timeline = new javafx.animation.Timeline[1];
        final boolean[] aiTipShown = {false};
        final boolean[] isConnected = {false};
        final long[] lastUpdateTime = {0};
        final int[] failedAttempts = {0};

        timeline[0] = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    // 异步获取数据，避免阻塞UI
                    Thread updateThread = new Thread(() -> {
                        try {
                            MongoDBUtil db = new MongoDBUtil();
                            MongoCollection<Document> col = db.getCollection("online_games");
                            Document doc = col.find(Filters.eq("roomId", roomId)).first();
                            db.close();

                            if (doc != null) {
                                long docTimestamp = doc.getLong("timestamp");

                                Platform.runLater(() -> {
                                    if (docTimestamp > lastUpdateTime[0]) {
                                        lastUpdateTime[0] = docTimestamp;

                                        // 更新连接状态
                                        if (!isConnected[0]) {
                                            isConnected[0] = true;
                                            connectionStatus.setText("🟢 已连接");
                                            connectionStatus.getStyleClass().remove("connection-status-disconnected");
                                            connectionStatus.getStyleClass().add("connection-status-connected");
                                        }
                                        failedAttempts[0] = 0;

                                        // 更新棋盘
                                        try {
                                            List<Document> blockDocs = (List<Document>) doc.get("blocks");
                                            List<GameFrame.Block> blocks = convertToBlockList(blockDocs);
                                            boardPane.setBlocks(blocks);
                                        } catch (Exception ex) {
                                            System.err.println("更新棋盘失败: " + ex.getMessage());
                                        }

                                        // 更新游戏信息 - 使用新的信息卡片
                                        String elapsedTime = doc.getString("elapsedTime");
                                        Integer moveCount = doc.getInteger("moveCount", 0);

                                        updateInfoCard(timeCard, elapsedTime != null ? elapsedTime : "--:--");
                                        updateInfoCard(moveCard, moveCount.toString());

                                        // 检查AI帮解状态
                                        Boolean aiSolving = doc.getBoolean("aiSolving", false);
                                        if (aiSolving != null && aiSolving) {
                                            if (!aiTipShown[0]) {
                                                aiTipShown[0] = true;
                                                aiStatusLabel.setText("🤖 AI正在帮助解题...");
                                                aiStatusLabel.getStyleClass().add("ai-status-active");
                                            } else {
                                                aiStatusLabel.setText("🤖 AI帮解中");
                                                aiStatusLabel.getStyleClass().add("ai-status-working");
                                            }
                                            aiStatusLabel.setVisible(true);
                                        } else {
                                            aiTipShown[0] = false;
                                            aiStatusLabel.setVisible(false);
                                            aiStatusLabel.getStyleClass().removeAll("ai-status-active", "ai-status-working");
                                        }
                                    } else {
                                        if (!isConnected[0]) {
                                            isConnected[0] = true;
                                            connectionStatus.setText("🟡 等待更新...");
                                            connectionStatus.getStyleClass().remove("connection-status-disconnected");
                                            connectionStatus.getStyleClass().add("connection-status-waiting");
                                        }
                                    }
                                });

                            } else {
                                // 房间已不存在
                                Platform.runLater(() -> {
                                    timeline[0].stop();

                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("观战结束");
                                    alert.setHeaderText("对局已结束");
                                    alert.setContentText("该对局已结束，观战窗口将自动关闭。");

                                    DialogPane dialogPane = alert.getDialogPane();
                                    dialogPane.getStyleClass().add("dialog-pane");
                                    dialogPane.getStyleClass().add("info-dialog");

                                    alert.showAndWait();
                                    stage.close();
                                });
                            }

                        } catch (Exception ex) {
                            // 网络异常处理
                            Platform.runLater(() -> {
                                failedAttempts[0]++;
                                isConnected[0] = false;

                                if (failedAttempts[0] <= 3) {
                                    connectionStatus.setText("🔄 重连中... (" + failedAttempts[0] + "/3)");
                                    connectionStatus.getStyleClass().removeAll("connection-status-connected", "connection-status-waiting");
                                    connectionStatus.getStyleClass().add("connection-status-reconnecting");
                                } else if (failedAttempts[0] <= 6) {
                                    connectionStatus.setText("⚠️ 网络异常");
                                    connectionStatus.getStyleClass().removeAll("connection-status-connected", "connection-status-waiting", "connection-status-reconnecting");
                                    connectionStatus.getStyleClass().add("connection-status-error");
                                } else {
                                    timeline[0].stop();

                                    Alert alert = new Alert(Alert.AlertType.WARNING);
                                    alert.setTitle("连接失败");
                                    alert.setHeaderText("无法连接到对局");
                                    alert.setContentText("网络连接出现问题，无法继续观战。");

                                    DialogPane dialogPane = alert.getDialogPane();
                                    dialogPane.getStyleClass().add("dialog-pane");
                                    dialogPane.getStyleClass().add("warning-dialog");

                                    alert.showAndWait();
                                    stage.close();
                                }
                            });

                            System.err.println("观战数据获取失败: " + ex.getMessage());
                        }
                    });

                    updateThread.setDaemon(true);
                    updateThread.start();
                })
        );

        timeline[0].setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline[0].play();

        // 窗口关闭时停止刷新
        stage.setOnCloseRequest(e -> {
            if (timeline[0] != null) {
                timeline[0].stop();
            }
        });
    }

    // 创建游戏信息卡片
    private VBox createInfoCard(String icon, String title, String value) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 10, 12, 10));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("watch-info-card");
        card.setPrefWidth(150);
        card.setMaxWidth(150);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("微软雅黑", 20));
        iconLabel.getStyleClass().add("feature-icon");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("微软雅黑", 12));
        titleLabel.getStyleClass().add("info-card-title");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("微软雅黑", 16));
        valueLabel.getStyleClass().add("info-card-value");

        card.getChildren().addAll(iconLabel, titleLabel, valueLabel);
        return card;
    }

    // 更新信息卡片
    private void updateInfoCard(VBox card, String newValue) {
        Label valueLabel = (Label) card.getChildren().get(2);
        valueLabel.setText(newValue);
    }

    // 创建观战控制卡片
    private VBox createControlCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("watch-control-card");
        card.setPrefWidth(150);
        card.setMaxWidth(150);

        Label iconLabel = new Label("🎮");
        iconLabel.setFont(Font.font("微软雅黑", 24));
        iconLabel.getStyleClass().add("feature-icon");

        Label titleLabel = new Label("观战控制");
        titleLabel.setFont(Font.font("微软雅黑", 14));
        titleLabel.getStyleClass().add("control-card-title");

        Button refreshBtn = new Button("🔄 刷新");
        refreshBtn.setPrefWidth(120);
        refreshBtn.setPrefHeight(30);
        refreshBtn.setFont(Font.font("微软雅黑", 12));
        refreshBtn.getStyleClass().add("watch-refresh-button");

        card.getChildren().addAll(iconLabel, titleLabel, refreshBtn);
        return card;
    }

    // 创建观战信息卡片
    private VBox createWatchInfoCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("watch-info-card");
        card.setPrefWidth(150);
        card.setMaxWidth(150);

        Label iconLabel = new Label("ℹ️");
        iconLabel.setFont(Font.font("微软雅黑", 20));
        iconLabel.getStyleClass().add("feature-icon");

        Label titleLabel = new Label("观战说明");
        titleLabel.setFont(Font.font("微软雅黑", 12));
        titleLabel.getStyleClass().add("info-card-title");

        Label infoLabel = new Label("实时观看好友\n的游戏进度");
        infoLabel.setFont(Font.font("微软雅黑", 10));
        infoLabel.getStyleClass().add("watch-info-text");
        infoLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        infoLabel.setWrapText(true);

        card.getChildren().addAll(iconLabel, titleLabel, infoLabel);
        return card;
    }

    private void restoreGame(String username, HistoryRecord record, Stage historyListStage, Stage layoutSelectStage) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            MongoCollection<Document> col = dbManager.getCollection("game_history");

            Document query = new Document("username", username)
                    .append("saveTime", record.getSaveTime())
                    .append("moveCount", record.getMoveCount())
                    .append("elapsedTime", record.getElapsedTime());

            Document gameDoc = col.find(query).first();

            if (gameDoc == null) {
                showAlert("错误", "记录不存在",
                        "找不到对应的游戏记录，可能已被删除。", Alert.AlertType.ERROR);
                return;
            }

            try {
                // 验证并解析游戏数据
                List<GameFrame.Block> savedBlocks = validateAndParseBlocks(gameDoc);
                int savedMoveCount = validateMoveCount(gameDoc);
                String savedElapsedTime = validateElapsedTime(gameDoc);
                List<String> savedHistoryStack = validateHistoryStack(gameDoc);
                String layoutName = gameDoc.getString("layout");

                // 验证布局名称
                if (layoutName == null || layoutName.trim().isEmpty()) {
                    throw new DataCorruptionException("布局名称缺失或为空");
                }

                // 如果数据验证成功，恢复游戏
                int layoutIndex = BoardLayouts.getLayoutNames().indexOf(layoutName);
                if (layoutIndex < 0) {
                    throw new DataCorruptionException("无效的布局名称: " + layoutName);
                }

                GameFrame gameFrame = new GameFrame();
                gameFrame.setCurrentLayoutIndex(layoutIndex);
                Stage gameStage = new Stage();
                gameFrame.show(gameStage, username, false, mainStage, false);
                mainStage.hide();
                gameFrame.restoreGame(savedBlocks, savedMoveCount, savedElapsedTime, savedHistoryStack, record.getSaveTime());

                // 先将新游戏窗口置顶
                gameStage.toFront();

                // 关闭历史记录和布局选择窗口
                if (historyListStage != null) historyListStage.close();
                if (layoutSelectStage != null) layoutSelectStage.close();

            } catch (DataCorruptionException e) {
                // 数据损坏异常处理
                System.err.println("发现损坏的历史记录: " + gameDoc.getObjectId("_id") +
                        ", 错误: " + e.getMessage());

                // 显示损坏提示对话框
                Alert corruptionAlert = new Alert(Alert.AlertType.WARNING);
                corruptionAlert.setTitle("数据损坏");
                corruptionAlert.setHeaderText("云端数据损坏");
                corruptionAlert.setContentText("检测到该游戏记录的云端数据已损坏：\n" +
                        e.getMessage() + "\n\n是否删除这条损坏的记录？");

                // 应用样式
                DialogPane dialogPane = corruptionAlert.getDialogPane();
                dialogPane.getStyleClass().add("dialog-pane");
                dialogPane.getStyleClass().add("warning-dialog");

                ButtonType deleteBtn = new ButtonType("删除损坏记录", ButtonBar.ButtonData.YES);
                ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
                corruptionAlert.getButtonTypes().setAll(deleteBtn, cancelBtn);

                Optional<ButtonType> result = corruptionAlert.showAndWait();

                if (result.isPresent() && result.get() == deleteBtn) {
                    // 删除损坏的记录
                    deleteCorruptedRecordAsync(username, record, gameDoc.getObjectId("_id"), historyListStage);
                }
            }

        } catch (Exception e) {
            ExceptionHandler.handleDatabaseException(e, "恢复游戏");
            e.printStackTrace();
        }
    }

    private List<GameFrame.Block> validateAndParseBlocks(Document gameDoc) throws DataCorruptionException {
        try {
            List<Document> blockDocs = (List<Document>) gameDoc.get("blocks");
            if (blockDocs == null || blockDocs.isEmpty()) {
                throw new DataCorruptionException("方块数据为空或缺失");
            }

            List<GameFrame.Block> blocks = new ArrayList<>();
            for (int i = 0; i < blockDocs.size(); i++) {
                Document blockDoc = blockDocs.get(i);

                // 验证必要字段
                if (!blockDoc.containsKey("name") || !blockDoc.containsKey("row") ||
                        !blockDoc.containsKey("col") || !blockDoc.containsKey("width") ||
                        !blockDoc.containsKey("height") || !blockDoc.containsKey("color")) {
                    throw new DataCorruptionException("方块数据字段缺失，索引: " + i);
                }

                String name = blockDoc.getString("name");
                Integer row = blockDoc.getInteger("row");
                Integer col = blockDoc.getInteger("col");
                Integer width = blockDoc.getInteger("width");
                Integer height = blockDoc.getInteger("height");
                String colorStr = blockDoc.getString("color");

                // 验证数据完整性
                if (name == null || row == null || col == null || width == null || height == null || colorStr == null) {
                    throw new DataCorruptionException("方块数据包含空值，索引: " + i);
                }

                // 验证数据范围
                if (row < 0 || row >= 5 || col < 0 || col >= 4) {
                    throw new DataCorruptionException("方块位置数据无效，索引: " + i + ", row=" + row + ", col=" + col);
                }

                if (width <= 0 || width > 4 || height <= 0 || height > 5) {
                    throw new DataCorruptionException("方块尺寸数据无效，索引: " + i + ", width=" + width + ", height=" + height);
                }

                if (name.trim().isEmpty()) {
                    throw new DataCorruptionException("方块名称为空，索引: " + i);
                }

                // 解析颜色
                javafx.scene.paint.Color color;
                try {
                    color = javafx.scene.paint.Color.valueOf(colorStr);
                } catch (Exception e) {
                    throw new DataCorruptionException("方块颜色数据无效，索引: " + i + ", 颜色: " + colorStr);
                }

                blocks.add(new GameFrame.Block(row, col, width, height, color, name));
            }

            // 验证方块数量是否合理
            if (blocks.size() < 5 || blocks.size() > 15) {
                throw new DataCorruptionException("方块数量异常: " + blocks.size());
            }

            // 验证是否存在曹操方块
            boolean hasCaocao = blocks.stream().anyMatch(block -> "曹操".equals(block.getName()));
            if (!hasCaocao) {
                throw new DataCorruptionException("缺少曹操方块");
            }

            return blocks;

        } catch (ClassCastException e) {
            throw new DataCorruptionException("方块数据类型错误", e);
        } catch (NullPointerException e) {
            throw new DataCorruptionException("方块数据包含空值", e);
        }
    }

    private int validateMoveCount(Document gameDoc) throws DataCorruptionException {
        try {
            Integer moveCount = gameDoc.getInteger("moveCount");
            if (moveCount == null) {
                throw new DataCorruptionException("步数数据缺失");
            }

            if (moveCount < 0 || moveCount > 10000) {
                throw new DataCorruptionException("步数数据异常: " + moveCount);
            }

            return moveCount;

        } catch (ClassCastException e) {
            throw new DataCorruptionException("步数数据类型错误", e);
        }
    }

    private String validateElapsedTime(Document gameDoc) throws DataCorruptionException {
        try {
            String elapsedTime = gameDoc.getString("elapsedTime");
            if (elapsedTime == null || elapsedTime.trim().isEmpty()) {
                throw new DataCorruptionException("用时数据缺失或为空");
            }

            // 验证时间格式 (MM:SS)
            if (!elapsedTime.matches("\\d{1,3}:\\d{2}")) {
                throw new DataCorruptionException("用时数据格式错误: " + elapsedTime);
            }

            // 验证时间合理性
            String[] parts = elapsedTime.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);

            if (minutes < 0 || minutes > 999 || seconds < 0 || seconds > 59) {
                throw new DataCorruptionException("用时数据值异常: " + elapsedTime);
            }

            return elapsedTime;

        } catch (NumberFormatException e) {
            throw new DataCorruptionException("用时数据包含非数字字符", e);
        }
    }

    private List<String> validateHistoryStack(Document gameDoc) throws DataCorruptionException {
        try {
            List<String> historyStack = (List<String>) gameDoc.get("historyStack");
            if (historyStack == null) {
                // 历史记录栈可以为空，返回空列表
                return new ArrayList<>();
            }

            // 验证历史记录格式
            for (int i = 0; i < historyStack.size(); i++) {
                String record = historyStack.get(i);
                if (record == null) {
                    throw new DataCorruptionException("历史记录栈包含空值，索引: " + i);
                }

                // 简单验证格式 (应该包含方块名称和坐标)
                if (!record.contains("(") || !record.contains(")") || !record.contains(",")) {
                    throw new DataCorruptionException("历史记录格式错误，索引: " + i + ", 内容: " + record);
                }
            }

            // 验证历史记录数量合理性
            if (historyStack.size() > 10000) {
                throw new DataCorruptionException("历史记录数量异常: " + historyStack.size());
            }

            return historyStack;

        } catch (ClassCastException e) {
            throw new DataCorruptionException("历史记录栈数据类型错误", e);
        }
    }

    // 4. 添加异步删除损坏记录的方法
    private void deleteCorruptedRecordAsync(String username, HistoryRecord record, org.bson.types.ObjectId objectId, Stage historyListStage) {
        Thread deleteThread = new Thread(() -> {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();

                // 使用 ObjectId 删除特定记录
                long deletedCount = dbManager.getCollection("game_history")
                        .deleteOne(Filters.eq("_id", objectId))
                        .getDeletedCount();

                Platform.runLater(() -> {
                    if (deletedCount > 0) {
                        showAlert("删除成功", "记录已删除",
                                "损坏的游戏记录已从云端删除。", Alert.AlertType.INFORMATION);

                        // 刷新历史记录列表
                        if (historyListStage != null && historyListStage.isShowing()) {
                            // 重新加载历史记录页面
                            historyListStage.close();
                            // 这里可以调用刷新历史记录列表的方法
                            showHistoryList(username, getCurrentLayoutFromRecord(record), null);
                        }
                    } else {
                        showAlert("删除失败", "记录未找到",
                                "无法找到要删除的记录，可能已被删除。", Alert.AlertType.WARNING);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    ExceptionHandler.handleDatabaseException(e, "删除损坏记录");
                });
                e.printStackTrace();
            }
        });

        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    // 6. 添加批量损坏记录对话框
    private void showCorruptedRecordsDialog(String username, String layoutName, int corruptedCount, Runnable onDelete) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("发现损坏数据");
        alert.setHeaderText("云端数据损坏");
        alert.setContentText("在 \"" + layoutName + "\" 布局中发现 " + corruptedCount + " 条损坏的历史记录。\n" +
                "这些记录可能由于网络传输错误或存储问题导致数据损坏。\n\n" +
                "建议删除这些损坏的记录以避免后续问题，是否继续？");

        // 应用样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("warning-dialog");

        ButtonType deleteAllBtn = new ButtonType("删除所有损坏记录", ButtonBar.ButtonData.YES);
        ButtonType ignoreBtn = new ButtonType("暂时忽略", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(deleteAllBtn, ignoreBtn);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == deleteAllBtn) {
            onDelete.run();
        }
    }


    private String getCurrentLayoutFromRecord(HistoryRecord record) {
        // 这个方法需要根据你的 HistoryRecord 实现来获取布局名称
        // 如果 HistoryRecord 没有保存布局信息，可能需要从其他地方获取
        // 这里返回一个默认值，你需要根据实际情况修改
        List<String> layoutNames = BoardLayouts.getLayoutNames();
        return layoutNames.isEmpty() ? "未知布局" : layoutNames.get(0);
    }

    // 9. 添加将Document转换为Block列表的方法（如果不存在）
    private List<GameFrame.Block> convertToBlockList(List<Document> blockDocs) throws DataCorruptionException {
        List<GameFrame.Block> blocks = new ArrayList<>();

        for (Document blockDoc : blockDocs) {
            String name = blockDoc.getString("name");
            int row = blockDoc.getInteger("row", 0);
            int col = blockDoc.getInteger("col", 0);
            int width = blockDoc.getInteger("width", 1);
            int height = blockDoc.getInteger("height", 1);
            String colorStr = blockDoc.getString("color");

            javafx.scene.paint.Color color;
            try {
                color = javafx.scene.paint.Color.valueOf(colorStr);
            } catch (Exception e) {
                throw new DataCorruptionException("无效的颜色值: " + colorStr);
            }

            blocks.add(new GameFrame.Block(row, col, width, height, color, name));
        }

        return blocks;
    }

    // 修复：更新现有的showAlert方法
    private void showAlert(String title, String header, String content, Alert.AlertType type) {
        showAdvancedAlert(title, header, content, type);
    }

    private void playReplay(String username, HistoryRecord record, Stage parentStage) {
        try {
            MongoDBUtil db = new MongoDBUtil();
            MongoCollection<Document> col = db.getCollection("game_history");
            Document doc = col.find(
                    Filters.and(
                            Filters.eq("username", username),
                            Filters.eq("saveTime", record.getSaveTime()),
                            Filters.eq("moveCount", record.getMoveCount()),
                            Filters.eq("elapsedTime", record.getElapsedTime())
                    )
            ).first();

            if (doc != null) {
                List<Document> blockDocs = (List<Document>) doc.get("blocks");
                List<String> historyStack = (List<String>) doc.get("historyStack");
                String layoutName = doc.getString("layout");
                int layoutIndex = BoardLayouts.getLayoutNames().indexOf(layoutName);

                List<GameFrame.Block> layoutBlocks = BoardLayouts.getLayout(layoutIndex);

                // 打开回放窗口
                showReplayStage(layoutBlocks, historyStack, layoutName, parentStage);
            } else {
                showAlert("错误", "回放失败", "未找到存档记录", Alert.AlertType.ERROR);
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("错误", "回放失败", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showReplayStage(List<GameFrame.Block> layoutBlocks, List<String> historyStack, String layoutName, Stage parentStage) {
        Stage stage = new Stage();
        stage.setTitle("回放 - " + layoutName);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 32, 24, 32));
        root.setAlignment(Pos.CENTER);
        // 使用CSS类替代内联样式
        root.getStyleClass().add("main-background");

        Label title = new Label("回放 - " + layoutName);
        title.setFont(Font.font("微软雅黑", 22));
        // 使用CSS类替代内联样式
        title.getStyleClass().add("section-title");

        // 步数显示
        Label stepLabel = new Label();
        stepLabel.setFont(Font.font("微软雅黑", 16));
        // 使用CSS类替代内联样式
        stepLabel.getStyleClass().add("step-label");

        // 回放棋盘
        ReplayBoardPane boardPane = new ReplayBoardPane(layoutBlocks, historyStack, stepLabel);

        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("上一步");
        Button nextBtn = new Button("下一步");
        prevBtn.setPrefWidth(100);
        nextBtn.setPrefWidth(100);
        // 使用CSS类替代内联样式
        prevBtn.getStyleClass().add("menu-button");
        nextBtn.getStyleClass().add("menu-button");

        prevBtn.setOnAction(e -> boardPane.prevStep());
        nextBtn.setOnAction(e -> boardPane.nextStep());

        btnBox.getChildren().addAll(prevBtn, nextBtn);

        root.getChildren().addAll(title, stepLabel, boardPane, btnBox);

        Scene scene = new Scene(root, 520, 700);
        // 加载CSS样式
        loadCSS(scene);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // 修复：在 showWeChatStyleChat 方法中添加本地消息跟踪
    private void showWeChatStyleChat(String currentUser, String otherUser, String sourceType) {
        Stage chatStage = new Stage();
        chatStage.setTitle("与 " + otherUser + " 的聊天");
        chatStage.setResizable(true);

        // 窗口同步绑定代码保持不变...
        chatStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - chatStage.getWidth()) > 2) {
                chatStage.setWidth(newVal.doubleValue());
            }
        });
        chatStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - chatStage.getHeight()) > 2) {
                chatStage.setHeight(newVal.doubleValue());
            }
        });
        chatStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - chatStage.getX()) > 2) {
                chatStage.setX(newVal.doubleValue());
            }
        });
        chatStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - chatStage.getY()) > 2) {
                chatStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步窗口位置和大小
        chatStage.setX(mainStage.getX());
        chatStage.setY(mainStage.getY());
        chatStage.setWidth(mainStage.getWidth());
        chatStage.setHeight(mainStage.getHeight());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("chat-background");

        // Header部分代码保持不变...
        HBox header = new HBox();
        header.setPadding(new Insets(10, 15, 10, 15));
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            if (chatRefreshTimeline != null) {
                chatRefreshTimeline.stop();
            }

            double chatX = chatStage.getX();
            double chatY = chatStage.getY();
            double chatWidth = chatStage.getWidth();
            double chatHeight = chatStage.getHeight();

            chatStage.close();

            Platform.runLater(() -> {
                if ("mailbox".equals(sourceType)) {
                    preloadedChatData.remove(currentUser);
                    showPrivateChatListDirectly(currentUser, chatX, chatY, chatWidth, chatHeight);
                } else {
                    mainStage.setX(chatX);
                    mainStage.setY(chatY);
                    mainStage.setWidth(chatWidth);
                    mainStage.setHeight(chatHeight);
                    showFriends(currentUser);
                }
            });
        });

        Label titleLabel = new Label("与 " + otherUser + " 的聊天");
        titleLabel.setFont(Font.font("微软雅黑", 18));
        titleLabel.getStyleClass().add("chat-header-title");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        header.getChildren().clear();
        header.getChildren().addAll(backBtn, spacer1, titleLabel, spacer2);

        root.setTop(header);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("chat-scroll-pane");

        VBox chatArea = new VBox(10);
        chatArea.setPadding(new Insets(15));
        chatArea.getStyleClass().add("chat-area");

        // 加载指示器代码保持不变...
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(40, 40);

        Label loadingLabel = new Label("正在加载聊天记录...");
        loadingLabel.setFont(Font.font("微软雅黑", 14));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(10);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(50));
        loadingBox.getChildren().addAll(loadingIndicator, loadingLabel);

        chatArea.getChildren().add(loadingBox);

        scrollPane.setContent(chatArea);
        root.setCenter(scrollPane);

        VBox bottomArea = new VBox(10);
        bottomArea.setPadding(new Insets(10, 15, 15, 15));
        bottomArea.getStyleClass().add("chat-bottom-area");

        TextArea messageInput = new TextArea();
        messageInput.setPromptText("输入消息...");
        messageInput.setPrefRowCount(3);
        messageInput.setMaxHeight(80);
        messageInput.setWrapText(true);
        messageInput.getStyleClass().add("chat-input");

        HBox buttonArea = new HBox(10);
        buttonArea.setAlignment(Pos.CENTER_RIGHT);

        Button sendBtn = new Button("发送");
        sendBtn.setPrefWidth(80);
        sendBtn.getStyleClass().add("send-button");

        buttonArea.getChildren().add(sendBtn);
        bottomArea.getChildren().addAll(messageInput, buttonArea);
        root.setBottom(bottomArea);

        // 修复：添加本地发送消息跟踪
        final long[] lastMessageTimestamp = {0};
        final java.util.Set<String> localSentMessages = new java.util.HashSet<>(); // 跟踪本地发送的消息

        // 异步加载聊天消息
        loadChatMessagesAsync(chatArea, currentUser, otherUser, loadingBox, scrollPane, lastMessageTimestamp);

        // 修复：改进发送消息方法，添加本地消息跟踪
        Runnable sendMessage = () -> {
            String message = messageInput.getText().trim();
            if (!message.isEmpty()) {
                String currentTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 修复：生成唯一标识符跟踪这条本地消息
                String messageId = currentUser + ":" + message + ":" + currentTime;
                localSentMessages.add(messageId);

                VBox messageBox = createMessageBubble(message, currentTime, true);
                chatArea.getChildren().add(messageBox);

                messageInput.clear();
                scrollToBottomSmoothly(scrollPane);

                Thread saveThread = new Thread(() -> {
                    try {
                        MongoDBUtil db = new MongoDBUtil();
                        MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                        long timestamp = System.currentTimeMillis();
                        Document messageDoc = new Document()
                                .append("from", currentUser)
                                .append("to", otherUser)
                                .append("message", message)
                                .append("timestamp", timestamp)
                                .append("time", currentTime)
                                .append("read", false);

                        messagesCol.insertOne(messageDoc);
                        db.close();

                        lastMessageTimestamp[0] = timestamp;

                        // 修复：成功保存到云端后，延迟移除本地跟踪
                        Platform.runLater(() -> {
                            preloadedChatData.remove(currentUser);

                            // 5秒后移除本地消息跟踪，避免永久累积
                            javafx.animation.Timeline removeTrack = new javafx.animation.Timeline(
                                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> {
                                        localSentMessages.remove(messageId);
                                    })
                            );
                            removeTrack.play();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            // 修复：发送失败时移除本地跟踪，避免影响后续刷新
                            localSentMessages.remove(messageId);
                            showAlert("错误", "发送私信失败", "网络错误，消息可能未发送成功", Alert.AlertType.WARNING);
                        });
                    }
                });

                saveThread.setDaemon(true);
                saveThread.start();
            }
        };

        sendBtn.setOnAction(e -> sendMessage.run());

        messageInput.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && e.isControlDown()) {
                sendMessage.run();
                e.consume();
            }
        });

        // 修复：启动自动刷新功能，传入本地消息跟踪集合
        startChatAutoRefresh(chatArea, currentUser, otherUser, scrollPane, lastMessageTimestamp, localSentMessages);

        chatStage.setOnCloseRequest(e -> {
            if (chatRefreshTimeline != null) {
                chatRefreshTimeline.stop();
            }
        });

        Scene scene = new Scene(root);
        loadCSS(scene);
        chatStage.setScene(scene);
        chatStage.show();
    }

    // 修复：更新启动聊天自动刷新方法
    private void startChatAutoRefresh(VBox chatArea, String currentUser, String otherUser, ScrollPane scrollPane, long[] lastMessageTimestamp, java.util.Set<String> localSentMessages) {
        if (chatRefreshTimeline != null) {
            chatRefreshTimeline.stop();
        }

        chatRefreshTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    checkForNewMessages(chatArea, currentUser, otherUser, scrollPane, lastMessageTimestamp, localSentMessages);
                })
        );
        chatRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        chatRefreshTimeline.play();
    }

    //金币
    private int getUserCoins(String username) {
        if ("离线用户".equals(username)) {
            return 0;
        }
        int coins = 0;
        try {
            MongoDBUtil db = new MongoDBUtil();
            Document userDoc = db.getUserByUsername(username);
            if (userDoc != null && userDoc.containsKey("coins")) {
                coins = userDoc.getInteger("coins", 0);
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return coins;
    }

    // 在线对局记录类
    public static class OnlineGameRecord {
        public String roomId;
        public String host;
        public String layoutName; // 新增
        public String elapsedTime;
        public int moveCount;
        public OnlineGameRecord(String roomId, String host, String layoutName, String elapsedTime, int moveCount) {
            this.roomId = roomId;
            this.host = host;
            this.layoutName = layoutName;
            this.elapsedTime = elapsedTime;
            this.moveCount = moveCount;
        }
        public String getHost() { return host; }
        public String getLayoutName() { return layoutName; }
        public String getElapsedTime() { return elapsedTime; }
        public int getMoveCount() { return moveCount; }
    }

    public static class MailRecord {
        private String from;
        private String status;

        public MailRecord(String from, String status) {
            this.from = from;
            this.status = status;
        }

        public String getFrom() { return from; }
        public String getStatus() { return status; }
    }

    public static class ReplayBoardPane extends GridPane {
        private List<GameFrame.Block> blocks;
        private List<String> historyStack;
        private Label stepLabel;
        private int currentStep = 0;

        public ReplayBoardPane(List<GameFrame.Block> blocks, List<String> historyStack, Label stepLabel) {
            this.blocks = new ArrayList<>(blocks);
            this.historyStack = historyStack != null ? historyStack : new ArrayList<>();
            this.stepLabel = stepLabel;

            setupBoard();
            updateStepLabel();
        }

        // 修复：确保棋盘为5行4列，格子和边框完全对齐
        private void setupBoard() {
            this.getChildren().clear();
            this.setPrefSize(320, 400); // 修复：4列×80像素=320，5行×80像素=400
            this.setMaxSize(320, 400);
            this.setMinSize(320, 400);

            // 修复：关键 - 设置GridPane的间距和内边距为0，确保格子完全贴合
            this.setHgap(0);
            this.setVgap(0);
            this.setPadding(new Insets(0));
            this.setAlignment(Pos.TOP_LEFT);

            // 修复：设置整个棋盘的边框样式
            this.setStyle("-fx-background-color: #8b7355; -fx-border-color: #654321; -fx-border-width: 2px;");
            this.getStyleClass().add("replay-board");

            // 修复：创建5行4列网格背景，格子大小精确计算
            for (int row = 0; row < 5; row++) {        // 5行
                for (int col = 0; col < 4; col++) {    // 4列
                    Rectangle cell = new Rectangle(80, 80); // 修复：80x80的正方形格子
                    cell.setFill(Color.web("#D2B48C")); // 修复：设置格子背景色为浅棕色
                    cell.setStroke(Color.web("#8b7355")); // 修复：格子边框颜色
                    cell.setStrokeWidth(1); // 修复：细边框
                    cell.setStrokeType(javafx.scene.shape.StrokeType.INSIDE); // 修复：关键 - 边框向内绘制，避免超出格子范围

                    // 修复：确保格子完全填充GridPane的单元格
                    this.add(cell, col, row);
                    GridPane.setHalignment(cell, HPos.CENTER);
                    GridPane.setValignment(cell, VPos.CENTER);
                }
            }

            drawBlocks();
        }

        private void drawBlocks() {
            // 清除现有的方块（保留格子背景）
            this.getChildren().removeIf(node -> node instanceof StackPane);

            for (GameFrame.Block block : blocks) {
                StackPane blockPane = new StackPane();

                // 修复：方块大小计算 - 使用80x80的格子大小，稍微缩小以显示格子边框
                double blockWidth = block.getWidth() * 80 - 2; // 减去2像素显示格子边框
                double blockHeight = block.getHeight() * 80 - 2; // 减去2像素显示格子边框

                Rectangle rect = new Rectangle(blockWidth, blockHeight);
                rect.setFill(block.getColor());
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(2);
                rect.setArcWidth(8);
                rect.setArcHeight(8);

                Label nameLabel = new Label(block.getName());
                nameLabel.setFont(Font.font("微软雅黑", 14));
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                blockPane.getChildren().addAll(rect, nameLabel);

                // 修复：精确定位方块，确保与格子对齐
                this.add(blockPane, block.getCol(), block.getRow(), block.getWidth(), block.getHeight());
                GridPane.setHalignment(blockPane, HPos.CENTER);
                GridPane.setValignment(blockPane, VPos.CENTER);
                GridPane.setMargin(blockPane, new Insets(1)); // 添加1像素边距，确保不完全覆盖格子边框
            }
        }

        public void nextStep() {
            if (currentStep < historyStack.size()) {
                applyMove(historyStack.get(currentStep));
                currentStep++;
                updateStepLabel();
                drawBlocks();
            }
        }

        public void prevStep() {
            if (currentStep > 0) {
                currentStep--;
                undoMove(historyStack.get(currentStep));
                updateStepLabel();
                drawBlocks();
            }
        }

        private void applyMove(String move) {
            // 解析移动字符串并应用到方块
            // 格式: "blockName:fromRow,fromCol:toRow,toCol"
            String[] parts = move.split(":");
            if (parts.length == 3) {
                String blockName = parts[0];
                String[] from = parts[1].split(",");
                String[] to = parts[2].split(",");

                for (GameFrame.Block block : blocks) {
                    if (block.getName().equals(blockName)) {
                        block.setRow(Integer.parseInt(to[0]));
                        block.setCol(Integer.parseInt(to[1]));
                        break;
                    }
                }
            }
        }

        private void undoMove(String move) {
            // 撤销移动
            String[] parts = move.split(":");
            if (parts.length == 3) {
                String blockName = parts[0];
                String[] from = parts[1].split(",");

                for (GameFrame.Block block : blocks) {
                    if (block.getName().equals(blockName)) {
                        block.setRow(Integer.parseInt(from[0]));
                        block.setCol(Integer.parseInt(from[1]));
                        break;
                    }
                }
            }
        }

        private void updateStepLabel() {
            if (stepLabel != null) {
                stepLabel.setText("步骤: " + currentStep + " / " + historyStack.size());
            }
        }

        public void setBlocks(List<GameFrame.Block> newBlocks) {
            this.blocks = new ArrayList<>(newBlocks);
            drawBlocks();
        }
    }

    private void showMailbox(String username) {
        Stage mailboxStage = new Stage();
        mailboxStage.setTitle("信箱中心");
        mailboxStage.setResizable(true);

        // 窗口同步绑定（与主界面保持一致）
        mailboxStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mailboxStage.getWidth()) > 2) {
                mailboxStage.setWidth(newVal.doubleValue());
            }
        });
        mailboxStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mailboxStage.getHeight()) > 2) {
                mailboxStage.setHeight(newVal.doubleValue());
            }
        });
        mailboxStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mainStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mailboxStage.getX()) > 2) {
                mailboxStage.setX(newVal.doubleValue());
            }
        });
        mailboxStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });
        mainStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mailboxStage.getY()) > 2) {
                mailboxStage.setY(newVal.doubleValue());
            }
        });

        // 初始同步
        mailboxStage.setX(mainStage.getX());
        mailboxStage.setY(mainStage.getY());
        mailboxStage.setWidth(mainStage.getWidth());
        mailboxStage.setHeight(mainStage.getHeight());

        // 添加关闭事件处理
        mailboxStage.setOnCloseRequest(e -> {
            mainStage.setX(mailboxStage.getX());
            mainStage.setY(mailboxStage.getY());
            mainStage.setWidth(mailboxStage.getWidth());
            mainStage.setHeight(mailboxStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        // 修复：减少整体边距和间距，紧凑排版
        VBox root = new VBox(20); // 减少间距从30到20
        root.setPadding(new Insets(25, 40, 25, 40)); // 减少上下边距从40到25
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：返回按钮区域 - 显著增加按钮宽度
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0)); // 减少底部边距

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(180); // 显著增加按钮宽度，从150到180
        backBtn.setPrefHeight(30); // 略微增加高度
        backBtn.setMinWidth(180); // 设置最小宽度
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            mailboxStage.close();
            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 修复：紧凑的标题区域 - 减少间距和尺寸
        VBox titleArea = new VBox(8); // 减少间距从10到8
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("📬");
        titleIcon.setFont(Font.font("微软雅黑", 32)); // 减少图标大小从36到32
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("信箱中心");
        title.setFont(Font.font("微软雅黑", 24)); // 减少标题字体从28到24
        title.getStyleClass().add("mailbox-main-title");

        Label subtitle = new Label("管理您的好友申请和私信消息");
        subtitle.setFont(Font.font("微软雅黑", 14)); // 减少副标题字体从16到14
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 修复：紧凑的状态显示区域
        VBox statusArea = new VBox(8); // 减少间距
        statusArea.setAlignment(Pos.CENTER);

        ProgressIndicator statusProgress = new ProgressIndicator();
        statusProgress.setPrefSize(20, 20); // 减少进度指示器大小
        statusProgress.getStyleClass().add("mailbox-progress");

        Label statusLabel = new Label("正在检查未读消息...");
        statusLabel.setFont(Font.font("微软雅黑", 10)); // 减少字体大小
        statusLabel.getStyleClass().add("loading-label");

        statusArea.getChildren().addAll(statusProgress, statusLabel);

        // 修复：功能按钮区域 - 减少间距
        HBox buttonArea = new HBox(25); // 减少间距从30到25
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(20, 0, 0, 0)); // 减少上边距

        // 好友申请卡片
        VBox friendRequestCard = createMailboxCard(
                "👥", "好友申请", "查看和处理好友申请",
                "friend-request-card",
                e -> showFriendRequestsAndClose(username, mailboxStage)
        );

        // 私信聊天卡片
        VBox privateChatCard = createMailboxCard(
                "💬", "私信聊天", "查看和回复私信消息",
                "private-chat-card",
                e -> showPrivateChatListAndClose(username, mailboxStage)
        );

        buttonArea.getChildren().addAll(friendRequestCard, privateChatCard);

        root.getChildren().addAll(headerBox, titleArea, statusArea, buttonArea);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        mailboxStage.setScene(scene);

        mailboxStage.show();
        loadMailboxStatusAsync(username, statusArea, friendRequestCard, privateChatCard);
    }

    // 修复：更新信箱功能卡片创建方法，确保图标颜色正确
    private VBox createMailboxCard(String icon, String title, String description, String styleClass, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(12); // 减少间距
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 18, 20, 18)); // 减少内边距
        card.setPrefWidth(280); // 略微减少宽度
        card.setMaxWidth(280);
        card.setPrefHeight(180); // 减少高度
        card.getStyleClass().add("mailbox-card");
        card.getStyleClass().add(styleClass);

        // 图标
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("微软雅黑", 32)); // 减少图标大小
        iconLabel.getStyleClass().add("feature-icon");

        // 标题
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("微软雅黑", 18)); // 减少标题字体
        titleLabel.getStyleClass().add("mailbox-card-title");

        // 描述
        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("微软雅黑", 13)); // 减少描述字体
        descLabel.getStyleClass().add("mailbox-card-description");
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLabel.setWrapText(true);

        // 未读数量标签（初始隐藏）
        Label unreadBadge = new Label();
        unreadBadge.getStyleClass().add("unread-badge");
        unreadBadge.setVisible(false);
        unreadBadge.setManaged(false);

        // 进入按钮
        Button enterBtn = new Button("进入 →");
        enterBtn.setFont(Font.font("微软雅黑", 15)); // 减少按钮字体
        enterBtn.setPrefWidth(110); // 减少按钮宽度
        enterBtn.setPrefHeight(38); // 减少按钮高度
        enterBtn.getStyleClass().add("mailbox-enter-button");
        enterBtn.setOnAction(action);

        card.getChildren().addAll(iconLabel, titleLabel, descLabel, unreadBadge, enterBtn);

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("mailbox-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("mailbox-card-hover"));

        return card;
    }

    // 异步加载信箱状态
    private void loadMailboxStatusAsync(String username, VBox statusArea, VBox friendRequestCard, VBox privateChatCard) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();

                // 统计未读好友申请
                long friendRequests = db.getCollection("mailbox").countDocuments(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("to", username),
                                Filters.eq("status", "pending")
                        )
                );

                // 统计未读私信
                long unreadMessages = db.getCollection("private_messages").countDocuments(
                        Filters.and(
                                Filters.eq("to", username),
                                Filters.eq("read", false)
                        )
                );

                db.close();

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    // 移除加载状态
                    statusArea.getChildren().clear();

                    // 创建状态显示
                    VBox statusDisplay = new VBox(8);
                    statusDisplay.setAlignment(Pos.CENTER);

                    if (friendRequests > 0 || unreadMessages > 0) {

                        Label statusText = new Label(String.format("您有 %d 个好友申请和 %d 条未读私信", friendRequests, unreadMessages));
                        statusText.setFont(Font.font("微软雅黑", 16));
                        statusText.getStyleClass().add("mailbox-status-active");

                        statusDisplay.getChildren().addAll( statusText);

                        // 更新卡片上的未读标记
                        updateCardUnreadBadge(friendRequestCard, (int) friendRequests);
                        updateCardUnreadBadge(privateChatCard, (int) unreadMessages);
                    } else {

                        Label statusText = new Label("暂无未读消息");
                        statusText.setFont(Font.font("微软雅黑", 10));
                        statusText.getStyleClass().add("mailbox-status-empty");

                        statusDisplay.getChildren().addAll( statusText);
                    }

                    statusArea.getChildren().add(statusDisplay);
                });

                // 开始预加载好友申请数据
                preloadFriendRequestsAsync(username);

                // 开始预加载聊天列表数据
                preloadPrivateChatListAsync(username);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // 显示错误状态
                    statusArea.getChildren().clear();

                    VBox errorDisplay = new VBox(8);
                    errorDisplay.setAlignment(Pos.CENTER);

                    Label errorIcon = new Label("⚠️");
                    errorIcon.setFont(Font.font("微软雅黑", 24));

                    Label errorText = new Label("获取消息状态失败");
                    errorText.setFont(Font.font("微软雅黑", 16));
                    errorText.getStyleClass().add("mailbox-status-error");

                    errorDisplay.getChildren().addAll(errorIcon, errorText);
                    statusArea.getChildren().add(errorDisplay);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 预加载好友申请数据
    private void preloadFriendRequestsAsync(String username) {
        Thread preloadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> mailbox = db.getCollection("mailbox");
                FindIterable<Document> docs = mailbox.find(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("to", username)
                        )
                ).sort(Sorts.descending("time"));

                List<MailRecord> data = new ArrayList<>();
                for (Document doc : docs) {
                    String from = doc.getString("from");
                    String status = doc.getString("status");
                    data.add(new MailRecord(from, status));
                }
                db.close();

                // 缓存数据
                preloadedFriendRequestData.put(username, data);

            } catch (Exception e) {
                e.printStackTrace();
                // 预加载失败不影响主流程
            }
        });

        preloadThread.setDaemon(true);
        preloadThread.start();
    }

    // 预加载私信聊天列表数据
    private void preloadPrivateChatListAsync(String username) {
        Thread preloadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                java.util.Set<String> contacts = new java.util.HashSet<>();
                FindIterable<Document> docs = messagesCol.find(
                        Filters.or(
                                Filters.eq("from", username),
                                Filters.eq("to", username)
                        )
                );

                for (Document doc : docs) {
                    String from = doc.getString("from");
                    String to = doc.getString("to");
                    if (!from.equals(username)) {
                        contacts.add(from);
                    }
                    if (!to.equals(username)) {
                        contacts.add(to);
                    }
                }

                List<ChatListRecord> data = new ArrayList<>();
                for (String contact : contacts) {
                    Document lastMsg = messagesCol.find(
                            Filters.or(
                                    Filters.and(Filters.eq("from", username), Filters.eq("to", contact)),
                                    Filters.and(Filters.eq("from", contact), Filters.eq("to", username))
                            )
                    ).sort(Sorts.descending("timestamp")).first();

                    String lastMessage = lastMsg != null ? lastMsg.getString("message") : "暂无消息";
                    long lastMessageTime = lastMsg != null ? lastMsg.getLong("timestamp") : System.currentTimeMillis();

                    long unreadCount = messagesCol.countDocuments(
                            Filters.and(
                                    Filters.eq("from", contact),
                                    Filters.eq("to", username),
                                    Filters.eq("read", false)
                            )
                    );

                    data.add(new ChatListRecord(contact, lastMessage, (int)unreadCount, lastMessageTime));
                }

                db.close();

                // 缓存数据
                preloadedChatData.put(username, data);

            } catch (Exception e) {
                e.printStackTrace();
                // 预加载失败不影响主流程
            }
        });

        preloadThread.setDaemon(true);
        preloadThread.start();
    }

    // 更新卡片未读标记
    private void updateCardUnreadBadge(VBox card, int unreadCount) {
        // 找到未读标记标签
        Label unreadBadge = null;
        for (javafx.scene.Node child : card.getChildren()) {
            if (child instanceof Label && child.getStyleClass().contains("unread-badge")) {
                unreadBadge = (Label) child;
                break;
            }
        }

        if (unreadBadge != null && unreadCount > 0) {
            unreadBadge.setText(String.valueOf(unreadCount));
            unreadBadge.setVisible(true);
            unreadBadge.setManaged(true);
        }
    }

    // showPrivateChatListAndClose 方法中的返回按钮逻辑 - 修复重复新建信箱窗口的问题
    private void showPrivateChatListAndClose(String username, Stage fromMailboxStage) {
        Stage chatListStage = new Stage();
        chatListStage.setTitle("私信聊天");
        chatListStage.setResizable(true);

        // 继承信箱窗口的位置和大小
        chatListStage.setX(fromMailboxStage.getX());
        chatListStage.setY(fromMailboxStage.getY());
        chatListStage.setWidth(fromMailboxStage.getWidth());
        chatListStage.setHeight(fromMailboxStage.getHeight());

        // 窗口同步绑定
        chatListStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        chatListStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        chatListStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        chatListStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });

        // 关闭事件处理
        chatListStage.setOnCloseRequest(e -> {
            mainStage.setX(chatListStage.getX());
            mainStage.setY(chatListStage.getY());
            mainStage.setWidth(chatListStage.getWidth());
            mainStage.setHeight(chatListStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        VBox root = new VBox(25);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：返回按钮 - 直接显示原信箱窗口而不是新建
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← 返回信箱");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            chatListStage.close();
            // 直接显示原信箱窗口，不新建
            fromMailboxStage.show();
            fromMailboxStage.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 修复：标题区域 - 确保图标显示正确颜色
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("💬");
        titleIcon.setFont(Font.font("微软雅黑", 25));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("私信聊天");
        title.setFont(Font.font("微软雅黑", 20));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("查看和回复私信消息");
        subtitle.setFont(Font.font("微软雅黑", 12));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 重新加载数据（因为可能有新消息）
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.getStyleClass().add("mailbox-progress");

        Label loadingLabel = new Label("正在刷新聊天列表...");
        loadingLabel.setFont(Font.font("微软雅黑", 16));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

        root.getChildren().addAll(headerBox, titleArea, loadingBox);

        // 异步加载最新数据
        loadPrivateChatListAsync(username, root, loadingBox, chatListStage);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        chatListStage.setScene(scene);
        chatListStage.show();

        // 先显示新窗口，然后隐藏（不关闭）原信箱窗口
        Platform.runLater(() -> {
            fromMailboxStage.hide();
        });
    }

    // 带位置参数的信箱显示方法
    private void showMailboxWithPosition(String username, double x, double y, double width, double height) {
        Stage mailboxStage = new Stage();
        mailboxStage.setTitle("信箱中心");
        mailboxStage.setResizable(true);

        // 设置指定的位置和大小
        mailboxStage.setX(x);
        mailboxStage.setY(y);
        mailboxStage.setWidth(width);
        mailboxStage.setHeight(height);

        // 简化窗口同步绑定
        mailboxStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        mailboxStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        mailboxStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        mailboxStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });

        mailboxStage.setOnCloseRequest(e -> {
            mainStage.setX(mailboxStage.getX());
            mainStage.setY(mailboxStage.getY());
            mainStage.setWidth(mailboxStage.getWidth());
            mainStage.setHeight(mailboxStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        // 修复：应用相同的紧凑布局
        VBox root = new VBox(20); // 减少间距
        root.setPadding(new Insets(25, 40, 25, 40)); // 减少边距
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：返回按钮区域 - 使用相同的大尺寸
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        Button backBtn = new Button("← 返回");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(180); // 使用相同的大宽度
        backBtn.setPrefHeight(42);
        backBtn.setMinWidth(180);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            mailboxStage.close();
            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域 - 使用相同的紧凑设计
        VBox titleArea = new VBox(8);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("📬");
        titleIcon.setFont(Font.font("微软雅黑", 32));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("信箱中心");
        title.setFont(Font.font("微软雅黑", 24));
        title.getStyleClass().add("mailbox-main-title");

        Label subtitle = new Label("管理您的好友申请和私信消息");
        subtitle.setFont(Font.font("微软雅黑", 14));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 状态显示区域 - 使用相同的紧凑设计
        VBox statusArea = new VBox(8);
        statusArea.setAlignment(Pos.CENTER);

        ProgressIndicator statusProgress = new ProgressIndicator();
        statusProgress.setPrefSize(35, 35);
        statusProgress.getStyleClass().add("mailbox-progress");

        Label statusLabel = new Label("正在检查未读消息...");
        statusLabel.setFont(Font.font("微软雅黑", 13));
        statusLabel.getStyleClass().add("loading-label");

        statusArea.getChildren().addAll(statusProgress, statusLabel);

        // 功能按钮区域 - 使用相同的紧凑设计
        HBox buttonArea = new HBox(25);
        buttonArea.setAlignment(Pos.CENTER);
        buttonArea.setPadding(new Insets(20, 0, 0, 0));

        // 好友申请卡片
        VBox friendRequestCard = createMailboxCard(
                "👥", "好友申请", "查看和处理好友申请",
                "friend-request-card",
                e -> showFriendRequestsAndClose(username, mailboxStage)
        );

        // 私信聊天卡片
        VBox privateChatCard = createMailboxCard(
                "💬", "私信聊天", "查看和回复私信消息",
                "private-chat-card",
                e -> showPrivateChatListAndClose(username, mailboxStage)
        );

        buttonArea.getChildren().addAll(friendRequestCard, privateChatCard);

        root.getChildren().addAll(headerBox, titleArea, statusArea, buttonArea);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        mailboxStage.setScene(scene);

        mailboxStage.show();
        loadMailboxStatusAsync(username, statusArea, friendRequestCard, privateChatCard);
    }

    // 在预加载数据的显示中也使用卡片布局
    private void showFriendRequestsAndClose(String username, Stage mailboxStage) {
        Stage friendRequestStage = new Stage();
        friendRequestStage.setTitle("好友申请");
        friendRequestStage.setResizable(true);

        // 继承信箱窗口的位置和大小
        friendRequestStage.setX(mailboxStage.getX());
        friendRequestStage.setY(mailboxStage.getY());
        friendRequestStage.setWidth(mailboxStage.getWidth());
        friendRequestStage.setHeight(mailboxStage.getHeight());

        // 窗口同步绑定
        friendRequestStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        friendRequestStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        friendRequestStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        friendRequestStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });

        // 关闭事件处理
        friendRequestStage.setOnCloseRequest(e -> {
            mainStage.setX(friendRequestStage.getX());
            mainStage.setY(friendRequestStage.getY());
            mainStage.setWidth(friendRequestStage.getWidth());
            mainStage.setHeight(friendRequestStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        VBox root = new VBox(25);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：返回按钮 - 直接显示原信箱窗口而不是新建
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← 返回信箱");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            friendRequestStage.close();
            // 直接显示原信箱窗口，不新建
            mailboxStage.show();
            mailboxStage.toFront();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 修复：标题区域 - 确保图标显示正确颜色
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("👥");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("好友申请");
        title.setFont(Font.font("微软雅黑", 28));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("管理您的好友申请");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 检查是否有预加载的数据
        if (preloadedFriendRequestData.containsKey(username)) {
            List<MailRecord> data = preloadedFriendRequestData.get(username);

            if (data.isEmpty()) {
                VBox emptyStateBox = createFriendRequestEmptyState();
                root.getChildren().addAll(headerBox, titleArea, emptyStateBox);
            } else {
                // 使用新的卡片布局替代表格
                VBox requestCards = createFriendRequestCards(username, data);

                ScrollPane cardsScrollPane = new ScrollPane(requestCards);
                cardsScrollPane.setFitToWidth(true);
                cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                cardsScrollPane.getStyleClass().add("friend-request-cards-scroll");
                cardsScrollPane.setPrefHeight(400);

                root.getChildren().addAll(headerBox, titleArea, cardsScrollPane);
            }
        } else {
            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefSize(50, 50);
            progressIndicator.getStyleClass().add("mailbox-progress");

            Label loadingLabel = new Label("正在加载好友申请...");
            loadingLabel.setFont(Font.font("微软雅黑", 16));
            loadingLabel.getStyleClass().add("loading-label");

            VBox loadingBox = new VBox(15);
            loadingBox.setAlignment(Pos.CENTER);
            loadingBox.setPadding(new Insets(60));
            loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

            root.getChildren().addAll(headerBox, titleArea, loadingBox);
            loadFriendRequestsAsync(username, root, loadingBox);
        }

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        friendRequestStage.setScene(scene);
        friendRequestStage.show();

        // 先显示新窗口，然后隐藏（不关闭）原信箱窗口
        Platform.runLater(() -> {
            mailboxStage.hide();
        });
    }

    // 在 loadFriendRequestsAsync 方法中使用新的卡片布局
    private void loadFriendRequestsAsync(String username, VBox root, VBox loadingBox) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> mailbox = db.getCollection("mailbox");
                FindIterable<Document> docs = mailbox.find(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("to", username)
                        )
                ).sort(Sorts.descending("time"));

                List<MailRecord> data = new ArrayList<>();
                for (Document doc : docs) {
                    String from = doc.getString("from");
                    String status = doc.getString("status");
                    data.add(new MailRecord(from, status));
                }
                db.close();

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    if (data.isEmpty()) {
                        // 显示空状态
                        VBox emptyStateBox = createFriendRequestEmptyState();
                        root.getChildren().add(emptyStateBox);
                    } else {
                        // 创建好友申请卡片（替代表格）
                        VBox requestCards = createFriendRequestCards(username, data);

                        // 将卡片放在ScrollPane中
                        ScrollPane cardsScrollPane = new ScrollPane(requestCards);
                        cardsScrollPane.setFitToWidth(true);
                        cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                        cardsScrollPane.getStyleClass().add("friend-request-cards-scroll");
                        cardsScrollPane.setPrefHeight(400);

                        root.getChildren().add(cardsScrollPane);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    // 显示错误状态
                    VBox errorStateBox = createErrorStateBox("加载好友申请失败", () -> {
                        root.getChildren().clear();
                        VBox titleArea = (VBox) root.getChildren().get(0); // 保留标题
                        root.getChildren().addAll(titleArea, loadingBox);
                        loadFriendRequestsAsync(username, root, loadingBox);
                    });
                    root.getChildren().add(errorStateBox);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 创建好友申请空状态
    private VBox createFriendRequestEmptyState() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));

        Label emptyIcon = new Label("👥");
        emptyIcon.setFont(Font.font("微软雅黑", 48));
        emptyIcon.getStyleClass().add("feature-icon"); // 修复：确保图标显示原色

        Label emptyTitle = new Label("暂无好友申请");
        emptyTitle.setFont(Font.font("微软雅黑", 20));
        emptyTitle.getStyleClass().add("empty-state-title");

        Label emptyMessage = new Label("目前没有收到好友申请");
        emptyMessage.setFont(Font.font("微软雅黑", 14));
        emptyMessage.getStyleClass().add("empty-state-message");

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        return emptyBox;
    }


    // 统一的平滑滚动到底部方法
    private void scrollToBottomSmoothly(ScrollPane scrollPane) {
        // 使用多次延迟执行确保滚动生效
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);

            // 第一次延迟
            Platform.runLater(() -> {
                scrollPane.setVvalue(1.0);

                // 第二次延迟（使用Timeline确保在UI完全渲染后执行）
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), event -> {
                            scrollPane.setVvalue(1.0);
                        })
                );
                timeline.setCycleCount(2); // 执行2次
                timeline.play();

                // 最后一次保险滚动
                javafx.animation.Timeline finalScroll = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.millis(150), event -> {
                            scrollPane.setVvalue(1.0);
                        })
                );
                finalScroll.play();
            });
        });
    }

    // 修复：更新检查新消息方法，过滤本地已发送的消息
    private void checkForNewMessages(VBox chatArea, String currentUser, String otherUser, ScrollPane scrollPane, long[] lastMessageTimestamp, java.util.Set<String> localSentMessages) {
        Thread checkThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                FindIterable<Document> newDocs = messagesCol.find(
                        Filters.and(
                                Filters.or(
                                        Filters.and(Filters.eq("from", currentUser), Filters.eq("to", otherUser)),
                                        Filters.and(Filters.eq("from", otherUser), Filters.eq("to", currentUser))
                                ),
                                Filters.gt("timestamp", lastMessageTimestamp[0])
                        )
                ).sort(Sorts.ascending("timestamp"));

                List<VBox> newMessageBubbles = new ArrayList<>();
                List<String> messageIds = new ArrayList<>();
                long latestTimestamp = lastMessageTimestamp[0];

                for (Document doc : newDocs) {
                    String from = doc.getString("from");
                    String message = doc.getString("message");
                    String time = doc.getString("time");
                    long timestamp = doc.getLong("timestamp");

                    // 修复：检查是否为本地已发送的消息，如果是则跳过
                    String messageId = from + ":" + message + ":" + time;
                    if (localSentMessages.contains(messageId)) {
                        // 这是本地已发送的消息，跳过显示但更新时间戳
                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp;
                        }
                        continue;
                    }

                    VBox messageBox = createMessageBubble(message, time, from.equals(currentUser));
                    newMessageBubbles.add(messageBox);

                    if (from.equals(otherUser)) {
                        messageIds.add(doc.getObjectId("_id").toString());
                    }

                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                    }
                }

                // 批量标记消息为已读
                if (!messageIds.isEmpty()) {
                    for (String id : messageIds) {
                        messagesCol.updateOne(
                                Filters.eq("_id", new org.bson.types.ObjectId(id)),
                                new Document("$set", new Document("read", true))
                        );
                    }
                }

                db.close();

                if (!newMessageBubbles.isEmpty()) {
                    final long finalLatestTimestamp = latestTimestamp;
                    Platform.runLater(() -> {
                        chatArea.getChildren().removeIf(node -> {
                            if (node instanceof VBox) {
                                VBox vbox = (VBox) node;
                                return vbox.getChildren().stream().anyMatch(child ->
                                        child instanceof Label &&
                                                ((Label) child).getText().contains("暂无聊天记录"));
                            }
                            return false;
                        });

                        chatArea.getChildren().addAll(newMessageBubbles);
                        lastMessageTimestamp[0] = finalLatestTimestamp;
                        scrollToBottomSmoothly(scrollPane);
                    });
                } else {
                    // 修复：即使没有新消息要显示，也要更新时间戳
                    final long finalLatestTimestamp = latestTimestamp;
                    if (finalLatestTimestamp > lastMessageTimestamp[0]) {
                        Platform.runLater(() -> {
                            lastMessageTimestamp[0] = finalLatestTimestamp;
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        checkThread.setDaemon(true);
        checkThread.start();
    }

    //
    private javafx.animation.Timeline chatRefreshTimeline = null;

    // 检查新消息
    private void checkForNewMessages(VBox chatArea, String currentUser, String otherUser, ScrollPane scrollPane, long[] lastMessageTimestamp) {
        Thread checkThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                // 只查询比最后一条消息更新的消息
                FindIterable<Document> newDocs = messagesCol.find(
                        Filters.and(
                                Filters.or(
                                        Filters.and(Filters.eq("from", currentUser), Filters.eq("to", otherUser)),
                                        Filters.and(Filters.eq("from", otherUser), Filters.eq("to", currentUser))
                                ),
                                Filters.gt("timestamp", lastMessageTimestamp[0])
                        )
                ).sort(Sorts.ascending("timestamp"));

                List<VBox> newMessageBubbles = new ArrayList<>();
                List<String> messageIds = new ArrayList<>();
                long latestTimestamp = lastMessageTimestamp[0];

                for (Document doc : newDocs) {
                    String from = doc.getString("from");
                    String message = doc.getString("message");
                    String time = doc.getString("time");
                    long timestamp = doc.getLong("timestamp");

                    VBox messageBox = createMessageBubble(message, time, from.equals(currentUser));
                    newMessageBubbles.add(messageBox);

                    // 收集需要标记为已读的消息ID（对方发送的消息）
                    if (from.equals(otherUser)) {
                        messageIds.add(doc.getObjectId("_id").toString());
                    }

                    // 更新最新时间戳
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                    }
                }

                // 批量标记消息为已读
                if (!messageIds.isEmpty()) {
                    for (String id : messageIds) {
                        messagesCol.updateOne(
                                Filters.eq("_id", new org.bson.types.ObjectId(id)),
                                new Document("$set", new Document("read", true))
                        );
                    }
                }

                db.close();

                // 如果有新消息，在UI线程中添加
                if (!newMessageBubbles.isEmpty()) {
                    final long finalLatestTimestamp = latestTimestamp;
                    Platform.runLater(() -> {
                        // 移除可能存在的空状态提示
                        chatArea.getChildren().removeIf(node -> {
                            if (node instanceof VBox) {
                                VBox vbox = (VBox) node;
                                return vbox.getChildren().stream().anyMatch(child ->
                                        child instanceof Label &&
                                                ((Label) child).getText().contains("暂无聊天记录"));
                            }
                            return false;
                        });

                        // 添加新消息
                        chatArea.getChildren().addAll(newMessageBubbles);

                        // 更新最后消息时间戳
                        lastMessageTimestamp[0] = finalLatestTimestamp;

                        // 滚动到底部
                        scrollToBottomSmoothly(scrollPane);
                    });
                }

            } catch (Exception e) {
                // 静默处理异常，避免干扰用户体验
                e.printStackTrace();
            }
        });

        checkThread.setDaemon(true);
        checkThread.start();
    }

    // 修复：改进聊天消息加载和滚动逻辑
    private void loadChatMessagesAsync(VBox chatArea, String currentUser, String otherUser, VBox loadingBox, ScrollPane scrollPane, long[] lastMessageTimestamp) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                FindIterable<Document> docs = messagesCol.find(
                        Filters.or(
                                Filters.and(Filters.eq("from", currentUser), Filters.eq("to", otherUser)),
                                Filters.and(Filters.eq("from", otherUser), Filters.eq("to", currentUser))
                        )
                ).sort(Sorts.ascending("timestamp"));

                List<VBox> messageBubbles = new ArrayList<>();
                List<String> messageIds = new ArrayList<>();
                long latestTimestamp = 0;

                for (Document doc : docs) {
                    String from = doc.getString("from");
                    String message = doc.getString("message");
                    String time = doc.getString("time");
                    long timestamp = doc.getLong("timestamp");

                    VBox messageBox = createMessageBubble(message, time, from.equals(currentUser));
                    messageBubbles.add(messageBox);

                    // 收集需要标记为已读的消息ID
                    if (from.equals(otherUser)) {
                        messageIds.add(doc.getObjectId("_id").toString());
                    }

                    // 记录最新的时间戳
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                    }
                }

                // 批量标记消息为已读
                if (!messageIds.isEmpty()) {
                    for (String id : messageIds) {
                        messagesCol.updateOne(
                                Filters.eq("_id", new org.bson.types.ObjectId(id)),
                                new Document("$set", new Document("read", true))
                        );
                    }
                }

                db.close();

                // 设置最后消息时间戳
                final long finalLatestTimestamp = latestTimestamp;

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    // 移除加载指示器
                    chatArea.getChildren().remove(loadingBox);

                    // 添加所有消息
                    chatArea.getChildren().addAll(messageBubbles);

                    // 设置最后消息时间戳
                    lastMessageTimestamp[0] = finalLatestTimestamp;

                    // 如果没有消息，显示提示
                    if (messageBubbles.isEmpty()) {
                        Label noMessageLabel = new Label("暂无聊天记录，开始你们的对话吧！");
                        noMessageLabel.setFont(Font.font("微软雅黑", 14));
                        noMessageLabel.getStyleClass().add("no-message-label");

                        VBox emptyBox = new VBox();
                        emptyBox.setAlignment(Pos.CENTER);
                        emptyBox.setPadding(new Insets(50));
                        emptyBox.getChildren().add(noMessageLabel);

                        chatArea.getChildren().add(emptyBox);
                    }

                    // 统一的滚动到底部方法
                    scrollToBottomSmoothly(scrollPane);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // 移除加载指示器
                    chatArea.getChildren().remove(loadingBox);

                    // 显示错误信息
                    VBox errorBox = new VBox(10);
                    errorBox.setAlignment(Pos.CENTER);
                    errorBox.setPadding(new Insets(50));

                    Label errorIcon = new Label("⚠");
                    errorIcon.setFont(Font.font("微软雅黑", 24));
                    errorIcon.setStyle("-fx-text-fill: #e74c3c;");

                    Label errorLabel = new Label("加载聊天记录失败");
                    errorLabel.setFont(Font.font("微软雅黑", 16));
                    errorLabel.setStyle("-fx-text-fill: #e74c3c;");

                    Label errorDetail = new Label("请检查网络连接或稍后重试");
                    errorDetail.setFont(Font.font("微软雅黑", 12));
                    errorDetail.setStyle("-fx-text-fill: #7f8c8d;");

                    Button retryBtn = new Button("重试");
                    retryBtn.getStyleClass().add("menu-button");
                    retryBtn.setOnAction(event -> {
                        chatArea.getChildren().clear();
                        chatArea.getChildren().add(loadingBox);
                        loadChatMessagesAsync(chatArea, currentUser, otherUser, loadingBox, scrollPane, lastMessageTimestamp);
                    });

                    errorBox.getChildren().addAll(errorIcon, errorLabel, errorDetail, retryBtn);
                    chatArea.getChildren().add(errorBox);

                    showAlert("错误", "加载聊天记录失败", e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 直接显示私信聊天列表的方法（不依赖父窗口）
    private void showPrivateChatListDirectly(String username, double x, double y, double width, double height) {
        Stage chatListStage = new Stage();
        chatListStage.setTitle("私信聊天");
        chatListStage.setResizable(true);

        // 设置指定的位置和大小
        chatListStage.setX(x);
        chatListStage.setY(y);
        chatListStage.setWidth(width);
        chatListStage.setHeight(height);

        // 窗口同步绑定
        chatListStage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getWidth()) > 2) {
                mainStage.setWidth(newVal.doubleValue());
            }
        });
        chatListStage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getHeight()) > 2) {
                mainStage.setHeight(newVal.doubleValue());
            }
        });
        chatListStage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getX()) > 2) {
                mainStage.setX(newVal.doubleValue());
            }
        });
        chatListStage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (Math.abs(newVal.doubleValue() - mainStage.getY()) > 2) {
                mainStage.setY(newVal.doubleValue());
            }
        });

        // 关闭事件处理
        chatListStage.setOnCloseRequest(e -> {
            mainStage.setX(chatListStage.getX());
            mainStage.setY(chatListStage.getY());
            mainStage.setWidth(chatListStage.getWidth());
            mainStage.setHeight(chatListStage.getHeight());

            if (!mainStage.isShowing()) {
                mainStage.show();
            }
            mainStage.toFront();
        });

        VBox root = new VBox(25);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("main-background");

        // 修复：返回按钮 - 返回到信箱中心
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← 返回信箱");
        backBtn.setFont(Font.font("微软雅黑", 14));
        backBtn.setPrefWidth(120);
        backBtn.setPrefHeight(40);
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> {
            // 记录当前窗口位置
            double currentX = chatListStage.getX();
            double currentY = chatListStage.getY();
            double currentWidth = chatListStage.getWidth();
            double currentHeight = chatListStage.getHeight();

            chatListStage.close();

            // 返回信箱中心（使用当前窗口位置）
            Platform.runLater(() -> {
                showMailboxWithPosition(username, currentX, currentY, currentWidth, currentHeight);
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(backBtn, spacer);

        // 标题区域
        VBox titleArea = new VBox(12);
        titleArea.setAlignment(Pos.CENTER);

        Label titleIcon = new Label("💬");
        titleIcon.setFont(Font.font("微软雅黑", 36));
        titleIcon.getStyleClass().add("feature-icon");

        Label title = new Label("私信聊天");
        title.setFont(Font.font("微软雅黑", 28));
        title.getStyleClass().add("section-title");

        Label subtitle = new Label("查看和回复私信消息");
        subtitle.setFont(Font.font("微软雅黑", 16));
        subtitle.getStyleClass().add("mailbox-subtitle");

        titleArea.getChildren().addAll(titleIcon, title, subtitle);

        // 重新加载数据（因为可能有新消息）
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        progressIndicator.getStyleClass().add("mailbox-progress");

        Label loadingLabel = new Label("正在刷新聊天列表...");
        loadingLabel.setFont(Font.font("微软雅黑", 16));
        loadingLabel.getStyleClass().add("loading-label");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(60));
        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);

        root.getChildren().addAll(headerBox, titleArea, loadingBox);

        // 异步加载最新数据
        loadPrivateChatListAsync(username, root, loadingBox, chatListStage);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("main-scroll-pane");

        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        chatListStage.setScene(scene);
        chatListStage.show();
    }

    // 创建消息气泡
    // 创建消息气泡 - 修复消息不显示的问题
    private VBox createMessageBubble(String message, String time, boolean isFromCurrentUser) {
        VBox messageContainer = new VBox(5);
        messageContainer.setPadding(new Insets(5, 0, 5, 0));

        HBox messageBox = new HBox();

        if (isFromCurrentUser) {
            // 当前用户发送的消息（右侧）
            messageBox.setAlignment(Pos.CENTER_RIGHT);

            VBox bubble = new VBox(5);
            bubble.setMaxWidth(350);
            bubble.setPadding(new Insets(10, 15, 10, 15));

            // 修复：明确设置发送消息的样式
            bubble.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 15px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 1, 1);");

            Label messageLabel = new Label(message);
            messageLabel.setFont(Font.font("微软雅黑", 14));
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(320);
            // 修复：明确设置文字颜色为白色
            messageLabel.setStyle("-fx-text-fill: white; -fx-font-weight: normal;");

            Label timeLabel = new Label(time);
            timeLabel.setFont(Font.font("微软雅黑", 10));
            // 修复：设置时间标签的颜色
            timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");

            bubble.getChildren().addAll(messageLabel, timeLabel);
            messageBox.getChildren().add(bubble);

        } else {
            // 对方发送的消息（左侧）
            messageBox.setAlignment(Pos.CENTER_LEFT);

            VBox bubble = new VBox(5);
            bubble.setMaxWidth(350);
            bubble.setPadding(new Insets(10, 15, 10, 15));

            // 修复：明确设置接收消息的样式
            bubble.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 15px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 1, 1);");

            Label messageLabel = new Label(message);
            messageLabel.setFont(Font.font("微软雅黑", 14));
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(320);
            // 修复：明确设置文字颜色为深色
            messageLabel.setStyle("-fx-text-fill: #333333; -fx-font-weight: normal;");

            Label timeLabel = new Label(time);
            timeLabel.setFont(Font.font("微软雅黑", 10));
            // 修复：设置时间标签的颜色
            timeLabel.setStyle("-fx-text-fill: #666666;");

            bubble.getChildren().addAll(messageLabel, timeLabel);
            messageBox.getChildren().add(bubble);
        }

        messageContainer.getChildren().add(messageBox);
        return messageContainer;
    }

    // 创建好友申请表格的方法
    private TableView<MailRecord> createFriendRequestTable(String username, List<MailRecord> data) {
        TableView<MailRecord> table = new TableView<>();
        table.setPrefWidth(550);
        table.getStyleClass().add("friend-request-table");

        TableColumn<MailRecord, String> fromCol = new TableColumn<>("请求来自");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("from"));
        fromCol.setPrefWidth(150);

        TableColumn<MailRecord, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(column -> {
            return new TableCell<MailRecord, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        switch (item) {
                            case "pending":
                                setText("待处理");
                                setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 5px;");
                                break;
                            case "agreed":
                                setText("已同意");
                                setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5px;");
                                break;
                            case "rejected":
                                setText("已拒绝");
                                setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5px;");
                                break;
                            default:
                                setText(item);
                                setStyle("");
                        }
                    }
                }
            };
        });

        TableColumn<MailRecord, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(200);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button agreeBtn = new Button("✓ 同意");
            private final Button rejectBtn = new Button("✗ 拒绝");
            private final HBox buttonBox = new HBox(8);

            {
                // 使用CSS类替代内联样式
                agreeBtn.getStyleClass().add("friend-agree-button");
                rejectBtn.getStyleClass().add("friend-reject-button");

                agreeBtn.setPrefWidth(80);
                rejectBtn.setPrefWidth(80);

                buttonBox.getChildren().addAll(agreeBtn, rejectBtn);
                buttonBox.setAlignment(Pos.CENTER);

                agreeBtn.setOnAction(event -> {
                    MailRecord record = getTableRow().getItem();
                    if (record != null && "pending".equals(record.getStatus())) {
                        // 显示确认对话框
                        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmAlert.setTitle("确认操作");
                        confirmAlert.setHeaderText("同意好友申请");
                        confirmAlert.setContentText("确定要同意来自 " + record.getFrom() + " 的好友申请吗？");

                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            agreeFriendRequestAsync(username, record.getFrom(), () -> {
                                // 成功后更新UI
                                Platform.runLater(() -> {
                                    getTableView().getItems().remove(record);
                                    showAlert("成功", "好友申请已处理", "已同意来自 " + record.getFrom() + " 的好友申请", Alert.AlertType.INFORMATION);
                                });
                            });
                        }
                    }
                });

                rejectBtn.setOnAction(event -> {
                    MailRecord record = getTableRow().getItem();
                    if (record != null && "pending".equals(record.getStatus())) {
                        // 显示确认对话框
                        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        confirmAlert.setTitle("确认操作");
                        confirmAlert.setHeaderText("拒绝好友申请");
                        confirmAlert.setContentText("确定要拒绝来自 " + record.getFrom() + " 的好友申请吗？");

                        Optional<ButtonType> result = confirmAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            rejectFriendRequestAsync(username, record.getFrom(), () -> {
                                // 成功后更新UI
                                Platform.runLater(() -> {
                                    getTableView().getItems().remove(record);
                                    showAlert("操作完成", "好友申请已处理", "已拒绝来自 " + record.getFrom() + " 的好友申请", Alert.AlertType.INFORMATION);
                                });
                            });
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MailRecord record = getTableRow().getItem();
                    if (record != null && "pending".equals(record.getStatus())) {
                        setGraphic(buttonBox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        table.getColumns().addAll(fromCol, statusCol, actionCol);

        // 设置数据
        ObservableList<MailRecord> observableData = FXCollections.observableArrayList(data);
        table.setItems(observableData);

        return table;
    }

    // 创建微信风格的聊天列表卡片
    private VBox createChatListCards(String username, List<ChatListRecord> data, Stage parentStage) {
        VBox cardsContainer = new VBox(12);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        // 按最后消息时间排序
        data.sort((a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

        for (ChatListRecord record : data) {
            HBox chatCard = createChatCard(username, record, parentStage);
            cardsContainer.getChildren().add(chatCard);
        }

        return cardsContainer;
    }

    // 创建现代化的私信聊天卡片
    private HBox createChatCard(String username, ChatListRecord record, Stage parentStage) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setPrefWidth(650);
        card.setMaxWidth(650);

        // 根据是否有未读消息设置不同的卡片样式
        if (record.getUnreadCount() > 0) {
            card.getStyleClass().add("chat-card-unread");
        } else {
            card.getStyleClass().add("chat-card-read");
        }

        // 用户头像指示器区域
        StackPane avatarIndicator = new StackPane();
        avatarIndicator.setPrefSize(40, 40);
        avatarIndicator.setMaxSize(40, 40);
        avatarIndicator.getStyleClass().add("chat-avatar-indicator");

        // 根据用户名的哈希值选择头像颜色
        String[] avatarColors = {"chat-avatar-red", "chat-avatar-green", "chat-avatar-orange", "chat-avatar-purple", "chat-avatar-blue"};
        int colorIndex = Math.abs(record.getFriendName().hashCode()) % avatarColors.length;
        avatarIndicator.getStyleClass().add(avatarColors[colorIndex]);

        // 用户名首字母作为头像
        Label avatarLabel = new Label(record.getFriendName().substring(0, 1).toUpperCase());
        avatarLabel.setFont(Font.font("微软雅黑", 20));
        avatarLabel.getStyleClass().add("chat-avatar-text");

        avatarIndicator.getChildren().add(avatarLabel);

        // 聊天信息区域
        VBox chatInfo = new VBox(8);
        chatInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chatInfo, Priority.ALWAYS);

        // 用户信息行
        HBox userRow = new HBox(12);
        userRow.setAlignment(Pos.CENTER_LEFT);

        // 用户信息文本
        VBox userTextInfo = new VBox(3);
        userTextInfo.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label(record.getFriendName());
        usernameLabel.setFont(Font.font("微软雅黑", 18));
        usernameLabel.getStyleClass().add("chat-username");

        // 在线状态文本
        Label statusText = new Label("点击进入聊天");
        statusText.setFont(Font.font("微软雅黑", 14));
        statusText.getStyleClass().add("chat-status-text");

        userTextInfo.getChildren().addAll(usernameLabel, statusText);
        userRow.getChildren().add(userTextInfo);

        // 最后消息显示
        Label messagePreview = new Label();
        messagePreview.setFont(Font.font("微软雅黑", 14));

        String lastMessage = record.getLastMessage();
        if (lastMessage.length() > 40) {
            lastMessage = lastMessage.substring(0, 40) + "...";
        }

        if (record.getUnreadCount() > 0) {
            messagePreview.setText("💬 " + lastMessage);
            messagePreview.getStyleClass().add("chat-message-preview-unread");
        } else {
            messagePreview.setText("📝 " + lastMessage);
            messagePreview.getStyleClass().add("chat-message-preview-read");
        }

        // 时间和未读数量显示
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label("⏰ " + getRelativeTime(record));
        timeLabel.setFont(Font.font("微软雅黑", 12));
        timeLabel.getStyleClass().add("chat-time-label");

        Region timeSpacer = new Region();
        HBox.setHgrow(timeSpacer, Priority.ALWAYS);

        bottomRow.getChildren().addAll(timeLabel, timeSpacer);

        chatInfo.getChildren().addAll(userRow, messagePreview, bottomRow);

        // 状态和操作区域
        VBox actionArea = new VBox(10);
        actionArea.setAlignment(Pos.CENTER_RIGHT);

        // 未读消息徽章
        if (record.getUnreadCount() > 0) {
            StackPane unreadBadge = new StackPane();
            unreadBadge.setPrefSize(35, 35);
            unreadBadge.setMaxSize(35, 35);
            unreadBadge.getStyleClass().add("chat-unread-badge-large");

            Label unreadCountLabel = new Label(record.getUnreadCount() > 99 ? "99+" : String.valueOf(record.getUnreadCount()));
            unreadCountLabel.setFont(Font.font("微软雅黑", 12));
            unreadCountLabel.getStyleClass().add("chat-unread-count");

            unreadBadge.getChildren().add(unreadCountLabel);
            actionArea.getChildren().add(unreadBadge);
        }

        // 进入聊天按钮
        Button enterChatBtn = new Button("💬 聊天");
        enterChatBtn.setPrefWidth(100);
        enterChatBtn.setPrefHeight(40);
        enterChatBtn.setFont(Font.font("微软雅黑", 14));

        if (record.getUnreadCount() > 0) {
            enterChatBtn.getStyleClass().add("chat-enter-button-unread");
        } else {
            enterChatBtn.getStyleClass().add("chat-enter-button-read");
        }

        enterChatBtn.setOnAction(e -> {
            // 记录父窗口的位置和大小
            double parentX = parentStage.getX();
            double parentY = parentStage.getY();
            double parentWidth = parentStage.getWidth();
            double parentHeight = parentStage.getHeight();

            parentStage.close();

            // 延迟一点再打开聊天窗口，确保父窗口完全关闭
            Platform.runLater(() -> {
                // 临时设置主窗口的位置和大小，确保聊天窗口继承正确的尺寸
                mainStage.setX(parentX);
                mainStage.setY(parentY);
                mainStage.setWidth(parentWidth);
                mainStage.setHeight(parentHeight);

                // 从信箱私信进入，传入"mailbox"作为来源
                showWeChatStyleChat(username, record.getFriendName(), "mailbox");
            });
        });

        actionArea.getChildren().add(enterChatBtn);

        card.getChildren().addAll(avatarIndicator, chatInfo, actionArea);

        // 添加点击整个卡片也能进入聊天的功能
        card.setOnMouseClicked(e -> {
            // 如果点击的不是按钮区域，就进入聊天
            if (!actionArea.contains(e.getX() - card.getLayoutX(), e.getY() - card.getLayoutY())) {
                enterChatBtn.fire(); // 触发按钮事件
            }
        });

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("chat-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("chat-card-hover"));

        return card;
    }

    // 创建私信聊天空状态 - 使用与好友申请一致的风格
    private VBox createChatListEmptyState() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));

        Label emptyIcon = new Label("💬");
        emptyIcon.setFont(Font.font("微软雅黑", 48));
        emptyIcon.getStyleClass().add("feature-icon"); // 确保图标显示原色

        Label emptyTitle = new Label("暂无聊天记录");
        emptyTitle.setFont(Font.font("微软雅黑", 20));
        emptyTitle.getStyleClass().add("empty-state-title");

        Label emptyMessage = new Label("快去找好友聊天吧！\n或者等待好友给您发送消息");
        emptyMessage.setFont(Font.font("微软雅黑", 14));
        emptyMessage.getStyleClass().add("empty-state-message");
        emptyMessage.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        return emptyBox;
    }

    // 使用新的滚动面板样式类
    private void loadPrivateChatListAsync(String username, VBox root, VBox loadingBox, Stage parentStage) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                MongoCollection<Document> messagesCol = db.getCollection("private_messages");

                java.util.Set<String> contacts = new java.util.HashSet<>();
                FindIterable<Document> docs = messagesCol.find(
                        Filters.or(
                                Filters.eq("from", username),
                                Filters.eq("to", username)
                        )
                );

                for (Document doc : docs) {
                    String from = doc.getString("from");
                    String to = doc.getString("to");
                    if (!from.equals(username)) {
                        contacts.add(from);
                    }
                    if (!to.equals(username)) {
                        contacts.add(to);
                    }
                }

                List<ChatListRecord> data = new ArrayList<>();
                for (String contact : contacts) {
                    Document lastMsg = messagesCol.find(
                            Filters.or(
                                    Filters.and(Filters.eq("from", username), Filters.eq("to", contact)),
                                    Filters.and(Filters.eq("from", contact), Filters.eq("to", username))
                            )
                    ).sort(Sorts.descending("timestamp")).first();

                    String lastMessage = lastMsg != null ? lastMsg.getString("message") : "暂无消息";
                    long lastMessageTime = lastMsg != null ? lastMsg.getLong("timestamp") : System.currentTimeMillis();

                    long unreadCount = messagesCol.countDocuments(
                            Filters.and(
                                    Filters.eq("from", contact),
                                    Filters.eq("to", username),
                                    Filters.eq("read", false)
                            )
                    );

                    data.add(new ChatListRecord(contact, lastMessage, (int)unreadCount, lastMessageTime));
                }

                db.close();

                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    if (data.isEmpty()) {
                        // 显示空状态
                        VBox emptyStateBox = createChatListEmptyState();
                        root.getChildren().add(emptyStateBox);
                    } else {
                        // 创建聊天列表卡片（替代表格）
                        VBox chatCards = createChatListCards(username, data, parentStage);

                        // 将卡片放在ScrollPane中 - 使用与好友申请一致的样式类
                        ScrollPane cardsScrollPane = new ScrollPane(chatCards);
                        cardsScrollPane.setFitToWidth(true);
                        cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                        cardsScrollPane.getStyleClass().add("chat-list-cards-scroll"); // 使用新的样式类
                        cardsScrollPane.setPrefHeight(400);

                        root.getChildren().add(cardsScrollPane);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    // 显示错误状态
                    VBox errorStateBox = createErrorStateBox("加载聊天列表失败", () -> {
                        root.getChildren().clear();
                        VBox titleArea = (VBox) root.getChildren().get(0); // 保留标题
                        root.getChildren().addAll(titleArea, loadingBox);
                        loadPrivateChatListAsync(username, root, loadingBox, parentStage);
                    });
                    root.getChildren().add(errorStateBox);
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 创建单个好友卡片 - 传入"friends"作为来源
    private HBox createFriendCard(String username, String friendUsername, Stage parentStage, VBox container) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 15, 12, 15));
        card.setPrefWidth(500);
        card.setMaxWidth(500);
        card.getStyleClass().add("friend-card");

        // 头像区域
        StackPane avatarArea = new StackPane();
        avatarArea.setPrefSize(45, 45);
        avatarArea.setMaxSize(45, 45);
        avatarArea.getStyleClass().add("friend-avatar");

        // 根据用户名的哈希值选择头像颜色
        String[] avatarColors = {"friend-avatar-red", "friend-avatar-green", "friend-avatar-orange", "friend-avatar-purple", "friend-avatar-blue"};
        int colorIndex = Math.abs(friendUsername.hashCode()) % avatarColors.length;
        avatarArea.getStyleClass().add(avatarColors[colorIndex]);

        // 用户名首字母作为头像
        Label avatarLabel = new Label(friendUsername.substring(0, 1).toUpperCase());
        avatarLabel.setFont(Font.font("微软雅黑", 16));
        avatarLabel.getStyleClass().add("friend-avatar-text");

        avatarArea.getChildren().add(avatarLabel);

        // 好友信息区域
        VBox friendInfo = new VBox(2);
        friendInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(friendInfo, Priority.ALWAYS);

        Label nameLabel = new Label(friendUsername);
        nameLabel.setFont(Font.font("微软雅黑", 16));
        nameLabel.getStyleClass().add("friend-name");

        Label statusLabel = new Label("点击开始聊天");
        statusLabel.setFont(Font.font("微软雅黑", 12));
        statusLabel.getStyleClass().add("friend-status");

        friendInfo.getChildren().addAll(nameLabel, statusLabel);

        // 操作按钮区域
        HBox buttonArea = new HBox(8);
        buttonArea.setAlignment(Pos.CENTER_RIGHT);

        Button chatBtn = new Button("💬");
        chatBtn.setPrefWidth(40);
        chatBtn.setPrefHeight(35);
        chatBtn.getStyleClass().add("friend-chat-button");
        chatBtn.setOnAction(e -> {
            parentStage.close();
            // 从好友列表进入，传入"friends"作为来源
            showWeChatStyleChat(username, friendUsername, "friends");
        });

        Button deleteBtn = new Button("🗑");
        deleteBtn.setPrefWidth(40);
        deleteBtn.setPrefHeight(35);
        deleteBtn.getStyleClass().add("friend-delete-button");
        deleteBtn.setOnAction(e -> {
            // 显示确认对话框
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认删除");
            confirmAlert.setHeaderText("删除好友");
            confirmAlert.setContentText("确定要删除好友 " + friendUsername + " 吗？");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteFriendBothSides(username, friendUsername);
                container.getChildren().remove(card);
                showAlert("成功", "删除好友成功", "已将 " + friendUsername + " 从好友列表移除", Alert.AlertType.INFORMATION);
            }
        });

        buttonArea.getChildren().addAll(chatBtn, deleteBtn);

        card.getChildren().addAll(avatarArea, friendInfo, buttonArea);

        // 添加点击整个卡片也能进入聊天的功能
        card.setOnMouseClicked(e -> {
            // 如果点击的不是按钮区域，就进入聊天
            if (!buttonArea.contains(e.getX() - card.getLayoutX(), e.getY() - card.getLayoutY())) {
                parentStage.close();
                // 从好友列表进入，传入"friends"作为来源
                showWeChatStyleChat(username, friendUsername, "friends");
            }
        });

        // 添加悬停效果
        card.setOnMouseEntered(e -> card.getStyleClass().add("friend-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("friend-card-hover"));

        return card;
    }

    private String getRelativeTime(ChatListRecord record) {
        long now = System.currentTimeMillis();
        long diff = now - record.getLastMessageTime();

        if (diff < 60 * 1000) { // 1分钟内
            return "刚刚";
        } else if (diff < 60 * 60 * 1000) { // 1小时内
            return (diff / (60 * 1000)) + "分钟前";
        } else if (diff < 24 * 60 * 60 * 1000) { // 24小时内
            return (diff / (60 * 60 * 1000)) + "小时前";
        } else if (diff < 7 * 24 * 60 * 60 * 1000) { // 7天内
            return (diff / (24 * 60 * 60 * 1000)) + "天前";
        } else {
            // 超过7天显示具体日期
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(record.getLastMessageTime()),
                    java.time.ZoneId.systemDefault()
            );
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
        }
    }

    // 异步同意好友申请
    private void agreeFriendRequestAsync(String username, String fromUser, Runnable onSuccess) {
        Thread agreeThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();

                // 更新信箱状态
                db.getCollection("mailbox").updateOne(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("from", fromUser),
                                Filters.eq("to", username),
                                Filters.eq("status", "pending")
                        ),
                        new Document("$set", new Document("status", "agreed"))
                );

                // 双方加好友
                Document userDoc = db.getUserByUsername(username);
                Document fromDoc = db.getUserByUsername(fromUser);

                if (userDoc != null && fromDoc != null) {
                    // 更新当前用户的好友列表
                    List<String> userFriends = (List<String>) userDoc.get("friends");
                    if (userFriends == null) userFriends = new ArrayList<>();
                    if (!userFriends.contains(fromUser)) userFriends.add(fromUser);

                    // 更新申请方的好友列表
                    List<String> fromFriends = (List<String>) fromDoc.get("friends");
                    if (fromFriends == null) fromFriends = new ArrayList<>();
                    if (!fromFriends.contains(username)) fromFriends.add(username);

                    // 保存到数据库
                    db.getCollection("users").updateOne(
                            Filters.eq("username", username),
                            new Document("$set", new Document("friends", userFriends))
                    );
                    db.getCollection("users").updateOne(
                            Filters.eq("username", fromUser),
                            new Document("$set", new Document("friends", fromFriends))
                    );
                }

                db.close();

                // 执行成功回调
                if (onSuccess != null) {
                    onSuccess.run();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("错误", "添加好友失败", "处理好友申请时发生错误：" + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });

        agreeThread.setDaemon(true);
        agreeThread.start();
    }

    // 异步拒绝好友申请
    private void rejectFriendRequestAsync(String username, String fromUser, Runnable onSuccess) {
        Thread rejectThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();
                db.getCollection("mailbox").updateOne(
                        Filters.and(
                                Filters.eq("type", "friend_request"),
                                Filters.eq("from", fromUser),
                                Filters.eq("to", username),
                                Filters.eq("status", "pending")
                        ),
                        new Document("$set", new Document("status", "rejected"))
                );
                db.close();

                // 执行成功回调
                if (onSuccess != null) {
                    onSuccess.run();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("错误", "拒绝好友申请失败", "处理好友申请时发生错误：" + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });

        rejectThread.setDaemon(true);
        rejectThread.start();
    }

    // 用现代化卡片替代好友申请表格
    private VBox createFriendRequestCards(String username, List<MailRecord> data) {
        VBox cardsContainer = new VBox(12);
        cardsContainer.setAlignment(Pos.TOP_CENTER);
        cardsContainer.setPadding(new Insets(10));

        for (MailRecord record : data) {
            HBox requestCard = createFriendRequestCard(username, record, cardsContainer);
            cardsContainer.getChildren().add(requestCard);
        }

        return cardsContainer;
    }

    // 创建单个好友申请卡片
    private HBox createFriendRequestCard(String username, MailRecord record, VBox container) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 25, 20, 25));
        card.setPrefWidth(650);
        card.setMaxWidth(650);

        // 根据状态设置不同的卡片样式
        switch (record.getStatus()) {
            case "pending":
                card.getStyleClass().add("friend-request-card-pending");
                break;
            case "agreed":
                card.getStyleClass().add("friend-request-card-agreed");
                break;
            case "rejected":
                card.getStyleClass().add("friend-request-card-rejected");
                break;
            default:
                card.getStyleClass().add("friend-request-card-default");
        }

        // 状态指示器区域
        StackPane statusIndicator = new StackPane();
        statusIndicator.setPrefSize(60, 60);
        statusIndicator.setMaxSize(60, 60);
        statusIndicator.getStyleClass().add("friend-request-status-indicator");

        Label statusIcon = new Label();
        statusIcon.setFont(Font.font("微软雅黑", 24));
        statusIcon.getStyleClass().add("feature-icon");

        switch (record.getStatus()) {
            case "pending":
                statusIcon.setText("⏳");
                statusIndicator.getStyleClass().add("status-indicator-pending");
                break;
            case "agreed":
                statusIcon.setText("✅");
                statusIndicator.getStyleClass().add("status-indicator-agreed");
                break;
            case "rejected":
                statusIcon.setText("❌");
                statusIndicator.getStyleClass().add("status-indicator-rejected");
                break;
            default:
                statusIcon.setText("❓");
                statusIndicator.getStyleClass().add("status-indicator-default");
        }

        statusIndicator.getChildren().add(statusIcon);

        // 用户信息区域
        VBox userInfo = new VBox(8);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        // 用户头像和名称行
        HBox userRow = new HBox(12);
        userRow.setAlignment(Pos.CENTER_LEFT);

        // 用户头像
        StackPane userAvatar = new StackPane();
        userAvatar.setPrefSize(45, 45);
        userAvatar.setMaxSize(45, 45);
        userAvatar.getStyleClass().add("friend-request-avatar");

        // 根据用户名选择头像颜色
        String[] avatarColors = {"request-avatar-red", "request-avatar-green", "request-avatar-orange", "request-avatar-purple", "request-avatar-blue"};
        int colorIndex = Math.abs(record.getFrom().hashCode()) % avatarColors.length;
        userAvatar.getStyleClass().add(avatarColors[colorIndex]);

        Label avatarLabel = new Label(record.getFrom().substring(0, 1).toUpperCase());
        avatarLabel.setFont(Font.font("微软雅黑", 16));
        avatarLabel.getStyleClass().add("friend-request-avatar-text");

        userAvatar.getChildren().add(avatarLabel);

        // 用户信息文本
        VBox userTextInfo = new VBox(3);
        userTextInfo.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label(record.getFrom());
        usernameLabel.setFont(Font.font("微软雅黑", 18));
        usernameLabel.getStyleClass().add("friend-request-username");

        Label requestText = new Label("想要添加您为好友");
        requestText.setFont(Font.font("微软雅黑", 14));
        requestText.getStyleClass().add("friend-request-text");

        userTextInfo.getChildren().addAll(usernameLabel, requestText);
        userRow.getChildren().addAll(userAvatar, userTextInfo);

        // 状态显示文本
        Label statusLabel = new Label();
        statusLabel.setFont(Font.font("微软雅黑", 14));

        switch (record.getStatus()) {
            case "pending":
                statusLabel.setText("📝 等待您的回复");
                statusLabel.getStyleClass().add("friend-request-status-pending");
                break;
            case "agreed":
                statusLabel.setText("🎉 已同意，现在是好友了");
                statusLabel.getStyleClass().add("friend-request-status-agreed");
                break;
            case "rejected":
                statusLabel.setText("🚫 已拒绝此申请");
                statusLabel.getStyleClass().add("friend-request-status-rejected");
                break;
            default:
                statusLabel.setText("❓ 状态未知");
                statusLabel.getStyleClass().add("friend-request-status-default");
        }

        userInfo.getChildren().addAll(userRow, statusLabel);

        // 操作按钮区域
        VBox actionArea = new VBox(10);
        actionArea.setAlignment(Pos.CENTER_RIGHT);

        if ("pending".equals(record.getStatus())) {
            // 待处理状态显示操作按钮
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);

            Button agreeBtn = new Button("✓ 同意");
            agreeBtn.setPrefWidth(90);
            agreeBtn.setPrefHeight(40);
            agreeBtn.setFont(Font.font("微软雅黑", 14));
            agreeBtn.getStyleClass().add("friend-request-agree-button");

            Button rejectBtn = new Button("✗ 拒绝");
            rejectBtn.setPrefWidth(90);
            rejectBtn.setPrefHeight(40);
            rejectBtn.setFont(Font.font("微软雅黑", 14));
            rejectBtn.getStyleClass().add("friend-request-reject-button");

            // 同意按钮事件
            agreeBtn.setOnAction(event -> {
                // 显示高级确认对话框
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认操作");
                confirmAlert.setHeaderText("同意好友申请");
                confirmAlert.setContentText("确定要同意来自 \"" + record.getFrom() + "\" 的好友申请吗？\n同意后对方将成为您的好友。");

                DialogPane dialogPane = confirmAlert.getDialogPane();
                dialogPane.getStyleClass().add("dialog-pane");
                dialogPane.getStyleClass().add("confirmation-dialog");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // 禁用按钮防止重复点击
                    agreeBtn.setDisable(true);
                    rejectBtn.setDisable(true);
                    agreeBtn.setText("处理中...");

                    agreeFriendRequestAsync(username, record.getFrom(), () -> {
                        // 成功后更新UI
                        Platform.runLater(() -> {
                            // 创建成功动画效果
                            card.getStyleClass().remove("friend-request-card-pending");
                            card.getStyleClass().add("friend-request-card-agreed");

                            // 更新卡片内容为已同意状态
                            updateCardToAgreedState(card, record);

                            // 显示成功提示
                            showSuccessToast("已同意来自 " + record.getFrom() + " 的好友申请！");

                            // 3秒后淡出移除卡片
                            javafx.animation.Timeline removeTimeline = new javafx.animation.Timeline(
                                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                                        // 淡出动画
                                        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                                                javafx.util.Duration.millis(500), card);
                                        fadeOut.setToValue(0.0);
                                        fadeOut.setOnFinished(evt -> container.getChildren().remove(card));
                                        fadeOut.play();
                                    })
                            );
                            removeTimeline.play();
                        });
                    });
                }
            });

            // 拒绝按钮事件
            rejectBtn.setOnAction(event -> {
                // 显示高级确认对话框
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("确认操作");
                confirmAlert.setHeaderText("拒绝好友申请");
                confirmAlert.setContentText("确定要拒绝来自 \"" + record.getFrom() + "\" 的好友申请吗？\n此操作无法撤销。");

                DialogPane dialogPane = confirmAlert.getDialogPane();
                dialogPane.getStyleClass().add("dialog-pane");
                dialogPane.getStyleClass().add("warning-dialog");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // 禁用按钮防止重复点击
                    agreeBtn.setDisable(true);
                    rejectBtn.setDisable(true);
                    rejectBtn.setText("处理中...");

                    rejectFriendRequestAsync(username, record.getFrom(), () -> {
                        // 成功后更新UI
                        Platform.runLater(() -> {
                            // 创建拒绝动画效果
                            card.getStyleClass().remove("friend-request-card-pending");
                            card.getStyleClass().add("friend-request-card-rejected");

                            // 更新卡片内容为已拒绝状态
                            updateCardToRejectedState(card, record);

                            // 显示操作完成提示
                            showInfoToast("已拒绝来自 " + record.getFrom() + " 的好友申请");

                            // 3秒后淡出移除卡片
                            javafx.animation.Timeline removeTimeline = new javafx.animation.Timeline(
                                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                                        // 淡出动画
                                        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                                                javafx.util.Duration.millis(500), card);
                                        fadeOut.setToValue(0.0);
                                        fadeOut.setOnFinished(evt -> container.getChildren().remove(card));
                                        fadeOut.play();
                                    })
                            );
                            removeTimeline.play();
                        });
                    });
                }
            });

            buttonBox.getChildren().addAll(agreeBtn, rejectBtn);
            actionArea.getChildren().add(buttonBox);
        } else {
            // 已处理状态显示时间标签
            Label timeLabel = new Label("刚刚处理");
            timeLabel.setFont(Font.font("微软雅黑", 12));
            timeLabel.getStyleClass().add("friend-request-time");
            actionArea.getChildren().add(timeLabel);
        }

        card.getChildren().addAll(statusIndicator, userInfo, actionArea);

        // 添加悬停效果（仅对待处理的申请）
        if ("pending".equals(record.getStatus())) {
            card.setOnMouseEntered(e -> card.getStyleClass().add("friend-request-card-hover"));
            card.setOnMouseExited(e -> card.getStyleClass().remove("friend-request-card-hover"));
        }

        return card;
    }

    // 更新卡片为已同意状态
    private void updateCardToAgreedState(HBox card, MailRecord record) {
        // 找到状态指示器并更新
        StackPane statusIndicator = (StackPane) card.getChildren().get(0);
        Label statusIcon = (Label) statusIndicator.getChildren().get(0);
        statusIcon.setText("✅");
        statusIndicator.getStyleClass().clear();
        statusIndicator.getStyleClass().addAll("friend-request-status-indicator", "status-indicator-agreed");

        // 找到用户信息区域并更新状态文本
        VBox userInfo = (VBox) card.getChildren().get(1);
        Label statusLabel = (Label) userInfo.getChildren().get(1);
        statusLabel.setText("🎉 已同意，现在是好友了");
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add("friend-request-status-agreed");

        // 移除按钮区域
        VBox actionArea = (VBox) card.getChildren().get(2);
        actionArea.getChildren().clear();

        Label timeLabel = new Label("刚刚处理");
        timeLabel.setFont(Font.font("微软雅黑", 12));
        timeLabel.getStyleClass().add("friend-request-time");
        actionArea.getChildren().add(timeLabel);
    }

    // 更新卡片为已拒绝状态
    private void updateCardToRejectedState(HBox card, MailRecord record) {
        // 找到状态指示器并更新
        StackPane statusIndicator = (StackPane) card.getChildren().get(0);
        Label statusIcon = (Label) statusIndicator.getChildren().get(0);
        statusIcon.setText("❌");
        statusIndicator.getStyleClass().clear();
        statusIndicator.getStyleClass().addAll("friend-request-status-indicator", "status-indicator-rejected");

        // 找到用户信息区域并更新状态文本
        VBox userInfo = (VBox) card.getChildren().get(1);
        Label statusLabel = (Label) userInfo.getChildren().get(1);
        statusLabel.setText("🚫 已拒绝此申请");
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add("friend-request-status-rejected");

        // 移除按钮区域
        VBox actionArea = (VBox) card.getChildren().get(2);
        actionArea.getChildren().clear();

        Label timeLabel = new Label("刚刚处理");
        timeLabel.setFont(Font.font("微软雅黑", 12));
        timeLabel.getStyleClass().add("friend-request-time");
        actionArea.getChildren().add(timeLabel);
    }

    // 显示成功提示
    private void showSuccessToast(String message) {
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("操作成功");
        successAlert.setHeaderText(null);
        successAlert.setContentText(message);

        DialogPane dialogPane = successAlert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("success-dialog");

        // 自动关闭
        javafx.animation.Timeline autoClose = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    if (successAlert.isShowing()) {
                        successAlert.close();
                    }
                })
        );
        autoClose.play();

        successAlert.showAndWait();
    }

    // 显示信息提示
    private void showInfoToast(String message) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("操作完成");
        infoAlert.setHeaderText(null);
        infoAlert.setContentText(message);

        DialogPane dialogPane = infoAlert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.getStyleClass().add("info-dialog");

        // 自动关闭
        javafx.animation.Timeline autoClose = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    if (infoAlert.isShowing()) {
                        infoAlert.close();
                    }
                })
        );
        autoClose.play();

        infoAlert.showAndWait();
    }

    // 快捷键支持
    private void setupKeyboardShortcuts(Scene scene) {
        scene.setOnKeyPressed(e -> {
            try {
                switch (e.getCode()) {
                    case M:
                        if (e.isControlDown()) {
                            // Ctrl+M 切换音乐
                            musicManager.toggleMusic();
                            if (musicManager.isMusicEnabled()) {
                                musicManager.playMusic(MusicManager.MAIN_MENU);
                            }
                        }
                        break;
                    case PLUS:
                    case ADD:
                        if (e.isControlDown()) {
                            // Ctrl+Plus 增加音量
                            double newVolume = Math.min(1.0, musicManager.getVolume() + 0.1);
                            musicManager.setVolume(newVolume);
                        }
                        break;
                    case MINUS:
                    case SUBTRACT:
                        if (e.isControlDown()) {
                            // Ctrl+Minus 减少音量
                            double newVolume = Math.max(0.0, musicManager.getVolume() - 0.1);
                            musicManager.setVolume(newVolume);
                        }
                        break;
                }
            } catch (Exception ex) {
                System.err.println("快捷键操作失败: " + ex.getMessage());
            }
        });
    }

    private static class DataCorruptionException extends Exception {
        public DataCorruptionException(String message) {
            super(message);
        }

        public DataCorruptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private void loadHistoryDataAsync(String username, String layoutName, VBox root, VBox loadingBox, Stage currentStage, Stage parentStage) {
        Thread loadThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil(); // 使用你现有的数据库连接

                List<Document> docs = db.getCollection("game_history")
                        .find(new Document("username", username).append("layout", layoutName))
                        .sort(new Document("timestamp", -1))
                        .into(new ArrayList<>());

                List<HistoryRecord> validRecords = new ArrayList<>();
                List<Document> corruptedDocs = new ArrayList<>();

                // 验证每条记录
                for (Document doc : docs) {
                    try {
                        // 基本验证 - 不做详细验证，避免影响加载速度
                        String saveTime = doc.getString("saveTime");
                        Integer moveCount = doc.getInteger("moveCount");
                        String elapsedTime = doc.getString("elapsedTime");
                        Object gameWonObj = doc.get("gameWon");

                        if (saveTime == null || moveCount == null || elapsedTime == null) {
                            throw new DataCorruptionException("基本字段缺失");
                        }

                        // 简单验证数据合理性
                        if (moveCount < 0 || moveCount > 10000) {
                            throw new DataCorruptionException("步数数据异常");
                        }

                        if (!elapsedTime.matches("\\d{1,3}:\\d{2}")) {
                            throw new DataCorruptionException("用时格式错误");
                        }

                        // 验证方块数据存在性（不做详细验证）
                        @SuppressWarnings("unchecked")
                        List<Document> blockDocs = (List<Document>) doc.get("blocks");
                        if (blockDocs == null || blockDocs.isEmpty()) {
                            throw new DataCorruptionException("方块数据缺失");
                        }

                        // 验证关键方块字段
                        for (Document blockDoc : blockDocs) {
                            if (!blockDoc.containsKey("name") || !blockDoc.containsKey("row") ||
                                    !blockDoc.containsKey("col") || !blockDoc.containsKey("width") ||
                                    !blockDoc.containsKey("height")) {
                                throw new DataCorruptionException("方块数据字段不完整");
                            }
                        }

                        // 兼容Boolean和String类型的gameWon
                        boolean gameWon;
                        if (gameWonObj instanceof Boolean) {
                            gameWon = (Boolean) gameWonObj;
                        } else if (gameWonObj instanceof String) {
                            gameWon = Boolean.parseBoolean((String) gameWonObj);
                        } else {
                            gameWon = false; // 默认值
                        }

                        // 如果验证通过，添加到有效记录列表
                        validRecords.add(new HistoryRecord(saveTime, moveCount, elapsedTime, gameWon));

                    } catch (Exception e) {
                        // 记录损坏的文档
                        corruptedDocs.add(doc);
                        System.err.println("发现损坏的历史记录: " + doc.getObjectId("_id") +
                                ", 错误: " + e.getMessage());
                    }
                }

                db.close(); // 确保关闭连接

                Platform.runLater(() -> {
                    // 移除加载指示器
                    root.getChildren().remove(loadingBox);

                    // 如果有损坏的记录，询问用户是否删除
                    if (!corruptedDocs.isEmpty()) {
                        showCorruptedRecordsDialog(username, layoutName, corruptedDocs.size(), () -> {
                            // 删除所有损坏的记录
                            deleteCorruptedRecordsAsync(username, layoutName, corruptedDocs, currentStage); // 使用 currentStage 参数
                        });
                    }

                    if (validRecords.isEmpty()) {
                        // 显示空状态
                        VBox emptyState = createHistoryEmptyState(layoutName);
                        root.getChildren().add(emptyState);
                    } else {
                        // 显示有效的历史记录
                        VBox historyCards = createHistoryCards(username, layoutName, validRecords, currentStage, parentStage); // 使用 currentStage 参数

                        ScrollPane cardsScrollPane = new ScrollPane(historyCards);
                        cardsScrollPane.setFitToWidth(true);
                        cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                        cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                        cardsScrollPane.getStyleClass().add("history-cards-scroll");
                        cardsScrollPane.setPrefHeight(400);

                        root.getChildren().add(cardsScrollPane);
                    }
                });

            } catch (Exception e) {
                System.err.println("加载历史记录时发生异常: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    root.getChildren().remove(loadingBox);

                    if (e.getMessage() != null && (e.getMessage().contains("connection") ||
                            e.getMessage().contains("timeout") || e.getMessage().contains("network"))) {
                        showAlert("网络错误", "加载失败",
                                "网络连接异常，请检查网络设置后重试。", Alert.AlertType.ERROR);
                    } else {
                        showAlert("错误", "加载失败",
                                "加载历史记录时发生错误：" + e.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }
        });

        loadThread.setDaemon(true);
        loadThread.start();
    }

    // 同时修改 deleteCorruptedRecordsAsync 方法的参数名
    private void deleteCorruptedRecordsAsync(String username, String layoutName, List<Document> corruptedDocs, Stage currentStage) {
        Thread deleteThread = new Thread(() -> {
            try {
                MongoDBUtil db = new MongoDBUtil();

                int deletedCount = 0;
                for (Document doc : corruptedDocs) {
                    try {
                        db.getCollection("game_history").deleteOne(
                                new Document("_id", doc.getObjectId("_id"))
                        );
                        deletedCount++;
                    } catch (Exception e) {
                        System.err.println("删除损坏记录失败: " + doc.getObjectId("_id") + ", 错误: " + e.getMessage());
                    }
                }

                db.close();

                final int finalDeletedCount = deletedCount;
                Platform.runLater(() -> {
                    showAlert("清理完成", "损坏记录已删除",
                            "成功删除了 " + finalDeletedCount + " 条损坏的历史记录。\n页面将自动刷新。",
                            Alert.AlertType.INFORMATION);

                    // 刷新当前页面
                    if (currentStage != null && currentStage.isShowing()) { // 使用 currentStage 参数
                        currentStage.close();
                        showHistoryList(username, layoutName, null);
                    }
                });

            } catch (Exception e) {
                System.err.println("批量删除损坏记录时发生异常: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    if (e.getMessage() != null && (e.getMessage().contains("connection") ||
                            e.getMessage().contains("timeout") || e.getMessage().contains("network"))) {
                        showAlert("网络错误", "清理失败",
                                "网络连接异常，无法清理损坏记录。", Alert.AlertType.ERROR);
                    } else {
                        showAlert("错误", "清理失败",
                                "清理损坏记录时发生错误：" + e.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }
        });

        deleteThread.setDaemon(true);
        deleteThread.start();
    }
}
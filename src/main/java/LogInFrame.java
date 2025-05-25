import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.bson.Document;
import com.mongodb.client.model.Projections;
import com.mongodb.client.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LogInFrame {

    public interface LoginSuccessListener {
        void onLoginSuccess(String username);
    }

    private boolean isRegisterMode = false;
    private VBox formCard;
    private Label title;
    private Label confirmLabel;
    private PasswordField confirmField;
    private Label mismatchTip;
    private HBox strengthBox;
    private Rectangle rect1, rect2, rect3;
    private Label passStrengthIcon;
    private Button actionBtn; // 登录/注册按钮
    private Button offlineBtn;
    private VBox btnBox; // 修复：确保这里是VBox类型，不是HBox
    // 本地用户名缓存
    private Set<String> existingUsers = new HashSet<>();
    private boolean userListLoaded = false;

    // 预检测结果缓存
    private String lastCheckedUsername = "";
    private boolean lastUserExists = false;

    // 延迟检查的 Timeline
    private javafx.animation.Timeline checkTimeline = null;

    public void show(Stage primaryStage, LoginSuccessListener onLoginSuccess) {
        primaryStage.setTitle("华容道 - 登录");

        // 异步加载用户列表
        loadUserListAsync();

        // 主容器 - 使用现代化的渐变背景
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("login-scroll-pane");

        // 修改：使用横向布局 - 左右分栏
        HBox mainLayout = new HBox(50); // 增加间距以适应横向布局
        mainLayout.setPadding(new Insets(40, 60, 40, 60));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getStyleClass().add("login-background");

        // 修改：左侧标题区域
        VBox leftSection = new VBox(20);
        leftSection.setAlignment(Pos.CENTER);
        leftSection.setPrefWidth(350); // 设置左侧区域宽度
        leftSection.setMaxWidth(400);
        leftSection.setPadding(new Insets(20, 0, 20, 0));

        // 华容道标题区域
        VBox titleSection = createTitleSection();

        // 添加一些装饰性内容到左侧
        VBox decorativeSection = new VBox(15);
        decorativeSection.setAlignment(Pos.CENTER);
        decorativeSection.setPadding(new Insets(30, 0, 0, 0));

        Label welcomeText = new Label("欢迎来到华容道世界");
        welcomeText.setFont(javafx.scene.text.Font.font("微软雅黑", 18));
        welcomeText.getStyleClass().add("welcome-text");

        Label gameDescription = new Label("挑战经典解谜游戏\n训练逻辑思维能力\n享受策略游戏乐趣");
        gameDescription.setFont(javafx.scene.text.Font.font("微软雅黑", 14));
        gameDescription.getStyleClass().add("game-description");
        gameDescription.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // 添加一些游戏特色图标
        HBox featuresBox = new HBox(20);
        featuresBox.setAlignment(Pos.CENTER);
        featuresBox.setPadding(new Insets(20, 0, 0, 0));

        VBox feature1 = createFeatureBox("🧩", "策略思考");
        VBox feature2 = createFeatureBox("🏆", "排行竞技");
        VBox feature3 = createFeatureBox("👥", "社交互动");

        featuresBox.getChildren().addAll(feature1, feature2, feature3);

        decorativeSection.getChildren().addAll(welcomeText, gameDescription, featuresBox);

        leftSection.getChildren().addAll(titleSection, decorativeSection);

        // 修改：右侧登录表单区域
        VBox rightSection = new VBox(20);
        rightSection.setAlignment(Pos.CENTER);
        rightSection.setPrefWidth(420); // 保持表单宽度
        rightSection.setMaxWidth(450);

        // 表单卡片
        formCard = new VBox(20);
        formCard.setAlignment(Pos.CENTER);
        formCard.setPadding(new Insets(30, 40, 30, 40));
        formCard.setPrefWidth(420);
        formCard.setMaxWidth(450);
        formCard.getStyleClass().add("login-form-card");

        // 表单标题
        title = new Label("用户登录");
        title.setFont(javafx.scene.text.Font.font("微软雅黑", 22));
        title.getStyleClass().add("form-title");

        // 输入字段
        VBox fieldsContainer = new VBox(15);
        fieldsContainer.setAlignment(Pos.CENTER);

        // 用户名输入
        VBox userContainer = createInputField("用户名", "请输入用户名");
        TextField userField = (TextField) ((VBox) userContainer.getChildren().get(1)).getChildren().get(0);

        // 密码输入
        VBox passContainer = createPasswordField("密码", "请输入密码");
        HBox passInputBox = (HBox) ((VBox) passContainer.getChildren().get(1)).getChildren().get(0);
        PasswordField passField = (PasswordField) passInputBox.getChildren().get(0);
        TextField passVisibleField = (TextField) passInputBox.getChildren().get(1);
        Button eyeButton = (Button) passInputBox.getChildren().get(2);

        // 确认密码输入（注册模式）
        confirmLabel = new Label("确认密码");
        confirmLabel.setFont(javafx.scene.text.Font.font("微软雅黑", 14));
        confirmLabel.getStyleClass().add("input-label");

        VBox confirmContainer = new VBox(8);
        confirmContainer.setAlignment(Pos.CENTER_LEFT);

        confirmField = new PasswordField();
        confirmField.setPromptText("请再次输入密码");
        confirmField.setPrefHeight(45);
        confirmField.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        confirmField.getStyleClass().add("login-input");

        confirmContainer.getChildren().addAll(confirmLabel, confirmField);

        // 密码不匹配提示
        mismatchTip = new Label("两次密码不一致");
        mismatchTip.getStyleClass().add("error-tip");
        mismatchTip.setVisible(false);
        mismatchTip.setManaged(false);

        // 密码强度指示器
        strengthBox = createPasswordStrengthBox();

        fieldsContainer.getChildren().addAll(userContainer, passContainer);

        // 按钮区域
        VBox buttonArea = createButtonArea(onLoginSuccess);
        btnBox = buttonArea;

        // 初始设置为登录模式
        formCard.getChildren().addAll(title, fieldsContainer, btnBox);

        rightSection.getChildren().add(formCard);

        // 修改：将左右两栏添加到主布局
        mainLayout.getChildren().addAll(leftSection, rightSection);

        // 设置事件监听器
        setupEventListeners(userField, passField, passVisibleField, eyeButton, confirmField, onLoginSuccess, primaryStage);

        scrollPane.setContent(mainLayout);

        // 创建场景并应用样式
        Scene scene = new Scene(scrollPane);
        loadCSS(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        // 修改：移除固定比例限制，允许自由调整大小
        primaryStage.setMinWidth(600);   // 设置合理的最小宽度
        primaryStage.setMinHeight(400);  // 设置合理的最小高度

        // 修改：设置默认窗口大小，但不限制比例
        primaryStage.setWidth(1110);
        primaryStage.setHeight(740);

        primaryStage.show();

        // 自动聚焦到用户名输入框
        Platform.runLater(() -> userField.requestFocus());
    }

    // 修改：更新标题区域 - 适应左侧布局
    private VBox createTitleSection() {
        VBox titleSection = new VBox(15);
        titleSection.setAlignment(Pos.CENTER);

        Label appIcon = new Label("🏯");
        appIcon.setFont(javafx.scene.text.Font.font("微软雅黑", 56)); // 增大图标
        appIcon.getStyleClass().add("feature-icon");

        Label appTitle = new Label("华容道");
        appTitle.setFont(javafx.scene.text.Font.font("微软雅黑", 42)); // 增大标题
        appTitle.getStyleClass().add("app-title");

        Label appSubtitle = new Label("经典益智解谜游戏");
        appSubtitle.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        appSubtitle.getStyleClass().add("app-subtitle");

        titleSection.getChildren().addAll(appIcon, appTitle, appSubtitle);
        return titleSection;
    }

    // 新增：创建特色功能小卡片
    private VBox createFeatureBox(String icon, String text) {
        VBox featureBox = new VBox(8);
        featureBox.setAlignment(Pos.CENTER);
        featureBox.setPrefWidth(80);

        Label iconLabel = new Label(icon);
        iconLabel.setFont(javafx.scene.text.Font.font("微软雅黑", 24));
        iconLabel.getStyleClass().add("feature-icon");

        Label textLabel = new Label(text);
        textLabel.setFont(javafx.scene.text.Font.font("微软雅黑", 12));
        textLabel.getStyleClass().add("feature-text");
        textLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        featureBox.getChildren().addAll(iconLabel, textLabel);
        return featureBox;
    }

    // 创建输入字段
    private VBox createInputField(String labelText, String placeholder) {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setFont(javafx.scene.text.Font.font("微软雅黑", 14));
        label.getStyleClass().add("input-label");

        VBox inputContainer = new VBox();
        inputContainer.setAlignment(Pos.CENTER);

        TextField textField = new TextField();
        textField.setPromptText(placeholder);
        textField.setPrefHeight(45);
        textField.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        textField.getStyleClass().add("login-input");

        inputContainer.getChildren().add(textField);
        container.getChildren().addAll(label, inputContainer);

        return container;
    }

    // 创建密码输入字段
    private VBox createPasswordField(String labelText, String placeholder) {
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setFont(javafx.scene.text.Font.font("微软雅黑", 14));
        label.getStyleClass().add("input-label");

        VBox inputContainer = new VBox();
        inputContainer.setAlignment(Pos.CENTER);

        HBox passInputBox = new HBox(0);
        passInputBox.setAlignment(Pos.CENTER_LEFT);

        PasswordField passField = new PasswordField();
        passField.setPromptText(placeholder);
        passField.setPrefHeight(45);
        passField.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        passField.getStyleClass().add("login-input");
        HBox.setHgrow(passField, Priority.ALWAYS);

        TextField passVisibleField = new TextField();
        passVisibleField.setPromptText(placeholder);
        passVisibleField.setPrefHeight(45);
        passVisibleField.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        passVisibleField.getStyleClass().add("login-input");
        passVisibleField.setVisible(false);
        passVisibleField.setManaged(false);
        HBox.setHgrow(passVisibleField, Priority.ALWAYS);

        Button eyeButton = new Button("👁");
        eyeButton.setPrefWidth(45);
        eyeButton.setPrefHeight(45);
        eyeButton.getStyleClass().add("eye-button");
        eyeButton.setFocusTraversable(false);

        // 眼睛按钮事件
        eyeButton.setOnAction(e -> {
            if (passVisibleField.isVisible()) {
                passField.setText(passVisibleField.getText());
                passVisibleField.setVisible(false);
                passVisibleField.setManaged(false);
                passField.setVisible(true);
                passField.setManaged(true);
                eyeButton.setText("👁");
            } else {
                passVisibleField.setText(passField.getText());
                passVisibleField.setVisible(true);
                passVisibleField.setManaged(true);
                passField.setVisible(false);
                passField.setManaged(false);
                eyeButton.setText("🙈");
            }
        });

        passInputBox.getChildren().addAll(passField, passVisibleField, eyeButton);

        inputContainer.getChildren().add(passInputBox);
        container.getChildren().addAll(label, inputContainer);

        return container;
    }

    // 创建密码强度指示器
    private HBox createPasswordStrengthBox() {
        HBox strengthContainer = new HBox(8);
        strengthContainer.setAlignment(Pos.CENTER_LEFT);
        strengthContainer.setVisible(false);
        strengthContainer.setManaged(false);

        Label strengthLabel = new Label("密码强度：");
        strengthLabel.setFont(javafx.scene.text.Font.font("微软雅黑", 12));
        strengthLabel.getStyleClass().add("strength-label");

        HBox strengthBar = new HBox(3);
        rect1 = new Rectangle(25, 4, Color.web("#e9ecef"));
        rect2 = new Rectangle(25, 4, Color.web("#e9ecef"));
        rect3 = new Rectangle(25, 4, Color.web("#e9ecef"));
        rect1.setArcWidth(4); rect1.setArcHeight(4);
        rect2.setArcWidth(4); rect2.setArcHeight(4);
        rect3.setArcWidth(4); rect3.setArcHeight(4);
        strengthBar.getChildren().addAll(rect1, rect2, rect3);

        passStrengthIcon = new Label();
        passStrengthIcon.setFont(javafx.scene.text.Font.font("微软雅黑", 14));

        strengthContainer.getChildren().addAll(strengthLabel, strengthBar, passStrengthIcon);

        return strengthContainer;
    }

    // 创建按钮区域
    private VBox createButtonArea(LoginSuccessListener onLoginSuccess) {
        VBox buttonArea = new VBox(15);
        buttonArea.setAlignment(Pos.CENTER);

        actionBtn = new Button("登录");
        actionBtn.setPrefWidth(340);
        actionBtn.setPrefHeight(50);
        actionBtn.setFont(javafx.scene.text.Font.font("微软雅黑", 18));
        actionBtn.getStyleClass().add("primary-button");

        offlineBtn = new Button("🎮 离线游玩");
        offlineBtn.setPrefWidth(340);
        offlineBtn.setPrefHeight(45);
        offlineBtn.setFont(javafx.scene.text.Font.font("微软雅黑", 16));
        offlineBtn.getStyleClass().add("secondary-button");

        // 修改：离线游玩事件 - 删除弹窗，直接进入游戏
        offlineBtn.setOnAction(e -> {
            if (onLoginSuccess != null) {
                onLoginSuccess.onLoginSuccess("离线用户");
            }
        });

        buttonArea.getChildren().addAll(actionBtn, offlineBtn);
        return buttonArea; // 返回VBox类型
    }

    // 设置事件监听器
    private void setupEventListeners(TextField userField, PasswordField passField, TextField passVisibleField,
                                     Button eyeButton, PasswordField confirmField,
                                     LoginSuccessListener onLoginSuccess, Stage primaryStage) {

        // 密码字段同步
        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passVisibleField.isVisible()) {
                passVisibleField.setText(newVal);
            }
            if (isRegisterMode) {
                updatePasswordStrength(newVal);
            }
        });

        passVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passVisibleField.isVisible()) {
                passField.setText(newVal);
            }
        });

        // 修复：Enter键事件 - 检查模式状态
        userField.setOnAction(e -> {
            // 用户名输入完成后，检查是否需要切换模式
            String username = userField.getText().trim();
            if (!username.isEmpty()) {
                boolean userExists = checkUserExistsLocal(username);
                lastCheckedUsername = username;
                lastUserExists = userExists;

                // 根据用户是否存在切换到正确的模式
                if (userExists && isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton, confirmField);
                } else if (!userExists && !isRegisterMode) {
                    switchToRegisterMode(userField, passField, passVisibleField, eyeButton, confirmField);
                }
            }
            // 聚焦到密码字段
            passField.requestFocus();
        });

        passField.setOnAction(e -> {
            // 修复：根据当前实际模式决定下一步操作
            if (isRegisterMode && confirmField.isVisible()) {
                confirmField.requestFocus();
            } else {
                actionBtn.fire();
            }
        });

        passVisibleField.setOnAction(e -> {
            // 修复：根据当前实际模式决定下一步操作
            if (isRegisterMode && confirmField.isVisible()) {
                confirmField.requestFocus();
            } else {
                actionBtn.fire();
            }
        });

        confirmField.setOnAction(e -> actionBtn.fire());

        // 修复：用户名变化监听 - 优化逻辑，避免频繁切换
        userField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (checkTimeline != null) {
                checkTimeline.stop();
            }

            String username = newVal.trim();
            if (username.isEmpty()) {
                lastCheckedUsername = "";
                lastUserExists = false;
                // 清空用户名时，如果是注册模式则切换回登录模式
                if (isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton, confirmField);
                }
                return;
            }

            // 修复：延迟检查用户存在性，但不立即切换模式
            checkTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.8), ev -> {
                        boolean userExists = checkUserExistsLocal(username);
                        lastCheckedUsername = username;
                        lastUserExists = userExists;

                        // 只在用户名输入框失去焦点或者用户明确按回车时才切换模式
                        // 这里只是缓存结果，不切换模式
                    })
            );
            checkTimeline.play();
        });

        // 修复：密码框焦点监听 - 只在必要时切换模式
        passField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // 密码框获得焦点时
                String username = userField.getText().trim();
                if (username.isEmpty()) return;

                // 确保用户存在性检查已完成
                if (!username.equals(lastCheckedUsername)) {
                    boolean userExists = checkUserExistsLocal(username);
                    lastCheckedUsername = username;
                    lastUserExists = userExists;
                }

                // 根据用户是否存在，切换到正确的模式
                boolean shouldBeRegisterMode = !lastUserExists;
                if (shouldBeRegisterMode && !isRegisterMode) {
                    switchToRegisterMode(userField, passField, passVisibleField, eyeButton, confirmField);
                } else if (!shouldBeRegisterMode && isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton, confirmField);
                }
            }
        });

        // 修复：可见密码框焦点监听
        passVisibleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // 可见密码框获得焦点时
                String username = userField.getText().trim();
                if (username.isEmpty()) return;

                // 确保用户存在性检查已完成
                if (!username.equals(lastCheckedUsername)) {
                    boolean userExists = checkUserExistsLocal(username);
                    lastCheckedUsername = username;
                    lastUserExists = userExists;
                }

                // 根据用户是否存在，切换到正确的模式
                boolean shouldBeRegisterMode = !lastUserExists;
                if (shouldBeRegisterMode && !isRegisterMode) {
                    switchToRegisterMode(userField, passField, passVisibleField, eyeButton, confirmField);
                } else if (!shouldBeRegisterMode && isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton, confirmField);
                }
            }
        });

        // 确认密码匹配检查
        Runnable checkMatch = () -> {
            if (isRegisterMode && confirmField.isVisible()) {
                if (!passField.getText().equals(confirmField.getText())) {
                    confirmField.getStyleClass().removeAll("login-input");
                    confirmField.getStyleClass().add("login-input-error");
                    mismatchTip.setVisible(true);
                    mismatchTip.setManaged(true);
                } else {
                    confirmField.getStyleClass().removeAll("login-input-error");
                    confirmField.getStyleClass().add("login-input");
                    mismatchTip.setVisible(false);
                    mismatchTip.setManaged(false);
                }
            }
        };

        passField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());
        confirmField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());

        // 主按钮事件
        actionBtn.setOnAction(e -> {
            if (isRegisterMode) {
                handleRegister(userField, passField, confirmField, onLoginSuccess, primaryStage);
            } else {
                handleLogin(userField, passField, passVisibleField, onLoginSuccess, primaryStage);
            }
        });
    }

    // 切换到登录模式
    private void switchToLoginMode(TextField userField, PasswordField passField, TextField passVisibleField,
                                   Button eyeButton, PasswordField confirmField) {
        if (!isRegisterMode) return;

        isRegisterMode = false;
        title.setText("用户登录");
        actionBtn.setText("登录");

        // 重建表单内容
        VBox fieldsContainer = new VBox(15);
        fieldsContainer.setAlignment(Pos.CENTER);

        // 修复：获取现有容器的方式
        VBox currentFieldsContainer = (VBox) formCard.getChildren().get(1);
        VBox userContainer = (VBox) currentFieldsContainer.getChildren().get(0);
        VBox passContainer = (VBox) currentFieldsContainer.getChildren().get(1);

        fieldsContainer.getChildren().addAll(userContainer, passContainer);

        formCard.getChildren().clear();
        formCard.getChildren().addAll(title, fieldsContainer, btnBox);

        // 隐藏密码强度指示器
        strengthBox.setVisible(false);
        strengthBox.setManaged(false);
    }

    // 切换到注册模式
    private void switchToRegisterMode(TextField userField, PasswordField passField, TextField passVisibleField,
                                      Button eyeButton, PasswordField confirmField) {
        if (isRegisterMode) return;

        isRegisterMode = true;
        title.setText("用户注册");
        actionBtn.setText("注册");

        // 重建表单内容
        VBox fieldsContainer = new VBox(15);
        fieldsContainer.setAlignment(Pos.CENTER);

        // 修复：获取已有的输入容器
        VBox currentFieldsContainer = (VBox) formCard.getChildren().get(1);
        VBox userContainer = (VBox) currentFieldsContainer.getChildren().get(0);
        VBox passContainer = (VBox) currentFieldsContainer.getChildren().get(1);

        // 确认密码容器
        VBox confirmContainer = new VBox(8);
        confirmContainer.setAlignment(Pos.CENTER_LEFT);
        confirmContainer.getChildren().addAll(confirmLabel, confirmField);

        fieldsContainer.getChildren().addAll(userContainer, passContainer, confirmContainer, mismatchTip, strengthBox);

        formCard.getChildren().clear();
        formCard.getChildren().addAll(title, fieldsContainer, btnBox);

        // 显示密码强度指示器
        strengthBox.setVisible(true);
        strengthBox.setManaged(true);

        // 重置确认密码字段
        confirmField.clear();
        confirmField.getStyleClass().removeAll("login-input-error");
        confirmField.getStyleClass().add("login-input");
        mismatchTip.setVisible(false);
        mismatchTip.setManaged(false);

        // 更新密码强度显示
        updatePasswordStrength(passField.getText());
    }

    // 加载CSS样式
    private void loadCSS(Scene scene) {
        try {
            String css = this.getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.out.println("无法加载CSS文件: " + e.getMessage());
        }
    }

    // 更新密码强度显示
    private void updatePasswordStrength(String password) {
        if (!isRegisterMode) return;

        int strength = getPasswordStrength(password);
        rect1.setFill(Color.web("#e9ecef"));
        rect2.setFill(Color.web("#e9ecef"));
        rect3.setFill(Color.web("#e9ecef"));
        passStrengthIcon.setText("");

        if (password.isEmpty()) {
            // 保持灰色
        } else if (strength <= 1) {
            rect1.setFill(Color.web("#e74c3c")); // 红色 - 弱
            passStrengthIcon.setText("弱");
            passStrengthIcon.setTextFill(Color.web("#e74c3c"));
        } else if (strength == 2) {
            rect1.setFill(Color.web("#f39c12")); // 橙色 - 中
            rect2.setFill(Color.web("#f39c12"));
            passStrengthIcon.setText("中");
            passStrengthIcon.setTextFill(Color.web("#f39c12"));
        } else {
            rect1.setFill(Color.web("#27ae60")); // 绿色 - 强
            rect2.setFill(Color.web("#27ae60"));
            rect3.setFill(Color.web("#27ae60"));
            passStrengthIcon.setText("强");
            passStrengthIcon.setTextFill(Color.web("#27ae60"));
        }
    }

    // 其他方法保持不变...
    private void loadUserListAsync() {
        new Thread(() -> {
            MongoDBUtil db = null;
            try {
                db = new MongoDBUtil();
                MongoCollection<Document> collection = db.getCollection("users");

                FindIterable<Document> docs = collection.find()
                        .projection(Projections.include("username"));

                Set<String> userList = new HashSet<>();
                for (Document doc : docs) {
                    String username = doc.getString("username");
                    if (username != null && !username.trim().isEmpty()) {
                        userList.add(username.toLowerCase());
                    }
                }

                synchronized (this) {
                    existingUsers = userList;
                    userListLoaded = true;
                }

                System.out.println("用户列表加载完成，共 " + userList.size() + " 个用户");

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("加载用户列表失败，将使用在线检查模式");
                synchronized (this) {
                    userListLoaded = false;
                }
            } finally {
                if (db != null) db.close();
            }
        }).start();
    }

    private boolean checkUserExistsLocal(String username) {
        synchronized (this) {
            if (userListLoaded) {
                return existingUsers.contains(username.toLowerCase());
            } else {
                return checkUserExistsOnline(username);
            }
        }
    }

    private boolean checkUserExistsOnline(String username) {
        MongoDBUtil db = null;
        try {
            db = new MongoDBUtil();
            Document userDoc = db.getUserByUsername(username);
            return userDoc != null;
        } catch (Exception e) {
            return true;
        } finally {
            if (db != null) db.close();
        }
    }

    private void refreshUserList(String newUsername) {
        synchronized (this) {
            if (userListLoaded && newUsername != null) {
                existingUsers.add(newUsername.toLowerCase());
            }
        }
    }

    private int getPasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*\\d.*") && password.matches(".*[!@#$%].*")) strength++;
        return strength;
    }

    private ArrayList<String> checkPassword(String password) {
        ArrayList<String> errors = new ArrayList<>();
        String specialChars = "!@#$%";
        if (password.length() < 8)
            errors.add("密码长度必须大于8位。");
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUpper = true;
            else if (Character.isLowerCase(ch)) hasLower = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else if (specialChars.indexOf(ch) != -1) hasSpecial = true;
        }
        if (!hasUpper) errors.add("密码必须包含大写字母。");
        if (!hasLower) errors.add("密码必须包含小写字母。");
        if (!hasDigit) errors.add("密码必须包含数字。");
        if (!hasSpecial) errors.add("密码必须包含特殊字符 ! @ # $ %。");
        return errors;
    }

    private void handleLogin(TextField userField, PasswordField passField, TextField passVisibleField,
                             LoginSuccessListener onLoginSuccess, Stage primaryStage) {
        String username = userField.getText().trim();
        String password = passField.isVisible() ? passField.getText() : passVisibleField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "用户名和密码不能为空！");
            return;
        }

        MongoDBUtil db = null;
        try {
            db = new MongoDBUtil();
            Document userDoc = db.getDocument("users", new org.bson.Document("username", username));
            if (userDoc == null) {
                showAlert(Alert.AlertType.ERROR, "用户名不存在！");
            } else if (!PasswordUtil.hash(password).equals(userDoc.getString("password"))) {
                showAlert(Alert.AlertType.ERROR, "密码错误！");
            } else {
                if (onLoginSuccess != null) {
                    onLoginSuccess.onLoginSuccess(username);
                }
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "没有联网，无法登录！\n请联网或离线游玩");
        } finally {
            if (db != null) db.close();
        }
    }

    private void handleRegister(TextField userField, PasswordField passField, PasswordField confirmField,
                                LoginSuccessListener onLoginSuccess, Stage primaryStage) {
        try {
            String username = userField.getText().trim();
            String password = passField.getText();
            String confirmPassword = confirmField.getText();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "所有字段不能为空！");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "两次输入的密码不一致！");
                return;
            }

            ArrayList<String> errors = checkPassword(password);
            if (!errors.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, String.join("\n", errors));
                return;
            }

            MongoDBUtil db = new MongoDBUtil();
            Document userDoc = db.getDocument("users", new Document("username", username));
            if (userDoc != null) {
                showAlert(Alert.AlertType.ERROR, "用户名已被占用，请重新设置。");
                db.close();
                return;
            }

            Document newUser = new Document("username", username)
                    .append("password", PasswordUtil.hash(password))
                    .append("coins", 400);
            db.insertOne("users", newUser);
            db.close();

            refreshUserList(username);

            showAlert(Alert.AlertType.INFORMATION, "注册成功！正在登录...");
            if (onLoginSuccess != null) onLoginSuccess.onLoginSuccess(username);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "注册失败：" + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误" : "提示");
        alert.setHeaderText(type == Alert.AlertType.ERROR ? "登录/注册错误" : "系统提示");
        alert.setContentText(msg);

        // 修复：为对话框应用高级样式
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

        // 设置对话框的最小尺寸
        dialogPane.setMinWidth(400);
        dialogPane.setPrefWidth(400);

        alert.showAndWait();
    }
}
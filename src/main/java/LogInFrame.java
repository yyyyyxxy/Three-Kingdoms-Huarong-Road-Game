import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
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
    private GridPane grid;
    private Label title;
    private Label confirmLabel;
    private PasswordField confirmField;
    private Label mismatchTip;
    private HBox strengthBox;
    private Rectangle rect1, rect2, rect3;
    private Label passStrengthIcon;
    private Button actionBtn; // 登录/注册按钮
    private Button offlineBtn;
    private HBox btnBox;

    // 本地用户名缓存
    private Set<String> existingUsers = new HashSet<>();
    private boolean userListLoaded = false;

    // 预检测结果缓存
    private String lastCheckedUsername = "";
    private boolean lastUserExists = false;

    // 延迟检查的 Timeline
    private javafx.animation.Timeline checkTimeline = null;


    public void show(Stage primaryStage, LoginSuccessListener onLoginSuccess) {
        primaryStage.setTitle("登录界面");

        // 异步加载用户列表
        loadUserListAsync();

        // 棕色渐变背景
        LinearGradient brownGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#c79050")),
                new Stop(1, Color.web("#7d5a3a"))
        );
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(brownGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // 卡片式表单
        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(18);
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.97);"
                        + "-fx-background-radius: 18;"
                        + "-fx-effect: dropshadow(gaussian, #b0a18a, 16, 0.2, 0, 4);"
        );
        // 让 grid 自动调整大小
        grid.setMaxWidth(Region.USE_COMPUTED_SIZE);
        grid.setMaxHeight(Region.USE_COMPUTED_SIZE);

        // 标题
        title = new Label("用户登录");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #7d5a3a;");
        VBox titleBox = new VBox(title, new Separator());
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setSpacing(8);
        root.setTop(titleBox);
        BorderPane.setMargin(titleBox, new Insets(0, 0, 20, 0));

        Label userLabel = new Label("用户名:");
        TextField userField = new TextField();
        userField.setPrefWidth(320);
        userField.setMaxWidth(Double.MAX_VALUE);
        userField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        Label passLabel = new Label("密码:");
        PasswordField passField = new PasswordField();
        passField.setPrefWidth(320);
        passField.setMaxWidth(Double.MAX_VALUE);
        passField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        // 密码可见切换
        TextField passVisibleField = new TextField();
        passVisibleField.setManaged(false);
        passVisibleField.setVisible(false);
        passVisibleField.setPrefWidth(320);
        passVisibleField.setMaxWidth(Double.MAX_VALUE);
        passVisibleField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        Button eyeButton = new Button("\uD83D\uDC41");
        eyeButton.setFocusTraversable(false);
        eyeButton.setStyle("-fx-background-radius: 8; -fx-font-size: 14px;");
        eyeButton.setOnAction(e -> {
            if (passVisibleField.isVisible()) {
                passField.setText(passVisibleField.getText());
                passVisibleField.setVisible(false);
                passVisibleField.setManaged(false);
                passField.setVisible(true);
                passField.setManaged(true);
                eyeButton.setText("\uD83D\uDC41");
            } else {
                passVisibleField.setText(passField.getText());
                passVisibleField.setVisible(true);
                passVisibleField.setManaged(true);
                passField.setVisible(false);
                passField.setManaged(false);
                eyeButton.setText("◯");
            }
        });

        // 新增：密码框回车键监听
        passField.setOnAction(e -> {
            actionBtn.fire();
        });
        passVisibleField.setOnAction(e -> {
            actionBtn.fire();
        });

        // 注册模式专用组件
        confirmLabel = new Label("确认密码:");
        confirmField = new PasswordField();
        confirmField.setPrefWidth(320);
        confirmField.setMaxWidth(Double.MAX_VALUE);
        confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        confirmField.setOnAction(e -> {
            actionBtn.fire();
        });

        passField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                String username = userField.getText().trim();
                if (username.isEmpty()) return;
                boolean shouldBeRegisterMode = !lastUserExists;
                if (shouldBeRegisterMode && !isRegisterMode) {
                    switchToRegisterMode(userField, passField, passVisibleField, eyeButton);
                } else if (!shouldBeRegisterMode && isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton);
                }
            }
        });

        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passVisibleField.isVisible()) {
                passVisibleField.setText(newVal);
            }
        });
        passVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (passVisibleField.isVisible()) {
                passField.setText(newVal);
            }
        });

        confirmLabel = new Label("确认密码:");
        confirmField = new PasswordField();
        confirmField.setPrefWidth(320);
        confirmField.setMaxWidth(Double.MAX_VALUE);
        confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        mismatchTip = new Label("两次密码不一致");
        mismatchTip.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
        mismatchTip.setVisible(false);

        HBox strengthBar = new HBox(3);
        rect1 = new Rectangle(28, 6, Color.LIGHTGRAY);
        rect2 = new Rectangle(28, 6, Color.LIGHTGRAY);
        rect3 = new Rectangle(28, 6, Color.LIGHTGRAY);
        rect1.setArcWidth(6); rect1.setArcHeight(6);
        rect2.setArcWidth(6); rect2.setArcHeight(6);
        rect3.setArcWidth(6); rect3.setArcHeight(6);
        strengthBar.getChildren().addAll(rect1, rect2, rect3);
        strengthBar.setAlignment(Pos.CENTER_LEFT);

        passStrengthIcon = new Label();
        passStrengthIcon.setStyle("-fx-font-size: 16px; -fx-padding: 0 0 0 6px;");
        strengthBox = new HBox(4, strengthBar, passStrengthIcon);
        strengthBox.setAlignment(Pos.CENTER_LEFT);

        actionBtn = new Button("登录");
        actionBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        );
        actionBtn.setOnMouseEntered(e -> actionBtn.setStyle(
                "-fx-background-color: #7d5a3a; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));
        actionBtn.setOnMouseExited(e -> actionBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));

        offlineBtn = new Button("离线游玩");
        offlineBtn.setStyle(
                "-fx-background-color: #d2b48c; -fx-text-fill: #7d5a3a; -fx-background-radius: 8; -fx-font-size: 16px;"
        );
        offlineBtn.setOnMouseEntered(e -> offlineBtn.setStyle(
                "-fx-background-color: #e6cfa7; -fx-text-fill: #7d5a3a; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));
        offlineBtn.setOnMouseExited(e -> offlineBtn.setStyle(
                "-fx-background-color: #d2b48c; -fx-text-fill: #7d5a3a; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));
        offlineBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("离线游玩提示");
            alert.setHeaderText(null);
            alert.setContentText("离线游玩将不会保存游玩记录，是否继续？");
            ButtonType okBtn = new ButtonType("继续", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(okBtn, cancelBtn);
            alert.showAndWait().ifPresent(type -> {
                if (type == okBtn) {
                    if (onLoginSuccess != null) onLoginSuccess.onLoginSuccess("离线用户");
                }
            });
        });

        btnBox = new HBox(20, offlineBtn, actionBtn);
        btnBox.setAlignment(Pos.CENTER);

        setupLoginMode(userField, passField, passVisibleField, eyeButton);
        root.setCenter(grid);

        userField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (checkTimeline != null) {
                checkTimeline.stop();
            }
            String username = newVal.trim();
            if (username.isEmpty()) {
                lastCheckedUsername = "";
                lastUserExists = false;
                if (isRegisterMode) {
                    switchToLoginMode(userField, passField, passVisibleField, eyeButton);
                }
                return;
            }
            checkTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(0.5), ev -> {
                        boolean userExists = checkUserExistsLocal(username);
                        lastCheckedUsername = username;
                        lastUserExists = userExists;
                    })
            );
            checkTimeline.play();
        });

        userField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String username = userField.getText().trim();
                if (username.isEmpty()) {
                    lastCheckedUsername = "";
                    lastUserExists = false;
                    if (isRegisterMode) {
                        switchToLoginMode(userField, passField, passVisibleField, eyeButton);
                    }
                    return;
                }
                if (checkTimeline != null) {
                    checkTimeline.stop();
                }
                boolean userExists = checkUserExistsLocal(username);
                lastCheckedUsername = username;
                lastUserExists = userExists;
            }
        });

        actionBtn.setOnAction(e -> {
            if (isRegisterMode) {
                handleRegister(userField, passField, confirmField, onLoginSuccess, primaryStage);
            } else {
                handleLogin(userField, passField, passVisibleField, onLoginSuccess, primaryStage);
            }
        });

        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isRegisterMode) {
                updatePasswordStrength(newVal);
            }
        });

        Runnable checkMatch = () -> {
            if (isRegisterMode && confirmField.isVisible()) {
                if (!passField.getText().equals(confirmField.getText())) {
                    confirmField.setStyle("-fx-border-color: red; -fx-background-radius: 8; -fx-border-radius: 8;");
                    mismatchTip.setVisible(true);
                } else {
                    confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");
                    mismatchTip.setVisible(false);
                }
            }
        };
        passField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());
        confirmField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());

        Scene scene = new Scene(root, 550, 450);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(350);
        primaryStage.show();
    }

    // 异步加载用户列表
    private void loadUserListAsync() {
        new Thread(() -> {
            MongoDBUtil db = null;
            try {
                db = new MongoDBUtil();
                MongoCollection<Document> collection = db.getCollection("users");

                // 只获取用户名字段，减少数据传输量
                FindIterable<Document> docs = collection.find()
                        .projection(Projections.include("username"));

                Set<String> userList = new HashSet<>();
                for (Document doc : docs) {
                    String username = doc.getString("username");
                    if (username != null && !username.trim().isEmpty()) {
                        userList.add(username.toLowerCase()); // 转小写统一比较
                    }
                }

                // 更新本地缓存
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

    // 本地检查用户是否存在
    private boolean checkUserExistsLocal(String username) {
        synchronized (this) {
            if (userListLoaded) {
                // 使用本地缓存检查
                return existingUsers.contains(username.toLowerCase());
            } else {
                // 如果用户列表还未加载完成，回退到在线检查
                return checkUserExistsOnline(username);
            }
        }
    }

    // 在线检查用户是否存在（回退方案）
    private boolean checkUserExistsOnline(String username) {
        MongoDBUtil db = null;
        try {
            db = new MongoDBUtil();
            Document userDoc = db.getUserByUsername(username);
            return userDoc != null;
        } catch (Exception e) {
            // 网络异常时默认返回true（假设用户存在，保持登录模式）
            return true;
        } finally {
            if (db != null) db.close();
        }
    }

    // 刷新用户列表（在注册成功后调用）
    private void refreshUserList(String newUsername) {
        synchronized (this) {
            if (userListLoaded && newUsername != null) {
                existingUsers.add(newUsername.toLowerCase());
            }
        }
    }

    private void setupLoginMode(TextField userField, PasswordField passField, TextField passVisibleField, Button eyeButton) {
        grid.getChildren().clear();

        Label userLabel = new Label("用户名:");
        Label passLabel = new Label("密码:");

        // 设置GridPane列约束，确保输入框能够充分扩展
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(80);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setFillWidth(true);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setHgrow(Priority.NEVER);
        col3.setPrefWidth(40);

        grid.getColumnConstraints().setAll(col1, col2, col3);

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0, 2, 1);
        grid.add(passLabel, 0, 1);
        grid.add(passField, 1, 1);
        grid.add(passVisibleField, 1, 1);
        grid.add(eyeButton, 2, 1);
        grid.add(btnBox, 0, 2, 3, 1);

        isRegisterMode = false;
        title.setText("用户登录");
        actionBtn.setText("登录");
    }

    private void switchToLoginMode(TextField userField, PasswordField passField, TextField passVisibleField, Button eyeButton) {
        setupLoginMode(userField, passField, passVisibleField, eyeButton);
    }

    private void switchToRegisterMode(TextField userField, PasswordField passField, TextField passVisibleField, Button eyeButton) {
        grid.getChildren().clear();

        // 确保密码框在注册模式下是可见且参与布局的
        passField.setVisible(true);
        passField.setManaged(true);
        passVisibleField.setVisible(false);
        passVisibleField.setManaged(false);

        Label userLabel = new Label("用户名:");
        Label passLabel = new Label("密码:");

        // 设置GridPane列约束，确保输入框能够充分扩展
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setPrefWidth(80);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setFillWidth(true);

        grid.getColumnConstraints().setAll(col1, col2);

        // 密码输入框和强度条放在同一行
        HBox passBox = new HBox(8, passField, strengthBox);
        passBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passField, Priority.ALWAYS); // 让密码框占据剩余空间

        // 确保 confirmField 清空内容并重置样式
        confirmField.clear();
        confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passBox, 1, 1); // 密码输入框和强度条同一行
        grid.add(confirmLabel, 0, 2);
        grid.add(confirmField, 1, 2);
        grid.add(mismatchTip, 1, 3);
        grid.add(btnBox, 0, 4, 2, 1);

        isRegisterMode = true;
        title.setText("用户注册");
        actionBtn.setText("注册");

        // 重置密码强度显示
        updatePasswordStrength(passField.getText());
        // 确保mismatchTip初始不可见
        mismatchTip.setVisible(false);
    }

    private void handleLogin(TextField userField,
                             PasswordField passField,
                             TextField passVisibleField,
                             LoginSuccessListener onLoginSuccess,
                             Stage primaryStage) {
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
                // 1. 登录成功回调
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

            // 存储哈希后的密码，并分配400金币
            Document newUser = new Document("username", username)
                    .append("password", PasswordUtil.hash(password))
                    .append("coins", 400);
            db.insertOne("users", newUser);
            db.close();

            // 注册成功后更新本地用户列表
            refreshUserList(username);

            showAlert(Alert.AlertType.INFORMATION, "注册成功！正在登录...");
            if (onLoginSuccess != null) onLoginSuccess.onLoginSuccess(username);
            // 不要关闭窗口，让主界面复用这个窗口
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "注册失败：" + ex.getMessage());
        }
    }

    private void updatePasswordStrength(String password) {
        if (!isRegisterMode) return;

        int strength = getPasswordStrength(password);
        rect1.setFill(Color.LIGHTGRAY);
        rect2.setFill(Color.LIGHTGRAY);
        rect3.setFill(Color.LIGHTGRAY);
        passStrengthIcon.setText("");

        if (password.isEmpty()) {
            // 保持灰色
        } else if (strength <= 1) {
            rect1.setFill(Color.web("#c0392b")); // 红色
        } else if (strength == 2) {
            rect1.setFill(Color.web("#e67e22")); // 橙色
            rect2.setFill(Color.web("#e67e22"));
        } else {
            rect1.setFill(Color.web("#27ae60")); // 绿色
            rect2.setFill(Color.web("#27ae60"));
            rect3.setFill(Color.web("#27ae60"));
            passStrengthIcon.setText("✔");
            passStrengthIcon.setTextFill(Color.web("#27ae60"));
        }
    }

    // 密码强度：1=弱，2=中，3=强
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

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
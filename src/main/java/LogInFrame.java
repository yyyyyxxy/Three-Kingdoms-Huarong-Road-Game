import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;
import org.bson.Document;

public class LogInFrame {

    // 登录成功回调接口
    public interface LoginSuccessListener {
        void onLoginSuccess(String username);
    }

    public void show(Stage primaryStage, LoginSuccessListener onLoginSuccess) {
        primaryStage.setTitle("登录界面");

        // 棕色渐变背景
        LinearGradient brownGradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#c79050")),
                new Stop(1, Color.web("#7d5a3a"))
        );
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(brownGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // 卡片式表单
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(18);
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.97);" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, #b0a18a, 16, 0.2, 0, 4);"
        );

        // 标题
        Label title = new Label("用户登录");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #7d5a3a;");
        VBox titleBox = new VBox(title, new Separator());
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setSpacing(8);
        root.setTop(titleBox);
        BorderPane.setMargin(titleBox, new Insets(0, 0, 20, 0));

        Label userLabel = new Label("用户名:");
        TextField userField = new TextField();
        userField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        Label passLabel = new Label("密码:");
        PasswordField passField = new PasswordField();
        passField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        // 密码可见切换
        TextField passVisibleField = new TextField();
        passVisibleField.setManaged(false);
        passVisibleField.setVisible(false);
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

        // 同步密码内容
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

        // 按钮区
        Button loginBtn = new Button("登录");
        loginBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        );
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                "-fx-background-color: #7d5a3a; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));

        Button registerBtn = new Button("注册");
        registerBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        );
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle(
                "-fx-background-color: #7d5a3a; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle(
                "-fx-background-color: #a67c52; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 16px;"
        ));

        // 离线游玩按钮
        Button offlineBtn = new Button("离线游玩");
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
                    primaryStage.close();
                    if (onLoginSuccess != null) onLoginSuccess.onLoginSuccess("离线用户");
                }
            });
        });

        // 三个按钮横向排列，注册-离线-登录
        HBox btnBox = new HBox(20, registerBtn, offlineBtn, loginBtn);
        btnBox.setAlignment(Pos.CENTER);

        // 布局
        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0, 2, 1);
        grid.add(passLabel, 0, 1);
        grid.add(passField, 1, 1);
        grid.add(passVisibleField, 1, 1);
        grid.add(eyeButton, 2, 1);
        grid.add(btnBox, 0, 2, 3, 1);

        root.setCenter(grid);

        // 登录事件
        loginBtn.setOnAction(e -> {
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
                    // 登录成功后直接进入主界面，不弹窗
                    primaryStage.close();
                    if (onLoginSuccess != null) onLoginSuccess.onLoginSuccess(username);
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "没有联网，无法登录！");
            } finally {
                if (db != null) db.close();
            }
        });

        // 注册事件
        registerBtn.setOnAction(e -> {
            // 弹出注册窗口
            new RegisterFrame().show(new Stage());
        });

        Scene scene = new Scene(root, 500, 420);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "错误" : "提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
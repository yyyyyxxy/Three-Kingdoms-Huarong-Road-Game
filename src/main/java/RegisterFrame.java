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

import java.util.ArrayList;

public class RegisterFrame {

    public void show(Stage primaryStage) {
        primaryStage.setTitle("注册界面");

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
        grid.setVgap(10);
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setStyle(
                "-fx-background-color: rgba(255,255,255,0.97);" +
                        "-fx-background-radius: 18;" +
                        "-fx-effect: dropshadow(gaussian, #b0a18a, 16, 0.2, 0, 4);"
        );

        // 标题
        Label title = new Label("用户注册");
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

        // 密码强度条（与密码输入框同行）
        HBox strengthBar = new HBox(3);
        Rectangle rect1 = new Rectangle(28, 6, Color.LIGHTGRAY);
        Rectangle rect2 = new Rectangle(28, 6, Color.LIGHTGRAY);
        Rectangle rect3 = new Rectangle(28, 6, Color.LIGHTGRAY);
        rect1.setArcWidth(6); rect1.setArcHeight(6);
        rect2.setArcWidth(6); rect2.setArcHeight(6);
        rect3.setArcWidth(6); rect3.setArcHeight(6);
        strengthBar.getChildren().addAll(rect1, rect2, rect3);
        strengthBar.setAlignment(Pos.CENTER_LEFT);

        Label passStrengthIcon = new Label();
        passStrengthIcon.setStyle("-fx-font-size: 16px; -fx-padding: 0 0 0 6px;");
        HBox strengthBox = new HBox(4, strengthBar, passStrengthIcon);
        strengthBox.setAlignment(Pos.CENTER_LEFT);

        // 密码输入框和强度条放在同一行
        HBox passBox = new HBox(8, passField, strengthBox);
        passBox.setAlignment(Pos.CENTER_LEFT);

        Label confirmLabel = new Label("确认密码:");
        PasswordField confirmField = new PasswordField();
        confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");

        Label mismatchTip = new Label("两次密码不一致");
        mismatchTip.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
        mismatchTip.setVisible(false);

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
        HBox btnBox = new HBox(20, registerBtn);
        btnBox.setAlignment(Pos.CENTER);

        // 统一输入框间距
        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passBox, 1, 1); // 密码输入框和强度条同一行
        grid.add(confirmLabel, 0, 2);
        grid.add(confirmField, 1, 2);
        grid.add(mismatchTip, 1, 3);
        grid.add(btnBox, 0, 4, 2, 1);

        root.setCenter(grid);

        // 实时监测密码强度
        passField.textProperty().addListener((obs, oldVal, newVal) -> {
            int strength = getPasswordStrength(newVal);
            rect1.setFill(Color.LIGHTGRAY);
            rect2.setFill(Color.LIGHTGRAY);
            rect3.setFill(Color.LIGHTGRAY);
            passStrengthIcon.setText("");
            if (newVal.isEmpty()) {
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
            if (strength < 3) {
                passStrengthIcon.setText("");
            }
        });

        // 实时监测两次密码是否一致
        Runnable checkMatch = () -> {
            if (!passField.getText().equals(confirmField.getText())) {
                confirmField.setStyle("-fx-border-color: red; -fx-background-radius: 8; -fx-border-radius: 8;");
                mismatchTip.setVisible(true);
            } else {
                confirmField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d2b48c;");
                mismatchTip.setVisible(false);
            }
        };
        passField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());
        confirmField.textProperty().addListener((obs, oldVal, newVal) -> checkMatch.run());

        registerBtn.setOnAction(e -> {
            try {
                String username = userField.getText().trim();
                String password = passField.getText();
                String confirmPassword = confirmField.getText();

                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "所有字段不能为空！");
                    return;
                }

                MongoDBUtil db = new MongoDBUtil();
                Document userDoc = db.getDocument("users", new Document("username", username));
                if (userDoc != null) {
                    showAlert(Alert.AlertType.ERROR, "用户名已被占用，请重新设置。");
                    db.close();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    showAlert(Alert.AlertType.ERROR, "两次输入的密码不一致！");
                    db.close();
                    return;
                }

                ArrayList<String> errors = checkPassword(password);
                if (!errors.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, String.join("\n", errors));
                    db.close();
                    return;
                }

                // 存储哈希后的密码
                Document newUser = new Document("username", username)
                        .append("password", PasswordUtil.hash(password));
                db.insertOne("users", newUser);
                db.close();

                showAlert(Alert.AlertType.INFORMATION, "注册成功！");
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "注册失败：" + ex.getMessage());
            }
        });

        Scene scene = new Scene(root, 500, 420);
        primaryStage.setScene(scene);
        primaryStage.show();
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
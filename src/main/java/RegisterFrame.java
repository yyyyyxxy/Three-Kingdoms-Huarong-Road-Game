import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

public class RegisterFrame extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("注册界面");

        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(Color.CYAN, CornerRadii.EMPTY, Insets.EMPTY)));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(20);
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10;");

        Label userLabel = new Label("用户名:");
        TextField userField = new TextField();
        Label passLabel = new Label("密码:");
        PasswordField passField = new PasswordField();

        Button registerBtn = new Button("注册");
        HBox btnBox = new HBox(20, registerBtn);
        btnBox.setAlignment(Pos.CENTER);

        grid.add(userLabel, 0, 0);
        grid.add(userField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passField, 1, 1);
        grid.add(btnBox, 0, 2, 2, 1);

        root.setCenter(grid);

        registerBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.getText();

            // 用户名校验（示例，实际应查数据库）
            if ("已存在用户名".equals(username)) {
                showAlert(Alert.AlertType.ERROR, "用户名已被占用，请重新设置。");
                return;
            }

            // 密码校验
            ArrayList<String> errors = checkPassword(password);
            if (!errors.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, String.join("\n", errors));
                return;
            }

            // 注册成功
            showAlert(Alert.AlertType.INFORMATION, "注册成功！");
        });

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
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

    public static void main(String[] args) {
        launch(args);
    }
}
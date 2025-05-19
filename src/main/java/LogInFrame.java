import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LogInFrame extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("登录界面");

        // 主布局
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(Color.CYAN, CornerRadii.EMPTY, Insets.EMPTY)));

        // 中间表单
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

        // 密码可见切换
        TextField passVisibleField = new TextField();
        passVisibleField.setManaged(false);
        passVisibleField.setVisible(false);

        Button eyeButton = new Button("\uD83D\uDC41");
        eyeButton.setFocusTraversable(false);
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
        Button registerBtn = new Button("注册");
        HBox btnBox = new HBox(20, loginBtn, registerBtn);
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

        // 事件（示例）
        loginBtn.setOnAction(e -> {
            String username = userField.getText();
            String password = passField.isVisible() ? passField.getText() : passVisibleField.getText();
            // 登录逻辑
            System.out.println("登录：" + username + " / " + password);
        });
        registerBtn.setOnAction(e -> {
            // 注册逻辑
            System.out.println("跳转注册界面");
        });

        Scene scene = new Scene(root, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
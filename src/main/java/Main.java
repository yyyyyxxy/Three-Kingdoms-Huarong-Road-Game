import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        LogInFrame loginFrame = new LogInFrame();
        loginFrame.show(primaryStage, username -> {
            // 登录成功，打开主界面并传递用户名
            MainInterfaceFrame mainFrame = new MainInterfaceFrame();
            mainFrame.show(primaryStage, username);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
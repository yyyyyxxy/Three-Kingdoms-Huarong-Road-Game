import javafx.application.Application;
import javafx.stage.Stage;

// Main.java
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // 启动注册窗口
        new RegisterFrame();

        new GameFrame().show(primaryStage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
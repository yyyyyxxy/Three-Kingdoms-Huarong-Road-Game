

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        // 初始化游戏界面
        // 例如：创建GameFrame实例并设置场景
    }

    public static void main(String[] args) {
        new LogInFrame();
        System.setProperty("javafx.version", "17");
        Application.launch(GameFrame.class, args);
        launch(args); // 启动JavaFX应用
    }

}
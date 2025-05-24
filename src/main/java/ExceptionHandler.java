import javafx.application.Platform;
import javafx.scene.control.Alert;

public class ExceptionHandler {

    public static void handleDatabaseException(Exception e, String operation) {
        e.printStackTrace();
        Platform.runLater(() -> {
            AlertUtils.showError("数据库错误",
                    "操作失败: " + operation,
                    "请检查网络连接或稍后重试\n错误详情: " + e.getMessage());
        });
    }

    public static void handleDatabaseException(Exception e, String operation, Runnable onRetry) {
        e.printStackTrace();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("数据库错误");
            alert.setHeaderText("操作失败: " + operation);
            alert.setContentText("请检查网络连接或稍后重试\n错误详情: " + e.getMessage());

            alert.showAndWait().ifPresent(response -> {
                if (response == alert.getButtonTypes().get(0) && onRetry != null) {
                    onRetry.run();
                }
            });
        });
    }

    public static void handleUIException(Exception e, String component) {
        e.printStackTrace();
        Platform.runLater(() -> {
            AlertUtils.showError("界面错误",
                    "组件加载失败: " + component,
                    "请重启应用程序\n错误详情: " + e.getMessage());
        });
    }

    public static void handleGeneralException(Exception e, String operation) {
        e.printStackTrace();
        Platform.runLater(() -> {
            AlertUtils.showError("操作失败",
                    operation + " 失败",
                    "发生未知错误，请稍后重试\n错误详情: " + e.getMessage());
        });
    }
}
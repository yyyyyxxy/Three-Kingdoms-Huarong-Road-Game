import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import java.util.Optional;

public class AlertUtils {

    public static void showError(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.ERROR);
    }

    public static void showInfo(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.INFORMATION);
    }

    public static void showWarning(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.WARNING);
    }

    public static Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        applyDialogStyle(alert.getDialogPane());

        return alert.showAndWait();
    }

    private static void showAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        applyDialogStyle(alert.getDialogPane());

        alert.showAndWait();
    }

    public static void applyDialogStyle(DialogPane dialogPane) {
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom,#f0e6d6,#e0c3a3);"
                + "-fx-font-family: '微软雅黑'; -fx-font-size: 16px;");

        // 设置标题样式
        if (dialogPane.lookup(".header-panel") != null) {
            dialogPane.lookup(".header-panel").setStyle("-fx-background-color: transparent;");
        }

        if (dialogPane.lookup(".header-panel .label") != null) {
            ((Label)dialogPane.lookup(".header-panel .label")).setStyle(
                    "-fx-text-fill: #7d5a3a; -fx-font-size: 22px; -fx-font-weight: bold;");
        }
    }
}
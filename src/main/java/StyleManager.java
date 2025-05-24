
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class StyleManager {

    private static final String CSS_FILE = "/styles.css";

    /**
     * 为Scene应用CSS样式
     */
    public static void applyStyles(Scene scene) {
        try {
            String cssResource = StyleManager.class.getResource(CSS_FILE).toExternalForm();
            scene.getStylesheets().add(cssResource);
        } catch (Exception e) {
            System.err.println("无法加载CSS文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用主界面背景样式
     */
    public static void applyMainBackground(Region region) {
        region.getStyleClass().add("main-background");
    }

    /**
     * 应用ScrollPane样式（解决警告问题的关键）
     */
    public static void applyScrollPaneStyle(ScrollPane scrollPane) {
        scrollPane.getStyleClass().add("scroll-pane");
        // 移除内联样式，完全依靠CSS
        scrollPane.setStyle(null);
    }

    /**
     * 应用标题样式
     */
    public static void applyTitleStyle(Label label, TitleType type) {
        switch (type) {
            case MAIN:
                label.getStyleClass().add("main-title");
                break;
            case SECTION:
                label.getStyleClass().add("section-title");
                break;
            case SUB:
                label.getStyleClass().add("sub-title");
                break;
        }
    }

    /**
     * 应用按钮样式
     */
    public static void applyButtonStyle(Button button, ButtonType type) {
        // 先清除可能的内联样式
        button.setStyle(null);

        switch (type) {
            case MENU:
                button.getStyleClass().add("menu-button");
                break;
            case LAYOUT_SELECT:
                button.getStyleClass().add("layout-select-button");
                break;
            case REPLAY:
                button.getStyleClass().add("action-button-replay");
                break;
            case RESTORE:
                button.getStyleClass().add("action-button-restore");
                break;
            case DELETE:
                button.getStyleClass().add("action-button-delete");
                break;
            case WATCH:
                button.getStyleClass().add("action-button-watch");
                break;
            case CHAT:
                button.getStyleClass().add("action-button-chat");
                break;
            case FRIEND_REQUEST:
                button.getStyleClass().add("friend-request-button");
                break;
            case PRIVATE_CHAT:
                button.getStyleClass().add("private-chat-button");
                break;
            case SEND:
                button.getStyleClass().add("send-button");
                break;
        }
    }

    /**
     * 应用聊天界面样式
     */
    public static void applyChatStyles(BorderPane root, HBox header, Label headerTitle,
                                       VBox chatArea, VBox bottomArea, TextArea messageInput) {
        root.getStyleClass().add("chat-background");
        header.getStyleClass().add("chat-header");
        headerTitle.getStyleClass().add("chat-header-title");
        chatArea.getStyleClass().add("chat-area");
        bottomArea.getStyleClass().add("chat-bottom-area");
        messageInput.getStyleClass().add("chat-input");
    }

    /**
     * 应用消息气泡样式
     */
    public static void applyMessageBubbleStyle(VBox bubble, boolean isFromCurrentUser) {
        if (isFromCurrentUser) {
            bubble.getStyleClass().add("message-bubble-sent");
        } else {
            bubble.getStyleClass().add("message-bubble-received");
        }
    }

    /**
     * 应用表格样式
     */
    public static void applyTableStyle(TableView<?> table) {
        // TableView会自动使用CSS中定义的样式
    }

    /**
     * 应用对话框样式
     */
    public static void applyDialogStyle(DialogPane dialogPane) {
        dialogPane.getStyleClass().add("dialog-pane");
        // 移除内联样式
        dialogPane.setStyle(null);
    }

    /**
     * 应用特殊标签样式
     */
    public static void applyLabelStyle(Label label, LabelType type) {
        switch (type) {
            case COIN:
                label.getStyleClass().add("coin-label");
                break;
            case UNREAD_MESSAGE:
                label.getStyleClass().add("unread-message-label");
                break;
            case AI_STATUS:
                label.getStyleClass().add("ai-status-label");
                break;
            case ELAPSED_TIME:
                label.getStyleClass().add("elapsed-time-label");
                break;
            case STEP:
                label.getStyleClass().add("step-label");
                break;
        }
    }

    // 枚举定义
    public enum TitleType {
        MAIN, SECTION, SUB
    }

    public enum ButtonType {
        MENU, LAYOUT_SELECT, REPLAY, RESTORE, DELETE,
        WATCH, CHAT, FRIEND_REQUEST, PRIVATE_CHAT, SEND
    }

    public enum LabelType {
        COIN, UNREAD_MESSAGE, AI_STATUS, ELAPSED_TIME, STEP
    }
}
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class ReplayBoardPane extends StackPane {
    private final int BOARD_ROWS = 5;
    private final int BOARD_COLS = 4;
    private final int CELL_SIZE = 100;
    private List<List<GameFrame.Block>> steps;
    private int currentStep = 0;
    private GridPane board;
    private Label stepLabel;

    public ReplayBoardPane(List<GameFrame.Block> layoutBlocks, List<String> historyStack, Label stepLabel) {
        this.stepLabel = stepLabel;
        setAlignment(Pos.CENTER);
        setPrefSize(BOARD_COLS * CELL_SIZE + 20, BOARD_ROWS * CELL_SIZE + 20);

        // 解析历史栈
        steps = new ArrayList<>();
        for (String serialized : historyStack) {
            List<GameFrame.Block> state = new ArrayList<>();
            String[] blockStrings = serialized.split(";");
            for (String blockString : blockStrings) {
                if (!blockString.isEmpty()) {
                    String[] parts = blockString.split("[()]");
                    String name = parts[0];
                    String[] coords = parts[1].split(",");
                    int row = Integer.parseInt(coords[0]);
                    int col = Integer.parseInt(coords[1]);
                    GameFrame.Block original = layoutBlocks.stream().filter(b -> b.getName().equals(name)).findFirst().orElse(null);
                    if (original != null) {
                        state.add(new GameFrame.Block(row, col, original.getWidth(), original.getHeight(), original.getColor(), original.getName()));
                    }
                }
            }
            steps.add(state);
        }
        if (steps.isEmpty()) {
            steps.add(layoutBlocks);
        }
        drawStep(0);
    }

    public void prevStep() {
        if (currentStep > 0) {
            currentStep--;
            drawStep(currentStep);
        }
    }

    public void nextStep() {
        if (currentStep < steps.size() - 1) {
            currentStep++;
            drawStep(currentStep);
        } else {
            // 已到最后一步
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("已到达最后一步");
            alert.showAndWait();
        }
    }

    private void drawStep(int stepIndex) {
        getChildren().clear();
        board = new GridPane();
        board.setHgap(0);
        board.setVgap(0);
        board.setPadding(Insets.EMPTY);
        board.setStyle("-fx-background-color: transparent;");
        board.setPrefSize(BOARD_COLS * CELL_SIZE, BOARD_ROWS * CELL_SIZE);

        // 背景格子
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int col = 0; col < BOARD_COLS; col++) {
                StackPane cellPane = new StackPane();
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setArcWidth(18);
                cell.setArcHeight(18);
                cell.setFill(Color.LIGHTGRAY);
                cell.setStroke(null);
                cellPane.getChildren().add(cell);
                board.add(cellPane, col, row);
            }
        }

        // 棋子
        for (GameFrame.Block block : steps.get(stepIndex)) {
            StackPane blockPane = new StackPane();
            Rectangle rect = new Rectangle(
                    block.getWidth() * CELL_SIZE - 4,
                    block.getHeight() * CELL_SIZE - 4
            );
            rect.setFill(block.getColor());
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(2);
            rect.setArcWidth(18);
            rect.setArcHeight(18);

            Label label = new Label(block.getName());
            label.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

            blockPane.getChildren().addAll(rect, label);
            board.add(blockPane, block.getCol(), block.getRow(), block.getWidth(), block.getHeight());
        }

        getChildren().add(board);

        // 步数显示
        if (stepLabel != null) {
            stepLabel.setText("当前步数：" + (stepIndex + 1) + " / " + steps.size());
        }
    }
    public void setBlocks(List<GameFrame.Block> blocks) {
        // 只读显示blocks，不允许操作
        this.steps = new ArrayList<>();
        this.steps.add(blocks);
        this.currentStep = 0;
        drawStep(0);
    }
}
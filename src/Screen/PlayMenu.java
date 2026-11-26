package src.Screen;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import src.Ai.MctsUct;
import src.Board.BoardData;
import src.Main.StateMachine;
import src.Window.Window;

public class PlayMenu extends Screen {
    private static PlayMenu instance;
    private static final double GRID_SPACING = 36;
    private static final double BOARD_MARGIN = 28;
    private static final double WINDOW_WIDTH = 1280;
    private static final double WINDOW_HEIGHT = 720;
    private static final int MCTS_ITERATIONS = 300;
    private static final double UCT_EXPLORATION = Math.sqrt(2.0);
    private double gridLeft;
    private double gridTop;
    private int currentPlayer = BoardData.PLAYER_X;
    private final MctsUct ai = new MctsUct();
    private final Group stones = new Group();
    private final Text statusText = new Text();

    private PlayMenu() {
        super();
        this.root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setStyle("-fx-background-color: #202a32;");
        drawBoard();
        statusText.setX(500);
        statusText.setY(42);
        statusText.setFill(Color.WHITE);
        statusText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        updateTurnText();
        root.getChildren().addAll(stones, statusText);
        root.setOnMouseClicked(this::handleBoardClick);
    }

    private void drawBoard() {
        double gridWidth = BoardData.COLS * GRID_SPACING;
        double gridHeight = BoardData.ROWS * GRID_SPACING;
        double boardWidth = gridWidth + 2 * BOARD_MARGIN;
        double boardHeight = gridHeight + 2 * BOARD_MARGIN;
        double offsetX = (WINDOW_WIDTH - boardWidth) / 2;
        double offsetY = (WINDOW_HEIGHT - boardHeight) / 2;
        gridLeft = offsetX + BOARD_MARGIN;
        gridTop = offsetY + BOARD_MARGIN;

        Rectangle boardBackground = new Rectangle(
            offsetX,
            offsetY,
            boardWidth,
            boardHeight
        );
        boardBackground.setFill(Color.BURLYWOOD);
        boardBackground.setStroke(Color.DARKGOLDENROD);
        root.getChildren().add(boardBackground);

        for (int row = 0; row <= BoardData.ROWS; row++) {
            double y = gridTop + row * GRID_SPACING;
            root.getChildren().add(
                new Line(gridLeft, y, gridLeft + gridWidth, y)
            );
        }

        for (int col = 0; col <= BoardData.COLS; col++) {
            double x = gridLeft + col * GRID_SPACING;
            root.getChildren().add(
                new Line(x, gridTop, x, gridTop + gridHeight)
            );
        }
    }

    private void handleBoardClick(MouseEvent event) {
        Point2D point = root.sceneToLocal(event.getSceneX(), event.getSceneY());
        double gridWidth = BoardData.COLS * GRID_SPACING;
        double gridHeight = BoardData.ROWS * GRID_SPACING;

        if (point.getX() < gridLeft || point.getX() > gridLeft + gridWidth
                || point.getY() < gridTop || point.getY() > gridTop + gridHeight) {
            statusText.setText("Nước đi không hợp lệ!");
            return;
        }

        int col = Math.min(
            (int) ((point.getX() - gridLeft) / GRID_SPACING),
            BoardData.COLS - 1
        );
        int row = Math.min(
            (int) ((point.getY() - gridTop) / GRID_SPACING),
            BoardData.ROWS - 1
        );
        if (!BoardData.placeStone(row, col, currentPlayer)) {
            statusText.setText("Nước đi không hợp lệ!");
            return;
        }

        drawStone(row, col, currentPlayer);
        if (finishGameIfNeeded(row, col, currentPlayer)) {
            return;
        }

        currentPlayer = BoardData.PLAYER_O;
        statusText.setText("AI đang suy nghĩ...");
        playAiTurn();
    }

    private void playAiTurn() {
        MctsUct.Move move = ai.findBestMove(
            BoardData.PLAYER_O,
            MCTS_ITERATIONS,
            UCT_EXPLORATION,
            true
        );

        if (move == null) {
            if (BoardData.isFull()) {
                showGameOver("Hòa");
                return;
            }
            throw new IllegalStateException(
                "AI khong tim thay nuoc di tren ban co con o trong."
            );
        }

        if (!BoardData.placeStone(move.row, move.col, BoardData.PLAYER_O)) {
            throw new IllegalStateException(
                "AI chon mot nuoc di khong hop le."
            );
        }

        drawStone(move.row, move.col, BoardData.PLAYER_O);
        if (finishGameIfNeeded(move.row, move.col, BoardData.PLAYER_O)) {
            return;
        }

        currentPlayer = BoardData.PLAYER_X;
        updateTurnText();
    }

    private boolean finishGameIfNeeded(int row, int col, int player) {
        if (BoardData.hasWon(row, col, player)) {
            String winner = player == BoardData.PLAYER_X ? "Bạn" : "Ai";
            showGameOver(winner + " thắng!");
            return true;
        }

        if (BoardData.isFull()) {
            showGameOver("Hòa");
            return true;
        }

        return false;
    }

    private void showGameOver(String result) {
        GameOver.getInstance().setResult(result);
        StateMachine.getInstance().setState(StateMachine.State.GAME_OVER);
        Window.getInstance().changeScreen();
    }

    private void drawStone(int row, int col, int player) {
        Color color = player == BoardData.PLAYER_X ? Color.BLACK : Color.WHITE;
        Circle stone = new Circle(
            gridLeft + (col + 0.5) * GRID_SPACING,
            gridTop + (row + 0.5) * GRID_SPACING,
            GRID_SPACING * 0.34,
            color
        );
        stone.setStroke(Color.DIMGRAY);
        stones.getChildren().add(stone);
    }

    private void updateTurnText() {
        String player = currentPlayer == BoardData.PLAYER_X ? "Bạn" : "Ai";
        statusText.setText("Lượt của " + player);
    }

    public void resetGame() {
        BoardData.resetBoard();
        currentPlayer = BoardData.PLAYER_X;
        stones.getChildren().clear();
        updateTurnText();
    }

    public static PlayMenu getInstance() {
        if (instance == null) {
            instance = new PlayMenu();
        }
        return instance;
    }
}

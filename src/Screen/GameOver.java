package src.Screen;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import src.Board.BoardData;
import src.Main.StateMachine;
import src.Window.Window;

public class GameOver extends Screen {
    private static GameOver instance;
    private final Text resultText = new Text("Hòa");

    private GameOver() {
        super();
        root.setPrefSize(1280, 720);
        root.setStyle("-fx-background-color: #202a32;");

        resultText.setX(600);
        resultText.setY(320);
        resultText.setFill(Color.WHITE);
        resultText.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Button restartButton = new Button("Chơi lại");
        restartButton.setLayoutX(590);
        restartButton.setLayoutY(370);
        restartButton.setOnAction(event -> {
            PlayMenu.getInstance().resetGame();
            StateMachine.getInstance().setState(StateMachine.State.MAIN_MENU);
            Window.getInstance().changeScreen();
        });

        root.getChildren().addAll(resultText, restartButton);
    }

    public static GameOver getInstance() {
        if (instance == null) {
            instance = new GameOver();
        }
        return instance;
    }

    public void setResult(String result) {
        resultText.setText(result);
    }
}

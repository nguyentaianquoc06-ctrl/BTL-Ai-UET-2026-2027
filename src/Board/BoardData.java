package src.Board;

public class BoardData {

    public static final int BOARD_SIZE = 15;
    public static final int ROWS = BOARD_SIZE;
    public static final int COLS = BOARD_SIZE;
    public static final int WIN_LENGTH = 5;

    public static final int EMPTY = 0;
    public static final int PLAYER_X = 1;
    public static final int PLAYER_O = 2;

    private static final int[][] board = new int[ROWS][COLS];

    static {
        resetBoard();
    }

    public static void resetBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = EMPTY;
            }
        }
    }

    public static boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < ROWS
            && col >= 0 && col < COLS;
    }

    public static int getCell(int row, int col) {
        if (!isInsideBoard(row, col)) {
            throw new IndexOutOfBoundsException(
                "Vi tri nam ngoai ban co: (" + row + ", " + col + ")"
            );
        }

        return board[row][col];
    }

    public static boolean placeStone(int row, int col, int player) {
        if (player != PLAYER_X && player != PLAYER_O) {
            throw new IllegalArgumentException(
                "Nguoi choi khong hop le: " + player
            );
        }
        if (!isInsideBoard(row, col) || board[row][col] != EMPTY) {
            return false;
        }

        board[row][col] = player;
        return true;
    }

    public static boolean undoMove(int row, int col, int player) {
        if (!isInsideBoard(row, col) || board[row][col] != player) {
            return false;
        }

        board[row][col] = EMPTY;
        return true;
    }

    public static boolean hasWon(int row, int col, int player) {
        if (!isInsideBoard(row, col) || board[row][col] != player) {
            return false;
        }

        int[][] directions = {
            {0, 1},
            {1, 0},
            {1, 1},
            {1, -1}
        };

        for (int[] direction : directions) {
            int count = 1;
            count += countInDirection(
                row, col, direction[0], direction[1], player
            );
            count += countInDirection(
                row, col, -direction[0], -direction[1], player
            );
            if (count >= WIN_LENGTH) {
                return true;
            }
        }

        return false;
    }

    private static int countInDirection(
            int row, int col, int rowStep, int colStep, int player) {
        int count = 0;
        int nextRow = row + rowStep;
        int nextCol = col + colStep;

        while (isInsideBoard(nextRow, nextCol)
                && board[nextRow][nextCol] == player) {
            count++;
            nextRow += rowStep;
            nextCol += colStep;
        }

        return count;
    }

    public static boolean isFull() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == EMPTY) {
                    return false;
                }
            }
        }

        return true;
    }
}

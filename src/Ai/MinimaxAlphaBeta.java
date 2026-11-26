package src.Ai;

import java.util.ArrayList;
import java.util.List;

import src.Board.BoardData;

public class MinimaxAlphaBeta {

    private static final int WIN_SCORE = 100_000;
    private static final int CANDIDATE_RADIUS = 2;
    private static final int[] PATTERN_SCORES = {
        0, 1, 10, 100, 1_000, WIN_SCORE
    };

    private int searchedNodes;

    public static final class Move {
        public final int row;
        public final int col;

        private Move(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    public Move findBestMove(int aiPlayer, int depth) {
        validateSearch(aiPlayer, depth);
        searchedNodes = 0;

        int opponent = getOpponent(aiPlayer);
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestScore = Integer.MIN_VALUE;
        Move bestMove = null;

        for (Move move : getCandidateMoves()) {
            makeSearchMove(move, aiPlayer);
            int score;
            try {
                score = minimax(
                    depth - 1,
                    alpha,
                    beta,
                    false,
                    aiPlayer,
                    opponent,
                    move.row,
                    move.col,
                    aiPlayer
                );
            } finally {
                undoSearchMove(move, aiPlayer);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestScore);
        }

        return bestMove;
    }

    public int getSearchedNodes() {
        return searchedNodes;
    }

    private int minimax(
            int depth,
            int alpha,
            int beta,
            boolean maximizing,
            int aiPlayer,
            int opponent,
            int lastRow,
            int lastCol,
            int lastPlayer) {
        searchedNodes++;

        if (BoardData.hasWon(lastRow, lastCol, lastPlayer)) {
            if (lastPlayer == aiPlayer) {
                return WIN_SCORE + depth;
            }
            return -WIN_SCORE - depth;
        }

        if (BoardData.isFull()) {
            return 0;
        }
        if (depth == 0) {
            return evaluatePosition(aiPlayer, opponent);
        }

        List<Move> moves = getCandidateMoves();
        if (moves.isEmpty()) {
            return evaluatePosition(aiPlayer, opponent);
        }

        int player = maximizing ? aiPlayer : opponent;
        int bestScore = maximizing
            ? Integer.MIN_VALUE
            : Integer.MAX_VALUE;

        for (Move move : moves) {
            makeSearchMove(move, player);
            int score;
            try {
                score = minimax(
                    depth - 1,
                    alpha,
                    beta,
                    !maximizing,
                    aiPlayer,
                    opponent,
                    move.row,
                    move.col,
                    player
                );
            } finally {
                undoSearchMove(move, player);
            }

            if (maximizing) {
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
            } else {
                bestScore = Math.min(bestScore, score);
                beta = Math.min(beta, bestScore);
            }

            if (beta <= alpha) {
                break;
            }
        }

        return bestScore;
    }

    private List<Move> getCandidateMoves() {
        List<Move> moves = new ArrayList<>();
        boolean[][] candidates =
            new boolean[BoardData.ROWS][BoardData.COLS];
        boolean boardHasStone = false;

        for (int row = 0; row < BoardData.ROWS; row++) {
            for (int col = 0; col < BoardData.COLS; col++) {
                if (BoardData.getCell(row, col) == BoardData.EMPTY) {
                    continue;
                }

                boardHasStone = true;
                for (int rowOffset = -CANDIDATE_RADIUS;
                        rowOffset <= CANDIDATE_RADIUS; rowOffset++) {
                    for (int colOffset = -CANDIDATE_RADIUS;
                            colOffset <= CANDIDATE_RADIUS; colOffset++) {
                        int candidateRow = row + rowOffset;
                        int candidateCol = col + colOffset;
                        if (BoardData.isInsideBoard(candidateRow, candidateCol)
                                && BoardData.getCell(
                                    candidateRow, candidateCol
                                ) == BoardData.EMPTY
                                && !candidates[candidateRow][candidateCol]) {
                            candidates[candidateRow][candidateCol] = true;
                            moves.add(new Move(candidateRow, candidateCol));
                        }
                    }
                }
            }
        }

        if (!boardHasStone) {
            int center = BoardData.BOARD_SIZE / 2;
            moves.add(new Move(center, center));
        }

        return moves;
    }

    private int evaluatePosition(int aiPlayer, int opponent) {
        int score = 0;
        int[][] directions = {
            {0, 1},
            {1, 0},
            {1, 1},
            {1, -1}
        };

        for (int row = 0; row < BoardData.ROWS; row++) {
            for (int col = 0; col < BoardData.COLS; col++) {
                for (int[] direction : directions) {
                    int endRow = row
                        + (BoardData.WIN_LENGTH - 1) * direction[0];
                    int endCol = col
                        + (BoardData.WIN_LENGTH - 1) * direction[1];
                    if (!BoardData.isInsideBoard(endRow, endCol)) {
                        continue;
                    }

                    int aiCount = 0;
                    int opponentCount = 0;
                    for (int step = 0; step < BoardData.WIN_LENGTH; step++) {
                        int cell = BoardData.getCell(
                            row + step * direction[0],
                            col + step * direction[1]
                        );
                        if (cell == aiPlayer) {
                            aiCount++;
                        } else if (cell == opponent) {
                            opponentCount++;
                        }
                    }

                    if (opponentCount == 0) {
                        score += PATTERN_SCORES[aiCount];
                    } else if (aiCount == 0) {
                        score -= PATTERN_SCORES[opponentCount];
                    }
                }
            }
        }

        return score;
    }

    private void makeSearchMove(Move move, int player) {
        if (!BoardData.placeStone(move.row, move.col, player)) {
            throw new IllegalStateException(
                "Minimax thu dat quan vao o khong hop le."
            );
        }
    }

    private void undoSearchMove(Move move, int player) {
        if (!BoardData.undoMove(move.row, move.col, player)) {
            throw new IllegalStateException(
                "Minimax khong the hoan tac nuoc di da thu."
            );
        }
    }

    private void validateSearch(int aiPlayer, int depth) {
        if (aiPlayer != BoardData.PLAYER_X
                && aiPlayer != BoardData.PLAYER_O) {
            throw new IllegalArgumentException("Ma nguoi choi khong hop le.");
        }
        if (depth < 1) {
            throw new IllegalArgumentException("Do sau phai lon hon 0.");
        }
    }

    private int getOpponent(int player) {
        return player == BoardData.PLAYER_X
            ? BoardData.PLAYER_O
            : BoardData.PLAYER_X;
    }
}

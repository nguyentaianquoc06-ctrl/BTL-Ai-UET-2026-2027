package src.Ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import src.Board.BoardData;

public class MctsUct {

    private static final int CANDIDATE_RADIUS = 2;
    private final Random random = new Random();

    public static final class Move {
        public final int row;
        public final int col;

        private Move(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private static final class BoardState {
        private final int[][] cells;
        private int movesPlayed;
        private int lastRow = -1;
        private int lastCol = -1;
        private int lastPlayer = BoardData.EMPTY;

        private BoardState() {
            cells = new int[BoardData.ROWS][BoardData.COLS];
            for (int row = 0; row < BoardData.ROWS; row++) {
                for (int col = 0; col < BoardData.COLS; col++) {
                    cells[row][col] = BoardData.getCell(row, col);
                    if (cells[row][col] != BoardData.EMPTY) {
                        movesPlayed++;
                    }
                }
            }
        }

        private BoardState(BoardState source) {
            cells = new int[BoardData.ROWS][BoardData.COLS];
            for (int row = 0; row < BoardData.ROWS; row++) {
                System.arraycopy(source.cells[row], 0, cells[row], 0,
                    BoardData.COLS);
            }
            movesPlayed = source.movesPlayed;
            lastRow = source.lastRow;
            lastCol = source.lastCol;
            lastPlayer = source.lastPlayer;
        }

        private void play(Move move, int player) {
            cells[move.row][move.col] = player;
            movesPlayed++;
            lastRow = move.row;
            lastCol = move.col;
            lastPlayer = player;
        }

        private boolean isTerminal() {
            return movesPlayed == BoardData.ROWS * BoardData.COLS
                || (lastPlayer != BoardData.EMPTY
                    && hasWon(cells, lastRow, lastCol, lastPlayer));
        }

        private List<Move> emptyMoves() {
            List<Move> moves = new ArrayList<>();
            for (int row = 0; row < BoardData.ROWS; row++) {
                for (int col = 0; col < BoardData.COLS; col++) {
                    if (cells[row][col] == BoardData.EMPTY) {
                        moves.add(new Move(row, col));
                    }
                }
            }
            return moves;
        }

        private List<Move> candidateMoves() {
            List<Move> moves = new ArrayList<>();
            boolean[][] candidates =
                new boolean[BoardData.ROWS][BoardData.COLS];
            boolean hasStone = false;

            for (int row = 0; row < BoardData.ROWS; row++) {
                for (int col = 0; col < BoardData.COLS; col++) {
                    if (cells[row][col] == BoardData.EMPTY) {
                        continue;
                    }

                    hasStone = true;
                    for (int rowOffset = -CANDIDATE_RADIUS;
                            rowOffset <= CANDIDATE_RADIUS; rowOffset++) {
                        for (int colOffset = -CANDIDATE_RADIUS;
                                colOffset <= CANDIDATE_RADIUS; colOffset++) {
                            int nextRow = row + rowOffset;
                            int nextCol = col + colOffset;
                            if (inside(nextRow, nextCol)
                                    && cells[nextRow][nextCol]
                                        == BoardData.EMPTY
                                    && !candidates[nextRow][nextCol]) {
                                candidates[nextRow][nextCol] = true;
                                moves.add(new Move(nextRow, nextCol));
                            }
                        }
                    }
                }
            }

            if (!hasStone) {
                moves.add(new Move(
                    BoardData.ROWS / 2,
                    BoardData.COLS / 2
                ));
            } else if (moves.isEmpty()) {
                return emptyMoves();
            }
            return moves;
        }
    }

    private static final class Node {
        private final BoardState state;
        private final Node parent;
        private final Move move;
        private final int playerToMove;
        private final int rootPlayer;
        private final List<Node> children = new ArrayList<>();
        private final List<Move> untriedMoves;
        private int visits;
        private double reward;

        private Node(
                BoardState state,
                Node parent,
                Move move,
                int playerToMove,
                int rootPlayer) {
            this.state = state;
            this.parent = parent;
            this.move = move;
            this.playerToMove = playerToMove;
            this.rootPlayer = rootPlayer;
            untriedMoves = state.candidateMoves();
        }

        private Node selectChild(double explorationConstant) {
            Node bestChild = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Node child : children) {
                double meanReward = child.reward / child.visits;
                if (playerToMove != rootPlayer) {
                    meanReward = -meanReward;
                }

                double exploration = explorationConstant * Math.sqrt(
                    Math.log(Math.max(1, visits)) / child.visits
                );
                double score = meanReward + exploration;
                if (score > bestScore) {
                    bestScore = score;
                    bestChild = child;
                }
            }

            return bestChild;
        }

        private Node expand(Random random) {
            int index = random.nextInt(untriedMoves.size());
            Move move = untriedMoves.remove(index);
            BoardState childState = new BoardState(state);
            childState.play(move, playerToMove);

            int nextPlayer = otherPlayer(playerToMove);
            Node child = new Node(
                childState, this, move, nextPlayer, rootPlayer
            );
            children.add(child);
            return child;
        }

        private void backpropagate(double result) {
            Node node = this;
            while (node != null) {
                node.visits++;
                node.reward += result;
                node = node.parent;
            }
        }
    }

    public Move findBestMove(
            int rootPlayer,
            int iterations,
            double explorationConstant,
            boolean heuristicRollout) {
        validateArguments(rootPlayer, iterations, explorationConstant);

        BoardState initialState = new BoardState();
        if (initialState.isTerminal()) {
            return null;
        }

        List<Move> rootMoves = initialState.candidateMoves();
        Move immediateWin = findImmediateWin(
            initialState, rootMoves, rootPlayer
        );
        if (immediateWin != null) {
            return immediateWin;
        }

        Move requiredBlock = findImmediateWin(
            initialState, rootMoves, otherPlayer(rootPlayer)
        );
        if (requiredBlock != null) {
            return requiredBlock;
        }

        Node root = new Node(
            initialState, null, null, rootPlayer, rootPlayer
        );

        for (int iteration = 0; iteration < iterations; iteration++) {
            Node node = root;

            while (!node.state.isTerminal()) {
                if (!node.untriedMoves.isEmpty()) {
                    node = node.expand(random);
                    break;
                }

                node = node.selectChild(explorationConstant);
                if (node == null) {
                    break;
                }
            }

            if (node == null) {
                throw new IllegalStateException(
                    "MCTS khong chon duoc nut tiep theo."
                );
            }

            double result = rollout(
                new BoardState(node.state),
                node.playerToMove,
                rootPlayer,
                heuristicRollout
            );
            node.backpropagate(result);
        }

        Node bestChild = null;
        for (Node child : root.children) {
            if (bestChild == null || child.visits > bestChild.visits) {
                bestChild = child;
            }
        }

        return bestChild == null ? null : bestChild.move;
    }

    private Move findImmediateWin(
            BoardState state,
            List<Move> moves,
            int player) {
        for (Move move : moves) {
            if (winsAfterMove(state, move, player)) {
                return move;
            }
        }
        return null;
    }

    private double rollout(
            BoardState state,
            int playerToMove,
            int rootPlayer,
            boolean heuristic) {
        while (!state.isTerminal()) {
            List<Move> moves = heuristic
                ? state.candidateMoves()
                : state.emptyMoves();
            if (moves.isEmpty()) {
                break;
            }

            Move move = heuristic
                ? chooseHeuristicMove(state, moves, playerToMove)
                : moves.get(random.nextInt(moves.size()));
            state.play(move, playerToMove);
            playerToMove = otherPlayer(playerToMove);
        }

        if (state.lastPlayer == BoardData.EMPTY
                || !hasWon(
                    state.cells,
                    state.lastRow,
                    state.lastCol,
                    state.lastPlayer
                )) {
            return 0.0;
        }
        return state.lastPlayer == rootPlayer ? 1.0 : -1.0;
    }

    private Move chooseHeuristicMove(
            BoardState state,
            List<Move> moves,
            int player) {
        for (Move move : moves) {
            if (winsAfterMove(state, move, player)) {
                return move;
            }
        }

        int opponent = otherPlayer(player);
        for (Move move : moves) {
            if (winsAfterMove(state, move, opponent)) {
                return move;
            }
        }

        int centerRow = BoardData.ROWS / 2;
        int centerCol = BoardData.COLS / 2;
        Move bestMove = moves.get(0);
        int bestDistance = Integer.MAX_VALUE;

        for (Move move : moves) {
            int distance = Math.abs(move.row - centerRow)
                + Math.abs(move.col - centerCol);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private boolean winsAfterMove(BoardState state, Move move, int player) {
        state.cells[move.row][move.col] = player;
        boolean wins = hasWon(state.cells, move.row, move.col, player);
        state.cells[move.row][move.col] = BoardData.EMPTY;
        return wins;
    }

    private static boolean hasWon(
            int[][] cells,
            int row,
            int col,
            int player) {
        int[][] directions = {
            {0, 1},
            {1, 0},
            {1, 1},
            {1, -1}
        };

        for (int[] direction : directions) {
            int count = 1;
            count += countInDirection(
                cells, row, col, direction[0], direction[1], player
            );
            count += countInDirection(
                cells, row, col, -direction[0], -direction[1], player
            );
            if (count >= BoardData.WIN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private static int countInDirection(
            int[][] cells,
            int row,
            int col,
            int rowStep,
            int colStep,
            int player) {
        int count = 0;
        int nextRow = row + rowStep;
        int nextCol = col + colStep;

        while (inside(nextRow, nextCol)
                && cells[nextRow][nextCol] == player) {
            count++;
            nextRow += rowStep;
            nextCol += colStep;
        }
        return count;
    }

    private static boolean inside(int row, int col) {
        return row >= 0 && row < BoardData.ROWS
            && col >= 0 && col < BoardData.COLS;
    }

    private static int otherPlayer(int player) {
        return player == BoardData.PLAYER_X
            ? BoardData.PLAYER_O
            : BoardData.PLAYER_X;
    }

    private static void validateArguments(
            int player,
            int iterations,
            double explorationConstant) {
        if (player != BoardData.PLAYER_X
                && player != BoardData.PLAYER_O) {
            throw new IllegalArgumentException("Ma nguoi choi khong hop le.");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException(
                "So iteration phai lon hon 0."
            );
        }
        if (!Double.isFinite(explorationConstant)
                || explorationConstant < 0.0) {
            throw new IllegalArgumentException(
                "He so UCT c phai la so huu han va khong am."
            );
        }
    }
}

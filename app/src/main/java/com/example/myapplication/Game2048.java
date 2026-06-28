package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Game2048 {

    public static final int SIZE = 4;
    public static final int WIN_VALUE = 2048;

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private int[][] board;
    private int score;
    private int nextTile;
    private Stack<GameState> history;
    private Random random;
    private boolean won;

    public Game2048() {
        board = new int[SIZE][SIZE];
        history = new Stack<>();
        random = new Random();
        newGame();
    }

    public void newGame() {
        board = new int[SIZE][SIZE];
        score = 0;
        won = false;
        history.clear();
        nextTile = generateNextTileValue();
        spawnTile();
    }

    private void spawnTile() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }
        if (emptyCells.isEmpty()) return;

        int[] pos = emptyCells.get(random.nextInt(emptyCells.size()));
        board[pos[0]][pos[1]] = nextTile;
        nextTile = generateNextTileValue();
    }

    private int generateNextTileValue() {
        return random.nextInt(10) < 9 ? 2 : 4;
    }

    public int getNextTile() {
        return nextTile;
    }

    public MoveResult move(Direction direction) {
        int[][] oldBoard = copyBoard();
        int oldScore = score;

        MoveResult result = new MoveResult();
        int[][] merged = new int[SIZE][SIZE];

        switch (direction) {
            case LEFT:
                for (int r = 0; r < SIZE; r++) {
                    RowResult rr = mergeLine(board[r]);
                    board[r] = rr.line;
                    score += rr.score;
                    for (int c = 0; c < SIZE; c++) {
                        if (rr.mergedAt[c] != 0) merged[r][c] = 1;
                    }
                }
                break;
            case RIGHT:
                for (int r = 0; r < SIZE; r++) {
                    int[] reversed = reverse(board[r]);
                    RowResult rr = mergeLine(reversed);
                    board[r] = reverse(rr.line);
                    score += rr.score;
                    for (int c = 0; c < SIZE; c++) {
                        if (rr.mergedAt[c] != 0) merged[r][SIZE - 1 - c] = 1;
                    }
                }
                break;
            case UP:
                for (int c = 0; c < SIZE; c++) {
                    int[] col = new int[SIZE];
                    for (int r = 0; r < SIZE; r++) col[r] = board[r][c];
                    RowResult rr = mergeLine(col);
                    score += rr.score;
                    for (int r = 0; r < SIZE; r++) {
                        board[r][c] = rr.line[r];
                        if (rr.mergedAt[r] != 0) merged[r][c] = 1;
                    }
                }
                break;
            case DOWN:
                for (int c = 0; c < SIZE; c++) {
                    int[] col = new int[SIZE];
                    for (int r = 0; r < SIZE; r++) col[r] = board[r][c];
                    int[] reversed = reverse(col);
                    RowResult rr = mergeLine(reversed);
                    score += rr.score;
                    int[] resultCol = reverse(rr.line);
                    for (int r = 0; r < SIZE; r++) {
                        board[r][c] = resultCol[r];
                        if (rr.mergedAt[r] != 0) merged[SIZE - 1 - r][c] = 1;
                    }
                }
                break;
        }

        result.mergedAt = merged;
        result.score = score - oldScore;
        result.moved = !boardsEqual(oldBoard, board);

        if (result.moved) {
            history.push(new GameState(oldBoard, oldScore));
            int[][] beforeSpawn = copyBoard();
            spawnTile();
            // 找到新方块位置
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (beforeSpawn[r][c] == 0 && board[r][c] != 0) {
                        result.spawnedRow = r;
                        result.spawnedCol = c;
                        result.newTileValue = board[r][c];
                        break;
                    }
                }
            }
            if (!won && hasWon()) won = true;
        }

        return result;
    }

    private RowResult mergeLine(int[] line) {
        int[] mergedAt = new int[SIZE];
        int[] result = new int[SIZE];
        int pos = 0;

        for (int i = 0; i < SIZE; i++) {
            if (line[i] == 0) continue;
            if (result[pos] == 0) {
                result[pos] = line[i];
            } else if (result[pos] == line[i]) {
                result[pos] *= 2;
                mergedAt[pos] = 1;
                pos++;
            } else {
                pos++;
                result[pos] = line[i];
            }
        }

        return new RowResult(result, getScoreFromMerge(mergedAt, result), mergedAt);
    }

    private int getScoreFromMerge(int[] mergedAt, int[] result) {
        int s = 0;
        for (int i = 0; i < SIZE; i++) {
            if (mergedAt[i] != 0) s += result[i];
        }
        return s;
    }

    private int[] reverse(int[] arr) {
        int[] rev = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            rev[i] = arr[arr.length - 1 - i];
        }
        return rev;
    }

    private boolean boardsEqual(int[][] a, int[][] b) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (a[r][c] != b[r][c]) return false;
            }
        }
        return true;
    }

    private int[][] copyBoard() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    private boolean hasWon() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] >= WIN_VALUE) return true;
            }
        }
        return false;
    }

    public boolean isWin() {
        return won;
    }

    public boolean isGameOver() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) return false;
                if (c < SIZE - 1 && board[r][c] == board[r][c + 1]) return false;
                if (r < SIZE - 1 && board[r][c] == board[r + 1][c]) return false;
            }
        }
        return true;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public boolean undo() {
        if (history.isEmpty()) return false;
        GameState state = history.pop();
        board = state.board;
        score = state.score;
        return true;
    }

    /**
     * 锤子道具：移除指定位置的方块（支持撤回）
     */
    public boolean removeTile(int row, int col) {
        if (board[row][col] == 0) return false;
        int[][] oldBoard = copyBoard();
        history.push(new GameState(oldBoard, score));
        board[row][col] = 0;
        return true;
    }

    /**
     * 魔法道具：将指定方块变幻为目标值（支持撤回）
     */
    public boolean transformTile(int row, int col, int newValue) {
        if (board[row][col] == 0) return false;
        int[][] oldBoard = copyBoard();
        history.push(new GameState(oldBoard, score));
        board[row][col] = newValue;
        return true;
    }

    public int getCell(int row, int col) {
        return board[row][col];
    }

    public int getScore() {
        return score;
    }

    public int[][] getBoard() {
        return copyBoard();
    }

    // 内部类
    private static class RowResult {
        int[] line;
        int score;
        int[] mergedAt;

        RowResult(int[] line, int score, int[] mergedAt) {
            this.line = line;
            this.score = score;
            this.mergedAt = mergedAt;
        }
    }

    private static class GameState {
        int[][] board;
        int score;

        GameState(int[][] board, int score) {
            this.board = board;
            this.score = score;
        }
    }

    public static class MoveResult {
        public boolean moved;
        public int score;
        public int[][] mergedAt;
        public int spawnedRow = -1;
        public int spawnedCol = -1;
        public int newTileValue;
    }
}
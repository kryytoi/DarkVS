package dev.darkvisuals.client.ui.games;

import dev.darkvisuals.client.managers.ThemeManager;
import dev.darkvisuals.client.util.animations.SmoothAnimation;
import dev.darkvisuals.client.util.renderer.Render2D;
import dev.darkvisuals.client.util.renderer.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamesScreen extends Screen {

    private enum GameMode {
        SNAKE,
        TICTACTOE
    }

    private enum Difficulty {
        EASY("Лёгкий"),
        NORM("Норм"),
        HARD("Сложный"),
        VERY_HARD("ХАРД");

        final String title;
        Difficulty(String title) {
            this.title = title;
        }
    }

    private GameMode currentMode = GameMode.SNAKE;
    private final SmoothAnimation openAnim = new SmoothAnimation(0f, 14f);

    private static final int GRID_SIZE = 20;
    private final List<int[]> snake = new ArrayList<>();
    private int foodX = 5, foodY = 5;
    private int dirX = 1, dirY = 0;
    private int nextDirX = 1, nextDirY = 0;
    private boolean snakeGameOver = false;
    private boolean snakePaused = false;
    private int snakeScore = 0;
    private static int snakeHighScore = 0;
    private long lastSnakeMoveTime = 0L;

    private final int[] board = new int[9];
    private boolean playerTurn = true;
    private int winner = 0;
    private boolean tttGameOver = false;
    private Difficulty difficulty = Difficulty.NORM;
    private int playerWins = 0;
    private int botWins = 0;
    private int draws = 0;
    private long botMoveTime = 0L;
    private final Random random = new Random();

    public GamesScreen() {
        super(Text.literal("Игрульки"));
        initSnake();
        resetTicTacToe();
    }

    @Override
    protected void init() {
        super.init();
        openAnim.snapTo(0f);
        openAnim.setTarget(1f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private void initSnake() {
        snake.clear();
        int startX = GRID_SIZE / 2;
        int startY = GRID_SIZE / 2;
        snake.add(new int[]{startX, startY});
        snake.add(new int[]{startX - 1, startY});
        snake.add(new int[]{startX - 2, startY});
        dirX = 1; dirY = 0;
        nextDirX = 1; nextDirY = 0;
        snakeScore = 0;
        snakeGameOver = false;
        snakePaused = false;
        spawnFood();
        lastSnakeMoveTime = System.currentTimeMillis();
    }

    private void spawnFood() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                if (!isSnakeAt(x, y)) {
                    emptyCells.add(new int[]{x, y});
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            foodX = cell[0];
            foodY = cell[1];
        }
    }

    private boolean isSnakeAt(int x, int y) {
        for (int[] part : snake) {
            if (part[0] == x && part[1] == y) return true;
        }
        return false;
    }

    private void updateSnake() {
        if (snakeGameOver || snakePaused) return;

        long now = System.currentTimeMillis();
        long stepDelay = Math.max(75L, 140L - (snakeScore * 2L));
        if (now - lastSnakeMoveTime < stepDelay) return;
        lastSnakeMoveTime = now;

        dirX = nextDirX;
        dirY = nextDirY;

        int[] head = snake.get(0);
        int newX = head[0] + dirX;
        int newY = head[1] + dirY;

        if (newX < 0 || newX >= GRID_SIZE || newY < 0 || newY >= GRID_SIZE || isSnakeAt(newX, newY)) {
            snakeGameOver = true;
            if (snakeScore > snakeHighScore) {
                snakeHighScore = snakeScore;
            }
            return;
        }

        snake.add(0, new int[]{newX, newY});
        if (newX == foodX && newY == foodY) {
            snakeScore += 10;
            if (snakeScore > snakeHighScore) {
                snakeHighScore = snakeScore;
            }
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    private void resetTicTacToe() {
        for (int i = 0; i < 9; i++) board[i] = 0;
        playerTurn = true;
        winner = 0;
        tttGameOver = false;
        botMoveTime = 0L;
    }

    private void checkTicTacToeEnd() {
        int win = checkWinner(board);
        if (win != 0) {
            winner = win;
            tttGameOver = true;
            if (win == 1) playerWins++;
            else if (win == 2) botWins++;
            return;
        }

        boolean full = true;
        for (int v : board) {
            if (v == 0) { full = false; break; }
        }
        if (full) {
            winner = 3;
            tttGameOver = true;
            draws++;
        }
    }

    private int checkWinner(int[] b) {
        int[][] lines = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };
        for (int[] line : lines) {
            if (b[line[0]] != 0 && b[line[0]] == b[line[1]] && b[line[1]] == b[line[2]]) {
                return b[line[0]];
            }
        }
        return 0;
    }

    private void makeBotMove() {
        if (tttGameOver) return;

        int move = -1;

        if (difficulty == Difficulty.EASY) {
            move = getRandomMove();
        } else if (difficulty == Difficulty.NORM) {
            move = findImmediateWinOrBlock(2);
            if (move == -1 && random.nextFloat() < 0.6f) {
                move = findImmediateWinOrBlock(1);
            }
            if (move == -1) move = getRandomMove();
        } else if (difficulty == Difficulty.HARD) {
            move = findImmediateWinOrBlock(2);
            if (move == -1) move = findImmediateWinOrBlock(1);
            if (move == -1 && board[4] == 0) move = 4;
            if (move == -1) move = getRandomMove();
        } else if (difficulty == Difficulty.VERY_HARD) {
            move = getBestMoveMinimax();
        }

        if (move >= 0 && move < 9 && board[move] == 0) {
            board[move] = 2;
            playerTurn = true;
            checkTicTacToeEnd();
        }
    }

    private int getRandomMove() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == 0) empty.add(i);
        }
        return empty.isEmpty() ? -1 : empty.get(random.nextInt(empty.size()));
    }

    private int findImmediateWinOrBlock(int targetPlayer) {
        for (int i = 0; i < 9; i++) {
            if (board[i] == 0) {
                board[i] = targetPlayer;
                boolean wins = checkWinner(board) == targetPlayer;
                board[i] = 0;
                if (wins) return i;
            }
        }
        return -1;
    }

    private int getBestMoveMinimax() {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (board[i] == 0) {
                board[i] = 2;
                int score = minimax(board, 0, false);
                board[i] = 0;
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }
            }
        }
        return bestMove != -1 ? bestMove : getRandomMove();
    }

    private int minimax(int[] b, int depth, boolean isMax) {
        int win = checkWinner(b);
        if (win == 2) return 10 - depth;
        if (win == 1) return depth - 10;

        boolean full = true;
        for (int v : b) {
            if (v == 0) { full = false; break; }
        }
        if (full) return 0;

        if (isMax) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (b[i] == 0) {
                    b[i] = 2;
                    int eval = minimax(b, depth + 1, false);
                    b[i] = 0;
                    maxEval = Math.max(maxEval, eval);
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (b[i] == 0) {
                    b[i] = 1;
                    int eval = minimax(b, depth + 1, true);
                    b[i] = 0;
                    minEval = Math.min(minEval, eval);
                }
            }
            return minEval;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        openAnim.update();
        float t = openAnim.getValue();

        MatrixStack stack = context.getMatrices();
        int W = this.width;
        int H = this.height;

        Render2D.drawRect(stack, 0, 0, W, H, new Color(10, 10, 14, (int) (210 * t)));

        float panelW = 540f;
        float panelH = 360f;
        float panelX = (W - panelW) / 2f;
        float panelY = (H - panelH) / 2f;

        Render2D.drawBlurredRect(stack, panelX - 6, panelY - 6, panelW + 12, panelH + 12, 14f, 16f,
                new Color(120, 40, 180, (int) (50 * t)));

        Render2D.drawRoundedRect(stack, panelX, panelY, panelW, panelH, 12f,
                new Color(18, 18, 24, (int) (245 * t)));
        Render2D.drawBorder(stack, panelX, panelY, panelW, panelH, 12f, 0f, 1.2f,
                new Color(255, 255, 255, (int) (35 * t)));

        float tabW = 140f;
        float tabH = 26f;
        float tabY = panelY + 14f;
        float tab1X = panelX + (panelW / 2f) - tabW - 8f;
        float tab2X = panelX + (panelW / 2f) + 8f;

        boolean hoverTab1 = mouseX >= tab1X && mouseX <= tab1X + tabW && mouseY >= tabY && mouseY <= tabY + tabH;
        boolean hoverTab2 = mouseX >= tab2X && mouseX <= tab2X + tabW && mouseY >= tabY && mouseY <= tabY + tabH;

        ThemeManager tm = ThemeManager.getInstance();
        Color accent = tm != null && tm.getCurrentTheme() != null ? tm.getCurrentTheme().getAccentColor() : new Color(160, 60, 240);

        Color tab1Bg = currentMode == GameMode.SNAKE ? accent : (hoverTab1 ? new Color(40, 40, 52) : new Color(28, 28, 36));
        Color tab2Bg = currentMode == GameMode.TICTACTOE ? accent : (hoverTab2 ? new Color(40, 40, 52) : new Color(28, 28, 36));

        Render2D.drawRoundedRect(stack, tab1X, tabY, tabW, tabH, 6f, tab1Bg);
        Render2D.drawFont(stack, Fonts.BOLD.getFont(9.5f), "Змейка",
                tab1X + (tabW - Fonts.BOLD.getWidth("Змейка", 9.5f)) / 2f, tabY + 7f, Color.WHITE);

        Render2D.drawRoundedRect(stack, tab2X, tabY, tabW, tabH, 6f, tab2Bg);
        Render2D.drawFont(stack, Fonts.BOLD.getFont(9.5f), "Крестики Нолики",
                tab2X + (tabW - Fonts.BOLD.getWidth("Крестики Нолики", 9.5f)) / 2f, tabY + 7f, Color.WHITE);

        float closeBtnSize = 22f;
        float closeBtnX = panelX + panelW - closeBtnSize - 12f;
        float closeBtnY = panelY + 12f;
        boolean hoverClose = mouseX >= closeBtnX && mouseX <= closeBtnX + closeBtnSize && mouseY >= closeBtnY && mouseY <= closeBtnY + closeBtnSize;
        Render2D.drawRoundedRect(stack, closeBtnX, closeBtnY, closeBtnSize, closeBtnSize, 5f,
                hoverClose ? new Color(180, 40, 50) : new Color(34, 34, 44));
        float midX = closeBtnX + closeBtnSize / 2f;
        float midY = closeBtnY + closeBtnSize / 2f;
        float cd = 4.5f;
        Render2D.drawLine(stack, midX - cd, midY - cd, midX + cd, midY + cd, 1.8f, Color.WHITE);
        Render2D.drawLine(stack, midX + cd, midY - cd, midX - cd, midY + cd, 1.8f, Color.WHITE);

        if (currentMode == GameMode.SNAKE) {
            renderSnakeGame(stack, panelX, panelY + 48f, panelW, panelH - 52f, mouseX, mouseY);
        } else {
            renderTicTacToeGame(stack, panelX, panelY + 48f, panelW, panelH - 52f, mouseX, mouseY);
        }
    }

    private void renderSnakeGame(MatrixStack stack, float px, float py, float pw, float ph, int mx, int my) {
        updateSnake();

        float boardSize = 260f;
        float bx = px + (pw - boardSize) / 2f;
        float by = py + 16f;

        String scoreStr = "Счёт: " + snakeScore + "  |  Рекорд: " + snakeHighScore;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(10.5f), scoreStr,
                bx + (boardSize - Fonts.BOLD.getWidth(scoreStr, 10.5f)) / 2f, py - 4f, Color.WHITE);

        Render2D.drawRoundedRect(stack, bx - 3f, by - 3f, boardSize + 6f, boardSize + 6f, 8f, new Color(24, 24, 32));
        Render2D.drawRoundedRect(stack, bx, by, boardSize, boardSize, 6f, new Color(14, 14, 20));

        float cellSize = boardSize / GRID_SIZE;

        float fx = bx + foodX * cellSize + 1.5f;
        float fy = by + foodY * cellSize + 1.5f;
        Render2D.drawRoundedRect(stack, fx, fy, cellSize - 3f, cellSize - 3f, 3f, new Color(255, 60, 80));

        for (int i = 0; i < snake.size(); i++) {
            int[] part = snake.get(i);
            float sx = bx + part[0] * cellSize + 1f;
            float sy = by + part[1] * cellSize + 1f;
            Color bodyCol = (i == 0) ? new Color(130, 240, 100) : new Color(70, 190, 80);
            Render2D.drawRoundedRect(stack, sx, sy, cellSize - 2f, cellSize - 2f, 2.5f, bodyCol);
        }

        if (snakeGameOver) {
            Render2D.drawRoundedRect(stack, bx, by, boardSize, boardSize, 6f, new Color(0, 0, 0, 190));
            String over = "ИГРА ОКОНЧЕНА!";
            Render2D.drawFont(stack, Fonts.BOLD.getFont(15f), over,
                    bx + (boardSize - Fonts.BOLD.getWidth(over, 15f)) / 2f, by + 90f, new Color(255, 70, 70));

            String finalScore = "Ваш результат: " + snakeScore;
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(10f), finalScore,
                    bx + (boardSize - Fonts.MEDIUM.getWidth(finalScore, 10f)) / 2f, by + 120f, Color.WHITE);

            float rBtnW = 120f;
            float rBtnH = 26f;
            float rBtnX = bx + (boardSize - rBtnW) / 2f;
            float rBtnY = by + 155f;
            boolean hov = mx >= rBtnX && mx <= rBtnX + rBtnW && my >= rBtnY && my <= rBtnY + rBtnH;
            Render2D.drawRoundedRect(stack, rBtnX, rBtnY, rBtnW, rBtnH, 6f, hov ? new Color(70, 180, 90) : new Color(50, 140, 70));
            Render2D.drawFont(stack, Fonts.BOLD.getFont(9.5f), "Играть снова",
                    rBtnX + (rBtnW - Fonts.BOLD.getWidth("Играть снова", 9.5f)) / 2f, rBtnY + 7f, Color.WHITE);
        } else {
            String hint = "Управление: Стрелочки или WASD";
            Render2D.drawFont(stack, Fonts.REGULAR.getFont(8f), hint,
                    bx + (boardSize - Fonts.REGULAR.getWidth(hint, 8f)) / 2f, by + boardSize + 8f, new Color(160, 160, 180));
        }
    }

    private void renderTicTacToeGame(MatrixStack stack, float px, float py, float pw, float ph, int mx, int my) {
        if (!playerTurn && !tttGameOver) {
            if (botMoveTime == 0L) {
                botMoveTime = System.currentTimeMillis() + 250L;
            } else if (System.currentTimeMillis() >= botMoveTime) {
                botMoveTime = 0L;
                makeBotMove();
            }
        }

        float boardSize = 210f;
        float bx = px + 40f;
        float by = py + (ph - boardSize) / 2f;

        Render2D.drawRoundedRect(stack, bx - 4f, by - 4f, boardSize + 8f, boardSize + 8f, 8f, new Color(24, 24, 32));
        Render2D.drawRoundedRect(stack, bx, by, boardSize, boardSize, 6f, new Color(14, 14, 20));

        float cellSize = boardSize / 3f;

        Render2D.drawRect(stack, bx + cellSize - 1f, by + 8f, 2f, boardSize - 16f, new Color(50, 50, 68));
        Render2D.drawRect(stack, bx + cellSize * 2f - 1f, by + 8f, 2f, boardSize - 16f, new Color(50, 50, 68));
        Render2D.drawRect(stack, bx + 8f, by + cellSize - 1f, boardSize - 16f, 2f, new Color(50, 50, 68));
        Render2D.drawRect(stack, bx + 8f, by + cellSize * 2f - 1f, boardSize - 16f, 2f, new Color(50, 50, 68));

        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            float cx = bx + col * cellSize;
            float cy = by + row * cellSize;

            int val = board[i];
            if (val == 1) {
                float pad = cellSize * 0.24f;
                float x1 = cx + pad;
                float y1 = cy + pad;
                float x2 = cx + cellSize - pad;
                float y2 = cy + cellSize - pad;
                Color xCol = new Color(255, 80, 95);
                Render2D.drawLine(stack, x1, y1, x2, y2, 4.5f, xCol);
                Render2D.drawLine(stack, x2, y1, x1, y2, 4.5f, xCol);
            } else if (val == 2) {
                float pad = cellSize * 0.22f;
                float ow = cellSize - pad * 2f;
                Color oCol = new Color(70, 170, 255);
                Render2D.drawBorder(stack, cx + pad, cy + pad, ow, ow, ow / 2f, 4f, 1f, oCol);
            } else if (!tttGameOver && playerTurn) {
                boolean cellHov = mx >= cx && mx <= cx + cellSize && my >= cy && my <= cy + cellSize;
                if (cellHov) {
                    Render2D.drawRoundedRect(stack, cx + 4f, cy + 4f, cellSize - 8f, cellSize - 8f, 4f, new Color(255, 255, 255, 15));
                }
            }
        }

        float rx = px + boardSize + 70f;
        float ry = py + 20f;

        String turnStatus;
        Color statusCol;
        if (tttGameOver) {
            if (winner == 1) {
                turnStatus = "ВЫ ПОБЕДИЛИ!";
                statusCol = new Color(80, 240, 110);
            } else if (winner == 2) {
                turnStatus = "ПОБЕДИЛ БОТ!";
                statusCol = new Color(255, 75, 75);
            } else {
                turnStatus = "НИЧЬЯ!";
                statusCol = new Color(230, 210, 80);
            }
        } else {
            if (playerTurn) {
                turnStatus = "Ваш ход (X)";
                statusCol = new Color(255, 120, 130);
            } else {
                turnStatus = "Ход бота (O)...";
                statusCol = new Color(110, 180, 255);
            }
        }
        Render2D.drawFont(stack, Fonts.BOLD.getFont(13f), turnStatus, rx, ry, statusCol);

        float diffY = ry + 30f;
        Render2D.drawFont(stack, Fonts.BOLD.getFont(9.5f), "Сложность:", rx, diffY, Color.WHITE);

        Difficulty[] diffs = Difficulty.values();
        float btnW = 90f;
        float btnH = 22f;
        for (int i = 0; i < diffs.length; i++) {
            Difficulty d = diffs[i];
            float dy = diffY + 16f + i * (btnH + 6f);
            boolean isCur = (d == difficulty);
            boolean hov = mx >= rx && mx <= rx + btnW && my >= dy && my <= dy + btnH;

            Color bBg = isCur ? new Color(140, 50, 210) : (hov ? new Color(48, 48, 62) : new Color(30, 30, 40));
            Render2D.drawRoundedRect(stack, rx, dy, btnW, btnH, 5f, bBg);
            Render2D.drawFont(stack, Fonts.MEDIUM.getFont(9f), d.title,
                    rx + (btnW - Fonts.MEDIUM.getWidth(d.title, 9f)) / 2f, dy + 5.5f, Color.WHITE);
        }

        float statsY = diffY + 16f + diffs.length * (btnH + 6f) + 8f;
        String stats = "П: " + playerWins + "  Н: " + draws + "  Пр: " + botWins;
        Render2D.drawFont(stack, Fonts.REGULAR.getFont(8.5f), stats, rx, statsY, new Color(180, 180, 200));

        float resetBtnY = statsY + 18f;
        float resetW = 100f;
        float resetH = 24f;
        boolean hovReset = mx >= rx && mx <= rx + resetW && my >= resetBtnY && my <= resetBtnY + resetH;
        Render2D.drawRoundedRect(stack, rx, resetBtnY, resetW, resetH, 5f, hovReset ? new Color(65, 65, 85) : new Color(42, 42, 56));
        Render2D.drawFont(stack, Fonts.BOLD.getFont(9f), "Новая игра",
                rx + (resetW - Fonts.BOLD.getWidth("Новая игра", 9f)) / 2f, resetBtnY + 6f, Color.WHITE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int W = this.width;
        int H = this.height;
        float panelW = 540f;
        float panelH = 360f;
        float panelX = (W - panelW) / 2f;
        float panelY = (H - panelH) / 2f;

        float tabW = 140f;
        float tabH = 26f;
        float tabY = panelY + 14f;
        float tab1X = panelX + (panelW / 2f) - tabW - 8f;
        float tab2X = panelX + (panelW / 2f) + 8f;

        if (mx >= tab1X && mx <= tab1X + tabW && my >= tabY && my <= tabY + tabH) {
            currentMode = GameMode.SNAKE;
            return true;
        }
        if (mx >= tab2X && mx <= tab2X + tabW && my >= tabY && my <= tabY + tabH) {
            currentMode = GameMode.TICTACTOE;
            return true;
        }

        float closeBtnSize = 22f;
        float closeBtnX = panelX + panelW - closeBtnSize - 12f;
        float closeBtnY = panelY + 12f;
        if (mx >= closeBtnX && mx <= closeBtnX + closeBtnSize && my >= closeBtnY && my <= closeBtnY + closeBtnSize) {
            this.close();
            return true;
        }

        if (currentMode == GameMode.SNAKE) {
            float boardSize = 260f;
            float bx = panelX + (panelW - boardSize) / 2f;
            float by = panelY + 48f + 16f;

            if (snakeGameOver) {
                float rBtnW = 120f;
                float rBtnH = 26f;
                float rBtnX = bx + (boardSize - rBtnW) / 2f;
                float rBtnY = by + 155f;
                if (mx >= rBtnX && mx <= rBtnX + rBtnW && my >= rBtnY && my <= rBtnY + rBtnH) {
                    initSnake();
                    return true;
                }
            }
        } else if (currentMode == GameMode.TICTACTOE) {
            float boardSize = 210f;
            float bx = panelX + 40f;
            float by = panelY + 48f + (panelH - 52f - boardSize) / 2f;
            float cellSize = boardSize / 3f;

            if (playerTurn && !tttGameOver) {
                if (mx >= bx && mx <= bx + boardSize && my >= by && my <= by + boardSize) {
                    int col = (int) ((mx - bx) / cellSize);
                    int row = (int) ((my - by) / cellSize);
                    int idx = row * 3 + col;
                    if (idx >= 0 && idx < 9 && board[idx] == 0) {
                        board[idx] = 1;
                        playerTurn = false;
                        checkTicTacToeEnd();
                        return true;
                    }
                }
            }

            float rx = panelX + boardSize + 70f;
            float diffY = panelY + 48f + 20f + 30f;
            Difficulty[] diffs = Difficulty.values();
            float bW = 90f;
            float bH = 22f;
            for (int i = 0; i < diffs.length; i++) {
                float dy = diffY + 16f + i * (bH + 6f);
                if (mx >= rx && mx <= rx + bW && my >= dy && my <= dy + bH) {
                    difficulty = diffs[i];
                    resetTicTacToe();
                    return true;
                }
            }

            float statsY = diffY + 16f + diffs.length * (bH + 6f) + 8f;
            float resetBtnY = statsY + 18f;
            float resetW = 100f;
            float resetH = 24f;
            if (mx >= rx && mx <= rx + resetW && my >= resetBtnY && my <= resetBtnY + resetH) {
                resetTicTacToe();
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentMode == GameMode.SNAKE) {
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
                if (dirY != 1) { nextDirX = 0; nextDirY = -1; }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
                if (dirY != -1) { nextDirX = 0; nextDirY = 1; }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                if (dirX != 1) { nextDirX = -1; nextDirY = 0; }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                if (dirX != -1) { nextDirX = 1; nextDirY = 0; }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER) {
                if (snakeGameOver) {
                    initSnake();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

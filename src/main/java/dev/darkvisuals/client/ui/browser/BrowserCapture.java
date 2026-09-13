package dev.darkvisuals.client.ui.browser;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

/**
 * Захват окна реального браузера (Chrome / Edge / Brave / Firefox и т.п.)
 * через WinAPI и вывод кадра в {@link NativeImageBackedTexture}.
 *
 * Кадры снимаются вызовом PrintWindow(PW_RENDERFULLCONTENT) с фолбэком на
 * BitBlt, поэтому браузер может висеть в фоне — захват работает и тогда,
 * когда окно перекрыто окном игры.
 *
 * Ввод (клики, скролл, клавиши) пересылается в окно браузера через
 * PostMessage — Minecraft сохраняет фокус, а бинд модуля продолжает работать.
 *
 * Все структуры и объявления WinAPI объявлены локально, зависимости —
 * только от ядра JNA.
 */
public class BrowserCapture {

    /** Интервал захвата кадра (~20 fps). */
    private static final long CAPTURE_INTERVAL_MS = 50L;
    /** Интервал повторного поиска окна браузера. */
    private static final long SEARCH_INTERVAL_MS = 2000L;
    /** Ограничение размера снимка. */
    private static final int MAX_W = 2560, MAX_H = 1440;

    /** Классы окон известных браузеров (префиксы). */
    private static final String[] BROWSER_CLASSES = {
            "Chrome_WidgetWin_1", // Chrome, Edge, Brave, Opera, Vivaldi...
            "MozillaWindowClass", // Firefox
    };

    // ---- сообщения Windows ----
    private static final int WM_KEYDOWN = 0x0100;
    private static final int WM_KEYUP = 0x0101;
    private static final int WM_CHAR = 0x0102;
    private static final int WM_MOUSEMOVE = 0x0200;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP = 0x0205;
    private static final int WM_MBUTTONDOWN = 0x0207;
    private static final int WM_MBUTTONUP = 0x0208;
    private static final int WM_MOUSEWHEEL = 0x020A;
    private static final int WM_MOUSEHWHEEL = 0x020E;

    private static final long MK_LBUTTON = 0x0001;
    private static final long MK_RBUTTON = 0x0002;
    private static final long MK_MBUTTON = 0x0010;

    private static final int PW_CLIENTONLY = 0x01;
    private static final int PW_RENDERFULLCONTENT = 0x02;
    private static final int SRCCOPY = 0x00CC0020;
    private static final int SW_RESTORE = 9;
    private static final int DIB_RGB_COLORS = 0;
    private static final int BI_RGB = 0;
    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    private static final int PROCESS_QUERY_INFORMATION = 0x0400;

    // ------------------------------------------------------------------
    // Объявления WinAPI (только ядро JNA, никаких структур из jna-platform)
    // ------------------------------------------------------------------

    /** RECT — left/top/right/bottom. */
    public static class RECT extends Structure {
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("left", "top", "right", "bottom");
        }
    }

    /** POINT — x/y. */
    public static class POINT extends Structure {
        public int x, y;

        public POINT() {
        }

        public POINT(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("x", "y");
        }
    }

    /** BITMAPINFOHEADER (для 32bpp BI_RGB таблица цветов не нужна). */
    public static class BITMAPINFOHEADER extends Structure {
        public int biSize, biWidth, biHeight, biPlanes, biBitCount;
        public int biCompression, biSizeImage;
        public int biXPelsPerMeter, biYPelsPerMeter, biClrUsed, biClrImportant;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("biSize", "biWidth", "biHeight", "biPlanes", "biBitCount",
                    "biCompression", "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter",
                    "biClrUsed", "biClrImportant");
        }
    }

    // JNA ищет в колбэке метод с именем "callback" — другое имя может молча не вызываться
    private interface WndEnumProc extends StdCallLibrary.StdCallCallback {
        boolean callback(Pointer hWnd, Pointer data);
    }

    private interface User32Lib extends StdCallLibrary {
        User32Lib I = Native.load("user32", User32Lib.class);

        boolean EnumWindows(WndEnumProc proc, Pointer data);

        boolean IsWindow(Pointer hWnd);

        boolean IsWindowVisible(Pointer hWnd);

        boolean IsIconic(Pointer hWnd);

        int GetClassNameW(Pointer hWnd, char[] buffer, int max);

        int GetWindowTextW(Pointer hWnd, char[] buffer, int max);

        int GetWindowThreadProcessId(Pointer hWnd, int[] pid);

        boolean GetClientRect(Pointer hWnd, RECT rect);

        boolean ClientToScreen(Pointer hWnd, POINT point);

        Pointer GetDC(Pointer hWnd);

        int ReleaseDC(Pointer hWnd, Pointer hdc);

        boolean PrintWindow(Pointer hWnd, Pointer hdc, int flags);

        boolean ShowWindow(Pointer hWnd, int cmd);

        boolean PostMessageW(Pointer hWnd, int msg, long wParam, long lParam);

        Pointer FindWindowW(char[] className, char[] title);
    }

    private interface Kernel32Lib extends StdCallLibrary {
        Kernel32Lib I = Native.load("kernel32", Kernel32Lib.class);

        Pointer OpenProcess(int access, boolean inherit, int pid);

        boolean QueryFullProcessImageNameW(Pointer process, int flags, char[] name, int[] size);

        boolean CloseHandle(Pointer handle);
    }

    private interface Gdi32Lib extends StdCallLibrary {
        Gdi32Lib I = Native.load("gdi32", Gdi32Lib.class);

        Pointer CreateCompatibleDC(Pointer hdc);

        Pointer CreateCompatibleBitmap(Pointer hdc, int width, int height);

        Pointer SelectObject(Pointer hdc, Pointer object);

        boolean DeleteObject(Pointer object);

        boolean DeleteDC(Pointer hdc);

        boolean BitBlt(Pointer dest, int x, int y, int w, int h, Pointer src, int sx, int sy, int rop);

        int GetDIBits(Pointer hdc, Pointer bitmap, int start, int lines, int[] bits,
                      BITMAPINFOHEADER info, int usage);
    }

    // ------------------------------------------------------------------
    // Состояние
    // ------------------------------------------------------------------

    /** Найденное окно браузера. */
    private Pointer hwnd;
    /** Заголовок окна браузера. */
    private String windowTitle = "";
    /** Текст статуса поиска (показывается на плоскости). */
    private String searchStatus = "";
    /** Текстура с последним кадром. */
    private NativeImageBackedTexture texture;
    private int texW, texH;

    private long lastCapture;
    private long lastSearch;
    /** Кэш пикселей, чтобы не аллоцировать массив каждый кадр. */
    private int[] pixels;

    // диагностика последнего поиска — показывается в статусе на плоскости
    private int statVisible;
    private int statNoExe;
    private int statMatched;
    private boolean enumFailed;

    public boolean isAvailable() {
        return texture != null && hwnd != null && User32Lib.I.IsWindow(hwnd);
    }

    public NativeImageBackedTexture getTexture() {
        return texture;
    }

    public int getWidth() {
        return texW;
    }

    public int getHeight() {
        return texH;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    /** Статус поиска окна — для вывода на плоскость. */
    public String getSearchStatus() {
        return searchStatus;
    }

    /**
     * Актуализация кадра (вызывается из tick() модуля): ищет окно,
     * если его нет, и снимает кадр не чаще, чем раз в CAPTURE_INTERVAL_MS.
     *
     * @param exeNames имена процессов искомого браузера (например chrome.exe);
     *                 несколько имён — режим "Авто".
     */
    public void tick(Set<String> exeNames) {
        long now = System.currentTimeMillis();

        if (hwnd == null || !User32Lib.I.IsWindow(hwnd)) {
            if (now - lastSearch < SEARCH_INTERVAL_MS) return;
            lastSearch = now;
            searchBrowserWindow(exeNames);
            if (hwnd == null) return;
        }

        if (now - lastCapture < CAPTURE_INTERVAL_MS) return;
        lastCapture = now;
        try {
            captureNow();
        } catch (Throwable t) {
            // сбой GDI не должен ронять игру; покажем причину и поищем окно заново
            searchStatus = "Сбой захвата окна: " + t;
            hwnd = null;
        }
    }

    /**
     * Поиск окна браузера с диагностикой: заполняет hwnd/windowTitle
     * и searchStatus с причиной, если окно не нашлось.
     */
    private void searchBrowserWindow(Set<String> exeNames) {
        Pointer found = findBrowserWindow(exeNames);
        hwnd = found;
        windowTitle = found == null ? "" : windowText(found);
        if (found != null) return;

        String exeList = String.join(", ", exeNames);
        StringBuilder sb = new StringBuilder("Браузер не найден (искали: ")
                .append(exeList).append(')');
        if (enumFailed) {
            sb.append(" — перебор окон недоступен");
        } else if (statNoExe > 0 && statVisible > 0 && statMatched == 0) {
            sb.append(" — процесс не определён у ")
                    .append(statNoExe).append(" из ").append(statVisible).append(" окон");
        } else {
            sb.append(" — видимых окон просмотрено: ").append(statVisible);
        }
        searchStatus = sb.toString();
    }

    /**
     * Немедленный повторный захват (например, после клика).
     */
    public void requestRefresh() {
        lastCapture = 0L;
    }

    /**
     * Сброс привязки к окну (вызывается при выключении модуля).
     * Текстура сохраняется и переиспользуется при следующем включении.
     */
    public void reset() {
        hwnd = null;
        windowTitle = "";
        lastCapture = 0L;
        lastSearch = 0L;
        mouseButtons = 0L;
    }

    // ------------------------------------------------------------------
    // Поиск окна
    // ------------------------------------------------------------------

    /**
     * Ищет видимое окно, чей процесс совпадает с одним из exeNames.
     * При равенстве предпочтение — окну с наибольшей площадью.
     * Заодно заполняет счётчики диагностики для статуса.
     */
    private Pointer findBrowserWindow(Set<String> exeNames) {
        Pointer[] best = {null};
        long[] bestArea = {0};
        statVisible = 0;
        statNoExe = 0;
        statMatched = 0;
        enumFailed = false;

        try {
            User32Lib.I.EnumWindows((hWnd, data) -> {
                try {
                    if (!User32Lib.I.IsWindowVisible(hWnd)) return true;
                    statVisible++;

                    String exe = processExeName(hWnd);
                    if (exe == null) {
                        statNoExe++;
                        return true;
                    }
                    if (!exeNames.contains(exe)) return true;
                    statMatched++;

                    String title = windowText(hWnd);
                    if (title.isBlank()) return true; // фоновые/служебные окна без заголовка

                    RECT rc = new RECT();
                    if (!User32Lib.I.GetClientRect(hWnd, rc)) return true;
                    long area = (long) rc.right * rc.bottom;
                    if (area > bestArea[0]) {
                        bestArea[0] = area;
                        best[0] = hWnd;
                    }
                } catch (Throwable ignored) {
                    // одно плохое окно не должно останавливать перебор
                }
                return true;
            }, null);
        } catch (Throwable t) {
            enumFailed = true;
            // EnumWindows недоступен — пробуем прямое имя класса
        }

        if (best[0] == null) best[0] = findByClassFallback();

        return best[0];
    }

    /**
     * Резервный поиск по классу окна, если перебор по процессу ничего не нашёл.
     */
    private static Pointer findByClassFallback() {
        for (String cls : BROWSER_CLASSES) {
            try {
                char[] clsBuf = new char[cls.length() + 1];
                System.arraycopy(cls.toCharArray(), 0, clsBuf, 0, cls.length());
                Pointer hWnd = User32Lib.I.FindWindowW(clsBuf, null);
                if (hWnd != null && User32Lib.I.IsWindowVisible(hWnd)) return hWnd;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Имя исполняемого файла процесса, которому принадлежит окно
     * (например "chrome.exe"), в нижнем регистре; null — не удалось узнать.
     */
    private static String processExeName(Pointer hWnd) {
        int[] pid = new int[1];
        User32Lib.I.GetWindowThreadProcessId(hWnd, pid);
        if (pid[0] == 0) return null;

        Pointer process = Kernel32Lib.I.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid[0]);
        if (process == null) {
            // иногда LIMITED отклоняется — пробуем обычный QUERY
            process = Kernel32Lib.I.OpenProcess(PROCESS_QUERY_INFORMATION, false, pid[0]);
        }
        if (process == null) return null;

        try {
            char[] buf = new char[1024];
            int[] size = new int[]{buf.length};
            if (!Kernel32Lib.I.QueryFullProcessImageNameW(process, 0, buf, size)) return null;
            String path = new String(buf, 0, Math.max(0, size[0]));
            int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            return name.toLowerCase();
        } finally {
            Kernel32Lib.I.CloseHandle(process);
        }
    }

    private static String windowText(Pointer hWnd) {
        char[] buf = new char[512];
        int len = User32Lib.I.GetWindowTextW(hWnd, buf, buf.length);
        return new String(buf, 0, Math.max(0, len));
    }

    // ------------------------------------------------------------------
    // Захват кадра
    // ------------------------------------------------------------------

    private void captureNow() {
        if (hwnd == null) return;
        if (User32Lib.I.IsIconic(hwnd)) {
            // свёрнутое окно — восстановить, иначе снимать нечего
            User32Lib.I.ShowWindow(hwnd, SW_RESTORE);
            return;
        }

        RECT rc = new RECT();
        if (!User32Lib.I.GetClientRect(hwnd, rc)) return;

        int w = Math.min(rc.right, MAX_W);
        int h = Math.min(rc.bottom, MAX_H);
        if (w < 16 || h < 16) return;

        Pointer hdcWindow = User32Lib.I.GetDC(hwnd);
        if (hdcWindow == null) return;

        try {
            Pointer hdcMem = Gdi32Lib.I.CreateCompatibleDC(hdcWindow);
            Pointer bitmap = Gdi32Lib.I.CreateCompatibleBitmap(hdcWindow, w, h);
            Gdi32Lib.I.SelectObject(hdcMem, bitmap);

            boolean ok = User32Lib.I.PrintWindow(hwnd, hdcMem, PW_CLIENTONLY | PW_RENDERFULLCONTENT);
            if (!ok) {
                Gdi32Lib.I.BitBlt(hdcMem, 0, 0, w, h, hdcWindow, 0, 0, SRCCOPY);
            }

            // перед GetDIBits битмап не должен быть выбран в DC
            Gdi32Lib.I.SelectObject(hdcMem, null);

            if (pixels == null || pixels.length != w * h) pixels = new int[w * h];

            BITMAPINFOHEADER info = new BITMAPINFOHEADER();
            info.biSize = info.size();
            info.biWidth = w;
            info.biHeight = -h; // top-down
            info.biPlanes = 1;
            info.biBitCount = 32;
            info.biCompression = BI_RGB;
            info.biSizeImage = w * h * 4;

            int copied = Gdi32Lib.I.GetDIBits(hdcMem, bitmap, 0, h, pixels, info, DIB_RGB_COLORS);
            if (copied > 0) {
                uploadTexture(w, h);
            }

            Gdi32Lib.I.DeleteObject(bitmap);
            Gdi32Lib.I.DeleteDC(hdcMem);
        } finally {
            User32Lib.I.ReleaseDC(hwnd, hdcWindow);
        }
    }

    /**
     * Заливка пикселей в текстуру (должно вызываться на рендер-потоке).
     * При изменении размера окна браузера изображение пересоздаётся
     * через {@link NativeImageBackedTexture#setImage(NativeImage)}.
     */
    private void uploadTexture(int w, int h) {
        if (texture == null || texW != w || texH != h) {
            NativeImage image = new NativeImage(w, h, true);
            if (texture == null) {
                texture = new NativeImageBackedTexture(image);
            } else {
                texture.setImage(image);
            }
            texture.setFilter(true, false); // билинейная фильтрация
            texture.setClamp(true);
            texW = w;
            texH = h;
        }

        NativeImage image = texture.getImage();
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                // DIB отдаёт BGRA -> в int это ARGB; альфу принудительно в 255
                image.setColorArgb(x, y, 0xFF000000 | (pixels[row + x] & 0xFFFFFF));
            }
        }
        texture.upload();
    }

    // ------------------------------------------------------------------
    // Пересылка ввода
    // ------------------------------------------------------------------

    /** Текущие зажатые кнопки (MK_*) — для корректного drag в WM_MOUSEMOVE. */
    private long mouseButtons;

    public void sendMouseMove(int bx, int by) {
        post(WM_MOUSEMOVE, mouseButtons, lParam(bx, by));
    }

    public void sendMouseDown(int bx, int by, int button) {
        switch (button) {
            case 0 -> {
                mouseButtons |= MK_LBUTTON;
                post(WM_LBUTTONDOWN, MK_LBUTTON, lParam(bx, by));
            }
            case 1 -> {
                mouseButtons |= MK_RBUTTON;
                post(WM_RBUTTONDOWN, MK_RBUTTON, lParam(bx, by));
            }
            case 2 -> {
                mouseButtons |= MK_MBUTTON;
                post(WM_MBUTTONDOWN, MK_MBUTTON, lParam(bx, by));
            }
            default -> {
            }
        }
        requestRefresh();
    }

    public void sendMouseUp(int bx, int by, int button) {
        switch (button) {
            case 0 -> {
                mouseButtons &= ~MK_LBUTTON;
                post(WM_LBUTTONUP, 0, lParam(bx, by));
            }
            case 1 -> {
                mouseButtons &= ~MK_RBUTTON;
                post(WM_RBUTTONUP, 0, lParam(bx, by));
            }
            case 2 -> {
                mouseButtons &= ~MK_MBUTTON;
                post(WM_MBUTTONUP, 0, lParam(bx, by));
            }
            default -> {
            }
        }
        requestRefresh();
    }

    /**
     * Скролл колесом. lParam WM_MOUSEWHEEL требует экранные координаты.
     */
    public void sendWheel(int bx, int by, double vertical, double horizontal) {
        if (hwnd == null) return;

        if (vertical != 0) {
            long delta = Math.round(vertical * 120.0);
            postScreen(WM_MOUSEWHEEL, delta << 16, bx, by);
        }
        if (horizontal != 0) {
            long delta = Math.round(horizontal * 120.0);
            postScreen(WM_MOUSEHWHEEL, delta << 16, bx, by);
        }
        requestRefresh();
    }

    public void sendKey(int vk, boolean down) {
        if (hwnd == null) return;
        User32Lib.I.PostMessageW(hwnd, down ? WM_KEYDOWN : WM_KEYUP,
                vk & 0xFFFFL, down ? 1L : 0L);
        requestRefresh();
    }

    public void sendChar(char c) {
        if (hwnd == null || c < 32) return;
        User32Lib.I.PostMessageW(hwnd, WM_CHAR, c, 1L);
        requestRefresh();
    }

    private void post(int msg, long wParam, long lParam) {
        if (hwnd == null) return;
        User32Lib.I.PostMessageW(hwnd, msg, wParam, lParam);
    }

    /**
     * WM_MOUSEWHEEL/HWHEEL: координаты в lParam — экранные, не клиентские.
     */
    private void postScreen(int msg, long wParam, int bx, int by) {
        POINT pt = new POINT(bx, by);
        if (!User32Lib.I.ClientToScreen(hwnd, pt)) return;
        User32Lib.I.PostMessageW(hwnd, msg, wParam, lParam(pt.x, pt.y));
    }

    private static long lParam(int x, int y) {
        return ((long) (y & 0xFFFF) << 16) | (x & 0xFFFFL);
    }

    /**
     * Транслирует GLFW-код клавиши в Windows Virtual-Key.
     * Возвращает -1, если аналога нет.
     */
    public static int glfwToVirtualKey(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return 'A' + (keyCode - GLFW.GLFW_KEY_A);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return '0' + (keyCode - GLFW.GLFW_KEY_0);
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
            return 0x70 + (keyCode - GLFW.GLFW_KEY_F1);
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> 0x20;
            case GLFW.GLFW_KEY_APOSTROPHE -> 0xDE;
            case GLFW.GLFW_KEY_COMMA -> 0xBC;
            case GLFW.GLFW_KEY_MINUS -> 0xBD;
            case GLFW.GLFW_KEY_PERIOD -> 0xBE;
            case GLFW.GLFW_KEY_SLASH -> 0xBF;
            case GLFW.GLFW_KEY_SEMICOLON -> 0xBA;
            case GLFW.GLFW_KEY_EQUAL -> 0xBB;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 0xDB;
            case GLFW.GLFW_KEY_BACKSLASH -> 0xDC;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 0xDD;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> 0xC0;
            case GLFW.GLFW_KEY_ESCAPE -> 0x1B;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 0x0D;
            case GLFW.GLFW_KEY_TAB -> 0x09;
            case GLFW.GLFW_KEY_BACKSPACE -> 0x08;
            case GLFW.GLFW_KEY_INSERT -> 0x2D;
            case GLFW.GLFW_KEY_DELETE -> 0x2E;
            case GLFW.GLFW_KEY_RIGHT -> 0x27;
            case GLFW.GLFW_KEY_LEFT -> 0x25;
            case GLFW.GLFW_KEY_DOWN -> 0x28;
            case GLFW.GLFW_KEY_UP -> 0x26;
            case GLFW.GLFW_KEY_PAGE_UP -> 0x21;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 0x22;
            case GLFW.GLFW_KEY_HOME -> 0x24;
            case GLFW.GLFW_KEY_END -> 0x23;
            case GLFW.GLFW_KEY_CAPS_LOCK -> 0x14;
            case GLFW.GLFW_KEY_SCROLL_LOCK -> 0x91;
            case GLFW.GLFW_KEY_NUM_LOCK -> 0x90;
            case GLFW.GLFW_KEY_PRINT_SCREEN -> 0x2C;
            case GLFW.GLFW_KEY_PAUSE -> 0x13;
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> 0x10;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> 0x11;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> 0x12;
            case GLFW.GLFW_KEY_KP_ADD -> 0x6B;
            case GLFW.GLFW_KEY_KP_SUBTRACT -> 0x6D;
            case GLFW.GLFW_KEY_KP_MULTIPLY -> 0x6A;
            case GLFW.GLFW_KEY_KP_DIVIDE -> 0x6F;
            case GLFW.GLFW_KEY_KP_DECIMAL -> 0x6E;
            default -> -1;
        };
    }
}

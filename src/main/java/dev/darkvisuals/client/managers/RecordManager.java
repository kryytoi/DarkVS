package dev.darkvisuals.client.managers;

import dev.darkvisuals.client.util.Wrapper;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

 
public class RecordManager implements Wrapper {

    public enum State { IDLE, RECORDING, PAUSED }

     
    private static final String FFMPEG_PATH = "ffmpeg";
    private static final int TARGET_FPS = 30;

    private State state = State.IDLE;
    private Process ffmpegProcess;
    private OutputStream ffmpegStdin;
    private ExecutorService writerThread;
    private BlockingQueue<byte[]> frameQueue;

    private int width, height;
    private long lastFrameTimeNs = 0;
    private long recordStartMs = 0;
    private long pausedAccumMs = 0;
    private long pauseStartedMs = 0;

    public State getState() { return state; }
    public boolean isRecording() { return state == State.RECORDING; }
    public boolean isPaused() { return state == State.PAUSED; }
    public boolean isActive() { return state != State.IDLE; }

      
    public long getElapsedMs() {
        if (state == State.IDLE) return 0;
        long pausedNow = (state == State.PAUSED) ? (System.currentTimeMillis() - pauseStartedMs) : 0;
        return System.currentTimeMillis() - recordStartMs - pausedAccumMs - pausedNow;
    }

      
    public boolean start() {
        if (state != State.IDLE) return false;
        if (mc.getWindow() == null) return false;

        width = mc.getWindow().getFramebufferWidth();
        height = mc.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return false;

        try {
            Path dir = mc.runDirectory.toPath().resolve("darkvisuals").resolve("recordings");
            Files.createDirectories(dir);
            String fileName = "record_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp4";
            Path output = dir.resolve(fileName);

            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG_PATH, "-y",
                    "-f", "rawvideo",
                    "-pixel_format", "rgba",
                    "-video_size", width + "x" + height,
                    "-framerate", String.valueOf(TARGET_FPS),
                    "-i", "-",
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-pix_fmt", "yuv420p",
                    output.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

            ffmpegProcess = pb.start();
            ffmpegStdin = ffmpegProcess.getOutputStream();

            frameQueue = new LinkedBlockingQueue<>(8);
            writerThread = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "record-ffmpeg-writer");
                t.setDaemon(true);
                return t;
            });
            writerThread.submit(this::writerLoop);

            recordStartMs = System.currentTimeMillis();
            pausedAccumMs = 0;
            lastFrameTimeNs = 0;
            state = State.RECORDING;
            return true;
        } catch (IOException e) {
            state = State.IDLE;
            return false;  
        }
    }

      
    public void pause() {
        if (state != State.RECORDING) return;
        state = State.PAUSED;
        pauseStartedMs = System.currentTimeMillis();
    }

      
    public void resume() {
        if (state != State.PAUSED) return;
        pausedAccumMs += System.currentTimeMillis() - pauseStartedMs;
        state = State.RECORDING;
    }

      
    public void stop() {
        if (state == State.IDLE) return;
        state = State.IDLE;

        try {
            if (frameQueue != null) frameQueue.put(POISON_PILL);
        } catch (InterruptedException ignored) {}

        if (writerThread != null) {
            writerThread.shutdown();
            try { writerThread.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        try {
            if (ffmpegStdin != null) ffmpegStdin.close();
        } catch (IOException ignored) {}
        if (ffmpegProcess != null) {
            try { ffmpegProcess.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        ffmpegProcess = null;
        ffmpegStdin = null;
        frameQueue = null;
        writerThread = null;
    }

    private static final byte[] POISON_PILL = new byte[0];

    private void writerLoop() {
        try {
            while (true) {
                byte[] frame = frameQueue.take();
                if (frame == POISON_PILL) break;
                ffmpegStdin.write(frame);
            }
            ffmpegStdin.flush();
        } catch (IOException | InterruptedException ignored) {
             
        }
    }

 
    public void captureFrame() {
        if (state != State.RECORDING) return;
        if (frameQueue == null) return;

        long now = System.nanoTime();
        long frameIntervalNs = 1_000_000_000L / TARGET_FPS;
        if (lastFrameTimeNs != 0 && now - lastFrameTimeNs < frameIntervalNs) return;
        lastFrameTimeNs = now;

         
        int curW = mc.getWindow().getFramebufferWidth();
        int curH = mc.getWindow().getFramebufferHeight();
        if (curW != width || curH != height) {
            stop();
            return;
        }

        Framebuffer fb = mc.getFramebuffer();
        int prevReadFb = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.fbo);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFb);

         
        byte[] flipped = new byte[width * height * 4];
        int rowBytes = width * 4;
        byte[] row = new byte[rowBytes];
        for (int y = 0; y < height; y++) {
            buffer.position(y * rowBytes);
            buffer.get(row, 0, rowBytes);
            System.arraycopy(row, 0, flipped, (height - 1 - y) * rowBytes, rowBytes);
        }

         
        frameQueue.offer(flipped);
    }
}
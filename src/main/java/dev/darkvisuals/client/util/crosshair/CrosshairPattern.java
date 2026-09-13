package dev.darkvisuals.client.util.crosshair;

import java.nio.ByteBuffer;
import java.util.Base64;

 
public class CrosshairPattern {

    public static final int SIZE = 15;  

    private final int[] pixels;  

    public CrosshairPattern() {
        this.pixels = new int[SIZE * SIZE];
    }

    private CrosshairPattern(int[] pixels) {
        this.pixels = pixels;
    }

    public int getSize() {
        return SIZE;
    }

    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return 0;
        return pixels[y * SIZE + x];
    }

    public void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return;
        pixels[y * SIZE + x] = argb;
    }

    public void clear() {
        java.util.Arrays.fill(pixels, 0);
    }

    public boolean isEmpty() {
        for (int p : pixels) if ((p >>> 24) != 0) return false;
        return true;
    }

    public CrosshairPattern copy() {
        return new CrosshairPattern(pixels.clone());
    }

     

      
    public String serialize() {
        ByteBuffer buf = ByteBuffer.allocate(pixels.length * 4);
        for (int p : pixels) buf.putInt(p);
        return "v1:" + Base64.getEncoder().encodeToString(buf.array());
    }

      
    public static CrosshairPattern deserialize(String data) {
        if (data == null || data.isEmpty() || !data.startsWith("v1:")) return null;
        try {
            byte[] raw = Base64.getDecoder().decode(data.substring(3));
            if (raw.length != SIZE * SIZE * 4) return null;
            int[] px = new int[SIZE * SIZE];
            ByteBuffer buf = ByteBuffer.wrap(raw);
            for (int i = 0; i < px.length; i++) px[i] = buf.getInt();
            return new CrosshairPattern(px);
        } catch (Exception e) {
            return null;
        }
    }

      
    public static CrosshairPattern defaultPattern() {
        CrosshairPattern p = new CrosshairPattern();
        int c = SIZE / 2;
        int white = 0xFFFFFFFF;
        for (int i = 2; i <= 5; i++) {
            p.set(c, c - i, white);
            p.set(c, c + i, white);
            p.set(c - i, c, white);
            p.set(c + i, c, white);
        }
        return p;
    }
}
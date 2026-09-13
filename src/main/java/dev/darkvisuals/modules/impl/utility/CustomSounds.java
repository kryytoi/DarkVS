package dev.darkvisuals.modules.impl.utility;

import dev.darkvisuals.client.events.impl.EventAttackEntity;
import dev.darkvisuals.client.events.impl.EventPacket;
import dev.darkvisuals.client.events.impl.EventTick;
import dev.darkvisuals.darkvisuals;
import dev.darkvisuals.modules.api.Category;
import dev.darkvisuals.modules.api.Module;
import dev.darkvisuals.modules.settings.impl.BooleanSetting;
import dev.darkvisuals.modules.settings.impl.ButtonSetting;
import dev.darkvisuals.modules.settings.impl.NumberSetting;
import dev.darkvisuals.modules.settings.impl.StringSetting;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomSounds extends Module {


    private static final ThreadPoolExecutor AUDIO_POOL = new ThreadPoolExecutor(
            1, 3, 5L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "DarkVisuals-CustomSound");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardPolicy());


    private static final long KILL_WINDOW_MS = 4_000L;

    private static final long REMOVAL_WINDOW_MS = 1_200L;

    private static final long HIT_COOLDOWN_MS = 60L;


    private final StringSetting killPath = new StringSetting("kill.path", "", () -> false, false);
    private final BooleanSetting killEnabled = new BooleanSetting("Звук убийства", true);
    private final NumberSetting killVolume = new NumberSetting("Громкость убийства", 1.00f, 0.1f, 2.0f, 0.01f);
    private final ButtonSetting killPick = new ButtonSetting("Выбрать mp3 убийства", () -> pickSound("kill", killPath));
    private final ButtonSetting killTest = new ButtonSetting("Тест убийства", () -> play(killPath.getValue(), killVolume.getValue()));


    private final StringSetting hitPath = new StringSetting("hit.path", "", () -> false, false);
    private final BooleanSetting hitEnabled = new BooleanSetting("Звук удара", true);
    private final NumberSetting hitVolume = new NumberSetting("Громкость удара", 1.00f, 0.1f, 2.0f, 0.01f);
    private final ButtonSetting hitPick = new ButtonSetting("Выбрать mp3 удара", () -> pickSound("hit", hitPath));
    private final ButtonSetting hitTest = new ButtonSetting("Тест удара", () -> play(hitPath.getValue(), hitVolume.getValue()));


    private final Map<Integer, Long> attacked = new ConcurrentHashMap<>();
    private volatile long lastHitSound = 0L;

    public CustomSounds() {
        super("CustomSounds", Category.Utility, "Свои mp3-звуки на убийство и удар по существам");
        getSettings().add(killEnabled);
        getSettings().add(killPick);
        getSettings().add(killTest);
        getSettings().add(killVolume);
        getSettings().add(hitEnabled);
        getSettings().add(hitPick);
        getSettings().add(hitTest);
        getSettings().add(hitVolume);
        getSettings().add(killPath);
        getSettings().add(hitPath);
    }

    @Override
    public void onDisable() {
        attacked.clear();
        super.onDisable();
    }



    @EventHandler
    private void onAttackEntity(EventAttackEntity e) {
        if (fullNullCheck()) return;
        if (e.getPlayer() != mc.player) return;
        Entity target = e.getTarget();
        if (target == mc.player) return;
        if (!(target instanceof LivingEntity living)) return;


        attacked.put(target.getId(), System.currentTimeMillis());


        if (!hitEnabled.getValue()) return;
        if (!e.isEffectsAllowed()) return;
        if (!living.isAlive()) return;

        long now = System.currentTimeMillis();
        if (now - lastHitSound < HIT_COOLDOWN_MS) return;
        lastHitSound = now;
        play(hitPath.getValue(), hitVolume.getValue());
    }

    @EventHandler
    private void onPacketReceive(EventPacket.Receive e) {
        if (fullNullCheck()) return;
        if (!killEnabled.getValue()) return;
        if (!(e.getPacket() instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 3) return;

        mc.execute(() -> {
            if (fullNullCheck()) return;
            Entity entity = packet.getEntity(mc.world);
            if (entity == null) return;
            Long time = attacked.get(entity.getId());
            if (time == null) return;
            if (System.currentTimeMillis() - time > KILL_WINDOW_MS) {
                attacked.remove(entity.getId());
                return;
            }
            triggerKill(entity.getId());
        });
    }

    @EventHandler
    private void onTick(EventTick e) {
        if (fullNullCheck()) return;
        if (attacked.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : attacked.entrySet()) {
            int id = entry.getKey();
            long time = entry.getValue();

            if (now - time > KILL_WINDOW_MS) {
                attacked.remove(id);
                continue;
            }

            Entity entity = mc.world.getEntityById(id);
            if (entity instanceof LivingEntity living) {
                if (living.isDead() || living.getHealth() <= 0.0f) {
                    triggerKill(id);
                }
            } else if (entity == null) {
                if (now - time <= REMOVAL_WINDOW_MS) {
                    triggerKill(id);
                } else {
                    attacked.remove(id);
                }
            }
        }
    }


    private void triggerKill(int id) {
        if (attacked.remove(id) == null) return;
        if (!killEnabled.getValue()) return;
        play(killPath.getValue(), killVolume.getValue());
    }



    private void pickSound(String slot, StringSetting target) {
        try {
            File dir = new File(new File(mc.runDirectory, "darkvisuals"), "custom_sounds");
            dir.mkdirs();

            String selected;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.mp3"));
                filters.flip();
                selected = TinyFileDialogs.tinyfd_openFileDialog(
                        "Выберите mp3",
                        dir.getAbsolutePath(),
                        filters,
                        "Аудио (*.mp3)",
                        false
                );
            }

            if (selected == null || selected.isBlank()) return;

            File src = new File(selected);
            if (!src.isFile()) return;

            File dst = new File(dir, slot + ".mp3");
            try {
                if (!src.getCanonicalFile().equals(dst.getCanonicalFile())) {
                    Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable copyError) {
                dst = src;
            }

            String stored;
            try {
                stored = mc.runDirectory.toPath().relativize(dst.toPath()).toString().replace(File.separatorChar, '/');
            } catch (Throwable relativizeError) {
                stored = dst.getAbsolutePath();
            }

            target.setValue(stored);
            darkvisuals.LOGGER.info("[CustomSounds] {} mp3: {}", slot, dst.getAbsolutePath());
        } catch (Throwable t) {
            darkvisuals.LOGGER.warn("[CustomSounds] Не удалось выбрать mp3: {}", t.toString());
        }
    }

    private File resolveFile(String stored) {
        if (stored == null || stored.isBlank()) return null;
        try {
            File direct = new File(stored);
            if (direct.isAbsolute()) return direct.isFile() ? direct : null;
            File fromRun = new File(mc.runDirectory, stored);
            return fromRun.isFile() ? fromRun : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private void play(String stored, float volume) {
        File file = resolveFile(stored);
        if (file == null) return;
        AUDIO_POOL.submit(() -> decodeAndPlay(file, volume));
    }



    private static void decodeAndPlay(File file, float volume) {
        Bitstream bitstream = null;
        SourceDataLine line = null;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            byte[] pcmBytes = null;
            Header header;

            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);

                if (line == null) {
                    AudioFormat format = new AudioFormat(
                            output.getSampleFrequency(), 16, output.getChannelCount(), true, false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    applyGain(line, volume);
                    line.start();
                }

                short[] samples = output.getBuffer();
                int count = output.getBufferLength();
                int needed = count * 2;
                if (pcmBytes == null || pcmBytes.length < needed) pcmBytes = new byte[needed];
                for (int i = 0; i < count; i++) {
                    short s = samples[i];
                    pcmBytes[i * 2] = (byte) (s & 0xFF);
                    pcmBytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
                }
                line.write(pcmBytes, 0, needed);
                bitstream.closeFrame();
            }

            if (line != null) line.drain();
        } catch (Throwable t) {
            darkvisuals.LOGGER.warn("[CustomSounds] Ошибка воспроизведения mp3: {}", t.toString());
        } finally {
            if (line != null) {
                try { line.stop(); } catch (Throwable ignored) {}
                try { line.close(); } catch (Throwable ignored) {}
            }
            if (bitstream != null) {
                try { bitstream.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static void applyGain(SourceDataLine line, float volume) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float v = Math.max(0.0001f, volume);
                float db = (float) (Math.log10(v) * 20.0);
                db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
                gain.setValue(db);
            }
        } catch (Throwable ignored) {}
    }
}

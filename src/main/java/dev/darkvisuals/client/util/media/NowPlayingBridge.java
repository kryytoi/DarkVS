package dev.darkvisuals.client.util.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.darkvisuals.client.util.renderer.Render2D;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.AbstractTexture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

 
public final class NowPlayingBridge {

    private NowPlayingBridge() {}

    public static final class Snapshot {
        public final String title;
        public final String artist;
        public final String app;
        public final boolean playing;
        public final String thumbPath;
        public final String iconPath;
          
        public final long positionSec;
          
        public final long durationSec;
        public final long updatedAtMs;

        Snapshot(String title, String artist, String app, boolean playing,
                 String thumbPath, String iconPath, long positionSec, long durationSec) {
            this.title = title;
            this.artist = artist;
            this.app = app;
            this.playing = playing;
            this.thumbPath = thumbPath;
            this.iconPath = iconPath;
            this.positionSec = positionSec;
            this.durationSec = durationSec;
            this.updatedAtMs = System.currentTimeMillis();
        }

        public boolean hasTrack() {
            return title != null && !title.isEmpty();
        }

 
        public long smoothedPositionSec() {
            if (positionSec < 0) return -1;
            long pos = positionSec;
            if (playing) {
                pos += (System.currentTimeMillis() - updatedAtMs) / 1000L;
            }
            if (durationSec > 0 && pos > durationSec) pos = durationSec;
            return pos;
        }
    }

     
     
    private static final String SCRIPT = String.join("\n",
            "$ErrorActionPreference = 'SilentlyContinue'",
            "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
            "Add-Type -AssemblyName System.Drawing",
            "",
            "$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {",
            "    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'",
            "})[0]",
            "function Await($WinRtTask, $ResultType) {",
            "    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)",
            "    $netTask = $asTask.Invoke($null, @($WinRtTask))",
            "    $netTask.Wait(-1) | Out-Null",
            "    return $netTask.Result",
            "}",
            "",
            "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null",
            "[Windows.Storage.Streams.DataReader,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null",
            "",
            "$tempDir = Join-Path $env:TEMP 'darkvisuals_nowplaying'",
            "New-Item -ItemType Directory -Force -Path $tempDir | Out-Null",
            "$cmdPath = Join-Path $tempDir 'cmd.txt'",
            "$lastIconApp = ''",
            "$lastThumbKey = ''",
            "",
            "while ($true) {",
            "    try {",
            "        $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
            "        $session = $mgr.GetCurrentSession()",
            "        if ($session -ne $null) {",
            "            $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
            "            $playback = $session.GetPlaybackInfo()",
            "            $appId = $session.SourceAppUserModelId",
            "            $playing = $false",
            "            try { $playing = ($playback.PlaybackStatus.ToString() -eq 'Playing') } catch {}",
            "",
            "            $pos = -1",
            "            $dur = -1",
            "            try {",
            "                $tl = $session.GetTimelineProperties()",
            "                if ($tl -ne $null) {",
            "                    $pos = [long]($tl.Position.TotalSeconds)",
            "                    $dur = [long](($tl.EndTime - $tl.StartTime).TotalSeconds)",
            "                }",
            "            } catch {}",
            "",
            "            $thumbPath = ''",
            "            $thumbFile = Join-Path $tempDir 'thumb.png'",
            "            $trackKey = [string]$props.Title + '|' + [string]$props.Artist",
            "            if ($trackKey -eq $lastThumbKey -and (Test-Path $thumbFile)) {",
            "                $thumbPath = $thumbFile",
            "            } elseif ($props.Thumbnail -ne $null) {",
            "                try {",
            "                    $stream = Await ($props.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])",
            "                    $reader = [Windows.Storage.Streams.DataReader]::new($stream)",
            "                    Await ($reader.LoadAsync([uint32]$stream.Size)) ([uint32]) | Out-Null",
            "                    $bytes = New-Object byte[] $stream.Size",
            "                    $reader.ReadBytes($bytes)",
            "                    $tmpThumb = Join-Path $tempDir 'thumb_tmp.png'",
            "                    [System.IO.File]::WriteAllBytes($tmpThumb, $bytes)",
            "                    Move-Item -Force $tmpThumb $thumbFile",
            "                    $thumbPath = $thumbFile",
            "                    $lastThumbKey = $trackKey",
            "                } catch {}",
            "            }",
            "",
            "            $iconPath = ''",
            "            $iconFile = Join-Path $tempDir 'icon.png'",
            "            if ($appId -eq $lastIconApp -and (Test-Path $iconFile)) {",
            "                $iconPath = $iconFile",
            "            } else {",
            "                try {",
            "                    $cand = @()",
            "                    $leaf = (($appId -split '!')[0] -split '\\\\')[-1] -replace '\\.exe$',''",
            "                    if ($leaf) { $cand += $leaf }",
            "                    $map = [ordered]@{ 'chrome'='chrome'; 'edge'='msedge'; 'spotify'='Spotify'; 'yandex'='YandexMusic'; 'firefox'='firefox'; 'opera'='opera'; 'vivaldi'='vivaldi'; 'brave'='brave'; 'vlc'='vlc'; 'aimp'='AIMP'; 'foobar'='foobar2000'; 'zune'='Music.UI'; 'winamp'='winamp' }",
            "                    foreach ($k in $map.Keys) { if ($appId -match $k) { $cand += $map[$k] } }",
            "                    $procPath = $null",
            "                    foreach ($n in $cand) {",
            "                        foreach ($p in @(Get-Process -Name $n -ErrorAction SilentlyContinue)) {",
            "                            try { if ($p.Path) { $procPath = $p.Path; break } } catch {}",
            "                        }",
            "                        if ($procPath) { break }",
            "                    }",
            "                    if ($procPath) {",
            "                        $ico = [System.Drawing.Icon]::ExtractAssociatedIcon($procPath)",
            "                        if ($ico -ne $null) {",
            "                            $tmpIco = Join-Path $tempDir 'icon_tmp.png'",
            "                            $ico.ToBitmap().Save($tmpIco, [System.Drawing.Imaging.ImageFormat]::Png)",
            "                            Move-Item -Force $tmpIco $iconFile",
            "                            $iconPath = $iconFile",
            "                            $lastIconApp = $appId",
            "                        }",
            "                    }",
            "                } catch {}",
            "            }",
            "",
            "            $obj = [PSCustomObject]@{",
            "                title = [string]$props.Title",
            "                artist = [string]$props.Artist",
            "                app = [string]$appId",
            "                playing = $playing",
            "                thumb = $thumbPath",
            "                icon = $iconPath",
            "                pos = $pos",
            "                dur = $dur",
            "            }",
            "            $obj | ConvertTo-Json -Compress",
            "        } else {",
            "            '{}'",
            "        }",
            "    } catch {",
            "        '{}'",
            "    }",
            "    for ($i = 0; $i -lt 5; $i++) {",
            "        if (Test-Path $cmdPath) {",
            "            $cmd = ''",
            "            try { $cmd = (Get-Content $cmdPath -Raw).Trim() } catch {}",
            "            Remove-Item $cmdPath -Force -ErrorAction SilentlyContinue",
            "            try {",
            "                $s = $mgr.GetCurrentSession()",
            "                if ($s -ne $null) {",
            "                    if ($cmd -eq 'playpause') { Await ($s.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }",
            "                    if ($cmd -eq 'next') { Await ($s.TrySkipNextAsync()) ([bool]) | Out-Null }",
            "                    if ($cmd -eq 'prev') { Await ($s.TrySkipPreviousAsync()) ([bool]) | Out-Null }",
            "                }",
            "            } catch {}",
            "            Start-Sleep -Milliseconds 300",
            "            break",
            "        }",
            "        Start-Sleep -Milliseconds 250",
            "    }",
            "}"
    );

    private static final long STALE_TIMEOUT_MS = 5000L;

    private static volatile Snapshot latest = null;
    private static volatile boolean started = false;
    private static Process process;

    private static String cachedThumbPath = null;
    private static long cachedThumbMtime = 0L;
    private static AbstractTexture cachedThumbTexture = null;
    private static String cachedIconPath = null;
    private static long cachedIconMtime = 0L;
    private static AbstractTexture cachedIconTexture = null;

      
    public static void ensureStarted() {
        if (started) return;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            started = true;  
            return;
        }
        started = true;

        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("darkvisuals");
            Files.createDirectories(dir);
            Path scriptPath = dir.resolve("nowplaying.ps1");
             
            Files.write(scriptPath, SCRIPT.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-File", scriptPath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            process = pb.start();

             
            Runtime.getRuntime().addShutdownHook(new Thread(NowPlayingBridge::shutdown, "nowplaying-shutdown"));

            Thread reader = new Thread(NowPlayingBridge::readLoop, "nowplaying-reader");
            reader.setDaemon(true);
            reader.start();

            Thread watchdog = new Thread(NowPlayingBridge::watchdogLoop, "nowplaying-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        } catch (IOException t) {
            System.out.println("[darkvisuals] Failed to start now-playing script: " + t);
        }
    }

    private static void readLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line);
            }
        } catch (Throwable ignored) {
             
        }
    }

    private static void parseLine(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String title = obj.has("title") ? obj.get("title").getAsString() : "";
            String artist = obj.has("artist") ? obj.get("artist").getAsString() : "";
            String app = obj.has("app") ? obj.get("app").getAsString() : "";
            boolean playing = obj.has("playing") && obj.get("playing").getAsBoolean();
            String thumb = obj.has("thumb") ? obj.get("thumb").getAsString() : "";
            String icon = obj.has("icon") ? obj.get("icon").getAsString() : "";
            long pos = obj.has("pos") ? obj.get("pos").getAsLong() : -1L;
            long dur = obj.has("dur") ? obj.get("dur").getAsLong() : -1L;
            latest = new Snapshot(title, artist, app, playing, thumb, icon, pos, dur);
        } catch (Throwable ignored) {
             
        }
    }

    private static void watchdogLoop() {
        try {
            while (true) {
                Thread.sleep(3000L);
                if (process != null && !process.isAlive()) {
                    started = false;
                    ensureStarted();
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

      
    public static void shutdown() {
        try {
            if (process != null) process.destroy();
        } catch (Throwable ignored) {}
    }

      
    private static Path tempDirPath() {
        String tmp = System.getenv("TEMP");
        if (tmp == null || tmp.isEmpty()) tmp = System.getProperty("java.io.tmpdir");
        return Path.of(tmp, "darkvisuals_nowplaying");
    }

 
    public static void sendCommand(String cmd) {
        if (!started || cmd == null || cmd.isEmpty()) return;
        try {
            Path dir = tempDirPath();
            Files.createDirectories(dir);
            Files.write(dir.resolve("cmd.txt"), cmd.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Throwable ignored) {}
    }

 
    public static void optimisticTogglePlaying() {
        Snapshot s = latest;
        if (s == null) return;
        latest = new Snapshot(s.title, s.artist, s.app, !s.playing,
                s.thumbPath, s.iconPath, s.smoothedPositionSec(), s.durationSec);
    }

      
    public static Snapshot getCurrent() {
        Snapshot snap = latest;
        if (snap == null) return null;
        if (System.currentTimeMillis() - snap.updatedAtMs > STALE_TIMEOUT_MS) return null;
        if (!snap.hasTrack()) return null;
        return snap;
    }

      
    public static AbstractTexture getThumbTexture() {
        Snapshot snap = latest;
        if (snap == null || snap.thumbPath == null || snap.thumbPath.isEmpty()) return null;
        long mtime = new File(snap.thumbPath).lastModified();
        if (snap.thumbPath.equals(cachedThumbPath) && mtime == cachedThumbMtime && cachedThumbTexture != null) {
            return cachedThumbTexture;
        }
        AbstractTexture tex = load(snap.thumbPath);
        if (tex != null) {
            cachedThumbTexture = tex;
            cachedThumbPath = snap.thumbPath;
            cachedThumbMtime = mtime;
        }
        return tex != null ? tex : cachedThumbTexture;
    }

      
    public static AbstractTexture getAppIconTexture() {
        Snapshot snap = latest;
        if (snap == null || snap.iconPath == null || snap.iconPath.isEmpty()) return null;
        long mtime = new File(snap.iconPath).lastModified();
        if (snap.iconPath.equals(cachedIconPath) && mtime == cachedIconMtime && cachedIconTexture != null) {
            return cachedIconTexture;
        }
        AbstractTexture tex = load(snap.iconPath);
        if (tex != null) {
            cachedIconTexture = tex;
            cachedIconPath = snap.iconPath;
            cachedIconMtime = mtime;
        }
        return tex != null ? tex : cachedIconTexture;
    }

    private static AbstractTexture load(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) return null;
            return Render2D.convert(img);
        } catch (Throwable t) {
            return null;
        }
    }
}

package javax.microedition.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import javax.microedition.lcdui.CanvasView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SoundPoolPlayer implements Player {
    private static SoundPool soundPool;
    private static final Object spLock = new Object();

    private int state = UNREALIZED;
    private byte[] audioData;
    private String contentType;
    private int soundId = -1;
    private int streamId = 0;
    private volatile CountDownLatch loadLatch;
    private final List<PlayerListener> listeners = new ArrayList<>();

    static {
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        soundPool = new SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            // Notify any waiting SoundPoolPlayer that its sound loaded
        });
    }

    public SoundPoolPlayer(InputStream stream, String type) throws IOException {
        this.contentType = type;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        this.audioData = baos.toByteArray();
    }

    @Override
    public void realize() throws MediaException {
        if (state == CLOSED) throw new MediaException("Player closed");
        state = REALIZED;
    }

    @Override
    public void prefetch() throws MediaException {
        if (state == CLOSED) throw new MediaException("Player closed");
        if (soundId == -1 && audioData != null) {
            try {
                Context ctx = CanvasView.getAppContext();
                if (ctx == null) throw new MediaException("No context");
                File tempFile = File.createTempFile("snd_", ".wav", ctx.getCacheDir());
                tempFile.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(audioData);
                fos.close();

                loadLatch = new CountDownLatch(1);
                final int expectedId;
                synchronized (spLock) {
                    expectedId = soundPool.load(tempFile.getAbsolutePath(), 1);
                    soundId = expectedId;
                }

                // Set up load complete callback for this specific sound
                soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                    if (sampleId == expectedId && loadLatch != null) {
                        loadLatch.countDown();
                    }
                });

                // Wait up to 2 seconds for async load
                loadLatch.await(2, TimeUnit.SECONDS);
            } catch (IOException e) {
                throw new MediaException("Failed to load sound: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        state = PREFETCHED;
    }

    @Override
    public void start() throws MediaException {
        if (state == CLOSED) throw new MediaException("Player closed");
        if (soundId == -1) {
            prefetch();
        }
        streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        state = STARTED;

        // Fire endOfMedia after the WAV duration
        long durationMs = getWavDurationMs();
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                if (state == STARTED) {
                    state = PREFETCHED;
                    notifyListeners(PlayerListener.END_OF_MEDIA, null);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    @Override
    public void stop() throws MediaException {
        if (streamId != 0) {
            soundPool.stop(streamId);
            streamId = 0;
        }
        if (state == STARTED) {
            state = PREFETCHED;
            notifyListeners(PlayerListener.STOPPED, null);
        }
    }

    @Override
    public void deallocate() {
        if (soundId != -1) {
            soundPool.unload(soundId);
            soundId = -1;
        }
        state = REALIZED;
    }

    @Override
    public void close() {
        try { stop(); } catch (MediaException ignored) {}
        deallocate();
        state = CLOSED;
        notifyListeners(PlayerListener.CLOSED, null);
        listeners.clear();
    }

    @Override
    public long setMediaTime(long now) { return now; }

    @Override
    public long getMediaTime() { return TIME_UNKNOWN; }

    @Override
    public int getState() { return state; }

    @Override
    public long getDuration() { return TIME_UNKNOWN; }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public void setLoopCount(int count) {
        // SoundPool handles loops in play() call; not used for SFX
    }

    @Override
    public void addPlayerListener(PlayerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removePlayerListener(PlayerListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String event, Object data) {
        for (PlayerListener l : listeners) {
            try {
                l.playerUpdate(this, event, data);
            } catch (Exception ignored) {}
        }
    }

    /** Parse WAV header to get duration in milliseconds. */
    private long getWavDurationMs() {
        if (audioData == null || audioData.length < 44) return 1000;
        // WAV header bytes 28-31: byte rate (little-endian)
        int byteRate = (audioData[28] & 0xFF)
                     | ((audioData[29] & 0xFF) << 8)
                     | ((audioData[30] & 0xFF) << 16)
                     | ((audioData[31] & 0xFF) << 24);
        if (byteRate <= 0) return 1000;
        int dataSize = audioData.length - 44;
        return Math.max(100, (dataSize * 1000L) / byteRate);
    }
}

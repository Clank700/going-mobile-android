package javax.microedition.media;

import android.content.Context;
import android.media.MediaPlayer;
import javax.microedition.lcdui.CanvasView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerWrapper implements Player {
    private int state = UNREALIZED;
    private byte[] audioData;
    private String contentType;
    private MediaPlayer mediaPlayer;
    private int loopCount = 1;
    private final List<PlayerListener> listeners = new ArrayList<>();

    public MediaPlayerWrapper(InputStream stream, String type) throws IOException {
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
        if (mediaPlayer == null && audioData != null) {
            try {
                Context ctx = CanvasView.getAppContext();
                if (ctx == null) throw new MediaException("No context");
                String ext = (contentType != null && contentType.contains("midi")) ? ".mid" : ".wav";
                File tempFile = File.createTempFile("music_", ext, ctx.getCacheDir());
                tempFile.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(audioData);
                fos.close();

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                mediaPlayer.prepare();

                mediaPlayer.setOnCompletionListener(mp -> {
                    state = PREFETCHED;
                    notifyListeners(PlayerListener.END_OF_MEDIA, null);
                });
            } catch (IOException e) {
                throw new MediaException("Failed to prepare media: " + e.getMessage());
            }
        }
        state = PREFETCHED;
    }

    @Override
    public void start() throws MediaException {
        if (state == CLOSED) throw new MediaException("Player closed");
        if (mediaPlayer == null) prefetch();
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(loopCount == -1);
            mediaPlayer.start();
        }
        state = STARTED;
    }

    @Override
    public void stop() throws MediaException {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        state = PREFETCHED;
        notifyListeners(PlayerListener.STOPPED, null);
    }

    @Override
    public void deallocate() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
        state = REALIZED;
    }

    @Override
    public void close() {
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        state = CLOSED;
        notifyListeners(PlayerListener.CLOSED, null);
        listeners.clear();
    }

    @Override
    public long setMediaTime(long now) throws MediaException {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int)(now / 1000));
        }
        return now;
    }

    @Override
    public long getMediaTime() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition() * 1000L;
        }
        return TIME_UNKNOWN;
    }

    @Override
    public int getState() { return state; }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration() * 1000L;
        }
        return TIME_UNKNOWN;
    }

    @Override
    public String getContentType() { return contentType; }

    @Override
    public void setLoopCount(int count) {
        this.loopCount = count;
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(count == -1);
        }
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
}

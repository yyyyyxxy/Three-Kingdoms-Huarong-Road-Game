import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MusicManager {
    private static MusicManager instance;
    private MediaPlayer mediaPlayer;
    private boolean isMusicEnabled = true;
    private String currentMusicType = "";
    private double volume = 0.5; // 默认音量50%

    // 音乐类型常量
    public static final String MAIN_MENU = "main_menu";
    public static final String GAME_PLAY = "game_play";
    public static final String TIMED_MODE = "timed_mode";
    public static final String VICTORY = "victory";
    public static final String FAILURE = "failure";

    private MusicManager() {}

    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    // 播放指定类型的音乐
    public void playMusic(String musicType) {
        if (!isMusicEnabled) return;

        // 如果已经在播放相同类型的音乐，直接返回
        if (currentMusicType.equals(musicType) && mediaPlayer != null &&
                mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }

        stopMusic(); // 停止当前音乐

        try {
            String musicFile = getMusicFile(musicType);
            if (musicFile != null) {
                // 从资源文件夹加载音乐
                InputStream musicStream = getClass().getResourceAsStream("/music/" + musicFile);
                if (musicStream != null) {
                    // 创建临时文件
                    File tempFile = File.createTempFile("music_", ".mp3");
                    tempFile.deleteOnExit();
                    Files.copy(musicStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    Media media = new Media(tempFile.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setVolume(volume);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // 循环播放
                    mediaPlayer.play();
                    currentMusicType = musicType;
                } else {
                    System.err.println("无法找到音乐文件: " + musicFile);
                }
            }
        } catch (Exception e) {
            System.err.println("播放音乐失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 获取对应音乐类型的文件名
    private String getMusicFile(String musicType) {
        switch (musicType) {
            case MAIN_MENU:
                return "main_menu.mp3";
            case GAME_PLAY:
                return "game_play.mp3";
            case TIMED_MODE:
                return "timed_mode.mp3";
            case VICTORY:
                return "victory.mp3";
            case FAILURE:
                return "failure.mp3";
            default:
                return null;
        }
    }

    // 停止音乐
    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        currentMusicType = "";
    }

    // 暂停音乐
    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        }
    }

    // 恢复音乐
    public void resumeMusic() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
            mediaPlayer.play();
        }
    }

    // 切换音乐开关
    public void toggleMusic() {
        isMusicEnabled = !isMusicEnabled;
        if (!isMusicEnabled) {
            stopMusic();
        } else {
            // 如果有记录的音乐类型，重新播放
            if (!currentMusicType.isEmpty()) {
                playMusic(currentMusicType);
            }
        }
    }

    // 设置音乐开关
    public void setMusicEnabled(boolean enabled) {
        if (isMusicEnabled != enabled) {
            toggleMusic();
        }
    }

    // 获取音乐开关状态
    public boolean isMusicEnabled() {
        return isMusicEnabled;
    }

    // 设置音量
    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume)); // 限制在0-1之间
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(this.volume);
        }
    }

    // 获取音量
    public double getVolume() {
        return volume;
    }

    // 调整音量
    public void adjustVolume(double delta) {
        setVolume(volume + delta);
    }

    // 获取当前播放状态
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    // 释放资源
    public void dispose() {
        stopMusic();
        instance = null;
    }
}
package org.telegram.messenger.voip;

import static android.content.Context.AUDIO_SERVICE;

import android.media.AudioManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;

public class VoipAudioManager {

    private final AudioManager audioManager;
    private Boolean isSpeakerphoneOn;

    private VoipAudioManager() {
        audioManager = (AudioManager) ApplicationLoader.applicationContext.getSystemService(AUDIO_SERVICE);
    }

    private static final class InstanceHolder {
        static final VoipAudioManager instance = new VoipAudioManager();
    }

    public static VoipAudioManager get() {
        return InstanceHolder.instance;
    }

    public void setSpeakerphoneOn(boolean on) {
        isSpeakerphoneOn = on;
        Utilities.globalQueue.postRunnable(() -> {
            audioManager.setSpeakerphoneOn(on);
        });
    }

    public boolean isSpeakerphoneOn() {
        if (isSpeakerphoneOn == null) {
            return audioManager.isSpeakerphoneOn();
        }
        return isSpeakerphoneOn;
    }
}

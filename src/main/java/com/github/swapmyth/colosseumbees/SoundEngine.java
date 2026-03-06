package com.github.swapmyth.colosseumbees;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class SoundEngine
{
	@Inject
	private ColosseumBeesConfig config;

	private final List<Clip> activeClips = new CopyOnWriteArrayList<>();

	/**
	 * Play a sound in a continuous loop. Returns the Clip handle so it can be stopped later.
	 */
	public Clip playLoopingClip(Sound sound)
	{
		File soundFile = SoundFileManager.getSoundFile(sound);
		if (soundFile == null || !soundFile.exists())
		{
			log.warn("Sound file not found for {}", sound);
			return null;
		}

		try
		{
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);

			// Apply volume from config
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			{
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float gain = 20f * (float) Math.log10(config.announcementVolume() / 100f);
				gain = Math.max(gain, gainControl.getMinimum());
				gain = Math.min(gain, gainControl.getMaximum());
				gainControl.setValue(gain);
			}

			clip.loop(Clip.LOOP_CONTINUOUSLY);
			activeClips.add(clip);
			return clip;
		}
		catch (UnsupportedAudioFileException e)
		{
			log.warn("Unsupported audio file for {}", sound, e);
		}
		catch (IOException e)
		{
			log.warn("Failed to read sound file for {}", sound, e);
		}
		catch (LineUnavailableException e)
		{
			log.warn("Audio line unavailable for {}", sound, e);
		}
		return null;
	}

	/**
	 * Stop and close a specific looping clip.
	 */
	public void stopClip(Clip clip)
	{
		if (clip != null)
		{
			clip.stop();
			clip.close();
			activeClips.remove(clip);
		}
	}

	/**
	 * Stop and close all active looping clips.
	 */
	public void stopAllClips()
	{
		for (Clip clip : activeClips)
		{
			clip.stop();
			clip.close();
		}
		activeClips.clear();
	}

	public void close()
	{
		stopAllClips();
	}
}

package com.github.swapmyth.colosseumbees;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public abstract class SoundFileManager
{
	private static final File DOWNLOAD_DIR = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "colosseum-bees");

	private static final HttpUrl RAW_GITHUB = HttpUrl.parse("https://raw.githubusercontent.com/swapmyth/colosseumbees/sounds");

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void ensureDownloadDirectoryExists()
	{
		if (!DOWNLOAD_DIR.exists())
		{
			DOWNLOAD_DIR.mkdirs();
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void ensureSoundDirectoryExists(File soundDirectory)
	{
		if (!soundDirectory.exists())
		{
			soundDirectory.mkdirs();
		}
	}

	public static void downloadAllMissingSounds(final OkHttpClient okHttpClient)
	{
		for (Sound sound : Sound.values())
		{
			File soundDirectoryFile = new File(DOWNLOAD_DIR, sound.getDirectory());
			ensureSoundDirectoryExists(soundDirectoryFile);

			File soundFile = Paths.get(DOWNLOAD_DIR.getPath(), sound.getDirectory(), sound.getResourceName()).toFile();
			if (soundFile.exists())
			{
				continue;
			}

			if (!downloadSound(sound, okHttpClient))
			{
				log.error("Failed to download bee sound: {}", sound.getResourceName());
				return;
			}
		}
	}

	private static boolean downloadSound(Sound sound, OkHttpClient okHttpClient)
	{
		String soundDirectory = sound.getDirectory();
		String soundResourceName = sound.getResourceName();
		File soundDirectoryFile = new File(DOWNLOAD_DIR.getPath(), soundDirectory);
		ensureSoundDirectoryExists(soundDirectoryFile);

		HttpUrl soundUrl = RAW_GITHUB.newBuilder()
			.addPathSegment(soundDirectory)
			.addPathSegment(soundResourceName)
			.build();
		Path outputPath = Paths.get(soundDirectoryFile.getPath(), soundResourceName);

		try (Response res = okHttpClient.newCall(new Request.Builder().url(soundUrl).build()).execute())
		{
			if (res.body() != null)
			{
				Files.copy(new BufferedInputStream(res.body().byteStream()), outputPath, StandardCopyOption.REPLACE_EXISTING);
				log.info("Downloaded bee sound: {}", sound.getResourceName());
				return true;
			}
			return false;
		}
		catch (IOException e)
		{
			log.error("Could not download bee sound: {}", sound.getResourceName(), e);
			return false;
		}
	}

	public static File getSoundFile(Sound sound)
	{
		return Paths.get(DOWNLOAD_DIR.getPath(), sound.getDirectory(), sound.getResourceName()).toFile();
	}
}

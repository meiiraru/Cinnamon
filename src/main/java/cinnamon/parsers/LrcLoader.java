package cinnamon.parsers;

import cinnamon.sound.Lyrics;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static cinnamon.events.Events.LOGGER;

public class LrcLoader {

    //load a lyrics file using the LRC file standard
    public static Lyrics loadLyrics(Resource res) throws IOException {
        LOGGER.debug("Loading lyrics \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream); BufferedReader br = new BufferedReader(reader)) {
            Lyrics lyrics = new Lyrics();

            for (String line; (line = br.readLine()) != null; ) {
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String l = line.trim();
                String[] split = l.split(":", 2);

                if (split.length < 2)
                    continue;

                String key = split[0].toLowerCase().substring(1); //remove the '['
                String value = split[1].trim();

                //try a lyrics line first
                try {
                    int minutes = Integer.parseInt(key);
                    String[] time = value.split("]", 2);
                    float seconds = Float.parseFloat(time[0]);

                    int timeMillis = (int) ((minutes * 60 + seconds) * 1000);
                    lyrics.getLyrics().add(new Pair<>(timeMillis, time[1]));

                    continue;
                } catch (Exception ignored) {}

                //remove ']' from the value
                value = value.substring(0, value.length() - 1);

                switch (key) {
                    case "ti" -> lyrics.setTitle(value);
                    case "ar" -> lyrics.setArtist(value);
                    case "al" -> lyrics.setAlbum(value);
                    case "au" -> lyrics.setAuthor(value);
                    case "by" -> lyrics.setBy(value);
                    case "re", "tool" -> lyrics.setTool(value);
                    case "ve" -> lyrics.setVersion(value);
                    case "offset" -> lyrics.setOffset(Integer.parseInt(value));
                    case "length" -> {
                        String[] time = value.split(":", 2);
                        lyrics.setLength((int) (Integer.parseInt(time[0]) * 60 + Float.parseFloat(time[1]) * 1000));
                    }
                }
            }

            return lyrics;
        }
    }
}

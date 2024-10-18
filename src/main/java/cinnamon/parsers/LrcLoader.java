package cinnamon.parsers;

import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LrcLoader {

    //load a lyrics file using the LRC file standard
    public static Lyrics loadLyrics(Resource res) {
        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
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
                    lyrics.lyrics.add(new Pair<>(timeMillis, time[1]));

                    continue;
                } catch (Exception ignored) {}

                //remove ']' from the value
                value = value.substring(0, value.length() - 1);

                switch (key) {
                    case "ti" -> lyrics.title = value;
                    case "ar" -> lyrics.artist = value;
                    case "al" -> lyrics.album = value;
                    case "au" -> lyrics.author = value;
                    case "by" -> lyrics.by = value;
                    case "re", "tool" -> lyrics.tool = value;
                    case "ve" -> lyrics.version = value;
                    case "offset" -> lyrics.offset = Integer.parseInt(value);
                    case "length" -> {
                        String[] time = value.split(":", 2);
                        lyrics.length = (int) (Integer.parseInt(time[0]) * 60 + Float.parseFloat(time[1]) * 1000);
                    }
                }
            }

            return lyrics;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lyrics file \"" + res + "\"", e);
        }
    }

    public static class Lyrics {
        public String
                title = "",   //ti
                artist = "",  //ar
                album = "",   //al
                author = "",  //au
                by = "",      //by
                tool = "",    //re / tool
                version = ""; //ve

        public int
                length = 0, //length
                offset = 0; //offset

        public final List<Pair<Integer, String>> lyrics = new ArrayList<>();
    }
}

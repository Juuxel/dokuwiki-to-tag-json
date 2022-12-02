package juuxel.dwttj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class DeepReplace {
    public static void main(String[] args) throws IOException {
        Path path = Path.of("tags2.json");
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        JsonObject json;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            json = gson.fromJson(reader, JsonObject.class);
        }

        deepReplace(json);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            gson.toJson(json, writer);
        }
    }

    private static void deepReplace(JsonElement json) {
        Queue<JsonElement> queue = new ArrayDeque<>();
        queue.offer(json);

        while (!queue.isEmpty()) {
            JsonElement current = queue.remove();

            if (current.isJsonArray()) {
                JsonArray array = current.getAsJsonArray();
                List<JsonElement> encountered = new ArrayList<>();
                Iterator<JsonElement> iter = array.iterator();

                while (iter.hasNext()) {
                    JsonElement next = iter.next();

                    if (encountered.contains(next)) {
                        iter.remove();
                    } else {
                        encountered.add(next);
                        queue.offer(next);
                    }
                }
            } else if (current.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : current.getAsJsonObject().entrySet()) {
                    queue.offer(entry.getValue());
                }
            }
        }
    }
}

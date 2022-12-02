package juuxel.dwttj;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "converts a common tag dokuwiki to its json form")
public class DWTTJ implements Callable<Void> {
    private static final Pattern SECTION_PATTERN = Pattern.compile("^===== (.+) =====$");
    private static final Map<String, Section> SECTIONS_BY_NAME = Map.of(
            "Item Tags", Section.ITEMS,
            "Block Tags", Section.BLOCKS,
            "Fluid Tags", Section.FLUIDS,
            "Sources", Section.SOURCES
    );

    @CommandLine.Parameters(description = "the dokuwiki file")
    Path dokuwiki;

    @CommandLine.Parameters(description = "the json file")
    Path json;

    public static void main(String[] args) {
        System.exit(new CommandLine(new DWTTJ()).execute(args));
    }

    @Override
    public Void call() throws Exception {
        Map<Section, JsonElement> jsons = new EnumMap<>(Section.class);
        List<String> lines = Files.readAllLines(dokuwiki);
        Section section = null;
        JsonObject current = null;

        for (String line : lines) {
            if (line.startsWith("=====")) {
                Matcher matcher = SECTION_PATTERN.matcher(line);
                if (matcher.matches()) {
                    section = SECTIONS_BY_NAME.get(matcher.group(1));
                } else {
                    throw new IllegalArgumentException("what: " + line);
                }
                continue;
            } else if (line.isBlank() || line.startsWith("^")) {
                continue;
            }

            // Remove pipes
            line = line.substring(1, line.length() - 1);
            String[] parts = line.split("\\|");
            trim(parts);

            assert section != null;
            if (section.isSource()) {
                JsonObject source = new JsonObject();
                source.addProperty("id", parts[0]);
                source.addProperty("name", parts[1]);
                source.addProperty("version", parts[2]);
                source.addProperty("url", parts[3]);
                ((JsonObject) jsons.computeIfAbsent(Section.SOURCES, s -> new JsonObject())).add(parts[0], source);
            } else {
                if (!parts[0].equals(":::")) {
                    String tagId = parts[0];
                    current = new JsonObject();
                    current.addProperty("id", tagId.substring("c:".length()));
                    current.add("replaced_by", new JsonArray());
                    current.add("sources", new JsonArray());
                    current.add("content", new JsonArray());
                    ((JsonArray) jsons.computeIfAbsent(section, s -> new JsonArray())).add(current);
                }

                String value = parts[1];
                String[] sources = parts[2].split(",\\s");
                assert current != null;
                addNonMissing(current.getAsJsonArray("sources"), sources);
                JsonArray contentSources = new JsonArray();
                addNonMissing(contentSources, sources);
                JsonObject content = new JsonObject();
                content.addProperty("value", value);
                content.add("sources", contentSources);
                current.getAsJsonArray("content").add(content);
            }
        }

        JsonObject outputJson = new JsonObject();
        jsons.forEach((sec, json) -> {
            outputJson.add(sec.name().toLowerCase(Locale.ROOT), json);
        });

        try (BufferedWriter writer = Files.newBufferedWriter(json)) {
            new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(outputJson, writer);
        }

        return null;
    }

    private static void trim(String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i].trim();
        }
    }

    private static void addNonMissing(JsonArray array, String[] strings) {
        for (String str : strings) {
            JsonPrimitive json = new JsonPrimitive(str);
            if (!array.contains(json)) array.add(json);
        }
    }

    private enum Section {
        ITEMS,
        BLOCKS,
        FLUIDS,
        SOURCES;

        boolean isSource() {
            return this == SOURCES;
        }
    }
}

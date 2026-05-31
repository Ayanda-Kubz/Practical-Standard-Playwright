package org.example;

import org.junit.jupiter.params.provider.Arguments;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

public class ConfigReader {

    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();

        // Load properties file from resources folder
        InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("resources.properties");

        if (input == null) {
            System.out.println("Sorry, unable to find resources.properties in resources folder.");
        }
        // Load the properties from the input stream
        properties.load(input);

        return properties;
    }

    public Path createFolder() throws IOException {

        // Define the base directory where the folder will be created
        Properties config = ConfigReader.loadProperties();

        String baseDir =config.getProperty("baseDir"); // Change this to your desired path

        // Create a timestamp string (e.g., 2026-05-29_14-35-22)
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));

        // Combine base directory and timestamp to form the folder path
        Path folderPath = Paths.get(baseDir, "TestResults_" + timestamp);

        try {
            // Create the directory
            Files.createDirectories(folderPath);
            System.out.println("Folder created: " + folderPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error creating folder: " + e.getMessage());
        }
        return folderPath;
    }

    public Stream<Arguments> csvDataProvider() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(ConfigReader.class.getResourceAsStream
                        ("/test-data.csv"))))) {

            String headerLine = reader.readLine(); // first line = headers
            String[] headers = headerLine.split(",");

            return reader.lines()
                    .map(line -> {
                        String[] values = line.split(",");
                        Map<String, String> row = new HashMap<>();
                        for (int i = 0; i < headers.length; i++) {
                            row.put(headers[i], values[i]);
                        }
                        return Arguments.of(row);
                    });
        }
    }
}

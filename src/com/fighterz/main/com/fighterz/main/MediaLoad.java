package com.fighterz.main;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;

public final class MediaLoad {
    private MediaLoad() {}

    /** Load all media files under a classpath folder (e.g. "/audio/fight/"). */
    public static List<MediaPlayer> loadMediaPlayers(Class<?> anchor, String folderOnClassPath) throws IOException {
        Objects.requireNonNull(anchor, "anchor");
        if (!folderOnClassPath.startsWith("/")) folderOnClassPath = "/" + folderOnClassPath;
        if (!folderOnClassPath.endsWith("/"))   folderOnClassPath += "/";

        URL url = Objects.requireNonNull(anchor.getResource(folderOnClassPath),
                "Folder not found on classpath: " + folderOnClassPath);

        List<String> resourcePaths = ( "file".equals(url.getProtocol()) )
                ? listFromFileUrl(url)
                : listFromJarUrl(url, folderOnClassPath);

        List<MediaPlayer> players = new ArrayList<>();
        for (String path : resourcePaths) {
            String localUrl = extractToTempFile(anchor, folderOnClassPath + path); // produces file:///...
            players.add(new MediaPlayer(new Media(localUrl)));
        }
        return players;
    }

    /** List resources when running from classes on disk. */
    private static List<String> listFromFileUrl(URL folderUrl) throws IOException {
        try {
            Path dir = Paths.get(folderUrl.toURI());
            try (var s = Files.list(dir)) {
                return s.filter(Files::isRegularFile)
                        .map(p -> dir.relativize(p).toString())
                        .collect(Collectors.toList());
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /** List resources when running from a shaded JAR. */
    private static List<String> listFromJarUrl(URL folderUrl, String folderOnClassPath) throws IOException {
        JarURLConnection conn = (JarURLConnection) folderUrl.openConnection();
        JarFile jar = conn.getJarFile();
        String prefix = folderOnClassPath.substring(1); // remove leading '/'
        List<String> names = new ArrayList<>();
        for (JarEntry e : Collections.list(jar.entries())) {
            String name = e.getName();
            if (name.startsWith(prefix) && !e.isDirectory()) {
                names.add(name.substring(prefix.length())); // relative name
            }
        }
        return names;
    }

    /** Copy a classpath resource to a temp file and return its file: URL string. */
    private static String extractToTempFile(Class<?> anchor, String resourcePath) throws IOException {
        String full = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        String ext = full.contains(".") ? full.substring(full.lastIndexOf('.')) : null;
        Path tmp = Files.createTempFile("fxmedia-", ext);
        tmp.toFile().deleteOnExit();
        try (InputStream in = Objects.requireNonNull(anchor.getResourceAsStream(full), "Missing: " + full)) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp.toUri().toString();
    }
}
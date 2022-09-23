package com.regnosys.rosetta.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

public class UrlUtils {
	private static Charset CHARSET = StandardCharsets.UTF_8;
	
	public static Reader openURL(URL url) throws IOException {
		Reader reader = new InputStreamReader(url.openStream(), CHARSET);
		return new BufferedReader(reader);
	}
	
	public static String getFileName(URL url) {
		return toPath(url).getFileName().toString();
	}
	
	public static String getBaseFileName(URL url) {
		return FilenameUtils.getBaseName(url.getPath());
	}
	
	public static String getFileExtension(URL url) {
		return FilenameUtils.getExtension(url.getPath());
	}
	
	public static URL getParent(URL url) {
		try {
			URI uri = url.toURI();
			return uri.resolve(".").toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			throw new RuntimeException("Error calculating parent of URL " + url);
		}
		
	}
	
	public static Path toPath(URL resource) {
        try {
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error converting resource url to path " + resource);
        }
    }

    public static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error converting resource path to url " + path);
        }
    }
}

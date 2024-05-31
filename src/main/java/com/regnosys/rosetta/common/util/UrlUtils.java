package com.regnosys.rosetta.common.util;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

public class UrlUtils {
	private static final Charset CHARSET = StandardCharsets.UTF_8;

	public static final String PORTABLE_FILE_SEPARATOR = "/";

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
	
	public static URL resolve(URL url, String child) {
		try {
			return new URL(url + "/" + child);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error resolving child " + child + " of URL " + url);
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

	/**
	 * Path.toString() does not work well on Windows, this method provides toString() that works across platforms
	 */
	public static String toPortableString(Path path) {
		return Optional.ofNullable(path)
				.map(Objects::toString)
				.map(s -> s.replace("\\", PORTABLE_FILE_SEPARATOR))
				.orElse(null);
	}
}

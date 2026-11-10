/*
 * LICENSE
 * DiscordSRVUtils
 * -------------
 * Copyright (C) 2020 - 2024 BlueTree242
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package dev.bluetree242.discordsrvutils.updatechecker;

import dev.bluetree242.discordsrvutils.utils.Utils;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Version comparison is independent of GitHub ordering and upstream release numbering. */
public final class GitHubReleaseCheck {
    public static final String RELEASES = "https://github.com/jcthegamer2024/DiscordSRVUtilsUpdated/releases";
    private static final Pattern VERSION = Pattern.compile("(?i)^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-BETA-(\\d+))?$");

    private GitHubReleaseCheck() {}
    public record Status(int versionsBehind, String downloadUrl) {}
    private record Version(int major, int minor, int patch, int beta) implements Comparable<Version> {
        @Override public int compareTo(Version other) {
            int result = Integer.compare(major, other.major);
            if (result == 0) result = Integer.compare(minor, other.minor);
            if (result == 0) result = Integer.compare(patch, other.patch);
            if (result == 0) result = Integer.compare(beta, other.beta);
            return result;
        }
    }

    private static Version version(String text) {
        var match = VERSION.matcher(text);
        if (!match.matches()) return null;
        try {
            return new Version(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)),
                    Integer.parseInt(match.group(3)), match.group(4) == null ? Integer.MAX_VALUE : Integer.parseInt(match.group(4)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Status check(String currentVersion, boolean includeBetas, String json) throws IOException {
        Version current = version(currentVersion);
        if (current == null) throw new IOException("Cannot compare local version " + currentVersion);
        var releases = Utils.OBJECT_MAPPER.readTree(json);
        if (releases == null || !releases.isArray()) throw new IOException("Expected a GitHub releases list");
        boolean allowBetas = includeBetas || current.beta() != Integer.MAX_VALUE;
        Set<Version> newer = new HashSet<>();
        Version newest = current;
        String download = RELEASES;
        for (var release : releases) {
            if (release.path("draft").asBoolean()) continue;
            Version candidate = version(release.path("tag_name").asText());
            if (candidate == null || (!allowBetas && (release.path("prerelease").asBoolean()
                    || candidate.beta() != Integer.MAX_VALUE)) || candidate.compareTo(current) <= 0) continue;
            newer.add(candidate);
            if (candidate.compareTo(newest) > 0) {
                newest = candidate;
                String url = release.path("html_url").asText();
                download = url.startsWith(RELEASES + "/tag/") ? url : RELEASES;
            }
        }
        return new Status(newer.size(), download);
    }
}

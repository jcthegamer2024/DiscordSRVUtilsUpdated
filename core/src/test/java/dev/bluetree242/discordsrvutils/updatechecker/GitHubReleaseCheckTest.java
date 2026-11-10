package dev.bluetree242.discordsrvutils.updatechecker;

import org.junit.Test;
import static org.junit.Assert.*;

public class GitHubReleaseCheckTest {
    private String release(String tag, boolean prerelease) {
        return "{\"tag_name\":\"" + tag + "\",\"prerelease\":" + prerelease
                + ",\"html_url\":\"" + GitHubReleaseCheck.RELEASES + "/tag/" + tag + "\"}";
    }

    @Test public void currentForkBetaDoesNotCompareAgainstOlderUpstreamVersions() throws Exception {
        var result = GitHubReleaseCheck.check("1.3.0-BETA-1", false,
                "[" + release("v1.2.14", false) + "," + release("v1.3.0-BETA-1", true) + "]");
        assertEquals(0, result.versionsBehind());
    }

    @Test public void betaNumbersSortNumericallyRegardlessOfFeedOrder() throws Exception {
        var result = GitHubReleaseCheck.check("1.3.0-BETA-1", false,
                "[" + release("v1.3.0-BETA-10", true) + "," + release("v1.3.0-BETA-2", true) + "]");
        assertEquals(2, result.versionsBehind());
        assertTrue(result.downloadUrl().endsWith("v1.3.0-BETA-10"));
    }

    @Test public void stableInstallSkipsBetasUnlessOptedIn() throws Exception {
        String json = "[" + release("v1.4.0-BETA-1", true) + "]";
        assertEquals(0, GitHubReleaseCheck.check("1.3.0", false, json).versionsBehind());
        assertEquals(1, GitHubReleaseCheck.check("1.3.0", true, json).versionsBehind());
    }

    @Test public void finalReleaseIsNewerThanItsBeta() throws Exception {
        assertEquals(1, GitHubReleaseCheck.check("1.3.0-BETA-10", false,
                "[" + release("v1.3.0", false) + "]").versionsBehind());
    }

    @Test public void draftsUnsupportedTagsAndDuplicatesAreIgnored() throws Exception {
        String same = release("v1.3.0-BETA-2", true);
        String draft = release("v9.0.0", false).replace("{", "{\"draft\":true,");
        assertEquals(1, GitHubReleaseCheck.check("1.3.0-BETA-1", false,
                "[" + draft + "," + release("random", false) + "," + same + "," + same + "]").versionsBehind());
    }

    @Test public void localBuildAheadOfPublishedReleasesDoesNotWarn() throws Exception {
        assertEquals(0, GitHubReleaseCheck.check("1.4.0-BETA-1", true,
                "[" + release("v1.3.0", false) + "]").versionsBehind());
    }

    @Test public void malformedFeedAndUnsupportedLocalVersionsFailClearly() {
        assertThrows(java.io.IOException.class, () -> GitHubReleaseCheck.check("1.3.0", false, "{}"));
        assertThrows(java.io.IOException.class, () -> GitHubReleaseCheck.check("development", true, "[]"));
    }
}

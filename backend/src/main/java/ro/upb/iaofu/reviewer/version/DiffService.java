package ro.upb.iaofu.reviewer.version;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import java.util.List;

/** Unified diff between two versions, used as context for the observation generator. */
public final class DiffService {

    private static final int CONTEXT_LINES = 3;

    private DiffService() {
    }

    public static String unifiedDiff(int fromVersion, String from, int toVersion, String to) {
        List<String> a = lines(from);
        List<String> b = lines(to);
        Patch<String> patch = DiffUtils.diff(a, b);
        if (patch.getDeltas().isEmpty()) {
            return "";
        }
        return String.join("\n", UnifiedDiffUtils.generateUnifiedDiff(
                "v" + fromVersion + "/solution.py", "v" + toVersion + "/solution.py", a, patch, CONTEXT_LINES));
    }

    private static List<String> lines(String code) {
        return code.replace("\r\n", "\n").lines().toList();
    }
}

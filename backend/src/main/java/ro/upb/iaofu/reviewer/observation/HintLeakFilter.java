package ro.upb.iaofu.reviewer.observation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import ro.upb.iaofu.reviewer.problem.Problem;

/**
 * Rejects level 1 and 2 hints that name the technique (F16). Matching ignores case
 * and diacritics. Terms of at least {@value #PREFIX_MIN_LENGTH} letters also match as
 * the start of a word, so Romanian inflections are caught ("dicționarul"); shorter
 * terms match whole words only ("set" does not match "setează"). A term is allowed
 * when the student's code already matches its allowedIfCode pattern.
 */
public final class HintLeakFilter {

    /** Levels 1 and 2 are checked; level 3 may name the technique. */
    static final int CHECKED_LEVELS = 2;
    static final int PREFIX_MIN_LENGTH = 5;

    private HintLeakFilter() {
    }

    /** Forbidden terms found in the checked levels of the hints; empty when the hints are clean. */
    public static List<String> leakedTerms(List<String> hints, List<Problem.ForbiddenTerm> terms, String code) {
        List<String> leaked = new ArrayList<>();
        for (int level = 0; level < Math.min(CHECKED_LEVELS, hints.size()); level++) {
            String text = normalize(hints.get(level));
            for (Problem.ForbiddenTerm term : terms) {
                if (!allowed(term, code) && containsWord(text, normalize(term.term())) && !leaked.contains(term.term())) {
                    leaked.add(term.term());
                }
            }
        }
        return leaked;
    }

    private static boolean allowed(Problem.ForbiddenTerm term, String code) {
        return term.allowedIfCode() != null && code != null
                && Pattern.compile(term.allowedIfCode()).matcher(code).find();
    }

    private static boolean containsWord(String text, String term) {
        String end = term.length() >= PREFIX_MIN_LENGTH ? "" : "(?![\\p{L}\\p{N}_])";
        return Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(term) + end).matcher(text).find();
    }

    static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}

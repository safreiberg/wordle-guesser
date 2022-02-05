package wordle.guesser.utilities;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WordleFilter implements Predicate<String> {

    private static final Pattern AZ_PATTERN = Pattern.compile("[a-z]*");

    @Override
    public boolean test(String s) {
        return s.length() == 5 && AZ_PATTERN.matcher(s).matches();
    }
}

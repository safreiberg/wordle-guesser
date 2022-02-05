package wordle.guesser.test;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.WordleFilter;

import static org.assertj.core.api.Assertions.assertThat;

public class DictionaryTest {

    @Test
    public void testDictionary() {
        Dictionary dictionary = Dictionary.ofWords(ImmutableSet.of("ALOFT", "arise", "foo"))
                .filterTo(new WordleFilter())
                .uppercase();
        assertThat(dictionary.size()).isEqualTo(2);
        assertThat(dictionary.contains("ALOFT")).isTrue();
        assertThat(dictionary.aggregateLetterCount().get('A')).isEqualTo(2);
    }
}

package wordle.guesser.test;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.GuessScorer;
import wordle.guesser.utilities.KnownState;
import wordle.guesser.utilities.WordleFilter;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static wordle.guesser.utilities.KnownState.Outcome.*;

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

    @Test
    public void testKnownState() {
        KnownState knownState = new KnownState();
        assertThat(knownState.satisfies("ALOFT")).isTrue();

        knownState.addGuess("HOUSE", NOT_IN_WORD, WRONG_SPOT, NOT_IN_WORD, NOT_IN_WORD, NOT_IN_WORD);
        assertThat(knownState.satisfies("ALOFT")).isTrue();
        assertThat(knownState.satisfies("HAVER")).isFalse();

        knownState.addGuess("TRAIN", WRONG_SPOT, NOT_IN_WORD, WRONG_SPOT, NOT_IN_WORD, NOT_IN_WORD);
        assertThat(knownState.satisfies("ALOFT")).isTrue();
        assertThat(knownState.satisfies("HAVER")).isFalse();

        assertThat(Arrays.equals(KnownState.getOutcomes("ALOET", "AROSE"), new KnownState.Outcome[] {
                CORRECT, NOT_IN_WORD, CORRECT, NOT_IN_WORD, WRONG_SPOT
        })).isTrue();
    }

    @Test
    public void testThings() {
        Dictionary dict = Dictionary.defaultWordleDictionary();

        KnownState knownState = new KnownState();
        GuessScorer guessScorer = new GuessScorer(5);
        guessScorer.process(dict, knownState);
        System.out.println(guessScorer);
        System.out.println(guessScorer.getBestGuess());

        knownState.addGuess("AROSE", CORRECT, NOT_IN_WORD, CORRECT, NOT_IN_WORD, NOT_IN_WORD);

        guessScorer.process(dict, knownState);
        System.out.println(guessScorer);
        System.out.println(guessScorer.getBestGuess());
    }
}

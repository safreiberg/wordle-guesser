package wordle.guesser.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import wordle.guesser.utilities.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static wordle.guesser.utilities.KnownState.Outcome.*;

public class DictionaryTest {

    @Test
    public void testDictionary() {
        Dictionary dictionary = Dictionary.ofWords(ImmutableSet.of("ALOFT", "arise", "foo"))
                .filterTo(new WordleFilter())
                .uppercase();
        assertThat(dictionary.size()).isEqualTo(1);
        assertThat(dictionary.contains("ARISE")).isTrue();
        assertThat(dictionary.aggregateLetterCount().get('A')).isEqualTo(1);
    }

    @Test
    public void testDictionaryPrefilter() {
        Dictionary dictionary = Dictionary.ofWords(ImmutableSet.of("ALOFT", "ARISE", "FATLI"));
        KnownState knownState = new KnownState();
        knownState.addGuess("ART", CORRECT, NOT_IN_WORD, WRONG_SPOT);
        assertThat(dictionary.prefilterWordsIgnoringWrongSpot(knownState).collect(Collectors.toSet())).isEqualTo(ImmutableSet.of("ALOFT"));
        // filterToValid considers wrongspot. Prefilter doesn't
        dictionary = dictionary.filterToValid(knownState);
        assertThat(dictionary.getWords()).isEqualTo(ImmutableSet.of("ALOFT"));
        knownState.addGuess("O", NOT_IN_WORD);
        assertThat(dictionary.prefilterWordsIgnoringWrongSpot(knownState).collect(Collectors.toSet())).isEqualTo(ImmutableSet.of());
    }

    @Test
    public void testFrames() {
        Dictionary dict = Dictionary.ofWords(ImmutableSet.of("FRAME", "ARAME", "GRAME", "FROGS"));
        KnownState state = new KnownState();
        state.addGuess("LARES", NOT_IN_WORD, WRONG_SPOT, WRONG_SPOT, WRONG_SPOT, NOT_IN_WORD);
        state.addGuess("TRADE", NOT_IN_WORD, CORRECT, CORRECT, NOT_IN_WORD, CORRECT);
        state.addGuess("CRUMB", NOT_IN_WORD, CORRECT, NOT_IN_WORD, CORRECT, NOT_IN_WORD);
        BruteGuesser bruteGuesser = new BruteGuesser(5, dict);
        bruteGuesser.process(dict, state);
        System.out.println(state);
        System.out.println(bruteGuesser.printState());
        System.out.println(dict.sizeAfterFiltering(state));

        KnownState copy = state.deepCopy();
        copy.addGuess("FROGS", KnownState.getOutcomes("GRAME", "FROGS"));
        bruteGuesser.process(dict, copy);
        System.out.println(copy);
        System.out.println(bruteGuesser.printState());
    }

    @Test
    public void testETE() {
        BruteGuesser.DEBUG_FORCE_SINGLE_THREAD = false;
        Stopwatch started = Stopwatch.createStarted();
        Dictionary dictionary = Dictionary.wordle12k();
        System.out.println("Parse time: " + started.elapsed());
        KnownState knownState = new KnownState();
        String answer = "FAZES";
        BruteGuesser guesser = new BruteGuesser(5, dictionary);

        boolean done = false;
        while (!done) {
            dictionary = dictionary.filterToValid(knownState);
            done = guessAndUpdateState(dictionary, knownState, guesser, answer);
        }
        System.out.println("Total time " + started.elapsed());
    }

    private boolean guessAndUpdateState(Dictionary dictionary, KnownState state, Guesser guesser, String answer) {
        guesser.process(dictionary, state);
        String guess = Preconditions.checkNotNull(guesser.getBestGuess());
        System.out.println(guess);
        KnownState.Outcome[] outcomes = KnownState.getOutcomes(answer, guess);
        state.addGuess(guess, outcomes);
        System.out.println(dictionary.prefilterWordsIgnoringWrongSpot(state).count());
        return answer.equals(guess);
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
    public void timeDictionaryConstruction() {
        Stopwatch started = Stopwatch.createStarted();
        Dictionary dict = Dictionary.wordle12k();
        System.out.println("Construction takes " + started.elapsed());
    }

    @Test
    public void testThings() {
        Dictionary dict = Dictionary.wordle12k();
        KnownState knownState = new KnownState();
        BruteGuesser guessScorer = new BruteGuesser(5, dict);
        guessScorer.process(dict, knownState);
        System.out.println(guessScorer);
        System.out.println(guessScorer.getBestGuess());

        knownState.addGuess("AROSE", CORRECT, NOT_IN_WORD, CORRECT, NOT_IN_WORD, NOT_IN_WORD);

        guessScorer.process(dict, knownState);
        System.out.println(guessScorer);
        System.out.println(guessScorer.getBestGuess());
    }
}

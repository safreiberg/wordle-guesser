package wordle.guesser.utilities;

import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.*;

/**
 * In the remaining dictionary, we know the frequency of each letter.
 * Words that do not satisfy the known state are given a negative score.
 */
public class GuessScorer {

    private final SortedMap<Integer, Set<String>> scoreToGuesses;
    private final int guessesToKeep;

    public GuessScorer(int guessesToKeep) {
        this.scoreToGuesses = new TreeMap<>();
        this.guessesToKeep = guessesToKeep;
    }

    public void process(Dictionary dictionary, KnownState state) {
        this.scoreToGuesses.clear();
        dictionary = dictionary.filterToValid(state);
        Map<Character, Integer> remainingLetterCounts = dictionary.aggregateLetterCount();
        for (String word : dictionary.getWords()) {
            int score = score(remainingLetterCounts, word, state);
            scoreToGuesses.putIfAbsent(score, new HashSet<>());
            scoreToGuesses.get(score).add(word);
            if (scoreToGuesses.size() > guessesToKeep) {
                scoreToGuesses.remove(scoreToGuesses.firstKey());
            }
        }
    }

    @Nullable
    public String getBestGuess() {
        return Iterables.getFirst(scoreToGuesses.get(scoreToGuesses.lastKey()), null);
    }

    public String printState() {
        return scoreToGuesses.toString();
    }

    @Override
    public String toString() {
        return "GuessScorer{" +
                "scoreToGuesses=" + scoreToGuesses +
                ", guessesToKeep=" + guessesToKeep +
                '}';
    }

    private int score(Map<Character, Integer> letterCounts, String word, KnownState state) {
        if (!state.satisfies(word)) {
            return -1;
        }
        char[] chars = word.toCharArray();
        Set<Character> uniqueLetters = new HashSet<>();
        for (char aChar : chars) {
            uniqueLetters.add(aChar);
        }

        int score = 0;
        for (Character uniqueLetter : uniqueLetters) {
            Integer integer = letterCounts.get(uniqueLetter);
            if (integer != null) {
                score += integer;
            }
        }
        return score;
    }
}

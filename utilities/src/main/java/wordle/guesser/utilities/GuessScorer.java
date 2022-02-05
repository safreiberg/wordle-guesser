package wordle.guesser.utilities;

import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * In the remaining dictionary, we know the frequency of each letter.
 * Words that do not satisfy the known state are given a negative score.
 */
public class GuessScorer {

    // Scientifically chosen
    private static final String BEST_FIRST_WORD = "TARES";
    private final SortedMap<Integer, Set<String>> scoreToGuesses;
    private final int guessesToKeep;
    private final Dictionary rawDictionary;

    public GuessScorer(int guessesToKeep, Dictionary rawDictionary) {
        this.scoreToGuesses = new TreeMap<>();
        this.guessesToKeep = guessesToKeep;
        this.rawDictionary = rawDictionary;
    }

    public void determineFirstWords(Dictionary dictionary) {
        process(dictionary, new KnownState(), true);
    }

    public void process(Dictionary dictionary, KnownState state) {
        process(dictionary, state, false);
    }

    public void process(Dictionary dictionary, KnownState state, boolean force) {
        this.scoreToGuesses.clear();
        if (state.isEmpty() && !force) {
            // hacks
            return;
        }
        Dictionary prefilteredDictionary = dictionary.filterToValid(state);
        ExecutorService exec = Executors.newFixedThreadPool(24);
        Map<String, Future<Integer>> futureScores = new HashMap<>();
        for (String guess : rawDictionary.getWords()) {
            futureScores.put(guess, exec.submit(() -> {
                int aggregateScore = 0;
                for (String potentialAnswer : prefilteredDictionary.getWords()) {
                    KnownState copyState = state.deepCopy();
                    copyState.addGuess(guess, KnownState.getOutcomes(potentialAnswer, guess));
                    aggregateScore += prefilteredDictionary.sizeAfterFiltering(copyState);
                }
                return aggregateScore;
            }));
        }
        exec.shutdown();
        for (Map.Entry<String, Future<Integer>> entry : futureScores.entrySet()) {
            try {
                Integer score = entry.getValue().get();
                String guess = entry.getKey();
                scoreToGuesses.putIfAbsent(score, new HashSet<>());
                scoreToGuesses.get(score).add(guess);
                if (scoreToGuesses.size() > guessesToKeep) {
                    scoreToGuesses.remove(scoreToGuesses.lastKey());
                }
            } catch (Exception e) {
                throw new RuntimeException("ah fuck", e);
            }
        }
    }

    @Nullable
    public String getBestGuess() {
        if (scoreToGuesses.isEmpty()) {
            return BEST_FIRST_WORD;
        }
        return Iterables.getFirst(scoreToGuesses.get(scoreToGuesses.firstKey()), null);
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

    @SuppressWarnings("unused")
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

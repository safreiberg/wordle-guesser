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
public class BruteGuesser implements Guesser {

    // Scientifically chosen
    private static final String BEST_FIRST_WORD = "TARES";
    private final SortedMap<Integer, Set<String>> scoreToGuesses;
    private final int guessesToKeep;
    private final Dictionary rawDictionary;

    public BruteGuesser(int guessesToKeep, Dictionary rawDictionary) {
        this.scoreToGuesses = new TreeMap<>();
        this.guessesToKeep = guessesToKeep;
        this.rawDictionary = rawDictionary;
    }

    @Override
    public void determineFirstWords(Dictionary dictionary) {
        process(dictionary, new KnownState(), true);
    }

    @Override
    public void process(Dictionary dictionary, KnownState state) {
        process(dictionary, state, false);
    }

    private void process(Dictionary dictionary, KnownState state, boolean force) {
        this.scoreToGuesses.clear();
        if (state.isEmpty() && !force) {
            // hacks
            return;
        }
        Dictionary prefilteredDictionary = dictionary.filterToValid(state);
        ExecutorService exec = Executors.newFixedThreadPool(24);
        Map<String, Future<Integer>> futureScores = new HashMap<>();
        for (String guess : rawDictionary.getWords()) {
            futureScores.put(guess, exec.submit(() -> scoreBetter(state, prefilteredDictionary, guess)));
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

    private int scoreBetter(KnownState state, Dictionary prefilteredDictionary, String guess) {
        int aggregateScore = 0;
        for (String potentialAnswer : prefilteredDictionary.getWords()) {
            KnownState copyState = state.deepCopy();
            copyState.addGuess(guess, KnownState.getOutcomes(potentialAnswer, guess));
            aggregateScore += prefilteredDictionary.sizeAfterFiltering(copyState);
        }
        return aggregateScore;
    }

    @Override
    @Nullable
    public String getBestGuess() {
        if (scoreToGuesses.isEmpty()) {
            return BEST_FIRST_WORD;
        }
        return Iterables.getFirst(scoreToGuesses.get(scoreToGuesses.firstKey()), null);
    }

    @Override
    public String printState() {
        return scoreToGuesses.toString();
    }

}

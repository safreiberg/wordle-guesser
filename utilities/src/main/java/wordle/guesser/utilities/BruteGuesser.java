package wordle.guesser.utilities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

/**
 * In the remaining dictionary, we know the frequency of each letter.
 * Words that do not satisfy the known state are given a negative score.
 */
public class BruteGuesser implements Guesser {

    @VisibleForTesting
    public static boolean DEBUG_FORCE_SINGLE_THREAD = false;
    public static boolean DEBUG_SKIP_HARDCODED_ANSWER = false;
    // Scientifically chosen
    private static final String BEST_FIRST_GUESS = "CRAVE";
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
        if (state.isEmpty() && !force && !DEBUG_SKIP_HARDCODED_ANSWER) {
            return;
        }
        Dictionary prefilteredDictionary = dictionary.filterToValid(state);
        int threadCount = DEBUG_FORCE_SINGLE_THREAD ? 1 : Runtime.getRuntime().availableProcessors();
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        Map<String, Future<Integer>> futureScores = new HashMap<>();
        for (String guess : rawDictionary.getWords()) {
            futureScores.put(guess, exec.submit(() -> {
                return scoreBetter(state, prefilteredDictionary, guess);
            }));
        }
        exec.shutdown();
        int done = 0;
        Stopwatch timer = Stopwatch.createStarted();
        for (Map.Entry<String, Future<Integer>> entry : futureScores.entrySet()) {
            try {
                Integer score = entry.getValue().get();
                done++;
                if (done % 100 == 0 && timer.elapsed(TimeUnit.SECONDS) > 20) {
                    System.out.println("Completed " + done + " in " + timer.elapsed());
                }
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

    private int scoreBetter(KnownState state,
                            Dictionary prefilteredDictionary,
                            String guess) {
        int aggregateScore = 0;
        for (String potentialAnswer : prefilteredDictionary.getWords()) {
            KnownState copyState = state.deepCopy();
            copyState.addGuess(guess, KnownState.getOutcomes(potentialAnswer, guess));
            int score = prefilteredDictionary.sizeAfterFiltering(copyState);
            aggregateScore += score;
        }
        return aggregateScore;
    }

    @Override
    @Nullable
    public String getBestGuess() {
        if (scoreToGuesses.isEmpty()) {
            return BEST_FIRST_GUESS;
        }
        return Iterables.getFirst(scoreToGuesses.get(scoreToGuesses.firstKey()), null);
    }

    @Override
    public String printState() {
        return scoreToGuesses.toString();
    }

}

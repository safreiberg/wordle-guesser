package wordle.guesser.app;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import wordle.guesser.utilities.*;
import wordle.guesser.utilities.Dictionary;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DebugTotals {

    public static void main(String[] args) {
        BruteGuesser.DEBUG_FORCE_SINGLE_THREAD = false;
//        BruteGuesser.DEBUG_SKIP_HARDCODED_ANSWER = true;
        Dictionary dict = Dictionary.wordle12k();
        ExecutorService exec = Executors.newFixedThreadPool(1);
        Collection<Future<Map<String, Integer>>> futures = new ArrayList<>();
        for (Collection<String> batch : Iterables.partition(dict.getWords(), 100)) {
            futures.add(exec.submit(() -> {
                Map<String, Integer> wordToScore = new HashMap<>();
                for (String word : batch) {
                    int score = playOnce(word, dict);
                    wordToScore.put(word, score);
                }
                return wordToScore;
            }));
        }
        Multimap<Integer, String> scoreToWords = HashMultimap.create();
        int i = 0;
        for (Future<Map<String, Integer>> future : futures) {
            try {
                Map<String, Integer> wordToScore = future.get();
                i+= wordToScore.size();
                for (Map.Entry<String, Integer> entry : wordToScore.entrySet()) {
                    scoreToWords.put(entry.getValue(), entry.getKey());
                }
                System.out.println("Played " + i + " games.");
                System.out.println("Current state:");
                printState(scoreToWords);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Final state:");
        printState(scoreToWords);
    }

    private static void printState(Multimap<Integer, String> scoreToWords) {
        List<Integer> scores = Ordering.natural().sortedCopy(scoreToWords.keySet());
        for (Integer score : scores) {
            System.out.println("Score: " + score + ", count: " + scoreToWords.get(score).size());
        }
        System.out.println("");
        for (Integer score : scores) {
            System.out.println("Score: " + score + ", words: " + scoreToWords.get(score));
        }
        System.out.println("\n");
    }

    private static int playOnce(String answer, Dictionary dict) {
        int n = 0;
        KnownState knownState = new KnownState();
        Guesser guessScorer = new BruteGuesser(5, dict);
        while (true) {
            guessScorer.process(dict, knownState);
            String currentGuess = guessScorer.getBestGuess();
            if (currentGuess == null) {
                throw new IllegalStateException("bokr");
            }
            n++;
            KnownState.Outcome[] lastGuess = KnownState.getOutcomes(answer, currentGuess);
            knownState.addGuess(currentGuess, lastGuess);
            if (currentGuess.equals(answer)) {
                return n;
            }
        }
    }
}

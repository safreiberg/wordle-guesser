package wordle.guesser.app;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.GuessScorer;
import wordle.guesser.utilities.KnownState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugTotals {

    public static void main(String[] args) {
        Dictionary dict = Dictionary.defaultWordleDictionary();
        Multimap<Integer, String> scoreToWords = HashMultimap.create();
        int i = 0;
        for (String word : dict.getWords()) {
            int score = playOnce(word, dict);
            scoreToWords.put(score, word);
            i++;
            if (i % 100 == 0) {
                System.out.println("Played " + i + " games.");
                System.out.println("Current state:");
                printState(scoreToWords);
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
        GuessScorer guessScorer = new GuessScorer(5, dict);
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
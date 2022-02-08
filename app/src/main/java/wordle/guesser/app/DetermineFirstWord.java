package wordle.guesser.app;

import com.google.common.base.Stopwatch;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.BruteGuesser;

public class DetermineFirstWord {

    public static void main(String[] args) throws Exception {
        Stopwatch timer = Stopwatch.createStarted();
        Dictionary dict = Dictionary.wordle12k();
        BruteGuesser guessScorer = new BruteGuesser(5, dict);
        guessScorer.determineFirstWords(dict);
        System.out.println(guessScorer.getBestGuess());
        System.out.println(guessScorer.printState());
        System.out.println("Elapsed " + timer.elapsed());
        Thread.sleep(15000L);
    }
}

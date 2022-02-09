package wordle.guesser.app;

import com.google.common.base.Stopwatch;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.BruteGuesser;
import wordle.guesser.utilities.Guesser;
import wordle.guesser.utilities.SimpleGuesser;

public class DetermineFirstWord {

    public static void main(String[] args) throws Exception {
        Stopwatch timer = Stopwatch.createStarted();
        Dictionary dict = Dictionary.wordle12k();
        Guesser guessScorer = new SimpleGuesser(5);
        guessScorer.determineFirstWords(dict);
        System.out.println(guessScorer.getBestGuess());
        System.out.println(guessScorer.printState());
        System.out.println("Elapsed " + timer.elapsed());
    }
}

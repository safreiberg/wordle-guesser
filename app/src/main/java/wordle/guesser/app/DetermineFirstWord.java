package wordle.guesser.app;

import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.BruteGuesser;

public class DetermineFirstWord {

    public static void main(String[] args) {
        Dictionary dict = Dictionary.wordle12k();
        BruteGuesser guessScorer = new BruteGuesser(5, dict);
        guessScorer.determineFirstWords(dict);
        System.out.println(guessScorer.getBestGuess());
        System.out.println(guessScorer.printState());
    }
}

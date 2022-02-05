package wordle.guesser.app;

import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.GuessScorer;

public class DetermineFirstWord {

    public static void main(String[] args) {
        Dictionary dict = Dictionary.defaultWordleDictionary();
        GuessScorer guessScorer = new GuessScorer(5, dict);
        guessScorer.determineFirstWords(dict);
        System.out.println(guessScorer.getBestGuess());
        System.out.println(guessScorer.printState());
    }
}

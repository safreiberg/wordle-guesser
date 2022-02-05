/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package wordle.guesser.app;


import com.google.common.collect.Ordering;
import wordle.guesser.utilities.Dictionary;
import wordle.guesser.utilities.GuessScorer;
import wordle.guesser.utilities.KnownState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class App {
    public static void main(String[] args) throws IOException {
        KnownState knownState = new KnownState();
        Dictionary dict = Dictionary.defaultWordleDictionary();
        GuessScorer guessScorer = new GuessScorer(5, dict);

        System.out.println("Welcome to wordleguesser!");
        System.out.println("The app will present you with a guess.");
        System.out.println("Encode the outcomes into a string where");
        System.out.println("  B = Black = 'not in word'");
        System.out.println("  Y = Yellow = 'wrong location'");
        System.out.println("  G = Green = 'correct location'");
        System.out.println("");
        System.out.println("Enter 'Win!' when done.");
        System.out.println("Ready to play? Press enter when ready.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String firstEnter = reader.readLine().trim();
            if (firstEnter.contains("DEBUG ")) {
                secretDebugMode(knownState, guessScorer, dict, firstEnter);
            } else {
                playGame(knownState, guessScorer, dict, reader);
            }
        }
    }

    private static void secretDebugMode(KnownState knownState, GuessScorer guessScorer, Dictionary dict, String firstEnter) {
        String debugAnswer = firstEnter.split(" ")[1];
        System.out.println("Entering secret debug mode. Will play without you muahaha");
        int n = 0;
        while (true) {
            guessScorer.process(dict, knownState);
            Dictionary remaining = dict.filterToValid(knownState);
            System.out.println("Current dictionary size: " + remaining.size());
            if (remaining.size() < 100) {
                System.out.println("Remaining words: " + Ordering.natural().sortedCopy(remaining.getWords()));
            }
            System.out.println("The current best guesses are: " + guessScorer.printState());
            System.out.println("Our next guess is: " + guessScorer.getBestGuess());
            String currentGuess = guessScorer.getBestGuess();
            if (currentGuess == null) {
                System.out.println("How??");
                return;
            }
            n++;
            KnownState.Outcome[] lastGuess = KnownState.getOutcomes(debugAnswer, currentGuess);
            System.out.println("Scored: " + Arrays.toString(lastGuess));
            knownState.addGuess(currentGuess, lastGuess);
            System.out.println("");
            if (currentGuess.equals(debugAnswer)) {
                System.out.println("I win! Took me " + n + " tries.");
                return;
            }
        }
    }

    private static void playGame(KnownState knownState, GuessScorer guessScorer, Dictionary dict, BufferedReader reader) throws IOException {
        int n = 0;
        while (true) {
            guessScorer.process(dict, knownState);
            Dictionary remaining = dict.filterToValid(knownState);
            System.out.println("Current dictionary size: " + remaining.size());
            if (remaining.size() < 100) {
                System.out.println("Remaining words: " + Ordering.natural().sortedCopy(remaining.getWords()));
            }
            System.out.println("The current best guesses are: " + guessScorer.printState());
            System.out.println("Your next guess is: " + guessScorer.getBestGuess());
            String currentGuess = guessScorer.getBestGuess();
            n++;
            if (currentGuess == null) {
                System.out.println("No guess remains. You do something wrong?");
                return;
            }

            String input = reader.readLine().trim();
            if (input.length() == 5) {
                knownState.addGuess(currentGuess, decode(input));
            } else if (input.equals("Win!")) {
                System.out.println("Awesome! Took " + n + " guesses.");
                return;
            }
        }
    }

    private static KnownState.Outcome[] decode(String input) {
        KnownState.Outcome[] outcomes = new KnownState.Outcome[input.length()];
        for (int i = 0; i < input.length(); i++) {
            switch (input.charAt(i)) {
                case 'B':
                    outcomes[i] = KnownState.Outcome.NOT_IN_WORD;
                    break;
                case 'Y':
                    outcomes[i] = KnownState.Outcome.WRONG_SPOT;
                    break;
                case 'G':
                    outcomes[i] = KnownState.Outcome.CORRECT;
                    break;
                default:
                    throw new IllegalArgumentException("whhyyy");
            }
        }
        return outcomes;
    }
}

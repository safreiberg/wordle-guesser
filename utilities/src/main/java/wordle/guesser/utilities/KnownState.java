package wordle.guesser.utilities;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

public class KnownState {

    private final Multimap<Character, Integer> requiredLetterWrongSpot;
    private final Map<Integer, Character> requiredLocations;
    private final Set<Character> notInWord;
    private final Set<String> alreadyGuessedWords = new HashSet<>();

    public KnownState() {
        this.requiredLetterWrongSpot = HashMultimap.create();
        this.requiredLocations = new HashMap<>();
        this.notInWord = new HashSet<>();
    }

    public enum Outcome {
        // grey
        NOT_IN_WORD,
        // yellow
        WRONG_SPOT,
        // green,
        CORRECT;
    }

    public void addGuess(String guess, Outcome...outcomes) {
        if (outcomes.length != guess.length()) {
            throw new IllegalArgumentException("gimme more");
        }
        for (int i = 0; i < guess.length(); i++) {
            char character = guess.charAt(i);
            switch(outcomes[i]) {
                case CORRECT:
                    this.requiredLocations.put(i, character);
                    break;
                case WRONG_SPOT:
                    this.requiredLetterWrongSpot.put(character, i);
                    break;
                case NOT_IN_WORD:
                    this.notInWord.add(character);
                    break;
            }
        }
        this.alreadyGuessedWords.add(guess);
    }

    public boolean satisfies(String word) {
        if (alreadyGuessedWords.contains(word)) {
            return false;
        }
        char[] chars = word.toCharArray();
        for (char aChar : chars) {
            if (notInWord.contains(aChar)) {
                return false;
            }
        }
        for (Map.Entry<Character, Collection<Integer>> entry : requiredLetterWrongSpot.asMap().entrySet()) {
            // guarantees letter is in the word.
            Character requiredChar = entry.getKey();
            boolean found = false;
            for (char inWord : chars) {
                if (inWord == requiredChar) {
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
            // guarantees the letter is not at a disallowed location
            Collection<Integer> disallowedLocations = entry.getValue();
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == requiredChar && disallowedLocations.contains(i)) {
                    return false;
                }
            }
        }
        for (Map.Entry<Integer, Character> entry : requiredLocations.entrySet()) {
            if (chars[entry.getKey()] != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static Outcome[] getOutcomes(String answer, String guess) {
        if (answer.length() != guess.length() || answer.length() == 0) {
            throw new IllegalArgumentException("mean");
        }
        Outcome[] outcomes = new Outcome[answer.length()];
        Set<Character> charsInAnswer = new HashSet<>();
        char[] answerChars = answer.toCharArray();
        for (int i = 0; i < answerChars.length; i++) {
            charsInAnswer.add(answerChars[i]);
        }
        char[] guessChars = guess.toCharArray();
        for (int i = 0; i < guessChars.length; i++) {
            if (answerChars[i] == guessChars[i]) {
                outcomes[i] = Outcome.CORRECT;
            } else if (charsInAnswer.contains(guessChars[i])) {
                outcomes[i] = Outcome.WRONG_SPOT;
            } else {
                outcomes[i] = Outcome.NOT_IN_WORD;
            }
        }
        return outcomes;
    }
}

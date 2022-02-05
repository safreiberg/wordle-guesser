package wordle.guesser.utilities;

import java.util.*;

public class KnownState {

    private final Map<Character, Set<Integer>> requiredLetterWrongSpot;
    private final Map<Integer, Character> requiredLocations;
    private final Set<Character> notInWord;
    private final Set<String> alreadyGuessedWords = new HashSet<>();

    public KnownState() {
        this.requiredLetterWrongSpot = new HashMap<>();
        this.requiredLocations = new HashMap<>();
        this.notInWord = new HashSet<>();
    }

    public boolean isEmpty() {
        return requiredLetterWrongSpot.isEmpty() && requiredLocations.isEmpty()
                && notInWord.isEmpty() && alreadyGuessedWords.isEmpty();
    }

    public KnownState deepCopy() {
        KnownState copy = new KnownState();
        copy.notInWord.addAll(notInWord);
        copy.requiredLocations.putAll(requiredLocations);
        copy.alreadyGuessedWords.addAll(alreadyGuessedWords);
        for (Map.Entry<Character, Set<Integer>> entry : requiredLetterWrongSpot.entrySet()) {
            copy.requiredLetterWrongSpot.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
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
                    this.requiredLetterWrongSpot.putIfAbsent(character, new HashSet<>());
                    this.requiredLetterWrongSpot.get(character).add(i);
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
        for (int i = 0; i < chars.length; i++) {
            Character character = chars[i];
            if (notInWord.contains(character)) {
                return false;
            }
            if (requiredLocations.containsKey(i) && requiredLocations.get(i) != character) {
                return false;
            }
            if (requiredLetterWrongSpot.containsKey(character) && requiredLetterWrongSpot.get(character).contains(i)) {
                return false;
            }
        }
        for (Character character : requiredLetterWrongSpot.keySet()) {
            boolean seen = false;
            for (Character inWord : chars) {
                if (inWord == character) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
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

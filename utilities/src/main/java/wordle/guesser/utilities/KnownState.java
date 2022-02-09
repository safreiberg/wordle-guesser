package wordle.guesser.utilities;

import java.util.*;

public class KnownState {

    private final Map<Character, Set<Integer>> requiredLetterWrongSpot;
    private final Character[] requiredLocations;
    private final Set<Character> notInWord;
    private final Set<String> alreadyGuessedWords = new HashSet<>();

    public KnownState() {
        this.requiredLetterWrongSpot = new HashMap<>(10);
        this.requiredLocations = new Character[5];
        this.notInWord = new HashSet<>(10);
    }

    public boolean isEmpty() {
        for (Character character : requiredLocations) {
            if (character != null) {
                return false;
            }
        }
        return requiredLetterWrongSpot.isEmpty()
                && notInWord.isEmpty()
                && alreadyGuessedWords.isEmpty();
    }

    public KnownState deepCopy() {
        KnownState copy = new KnownState();
        copy.notInWord.addAll(notInWord);
        System.arraycopy(requiredLocations, 0, copy.requiredLocations, 0, requiredLocations.length);
        copy.alreadyGuessedWords.addAll(alreadyGuessedWords);
        for (Map.Entry<Character, Set<Integer>> entry : requiredLetterWrongSpot.entrySet()) {
            copy.requiredLetterWrongSpot.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public Set<Character> requiredUnknown() {
        return requiredLetterWrongSpot.keySet();
    }

    public Character[] requiredLocations() {
        return requiredLocations;
    }

    public Set<Character> disallowed() {
        return notInWord;
    }

    public Collection<String> guessed() {
        return alreadyGuessedWords;
    }

    public enum Outcome {
        // grey
        NOT_IN_WORD,
        // yellow
        WRONG_SPOT,
        // green,
        CORRECT;
    }

    public void addGuess(String guess, Outcome... outcomes) {
        if (outcomes.length != guess.length()) {
            throw new IllegalArgumentException("gimme more");
        }
        for (int i = 0; i < guess.length(); i++) {
            char character = guess.charAt(i);
            switch (outcomes[i]) {
                case CORRECT:
                    this.requiredLocations[i] = character;
                    break;
                case WRONG_SPOT:
                    this.requiredLetterWrongSpot.putIfAbsent(character, new HashSet<>(10));
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
        return satisfiesIgnoreGuessed(word);
    }

    public boolean satisfiesOnlyKnownLocs(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Character character = chars[i];
            if (requiredLetterWrongSpot.containsKey(character) && requiredLetterWrongSpot.get(character).contains(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean satisfiesIgnoreGuessed(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Character character = chars[i];
            if (notInWord.contains(character)) {
                return false;
            }
            if (requiredLocations[i] != null && requiredLocations[i] != character) {
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
        char[] answerChars = answer.toCharArray();
        char[] guessChars = guess.toCharArray();
        for (int i = 0; i < guessChars.length; i++) {
            if (answerChars[i] == guessChars[i]) {
                outcomes[i] = Outcome.CORRECT;
            } else {
                boolean seen = false;
                for (char answerChar : answerChars) {
                    if (answerChar == guessChars[i]) {
                        seen = true;
                        break;
                    }
                }
                if (seen) {
                    outcomes[i] = Outcome.WRONG_SPOT;
                } else {
                    outcomes[i] = Outcome.NOT_IN_WORD;
                }
            }
        }
        return outcomes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnownState that = (KnownState) o;
        return Objects.equals(requiredLetterWrongSpot, that.requiredLetterWrongSpot) &&
                Arrays.equals(requiredLocations, that.requiredLocations) &&
                Objects.equals(notInWord, that.notInWord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requiredLetterWrongSpot, Arrays.hashCode(requiredLocations), notInWord);
    }

    @Override
    public String toString() {
        return "KnownState{" +
                "requiredLetterWrongSpot=" + requiredLetterWrongSpot +
                ", requiredLocations=" + Arrays.toString(requiredLocations) +
                ", notInWord=" + notInWord +
                ", alreadyGuessedWords=" + alreadyGuessedWords +
                '}';
    }
}

package wordle.guesser.utilities;

import javax.annotation.Nullable;

public interface Guesser {

    void determineFirstWords(Dictionary dictionary);

    void process(Dictionary dictionary, KnownState state);

    @Nullable
    String getBestGuess();

    String printState();
}

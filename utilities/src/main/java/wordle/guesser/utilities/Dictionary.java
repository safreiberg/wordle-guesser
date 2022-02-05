package wordle.guesser.utilities;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Dictionary {

    private static final String DICTIONARY_LOCATION = "/usr/share/dict/american-english";
    private final ImmutableSet<String> words;

    private final Supplier<Map<Character, Integer>> LETTER_COUNT_CACHE = Suppliers.memoize(this::aggregateLetterCountInternal);

    private Dictionary(ImmutableSet<String> words) {
        this.words = words;
    }

    public static Dictionary ofWords(Collection<String> words) {
        return new Dictionary(ImmutableSet.copyOf(words));
    }

    public static Dictionary parseFromDefaultLocation() {
        try {
            List<String> lines = FileUtils.readLines(new File(DICTIONARY_LOCATION), StandardCharsets.UTF_8);
            return new Dictionary(ImmutableSet.copyOf(lines));
        } catch (IOException e) {
            throw new RuntimeException("unable to load from dictionary", e);
        }
    }

    public static Dictionary defaultWordleDictionary() {
        return parseFromDefaultLocation().filterTo(new WordleFilter()).uppercase();
    }

    public Dictionary filterTo(Predicate<String> predicate) {
        return new Dictionary(ImmutableSet.copyOf(words.stream().filter(predicate).collect(Collectors.toSet())));
    }

    public Dictionary uppercase() {
        return new Dictionary(ImmutableSet.copyOf(words.stream().map(String::toUpperCase).collect(Collectors.toSet())));
    }

    public Dictionary filterToValid(KnownState known) {
        return new Dictionary(ImmutableSet.copyOf(words.stream().filter(known::satisfies).collect(Collectors.toSet())));
    }

    public int size() {
        return words.size();
    }

    public boolean contains(String word) {
        return words.contains(word);
    }

    public Map<Character, Integer> aggregateLetterCount() {
        return LETTER_COUNT_CACHE.get();
    }

    private Map<Character, Integer> aggregateLetterCountInternal() {
        Map<Character, Integer> map = new HashMap<>();
        for (String word : words) {
            for (char c : word.toCharArray()) {
                map.putIfAbsent(c, 0);
                map.put(c, map.get(c) + 1);
            }
        }
        return map;
    }

    public ImmutableSet<String> getWords() {
        return this.words;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dictionary that = (Dictionary) o;
        return Objects.equals(words, that.words);
    }

    @Override
    public int hashCode() {
        return Objects.hash(words);
    }

    @Override
    public String toString() {
        return "Dictionary{" +
                "words=" + words +
                '}';
    }
}
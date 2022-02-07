package wordle.guesser.utilities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.immutables.value.Value;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Dictionary {

    private static final String DICTIONARY_LOCATION = "/usr/share/dict/american-english";
    private static final String WORDLE_DICTIONARY_LOCATION = "/home/safreiberg/code/wordle-guesser/total-words.txt";
    private static final ImmutableSet<Character> ALPHABET = ImmutableSet.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z');
    private final Supplier<Map<Character, Integer>> letterCountCache =
            Suppliers.memoize(this::aggregateLetterCountInternal);
    private final LoadingCache<KnownState, Integer> filteredSizeCache = CacheBuilder.newBuilder()
            .recordStats()
            .initialCapacity(100_000)
            .concurrencyLevel(24)
            .build(new CacheLoader<>() {
                @Override
                public Integer load(KnownState known) {
                    return (int) prefilterWordsIgnoringWrongSpot(known)
                            .filter(known::satisfiesOnlyKnownLocs)
                            .count();
                }
            });
    private final ImmutableSet<String> words;
    private final ImmutableMap<Character, ImmutableSet<String>> wordsWithCharacter;
    private final ImmutableMap<Character, ImmutableSet<String>> wordsWithoutCharacter;
    private final ImmutableMap<CharacterAndLocation, ImmutableSet<String>> wordsWithSpecificLocationChars;

    @Value.Immutable
    interface CharacterAndLocation {
        @Value.Parameter
        Character getCharacter();
        @Value.Parameter
        Integer getLocation();
    }

    private Dictionary(ImmutableSet<String> words) {
        this.words = words;
        Map<Character, ImmutableSet.Builder<String>> withCharacterMapBuilder = new HashMap<>();
        Map<Character, ImmutableSet.Builder<String>> withoutCharacterMapBuilder = new HashMap<>();
        Map<CharacterAndLocation, ImmutableSet.Builder<String>> specificLocBldr = new HashMap<>();
        for (String word : words) {
            Set<Character> seen = new HashSet<>();
            for (int i = 0; i < word.length(); i++) {
                Character c = word.charAt(i);
                seen.add(c);
                withCharacterMapBuilder.putIfAbsent(c, ImmutableSet.builder());
                withCharacterMapBuilder.get(c).add(word);

                CharacterAndLocation charLoc = ImmutableCharacterAndLocation.of(c, i);
                specificLocBldr.putIfAbsent(charLoc, ImmutableSet.builder());
                specificLocBldr.get(charLoc).add(word);
            }
            for (Character unseen : Sets.difference(ALPHABET, seen)) {
                withoutCharacterMapBuilder.putIfAbsent(unseen, ImmutableSet.builder());
                withoutCharacterMapBuilder.get(unseen).add(word);
            }
        }
        ImmutableMap.Builder<Character, ImmutableSet<String>> withChar = ImmutableMap.builder();
        for (Map.Entry<Character, ImmutableSet.Builder<String>> entry : withCharacterMapBuilder.entrySet()) {
            withChar.put(entry.getKey(), entry.getValue().build());
        }
        wordsWithCharacter = withChar.build();

        ImmutableMap.Builder<Character, ImmutableSet<String>> withoutChar = ImmutableMap.builder();
        for (Map.Entry<Character, ImmutableSet.Builder<String>> entry : withoutCharacterMapBuilder.entrySet()) {
            withoutChar.put(entry.getKey(), entry.getValue().build());
        }
        wordsWithoutCharacter = withoutChar.build();

        ImmutableMap.Builder<CharacterAndLocation, ImmutableSet<String>> charLoc = ImmutableMap.builder();
        for (Map.Entry<CharacterAndLocation, ImmutableSet.Builder<String>> entry : specificLocBldr.entrySet()) {
            charLoc.put(entry.getKey(), entry.getValue().build());
        }
        wordsWithSpecificLocationChars = charLoc.build();
    }

    public static Dictionary ofWords(Collection<String> words) {
        return new Dictionary(ImmutableSet.copyOf(words));
    }

    public static Dictionary parseFromDefaultLocation() {
        return parseFrom(DICTIONARY_LOCATION);
    }

    private static Dictionary parseFrom(String file) {
        try {
            List<String> lines = FileUtils.readLines(new File(file), StandardCharsets.UTF_8);
            return new Dictionary(lines.stream()
                    .collect(ImmutableSet.toImmutableSet()));
        } catch (IOException e) {
            throw new RuntimeException("unable to load from dictionary", e);
        }
    }

    public static Dictionary wordle12k() {
        return parseFrom(WORDLE_DICTIONARY_LOCATION).uppercase();
    }

    public static Dictionary linuxDictionary() {
        return parseFromDefaultLocation().filterTo(new WordleFilter()).uppercase();
    }

    public Dictionary filterTo(Predicate<String> predicate) {
        return new Dictionary(ImmutableSet.copyOf(words.stream().filter(predicate).collect(Collectors.toSet())));
    }

    public Dictionary uppercase() {
        return new Dictionary(words.stream()
                .map(String::toUpperCase)
                .map(String::intern)
                .collect(ImmutableSet.toImmutableSet()));
    }

    @VisibleForTesting
    public Stream<String> prefilterWordsIgnoringWrongSpot(KnownState known) {
        Set<String> filtered = words;
        Map<Integer, Character> requiredLocations = known.requiredLocations();
        for (Map.Entry<Integer, Character> entry : requiredLocations.entrySet()) {
            Set<String> withLoc = Preconditions.checkNotNull(wordsWithSpecificLocationChars.get(
                    ImmutableCharacterAndLocation.of(entry.getValue(), entry.getKey())));
            if (filtered == words) {
                filtered = withLoc;
            } else {
                filtered = Sets.intersection(filtered, withLoc);
            }
        }
        Set<Character> requiredChars = known.requiredUnknown();
        for (Character required : requiredChars) {
            ImmutableSet<String> withReqChar = Preconditions.checkNotNull(wordsWithCharacter.get(required));
            if (filtered == words) {
                filtered = withReqChar;
            } else {
                filtered = Sets.intersection(filtered, withReqChar);
            }
        }
        Set<Character> disallowedChars = known.disallowed();
        for (Character disallowed : disallowedChars) {
            ImmutableSet<String> missingChar = wordsWithoutCharacter.get(disallowed);
            if (missingChar != null) {
                if (filtered == words) {
                    filtered = missingChar;
                } else {
                    filtered = Sets.intersection(filtered, missingChar);
                }
            }
        }
        return filtered.stream();
    }

    private static final ConcurrentMap<Set<String>, Dictionary> DICT_CACHE = new ConcurrentHashMap<>();

    public Dictionary filterToValid(KnownState known) {
        return new Dictionary(prefilterWordsIgnoringWrongSpot(known)
                .filter(known::satisfies)
                .collect(ImmutableSet.toImmutableSet()));
//        Dictionary cached = DICT_CACHE.get(words);
//        if (cached != null) {
//            return cached;
//        } else {
//            Dictionary uncached = new Dictionary(words);
//            DICT_CACHE.put(words, uncached);
//            return uncached;
//        }
    }

    public int sizeAfterFilteringIgnoringGuesses(KnownState known) {
        return filteredSizeCache.getUnchecked(known);
    }

    public int size() {
        return words.size();
    }

    public boolean contains(String word) {
        return words.contains(word);
    }

    public Map<Character, Integer> aggregateLetterCount() {
        return letterCountCache.get();
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

    public Set<String> getWords() {
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

package Controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TextGenerator {
    private static final String[] INITIAL_WORDS = {
            "the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog", "and", "cat",
            "runs", "fast", "slow", "big", "small", "house", "tree", "sky", "blue", "green",
            "red", "sun", "moon", "star", "cloud", "rain", "wind", "snow", "day", "night",
            "time", "way", "man", "woman", "child", "book", "pen", "paper", "car", "road",
            "city", "town", "water", "fire", "earth", "air", "life", "love", "work", "play",
            "home", "food", "good", "bad", "new", "old", "hot", "cold", "warm", "cool",
            "happy", "sad", "glad", "mad", "safe", "free", "rich", "poor", "true", "false",
            "long", "short", "high", "low", "deep", "wide", "near", "far", "light", "dark",
            "bright", "dull", "loud", "soft", "hard", "easy", "fun", "kind", "mean", "fair",
            "clean", "dirty", "wet", "dry", "full", "empty", "strong", "weak", "young", "old",
            "first", "last", "next", "past", "now", "soon", "late", "early", "today", "week",
            "year", "month", "hour", "minute", "second", "place", "thing", "idea", "word", "name",
            "face", "hand", "foot", "eye", "ear", "heart", "mind", "body", "soul", "friend",
            "family", "group", "team", "class", "school", "game", "sport", "ball", "race", "win",
            "lose", "try", "hope", "wish", "dream", "plan", "goal", "path", "step", "walk",
            "run", "jump", "fly", "swim", "dance", "sing", "talk", "say", "hear", "see",
            "feel", "think", "know", "learn", "grow", "make", "build", "break", "fix", "find",
            "look", "search", "hide", "show", "give", "take", "push", "pull", "lift", "drop",
            "eat", "drink", "cook", "bake", "sleep", "wake", "rest", "move", "stop", "start",
            "end", "begin", "change", "stay", "leave", "come", "go", "bring", "send", "keep",
            "apple", "bear", "bird", "cake", "chair", "desk", "door", "fish", "flower", "grass",
            "hill", "horse", "lake", "leaf", "mountain", "river", "rock", "sea", "ship", "stone",
            "table", "train", "valley", "wall", "wave", "wood", "world", "yard", "zone", "arm",
            "back", "chest", "head", "leg", "smile", "voice", "blood", "bone", "skin", "dream",
            "fear", "joy", "peace", "truth", "art", "song", "story", "poem", "dance", "music",
            "beauty", "faith", "trust", "honor", "pride", "strength", "wisdom", "courage", "hope",
            "calm", "clear", "fine", "great", "nice", "pure", "sweet", "tall", "thin", "vast",
            "wild", "bold", "brave", "cute", "fierce", "gentle", "neat", "quiet", "shiny", "smooth",
            "tiny", "warm", "wise", "busy", "idle", "keen", "sharp", "swift", "tight", "cool",
            "deep", "huge", "large", "small", "bright", "dark", "loud", "soft", "hard", "easy",
            "fast", "slow", "high", "low", "near", "far", "hot", "cold", "wet", "dry", "full",
            "empty", "true", "false", "good", "bad", "new", "old", "big", "small", "long", "short",
            "always", "never", "often", "rarely", "sometimes", "soon", "late", "early", "now", "then",
            "here", "there", "up", "down", "in", "out", "on", "off", "above", "below", "behind",
            "front", "left", "right", "inside", "outside", "around", "through", "across", "along"
    };

    private final Random random = new Random();
    private final List<String> words = new ArrayList<>();

    public TextGenerator() {
        // Load additional words from a resource file (if available)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getResourceAsStream("/data/input.txt")))) {
            String line;
            while (words.size() < 2000 && (line = reader.readLine()) != null) {
                String[] additionalWords = line.trim().split("\\s+");
                for (String word : additionalWords) {
                    if (!word.isEmpty() && words.size() < 2000) {
                        words.add(word);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String generateText() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            String word = words.get(random.nextInt(words.size()));
            text.append(word);
            if (i < 14) {
                text.append(" ");
            }
        }
        return text.toString();
    }
}
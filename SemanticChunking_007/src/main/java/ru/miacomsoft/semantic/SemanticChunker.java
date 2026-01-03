package ru.miacomsoft.semantic;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Использование:
 // Простое использование с ConfigLoader
 ConfigLoader configLoader = new ConfigLoader("application.properties");
 SemanticChunker chunker1 = new SemanticChunker(configLoader);

 // Расширенное использование
 SemanticChunker chunker2 = new SemanticChunker(
 configLoader,
 20,      // batchSize - пачки по 20 предложений
 true,    // useSlidingWindow - использовать скользящее окно
 5        // windowSize - размер окна 5 предложений
 );
 */
public class SemanticChunker {

    private final HttpClient httpClient;
    private final String ollamaBaseUrl;
    private final String embeddingModel;
    private final double similarityThreshold;
    private final int batchSize;
    private final boolean useSlidingWindow;
    private final int windowSize;
    private final Cache<String, float[]> embeddingCache;

    // Кэш для хранения эмбеддингов (LRU кэш с ограниченным размером)
    private static class Cache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public Cache(int maxSize) {
            super(maxSize * 3/4, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    /**
     * Конструктор с ConfigLoader
     */
    public SemanticChunker(ConfigLoader configLoader) {
        this(configLoader, 10, false, 3);
    }

    /**
     * Расширенный конструктор с ConfigLoader
     */
    public SemanticChunker(ConfigLoader configLoader, int batchSize, boolean useSlidingWindow, int windowSize) {
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaBaseUrl = configLoader.getOllamaUrl();
        this.embeddingModel = configLoader.getProperties().getProperty("rag.embedding.model", "all-minilm:22m");
        this.similarityThreshold = Double.parseDouble(
                configLoader.getProperties().getProperty("rag.similarity.threshold", "0.7"));
        this.batchSize = batchSize;
        this.useSlidingWindow = useSlidingWindow;
        this.windowSize = windowSize;
        this.embeddingCache = new Cache<>(1000); // Кэш на 1000 предложений
    }

    /**
     * Конструктор для обратной совместимости
     */
    public SemanticChunker(String ollamaBaseUrl, String embeddingModel, double similarityThreshold) {
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
        this.batchSize = 10;
        this.useSlidingWindow = false;
        this.windowSize = 3;
        this.embeddingCache = new Cache<>(1000);
    }

    /**
     * Конструктор для обратной совместимости
     */
    public SemanticChunker(String ollamaBaseUrl, String embeddingModel, double similarityThreshold,
                           int batchSize, boolean useSlidingWindow, int windowSize) {
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
        this.batchSize = batchSize;
        this.useSlidingWindow = useSlidingWindow;
        this.windowSize = windowSize;
        this.embeddingCache = new Cache<>(1000);
    }

    /**
     * Основной метод для семантического разделения текста
     * Возвращает список объектов Chunk с текстом и эмбеддингом
     */
    public List<Chunk> semanticChunking(String text, int maxChunkSize) throws Exception {
        // 1. Разбиваем текст на предложения (улучшенная версия)
        List<String> sentences = splitIntoSentencesAdvanced(text);

        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Получаем эмбеддинги для всех предложений (пакетная обработка с кэшированием)
        List<float[]> embeddings = getEmbeddingsBatchWithCache(sentences);

        // 3. Выполняем семантическое группирование (с улучшенным алгоритмом)
        return useSlidingWindow
                ? groupSentencesWithSlidingWindow(sentences, embeddings, maxChunkSize)
                : groupSentencesWithHierarchicalClustering(sentences, embeddings, maxChunkSize);
    }

    /**
     * Улучшенное разделение текста на предложения
     * Обрабатывает сокращения, инициалы, цифры с точками и т.д.
     */
    private List<String> splitIntoSentencesAdvanced(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        // Убираем лишние пробелы и переносы строк
        text = text.replaceAll("\\s+", " ").trim();

        // Паттерн для разделения предложений, учитывающий сокращения
        // Более сложный паттерн для обработки различных случаев
        String regex = "(?<![A-Z][a-z]\\.)(?<![A-Z][a-z][a-z]\\.)(?<![A-Z]\\.)(?<![Мм]г\\.)(?<![Вв]т\\.)(?<![Дд]р\\.)(?<![Пп]р\\.)"
                + "(?<![Тт]\\.[А-Я])(?<![0-9]\\.)(?<=[.!?])\\s+(?=[А-ЯA-Z\"«(])";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.start()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }

        // Добавляем последнее предложение
        String lastSentence = text.substring(lastEnd).trim();
        if (!lastSentence.isEmpty()) {
            sentences.add(lastSentence);
        }

        // Если паттерн не сработал, используем простой fallback
        if (sentences.isEmpty()) {
            return splitIntoSentencesSimple(text);
        }

        return sentences;
    }

    /**
     * Простое разделение на предложения (fallback)
     */
    private List<String> splitIntoSentencesSimple(String text) {
        List<String> sentences = new ArrayList<>();
        String[] rawSentences = text.split("(?<=[.!?])\\s+");

        for (String sentence : rawSentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    /**
     * Пакетное получение эмбеддингов с использованием кэша
     */
    private List<float[]> getEmbeddingsBatchWithCache(List<String> sentences) throws Exception {
        List<float[]> embeddings = new ArrayList<>(sentences.size());
        List<String> uncachedSentences = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();

        // Проверяем кэш
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            float[] cachedEmbedding = embeddingCache.get(sentence);

            if (cachedEmbedding != null) {
                embeddings.add(cachedEmbedding);
            } else {
                embeddings.add(null); // Заполнитель
                uncachedSentences.add(sentence);
                uncachedIndices.add(i);
            }
        }

        // Получаем эмбеддинги для некэшированных предложений пачками
        if (!uncachedSentences.isEmpty()) {
            List<float[]> uncachedEmbeddings = getEmbeddingsBatch(uncachedSentences);

            // Заполняем результаты и кэшируем
            for (int i = 0; i < uncachedEmbeddings.size(); i++) {
                int originalIndex = uncachedIndices.get(i);
                float[] embedding = uncachedEmbeddings.get(i);

                embeddings.set(originalIndex, embedding);
                embeddingCache.put(uncachedSentences.get(i), embedding);
            }
        }

        return embeddings;
    }

    /**
     * Пакетное получение эмбеддингов через Ollama API
     * Ollama не поддерживает массив prompts, поэтому отправляем по одному
     */
    private List<float[]> getEmbeddingsBatch(List<String> sentences) throws Exception {
        List<float[]> embeddings = new ArrayList<>();

        // Обрабатываем предложения пачками для управления скоростью запросов
        for (int i = 0; i < sentences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sentences.size());
            List<String> batch = sentences.subList(i, end);

            // Для каждого предложения в пачке получаем эмбеддинг отдельно
            for (String sentence : batch) {
                float[] embedding = getSingleEmbedding(sentence);
                embeddings.add(embedding);

                // Небольшая задержка между запросами в одной пачке
                if (batch.indexOf(sentence) < batch.size() - 1) {
                    Thread.sleep(20);
                }
            }

            // Небольшая задержка между пачками
            if (end < sentences.size()) {
                Thread.sleep(50);
            }
        }

        return embeddings;
    }

    /**
     * Получает эмбеддинг для одного текста через Ollama API
     */
    private float[] getSingleEmbedding(String text) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", embeddingModel);
        requestBody.put("prompt", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaBaseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при получении эмбеддинга: " + response.body());
        }

        JSONObject responseJson = new JSONObject(response.body());
        JSONArray embeddingArray = responseJson.getJSONArray("embedding");

        float[] embedding = new float[embeddingArray.length()];
        for (int i = 0; i < embeddingArray.length(); i++) {
            embedding[i] = (float) embeddingArray.getDouble(i);
        }

        return embedding;
    }

    /**
     * Получает эмбеддинг для одного текста через Ollama API (для обратной совместимости)
     */
    public float[] getEmbedding(String text) throws Exception {
        // Проверяем кэш
        float[] cached = embeddingCache.get(text);
        if (cached != null) {
            return cached.clone();
        }
        float[] embedding = getSingleEmbedding(text);
        // Кэшируем результат
        embeddingCache.put(text, embedding.clone());
        return embedding;
    }

    /**
     * Вычисляет косинусное сходство между двумя векторами
     */
    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Векторы должны иметь одинаковую размерность");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Группирует предложения с использованием скользящего окна
     */
    private List<Chunk> groupSentencesWithSlidingWindow(List<String> sentences,
                                                        List<float[]> embeddings,
                                                        int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        int i = 0;
        while (i < sentences.size()) {
            // Определяем границы окна
            int windowEnd = Math.min(i + windowSize, sentences.size());
            List<String> windowSentences = sentences.subList(i, windowEnd);
            List<float[]> windowEmbeddings = embeddings.subList(i, windowEnd);

            // Вычисляем среднее сходство в окне
            double avgSimilarity = calculateAverageSimilarity(windowEmbeddings);

            // Определяем, где закончить чанк
            int chunkEnd = i + 1;
            if (avgSimilarity >= similarityThreshold) {
                // Расширяем чанк
                chunkEnd = findOptimalChunkEnd(sentences, embeddings, i, maxChunkSize);
            }

            // Создаем чанк
            List<String> chunkSentences = sentences.subList(i, chunkEnd);
            List<float[]> chunkEmbeddings = embeddings.subList(i, chunkEnd);

            String chunkText = String.join(" ", chunkSentences);
            float[] chunkEmbedding = calculateAverageEmbedding(chunkEmbeddings);
            chunks.add(new Chunk(chunkText, chunkEmbedding, i));

            i = chunkEnd;
        }

        return chunks;
    }

    /**
     * Иерархическая кластеризация предложений
     */
    private List<Chunk> groupSentencesWithHierarchicalClustering(List<String> sentences,
                                                                 List<float[]> embeddings,
                                                                 int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();

        if (sentences.size() <= 1) {
            if (!sentences.isEmpty()) {
                String chunkText = sentences.get(0);
                float[] chunkEmbedding = embeddings.get(0);
                chunks.add(new Chunk(chunkText, chunkEmbedding, 0));
            }
            return chunks;
        }

        // Строим матрицу сходства
        double[][] similarityMatrix = new double[sentences.size()][sentences.size()];
        for (int i = 0; i < sentences.size(); i++) {
            for (int j = i + 1; j < sentences.size(); j++) {
                double similarity = cosineSimilarity(embeddings.get(i), embeddings.get(j));
                similarityMatrix[i][j] = similarity;
                similarityMatrix[j][i] = similarity;
            }
        }

        // Иерархическая кластеризация (упрощенная версия)
        List<List<Integer>> clusters = performHierarchicalClustering(similarityMatrix, similarityThreshold);

        // Формируем чанки из кластеров
        for (List<Integer> cluster : clusters) {
            if (cluster.isEmpty()) continue;

            Collections.sort(cluster);
            List<String> clusterSentences = new ArrayList<>();
            List<float[]> clusterEmbeddings = new ArrayList<>();

            for (int idx : cluster) {
                clusterSentences.add(sentences.get(idx));
                clusterEmbeddings.add(embeddings.get(idx));
            }

            // Разбиваем большие кластеры на чанки по размеру
            List<List<String>> sizedChunks = splitBySize(clusterSentences, maxChunkSize);
            List<List<float[]>> sizedEmbeddings = splitEmbeddingsByIndices(clusterEmbeddings, sizedChunks);

            for (int j = 0; j < sizedChunks.size(); j++) {
                String chunkText = String.join(" ", sizedChunks.get(j));
                float[] chunkEmbedding = calculateAverageEmbedding(sizedEmbeddings.get(j));
                chunks.add(new Chunk(chunkText, chunkEmbedding, cluster.get(0) + j));
            }
        }

        // Сортируем чанки по позиции
        chunks.sort(Comparator.comparingInt(Chunk::getPosition));

        return chunks;
    }

    /**
     * Упрощенная иерархическая кластеризация
     */
    private List<List<Integer>> performHierarchicalClustering(double[][] similarityMatrix, double threshold) {
        List<List<Integer>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[similarityMatrix.length];

        for (int i = 0; i < similarityMatrix.length; i++) {
            if (!assigned[i]) {
                List<Integer> cluster = new ArrayList<>();
                cluster.add(i);
                assigned[i] = true;

                // Ищем похожие предложения
                for (int j = i + 1; j < similarityMatrix.length; j++) {
                    if (!assigned[j] && similarityMatrix[i][j] >= threshold) {
                        cluster.add(j);
                        assigned[j] = true;
                    }
                }

                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * Вычисляет среднее сходство в списке эмбеддингов
     */
    private double calculateAverageSimilarity(List<float[]> embeddings) {
        if (embeddings.size() <= 1) {
            return 1.0;
        }

        double totalSimilarity = 0.0;
        int comparisons = 0;

        for (int i = 0; i < embeddings.size(); i++) {
            for (int j = i + 1; j < embeddings.size(); j++) {
                totalSimilarity += cosineSimilarity(embeddings.get(i), embeddings.get(j));
                comparisons++;
            }
        }

        return comparisons > 0 ? totalSimilarity / comparisons : 0.0;
    }

    /**
     * Находит оптимальный конец чанка
     */
    private int findOptimalChunkEnd(List<String> sentences, List<float[]> embeddings,
                                    int start, int maxChunkSize) {
        int currentLength = 0;
        double lastSimilarity = 1.0;

        for (int i = start; i < sentences.size(); i++) {
            currentLength += sentences.get(i).length();

            // Проверяем максимальный размер
            if (currentLength > maxChunkSize && i > start) {
                return i;
            }

            // Проверяем сходство с предыдущим предложением
            if (i > start) {
                double similarity = cosineSimilarity(embeddings.get(i-1), embeddings.get(i));
                if (similarity < similarityThreshold) {
                    return i;
                }
                lastSimilarity = similarity;
            }
        }

        return sentences.size();
    }

    /**
     * Разбивает список предложений по размеру
     */
    private List<List<String>> splitBySize(List<String> sentences, int maxChunkSize) {
        List<List<String>> result = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        int currentLength = 0;

        for (String sentence : sentences) {
            int sentenceLength = sentence.length() + (currentChunk.isEmpty() ? 0 : 1);

            if (currentLength + sentenceLength > maxChunkSize && !currentChunk.isEmpty()) {
                result.add(new ArrayList<>(currentChunk));
                currentChunk.clear();
                currentLength = 0;
            }

            currentChunk.add(sentence);
            currentLength += sentenceLength;
        }

        if (!currentChunk.isEmpty()) {
            result.add(currentChunk);
        }

        return result;
    }

    /**
     * Разбивает эмбеддинги по тем же индексам, что и предложения
     */
    private List<List<float[]>> splitEmbeddingsByIndices(List<float[]> embeddings,
                                                         List<List<String>> sentenceChunks) {
        List<List<float[]>> result = new ArrayList<>();
        int currentIndex = 0;

        for (List<String> chunk : sentenceChunks) {
            List<float[]> embeddingChunk = new ArrayList<>();
            for (int i = 0; i < chunk.size(); i++) {
                embeddingChunk.add(embeddings.get(currentIndex + i));
            }
            result.add(embeddingChunk);
            currentIndex += chunk.size();
        }

        return result;
    }

    /**
     * Вычисляет средний эмбеддинг для группы предложений
     */
    private float[] calculateAverageEmbedding(List<float[]> embeddings) {
        if (embeddings.isEmpty()) return new float[0];

        int dimensions = embeddings.get(0).length;
        float[] average = new float[dimensions];

        for (float[] embedding : embeddings) {
            for (int j = 0; j < dimensions; j++) {
                average[j] += embedding[j];
            }
        }

        for (int j = 0; j < dimensions; j++) {
            average[j] /= embeddings.size();
        }

        return average;
    }

    /**
     * Класс для представления чанка с текстом, эмбеддингом и позицией
     */
    public static class Chunk {
        private final String text;
        private final float[] embedding;
        private final int position;

        public Chunk(String text, float[] embedding, int position) {
            this.text = text;
            this.embedding = embedding;
            this.position = position;
        }

        public String getText() { return text; }
        public float[] getEmbedding() { return embedding; }
        public int getPosition() { return position; }
        public int getLength() { return text.length(); }

        @Override
        public String toString() {
            return String.format("Chunk[position=%d, length=%d characters]\n%s\n",
                    position, text.length(), text);
        }

        /**
         * Метод для получения полной информации о чанке
         */
        public String getFullInfo() {
            return String.format("=== CHUNK INFO ===\nPosition: %d\nLength: %d characters\nText:\n%s\n",
                    position, text.length(), text);
        }
    }

    /**
     * Метод для очистки кэша
     */
    public void clearCache() {
        embeddingCache.clear();
    }

    /**
     * Метод для получения статистики кэша
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", embeddingCache.size());
        stats.put("maxSize", 1000);
        stats.put("hitRate", "N/A"); // Для точной статистики нужен счетчик попаданий
        return stats;
    }

    /**
     * Получает порог схожести
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
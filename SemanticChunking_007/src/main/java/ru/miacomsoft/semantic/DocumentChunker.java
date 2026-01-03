package ru.miacomsoft.semantic;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DocumentChunker {

    private final ConfigLoader configLoader;
    private final Properties properties;
    private final String dbUrl;
    private final String username;
    private final String password;
    private final SemanticChunker semanticChunker;

    // Добавляем отдельные параметры для удобства
    private final String dbHost;
    private final String dbPort;
    private final String dbName;

    public DocumentChunker() {
        this((ConfigLoader) null);
    }

    public DocumentChunker(ConfigLoader configLoader) {
        this(configLoader, null);
    }

    public DocumentChunker(ConfigLoader configLoader, SemanticChunker semanticChunker) {
        if (configLoader == null) {
            this.configLoader = new ConfigLoader();
        } else {
            this.configLoader = configLoader;
        }

        this.properties = this.configLoader.getProperties();
        this.dbUrl = this.configLoader.getDbUrl();
        this.username = this.configLoader.getDbUsername();
        this.password = this.configLoader.getDbPassword();

        // Получаем отдельные параметры
        this.dbHost = this.configLoader.getDbHost();
        this.dbPort = this.configLoader.getDbPort();
        this.dbName = this.configLoader.getDbName();

        // Инициализируем SemanticChunker если не передан
        if (semanticChunker == null) {
            this.semanticChunker = new SemanticChunker(configLoader);
        } else {
            this.semanticChunker = semanticChunker;
        }

        ensureDatabaseExists();
        initializeDatabase();
    }

    /**
     * Конструктор для обратной совместимости
     */
    public DocumentChunker(String configFile) {
        ConfigLoader loader = new ConfigLoader(configFile);
        this.configLoader = loader;
        this.properties = loader.getProperties();
        this.dbUrl = loader.getDbUrl();
        this.username = loader.getDbUsername();
        this.password = loader.getDbPassword();
        this.dbHost = loader.getDbHost();
        this.dbPort = loader.getDbPort();
        this.dbName = loader.getDbName();
        this.semanticChunker = new SemanticChunker(loader);

        ensureDatabaseExists();
        initializeDatabase();
    }

    public void ensureDatabaseExists() {
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection tempConn = DriverManager.getConnection(
                String.format("jdbc:postgresql://%s:%s/postgres", dbHost, dbPort),
                dbParams);
             Statement stmt = tempConn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
            if (!rs.next()) {
                stmt.executeUpdate("CREATE DATABASE " + dbName);
                System.out.println("База данных " + dbName + " успешно создана");
            } else {
                System.out.println("База данных " + dbName + " уже существует");
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при создании базы данных: " + e.getMessage());
            System.err.println("URL для подключения: jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres");
        }
    }

    /**
     * Инициализация базы данных PostgreSQL с расширением pg_vector
     */
    private void initializeDatabase() {
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams);
             Statement stmt = conn.createStatement()) {

            // Проверяем тип базы данных
            String dbProductName = conn.getMetaData().getDatabaseProductName();

            if (dbProductName.equalsIgnoreCase("PostgreSQL")) {
                // PostgreSQL с pg_vector
                initializePostgreSQL(conn);
            } else if (dbProductName.equalsIgnoreCase("SQLite")) {
                // SQLite
                initializeSQLite(conn);
            } else {
                throw new SQLException("Unsupported database: " + dbProductName);
            }

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            System.err.println("URL: " + dbUrl);
        }
    }

    private void initializePostgreSQL(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Проверяем наличие расширения vector
            try {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                System.out.println("Расширение vector доступно");
            } catch (SQLException e) {
                System.err.println("Ошибка при проверке расширения vector: " + e.getMessage());
            }

            // Создаем таблицы для PostgreSQL
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS documents (
                            id BIGSERIAL PRIMARY KEY,
                            content TEXT NOT NULL,
                            metadata JSONB,
                            client_id VARCHAR(255),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS embeddings (
                            id BIGSERIAL PRIMARY KEY,
                            document_id BIGINT REFERENCES documents(id) ON DELETE CASCADE,
                            embedding vector(384) NOT NULL,
                            embedding_norm DOUBLE PRECISION,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE(document_id)
                        )
                    """);

            // Создаем индексы
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_documents_client_id ON documents(client_id)");
            stmt.execute("""
                        CREATE INDEX IF NOT EXISTS idx_embeddings_embedding_ivfflat 
                        ON embeddings USING ivfflat (embedding vector_cosine_ops)
                        WITH (lists = 100)
                    """);

            System.out.println("PostgreSQL database initialized successfully with pg_vector support");
            System.out.println("Database URL: " + dbUrl);
        }
    }

    private void initializeSQLite(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Создаем таблицы для SQLite
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS documents (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            content TEXT NOT NULL,
                            metadata TEXT,
                            client_id TEXT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.execute("""
                        CREATE VIRTUAL TABLE IF NOT EXISTS embeddings USING vec0(
                            embedding float[384]
                        )
                    """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_documents_client_id ON documents(client_id)");

            System.out.println("SQLite database initialized successfully");
        }
    }

    /**
     * Класс для представления результата поиска похожих документов
     */
    public static class SimilarDocument {
        private final Long id;
        private final String content;
        private final String metadata;
        private final double similarity;
        private final float[] embedding;

        public SimilarDocument(Long id, String content, String metadata, double similarity, float[] embedding) {
            this.id = id;
            this.content = content;
            this.metadata = metadata;
            this.similarity = similarity;
            this.embedding = embedding;
        }

        public Long getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public String getMetadata() {
            return metadata;
        }

        public double getSimilarity() {
            return similarity;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        @Override
        public String toString() {
            return String.format("=== ДОКУМЕНТ (ID: %d, схожесть: %.3f) ===\n%s\nМетаданные: %s\n",
                    id, similarity, content, metadata);
        }
    }

    /**
     * Добавление документа в базу данных с эмбеддингом
     */
    public int addDocument(String content, JSONObject metadata, String clientId, float[] embedding) {
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams)) {
            conn.setAutoCommit(false);

            // Проверяем наличие дубликата
            if (isDuplicate(conn, content)) {
                System.out.println("Документ уже существует, пропускаем добавление");
                return -1;
            }

            // Добавляем clientId в метаданные
            if (metadata == null) {
                metadata = new JSONObject();
            }
            metadata.put("clientId", clientId);

            // Сохраняем документ в базу
            Long documentId = saveDocument(conn, content, metadata.toString(), clientId);

            if (documentId != null && embedding.length > 0) {
                // Сохраняем эмбеддинг
                saveEmbedding(conn, documentId, embedding);
                conn.commit();
                System.out.println("Документ успешно добавлен с ID: " + documentId);
                return documentId.intValue();
            } else {
                conn.rollback();
                System.out.println("Ошибка при добавлении документа");
                return -1;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении документа: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Добавление документа с автоматическим семантическим чанкингом
     */
    public int addDocumentWithChunking(String content, String clientId, String sourceFileName,
                                       int maxChunkSize) throws Exception {
        // Выполняем семантическое чанкинг документа
        List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(content, maxChunkSize);

        // Добавляем чанки в базу данных
        addDocuments(chunks, clientId, sourceFileName);

        return chunks.size();
    }

    /**
     * Проверка наличия дубликата документа
     */
    private boolean isDuplicate(Connection conn, String content) throws SQLException {
        String sql = "SELECT COUNT(*) FROM documents WHERE content = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, content);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Сохранение документа в базу данных
     */
    private Long saveDocument(Connection conn, String content, String metadata, String clientId) throws SQLException {
        String sql = "INSERT INTO documents (content, metadata, client_id) VALUES (?, ?::jsonb, ?) RETURNING id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, content);
            pstmt.setString(2, metadata);
            pstmt.setString(3, clientId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    /**
     * Сохранение эмбеддинга в базу данных
     */
    private void saveEmbedding(Connection conn, Long documentId, float[] embedding) throws SQLException {
        String sql = "INSERT INTO embeddings (document_id, embedding, embedding_norm) VALUES (?, ?::vector, ?)";

        // Вычисляем норму вектора
        double norm = 0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, documentId);
            pstmt.setString(2, embeddingToPgVectorString(embedding));
            pstmt.setDouble(3, norm);
            pstmt.executeUpdate();
        }
    }

    /**
     * Массовое добавление документов с эмбеддингами
     */
    public void addDocuments(List<SemanticChunker.Chunk> chunks, String clientId, String sourceFileName) {
        int count = 0;
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams)) {
            conn.setAutoCommit(false);

            for (SemanticChunker.Chunk chunk : chunks) {
                try {
                    JSONObject metadata = new JSONObject();
                    metadata.put("source", sourceFileName);
                    metadata.put("chunkType", "semantic");
                    count += 1;
                    System.out.print(chunks.size() + " " + count + ": ");

                    // Проверяем дубликат
                    if (!isDuplicate(conn, chunk.getText())) {
                        Long documentId = saveDocument(conn, chunk.getText(), metadata.toString(), clientId);
                        if (documentId != null) {
                            saveEmbedding(conn, documentId, chunk.getEmbedding());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при обработке чанка: " + e.getMessage());
                }
            }

            conn.commit();
            System.out.println("Добавлено " + count + " документов");

        } catch (SQLException e) {
            System.err.println("Ошибка при массовом добавлении документов: " + e.getMessage());
        }
    }

    /**
     * Получение количества документов в базе
     */
    public int getDocumentCount() {
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM documents")) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении количества документов: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Получение контекстных документов для списка чанков запроса
     */
    public List<SimilarDocument> getContextDocuments(List<SemanticChunker.Chunk> chunksQuery, String clientId, int maxCountDocFromBD) {
        return getContextDocuments(chunksQuery, clientId, maxCountDocFromBD, configLoader.getSimilarityThreshold());
    }

    /**
     * Получение контекстных документов для списка чанков запроса с указанием порога схожести
     */
    public List<SimilarDocument> getContextDocuments(List<SemanticChunker.Chunk> chunksQuery, String clientId,
                                                     int maxCountDocFromBD, double similarityThreshold) {
        List<SimilarDocument> contextDocuments = new ArrayList<>();
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams)) {
            System.out.println("Поиск контекстных документов для " + chunksQuery.size() + " чанков запроса");
            System.out.println("Порог схожести: " + similarityThreshold);

            for (SemanticChunker.Chunk chunk : chunksQuery) {
                System.out.println("Обработка чанка: " +
                        (chunk.getText().length() > 50 ? chunk.getText().substring(0, 50) + "..." : chunk.getText()));

                // Получаем документы для текущего чанка
                List<SimilarDocument> similarDocs = findSimilarDocuments(conn, chunk.getEmbedding(),
                        clientId, maxCountDocFromBD, similarityThreshold);

                System.out.println("Найдено документов для этого чанка: " + similarDocs.size());
                contextDocuments.addAll(similarDocs);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при получении контекстных документов: " + e.getMessage());
        }

        // Удаляем дубликаты по ID документа
        return removeDuplicates(contextDocuments);
    }

    /**
     * Получение контекстных документов для текста с автоматическим чанкингом
     */
    public List<SimilarDocument> getContextDocumentsForText(String text, String clientId, int maxChunkSize, int maxCountDocFromBD) throws Exception {
        System.out.println("\n=== Поиск контекстных документов для запроса ===");
        System.out.println("Запрос: " + text);

        // Выполняем семантическое чанкинг запроса
        List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(text, maxChunkSize);
        System.out.println("Запрос разбит на " + chunks.size() + " семантических чанков");

        // Получаем контекстные документы
        return getContextDocuments(chunks, clientId, maxCountDocFromBD);
    }

    /**
     * Поиск похожих документов в базе данных для заданного эмбеддинга
     */
    private List<SimilarDocument> findSimilarDocuments(Connection conn, float[] embedding,
                                                       String clientId, int topK, double threshold) throws SQLException {
        List<SimilarDocument> similarDocuments = new ArrayList<>();

        // Используем более эффективный запрос с косинусным сходством
        String sql = """
                    SELECT 
                        d.id, 
                        d.content, 
                        d.metadata,
                        e.embedding,
                        (1 - (e.embedding <=> ?::vector)) as similarity
                    FROM embeddings e
                    JOIN documents d ON e.document_id = d.id
                    WHERE d.client_id = ?
                    ORDER BY e.embedding <=> ?::vector
                    LIMIT ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String embeddingStr = embeddingToPgVectorString(embedding);

            pstmt.setString(1, embeddingStr);
            pstmt.setString(2, clientId);
            pstmt.setString(3, embeddingStr);
            pstmt.setInt(4, topK * 3); // Берем больше, чтобы потом отфильтровать

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String content = rs.getString("content");
                    String metadata = rs.getString("metadata");
                    double similarity = rs.getDouble("similarity");

                    // Фильтруем по порогу
                    if (similarity < threshold) {
                        continue;
                    }

                    // Получаем embedding как массив float
                    String embeddingArrayStr = rs.getString("embedding");
                    float[] embeddingArray = parsePgVectorString(embeddingArrayStr);

                    SimilarDocument similarDoc = new SimilarDocument(id, content, metadata, similarity, embeddingArray);
                    similarDocuments.add(similarDoc);

                    // Если набрали достаточно документов, выходим
                    if (similarDocuments.size() >= topK) {
                        break;
                    }
                }
            }
        }

        return similarDocuments;
    }

    /**
     * Удаляет дубликаты документов по ID
     */
    private List<SimilarDocument> removeDuplicates(List<SimilarDocument> documents) {
        List<SimilarDocument> uniqueDocs = new ArrayList<>();
        java.util.Set<Long> seenIds = new java.util.HashSet<>();

        for (SimilarDocument doc : documents) {
            if (!seenIds.contains(doc.getId())) {
                seenIds.add(doc.getId());
                uniqueDocs.add(doc);
            }
        }

        // Сортируем по схожести (от высокой к низкой)
        uniqueDocs.sort((d1, d2) -> Double.compare(d2.getSimilarity(), d1.getSimilarity()));

        return uniqueDocs;
    }

    /**
     * Конвертирует массив float в строку для PostgreSQL vector типа
     */
    private String embeddingToPgVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Парсит строку PostgreSQL vector в массив float
     */
    private float[] parsePgVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[0];
        }

        // Убираем квадратные скобки и разбиваем по запятым
        String cleanStr = vectorStr.replace("[", "").replace("]", "").trim();
        if (cleanStr.isEmpty()) {
            return new float[0];
        }

        String[] parts = cleanStr.split(",");
        float[] result = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }

        return result;
    }

    /**
     * Удаляет все документы для указанного client_id
     */
    public void clearDocuments(String clientId) {
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams);
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM documents WHERE client_id = ?")) {

            pstmt.setString(1, clientId);
            int deleted = pstmt.executeUpdate();
            System.out.println("Удалено " + deleted + " документов для client_id: " + clientId);

        } catch (SQLException e) {
            System.err.println("Ошибка при удалении документов: " + e.getMessage());
        }
    }

    /**
     * Получает экземпляр SemanticChunker
     */
    public SemanticChunker getSemanticChunker() {
        return semanticChunker;
    }

    /**
     * Метод для отладки: получает все документы для clientId
     */
    public List<SimilarDocument> getAllDocuments(String clientId, int limit) {
        List<SimilarDocument> documents = new ArrayList<>();
        Properties dbParams = new Properties();
        dbParams.setProperty("user", username);
        dbParams.setProperty("password", password);

        String sql = """
                    SELECT d.id, d.content, d.metadata
                    FROM documents d
                    WHERE d.client_id = ?
                    LIMIT ?
                """;

        try (Connection conn = DriverManager.getConnection(dbUrl, dbParams);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, clientId);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String content = rs.getString("content");
                    String metadata = rs.getString("metadata");

                    SimilarDocument doc = new SimilarDocument(id, content, metadata, 1.0, new float[0]);
                    documents.add(doc);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении документов: " + e.getMessage());
        }

        return documents;
    }

    /**
     * Метод для отладки: получает информацию о подключении к базе данных
     */
    public void printDatabaseInfo() {
        System.out.println("\n=== Database Connection Info ===");
        System.out.println("Host: " + dbHost);
        System.out.println("Port: " + dbPort);
        System.out.println("Database: " + dbName);
        System.out.println("URL: " + dbUrl);
        System.out.println("Username: " + username);
        System.out.println("==============================\n");
    }

    // =================== НОВЫЕ МЕТОДЫ ДЛЯ ГЕНЕРАЦИИ КОНТЕКСТА ===================

    /**
     * Генерация контекста для Ollama Chat на основе семантического поиска
     *
     * @param userQuery запрос пользователя
     * @param clientId идентификатор клиента
     * @param maxChunkSize максимальный размер чанка для запроса
     * @param maxContextDocuments максимальное количество контекстных документов
     * @param minSimilarity минимальный порог схожести (если null, используется из конфига)
     * @return форматированный контекст для Ollama Chat
     * @throws Exception
     */
    public String generateChatContext(String userQuery, String clientId,
                                      int maxChunkSize, int maxContextDocuments,
                                      Double minSimilarity) throws Exception {
        System.out.println("\n=== Генерация контекста для Ollama Chat ===");
        System.out.println("Запрос: " + userQuery);

        // Получаем контекстные документы
        List<SimilarDocument> contextDocs;
        if (minSimilarity != null) {
            // Выполняем семантическое чанкинг запроса
            List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(userQuery, maxChunkSize);
            contextDocs = getContextDocuments(chunks, clientId, maxContextDocuments, minSimilarity);
        } else {
            contextDocs = getContextDocumentsForText(userQuery, clientId, maxChunkSize, maxContextDocuments);
        }

        // Форматируем контекст
        return formatContextForChat(userQuery, contextDocs);
    }

    /**
     * Генерация контекста для Ollama Generation (генерация текста) на основе семантического поиска
     *
     * @param prompt промпт для генерации
     * @param clientId идентификатор клиента
     * @param maxChunkSize максимальный размер чанка для промпта
     * @param maxContextDocuments максимальное количество контекстных документов
     * @param minSimilarity минимальный порог схожести (если null, используется из конфига)
     * @return форматированный контекст для Ollama Generation
     * @throws Exception
     */
    public String generateGenerationContext(String prompt, String clientId,
                                            int maxChunkSize, int maxContextDocuments,
                                            Double minSimilarity) throws Exception {
        System.out.println("\n=== Генерация контекста для Ollama Generation ===");
        System.out.println("Промпт: " + prompt);

        // Получаем контекстные документы
        List<SimilarDocument> contextDocs;
        if (minSimilarity != null) {
            // Выполняем семантическое чанкинг промпта
            List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(prompt, maxChunkSize);
            contextDocs = getContextDocuments(chunks, clientId, maxContextDocuments, minSimilarity);
        } else {
            contextDocs = getContextDocumentsForText(prompt, clientId, maxChunkSize, maxContextDocuments);
        }

        // Форматируем контекст
        return formatContextForGeneration(prompt, contextDocs);
    }

    /**
     * Форматирование контекста для Ollama Chat
     */
    private String formatContextForChat(String userQuery, List<SimilarDocument> contextDocs) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("Ты - интеллектуальный ассистент, который использует предоставленный контекст для ответов.\n");
        contextBuilder.append("Контекст основан на семантическом поиске по базе знаний.\n\n");

        if (contextDocs.isEmpty()) {
            contextBuilder.append("Контекст не найден. Ответь, используя свои общие знания.\n");
            return contextBuilder.toString();
        }

        contextBuilder.append("=== КОНТЕКСТ ДЛЯ ОТВЕТА ===\n");
        contextBuilder.append("Запрос пользователя: ").append(userQuery).append("\n\n");
        contextBuilder.append("Найденные релевантные документы из базы знаний:\n\n");

        for (int i = 0; i < contextDocs.size(); i++) {
            SimilarDocument doc = contextDocs.get(i);
            contextBuilder.append("Документ ").append(i + 1).append(" (схожесть: ")
                    .append(String.format("%.2f", doc.getSimilarity())).append("):\n");
            contextBuilder.append(doc.getContent()).append("\n\n");

            // Добавляем метаданные если они есть
            if (doc.getMetadata() != null && !doc.getMetadata().isEmpty() && !doc.getMetadata().equals("null")) {
                try {
                    JSONObject metadata = new JSONObject(doc.getMetadata());
                    if (metadata.length() > 0) {
                        contextBuilder.append("Метаданные: ").append(metadata.toString(2)).append("\n");
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки парсинга метаданных
                }
            }
            contextBuilder.append("---\n\n");
        }

        contextBuilder.append("=== ИНСТРУКЦИЯ ===\n");
        contextBuilder.append("Используй предоставленный контекст для формирования точного и информативного ответа.\n");
        contextBuilder.append("Если в контексте нет нужной информации, так и скажи, но попробуй дать максимально полезный ответ.\n");
        contextBuilder.append("Ссылайся на документы из контекста, если это уместно.\n");
        contextBuilder.append("Отвечай на русском языке, если вопрос был на русском.\n");

        return contextBuilder.toString();
    }

    /**
     * Форматирование контекста для Ollama Generation
     */
    private String formatContextForGeneration(String prompt, List<SimilarDocument> contextDocs) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("Сгенерируй текст на основе предоставленного контекста.\n");
        contextBuilder.append("Контекст основан на семантическом поиске по базе знаний.\n\n");

        if (contextDocs.isEmpty()) {
            contextBuilder.append("Контекст не найден. Используй свои общие знания для генерации.\n");
            contextBuilder.append("Промпт: ").append(prompt).append("\n");
            return contextBuilder.toString();
        }

        contextBuilder.append("=== ИСХОДНЫЙ ПРОМПТ ===\n");
        contextBuilder.append(prompt).append("\n\n");
        contextBuilder.append("=== РЕЛЕВАНТНЫЙ КОНТЕКСТ ===\n\n");

        for (int i = 0; i < contextDocs.size(); i++) {
            SimilarDocument doc = contextDocs.get(i);
            contextBuilder.append("[Источник ").append(i + 1).append(", схожесть: ")
                    .append(String.format("%.2f", doc.getSimilarity())).append("]\n");
            contextBuilder.append(doc.getContent()).append("\n\n");
        }

        contextBuilder.append("=== ИНСТРУКЦИЯ ДЛЯ ГЕНЕРАЦИИ ===\n");
        contextBuilder.append("Используй предоставленный контекст для генерации текста.\n");
        contextBuilder.append("Сохраняй стиль и терминологию из контекста.\n");
        contextBuilder.append("Если контекст содержит противоречивую информацию, постарайся её синтезировать.\n");
        contextBuilder.append("Сгенерируй текст, который непосредственно отвечает на промпт, используя информацию из контекста.\n");

        return contextBuilder.toString();
    }

    /**
     * Упрощенный метод для генерации контекста с параметрами по умолчанию
     */
    public String generateChatContext(String userQuery, String clientId) throws Exception {
        return generateChatContext(userQuery, clientId, 500, 5, null);
    }

    /**
     * Упрощенный метод для генерации контекста с параметрами по умолчанию
     */
    public String generateGenerationContext(String prompt, String clientId) throws Exception {
        return generateGenerationContext(prompt, clientId, 500, 5, null);
    }

    /**
     * Комбинированный метод для получения как контекста, так и исходных документов
     *
     * @return массив из двух элементов: [0] - форматированный контекст, [1] - список документов
     */
    public Object[] getContextAndDocuments(String query, String clientId,
                                           int maxChunkSize, int maxContextDocuments,
                                           boolean forChat) throws Exception {
        List<SimilarDocument> contextDocs = getContextDocumentsForText(query, clientId, maxChunkSize, maxContextDocuments);

        String formattedContext;
        if (forChat) {
            formattedContext = formatContextForChat(query, contextDocs);
        } else {
            formattedContext = formatContextForGeneration(query, contextDocs);
        }

        return new Object[] { formattedContext, contextDocs };
    }
}
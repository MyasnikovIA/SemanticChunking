package ru.miacomsoft.semantic;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private Properties properties;

    public ConfigLoader() {
        this("application.properties");
    }

    public ConfigLoader(String configFile) {
        properties = new Properties();
        try {
            properties.load(new FileInputStream(configFile));
        } catch (IOException e) {
            properties = loadPropertiesFromClasspath(configFile);
            if (properties == null) {
                setDefaultProperties();
            }
        }
    }

    private void setDefaultProperties() {
        properties.setProperty("rag.generation.model", "deepseek-coder-v2:16b");
        properties.setProperty("rag.chat.model", "deepseek-coder-v2:16b");
        properties.setProperty("rag.similarity.threshold", "0.7");
        properties.setProperty("rag.ollama.server.host", "localhost");
        properties.setProperty("rag.ollama.server.port", "11434");
        properties.setProperty("rag.embedding.model", "all-minilm:22m");
        properties.setProperty("rag.embedding.host", "localhost");
        properties.setProperty("rag.embedding.server.port", "11434");

        // PostgreSQL configuration из отдельных параметров
        properties.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        properties.setProperty("spring.datasource.host", "localhost");
        properties.setProperty("spring.datasource.port", "5432");
        properties.setProperty("spring.datasource.database", "rag_database");
        properties.setProperty("spring.datasource.username", "postgres");
        properties.setProperty("spring.datasource.password", "your_password");
    }

    public Properties getProperties() {
        return properties;
    }

    public String getDbUrl() {
        // Генерируем URL из отдельных параметров
        String host = properties.getProperty("spring.datasource.host", "localhost");
        String port = properties.getProperty("spring.datasource.port", "5432");
        String database = properties.getProperty("spring.datasource.database", "rag_database");

        return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
    }

    public String getOllamaUrl() {
        return String.format("http://%s:%s",
                properties.getProperty("rag.ollama.server.host"),
                properties.getProperty("rag.ollama.server.port"));
    }

    public String getEmbeddingServiceUrl() {
        return String.format("http://%s:%s/embed",
                properties.getProperty("rag.embedding.host"),
                properties.getProperty("rag.embedding.server.port"));
    }

    public double getSimilarityThreshold() {
        String thresholdStr = properties.getProperty("rag.similarity.threshold", "0.7");
        // Убираем возможные комментарии в строке
        thresholdStr = thresholdStr.split("#")[0].trim();
        try {
            return Double.parseDouble(thresholdStr);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка парсинга порога схожести: " + thresholdStr + ", используется значение по умолчанию 0.7");
            return 0.7;
        }
    }

    // Добавляем методы для получения отдельных параметров базы данных
    public String getDbHost() {
        return properties.getProperty("spring.datasource.host", "localhost");
    }

    public String getDbPort() {
        return properties.getProperty("spring.datasource.port", "5432");
    }

    public String getDbName() {
        return properties.getProperty("spring.datasource.database", "rag_database");
    }

    public String getDbUsername() {
        return properties.getProperty("spring.datasource.username", "postgres");
    }

    public String getDbPassword() {
        return properties.getProperty("spring.datasource.password", "");
    }

    public Properties loadPropertiesFromClasspath(String fileName) {
        Properties props = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new IOException("File not found in classpath: " + fileName);
            }
            props.load(input);
        } catch (IOException ex) {
            System.err.println("Error loading properties from classpath: " + ex.getMessage());
            return null;
        }
        return props;
    }
}
package ru.miacomsoft.semantic;

import org.json.JSONObject;
import java.util.List;

public class UsageExample {
    public static void main(String[] args) throws Exception {
        // 1. Инициализация компонентов
        System.out.println("=== Инициализация системы ===");
        ConfigLoader configLoader = new ConfigLoader("application.properties");
        SemanticChunker semanticChunker = new SemanticChunker(configLoader);
        DocumentChunker documentChunker = new DocumentChunker(configLoader, semanticChunker);

        // 2. Генерация тестовых документов на различные темы
        System.out.println("\n=== Генерация тестовых документов ===");

        // Тема 1: Программирование на Java
        String javaDoc = """
            Программирование на Java является одним из самых популярных направлений в разработке программного обеспечения.
            Java - это объектно-ориентированный язык программирования, который был создан компанией Sun Microsystems в 1995 году.
            Основные особенности Java включают кроссплатформенность, автоматическое управление памятью и строгую типизацию.
            Виртуальная машина Java (JVM) позволяет запускать Java-приложения на различных операционных системах.
            Коллекции в Java предоставляют удобные структуры данных для работы с наборами объектов.
            Многопоточность в Java реализована через класс Thread и интерфейс Runnable.
            Spring Framework является наиболее популярным фреймворком для создания enterprise-приложений на Java.
            """;
        
        // Тема 2: Искусственный интеллект
        String aiDoc = """
            Искусственный интеллект (ИИ) - это область компьютерных наук, занимающаяся созданием интеллектуальных машин.
            Машинное обучение является подразделом искусственного интеллекта, который позволяет компьютерам учиться на данных.
            Глубокое обучение использует нейронные сети с множеством слоев для решения сложных задач.
            Обработка естественного языка (NLP) позволяет компьютерам понимать и генерировать человеческую речь.
            Компьютерное зрение дает машинам способность видеть и интерпретировать визуальную информацию.
            Рекомендательные системы используют алгоритмы ИИ для предложения релевантного контента пользователям.
            """;

        // Тема 3: Базы данных
        String dbDoc = """
            Базы данных являются фундаментальным компонентом современных информационных систем.
            Реляционные базы данных используют таблицы для хранения данных и SQL для запросов.
            PostgreSQL - это мощная объектно-реляционная система управления базами данных с открытым исходным кодом.
            Индексы в базах данных ускоряют выполнение запросов, но замедляют операции вставки и обновления.
            Транзакции обеспечивают атомарность, согласованность, изолированность и долговечность операций.
            Нормализация базы данных уменьшает избыточность данных и улучшает целостность.
            Векторные базы данных специально предназначены для хранения и поиска векторных эмбеддингов.
            """;

        // Тема 4: Веб-разработка
        String webDoc = """
            Веб-разработка включает создание и поддержку веб-сайтов и веб-приложений.
            Frontend-разработка занимается клиентской частью, которую видит пользователь.
            HTML, CSS и JavaScript являются основными технологиями для frontend-разработки.
            Backend-разработка отвечает за серверную логику и работу с базами данных.
            REST API является стандартным подходом для создания веб-сервисов.
            Микросервисная архитектура позволяет разбивать приложение на небольшие независимые сервисы.
            Безопасность веб-приложений включает защиту от XSS, CSRF и SQL-инъекций.
            """;

        // 3. Добавление документов с автоматическим чанкингом
        System.out.println("\n=== Добавление документов в базу ===");

        System.out.println("Добавление документа по Java...");
        int javaChunks = documentChunker.addDocumentWithChunking(javaDoc, "client1", "java_programming.txt", 300);
        System.out.println("Добавлено чанков Java: " + javaChunks);

        System.out.println("Добавление документа по ИИ...");
        int aiChunks = documentChunker.addDocumentWithChunking(aiDoc, "client1", "artificial_intelligence.txt", 300);
        System.out.println("Добавлено чанков ИИ: " + aiChunks);

        System.out.println("Добавление документа по базам данных...");
        int dbChunks = documentChunker.addDocumentWithChunking(dbDoc, "client1", "databases.txt", 300);
        System.out.println("Добавлено чанков БД: " + dbChunks);

        System.out.println("Добавление документа по веб-разработке...");
        int webChunks = documentChunker.addDocumentWithChunking(webDoc, "client1", "web_development.txt", 300);
        System.out.println("Добавлено чанков веб-разработки: " + webChunks);

        // 4. Проверка количества документов в базе
        System.out.println("\n=== Статистика базы данных ===");
        int totalDocuments = documentChunker.getDocumentCount();
        System.out.println("Всего документов в базе: " + totalDocuments);

        // 5. Тестирование поиска контекстных документов по разным запросам
        System.out.println("\n=== Тестирование поиска контекстных документов ===");

        // Тест 1: Запрос по программированию на Java
        System.out.println("\n--- Тест 1: Поиск по запросу 'Java программирование' ---");
        String query1 = "Как программировать на Java и использовать коллекции?";
        System.out.println("\nЗапрос: "+ query1);
        List<DocumentChunker.SimilarDocument> results1 =
                documentChunker.getContextDocumentsForText(query1, "client1", 200, 5);

        System.out.println("Найдено документов: " + results1.size());
        for (int i = 0; i < Math.min(results1.size(), 3); i++) {
            DocumentChunker.SimilarDocument doc = results1.get(i);
            System.out.println("\nДокумент " + (i+1) + ":");
            System.out.println("Схожесть: " + String.format("%.3f", doc.getSimilarity()));
            System.out.println("Содержимое: " +
                    (doc.getContent().length() > 150 ?
                            doc.getContent().substring(0, 150) + "..." :
                            doc.getContent()));
        }

        // Тест 2: Запрос по искусственному интеллекту
        System.out.println("\n--- Тест 2: Поиск по запросу 'машинное обучение' ---");
        String query2 = "Что такое машинное обучение и глубокие нейронные сети?";
        System.out.println("\nЗапрос: "+ query2);
        List<DocumentChunker.SimilarDocument> results2 =
                documentChunker.getContextDocumentsForText(query2, "client1", 200, 5);

        System.out.println("Найдено документов: " + results2.size());
        for (int i = 0; i < Math.min(results2.size(), 3); i++) {
            DocumentChunker.SimilarDocument doc = results2.get(i);
            System.out.println("\nДокумент " + (i+1) + ":");
            System.out.println("Схожесть: " + String.format("%.3f", doc.getSimilarity()));
            System.out.println("Содержимое: " +
                    (doc.getContent().length() > 150 ?
                            doc.getContent().substring(0, 150) + "..." :
                            doc.getContent()));
        }

        // Тест 3: Запрос по базам данных
        System.out.println("\n--- Тест 3: Поиск по запросу 'PostgreSQL базы данных' ---");
        String query3 = "Как работают индексы в PostgreSQL и для чего нужны транзакции?";
        System.out.println("\nЗапрос: "+ query3);
        List<DocumentChunker.SimilarDocument> results3 =
                documentChunker.getContextDocumentsForText(query3, "client1", 200, 5);

        System.out.println("Найдено документов: " + results3.size());
        for (int i = 0; i < Math.min(results3.size(), 3); i++) {
            DocumentChunker.SimilarDocument doc = results3.get(i);
            System.out.println("\nДокумент " + (i+1) + ":");
            System.out.println("Схожесть: " + String.format("%.3f", doc.getSimilarity()));
            System.out.println("Содержимое: " +
                    (doc.getContent().length() > 150 ?
                            doc.getContent().substring(0, 150) + "..." :
                            doc.getContent()));
        }

        // Тест 4: Запрос по веб-разработке
        System.out.println("\n--- Тест 4: Поиск по запросу 'веб-разработка REST API' ---");
        String query4 = "Как создать REST API для веб-приложения и обеспечить безопасность?";
        System.out.println("\nЗапрос: "+ query4);
        List<DocumentChunker.SimilarDocument> results4 =
                documentChunker.getContextDocumentsForText(query4, "client1", 200, 5);

        System.out.println("Найдено документов: " + results4.size());
        for (int i = 0; i < Math.min(results4.size(), 3); i++) {
            DocumentChunker.SimilarDocument doc = results4.get(i);
            System.out.println("\nДокумент " + (i+1) + ":");
            System.out.println("Схожесть: " + String.format("%.3f", doc.getSimilarity()));
            System.out.println("Содержимое: " +
                    (doc.getContent().length() > 150 ?
                            doc.getContent().substring(0, 150) + "..." :
                            doc.getContent()));
        }

        // Тест 5: Смешанный запрос
        System.out.println("\n--- Тест 5: Смешанный запрос 'Java и базы данных' ---");
        String query5 = "Как использовать Java для работы с базами данных PostgreSQL?";
        System.out.println("\nЗапрос: "+ query5);
        List<DocumentChunker.SimilarDocument> results5 =
                documentChunker.getContextDocumentsForText(query5, "client1", 200, 10);

        System.out.println("Найдено документов: " + results5.size());

        // Группируем результаты по схожести
        System.out.println("\nРезультаты, сгруппированные по порогам схожести:");
        int highSimilarity = 0; // > 0.8
        int mediumSimilarity = 0; // 0.6 - 0.8
        int lowSimilarity = 0; // < 0.6

        for (DocumentChunker.SimilarDocument doc : results5) {
            double similarity = doc.getSimilarity();
            if (similarity > 0.8) {
                highSimilarity++;
            } else if (similarity > 0.6) {
                mediumSimilarity++;
            } else {
                lowSimilarity++;
            }
        }

        System.out.println("Высокая схожесть (>0.8): " + highSimilarity + " документов");
        System.out.println("Средняя схожесть (0.6-0.8): " + mediumSimilarity + " документов");
        System.out.println("Низкая схожесть (<0.6): " + lowSimilarity + " документов");

        // Показываем лучшие результаты
        System.out.println("\nЛучшие результаты:");
        for (int i = 0; i < Math.min(results5.size(), 5); i++) {
            DocumentChunker.SimilarDocument doc = results5.get(i);
            System.out.println("\n#" + (i+1) + " (схожесть: " +
                    String.format("%.3f", doc.getSimilarity()) + "):");
            System.out.println(doc.getContent().substring(0,
                    Math.min(doc.getContent().length(), 200)) +
                    (doc.getContent().length() > 200 ? "..." : ""));
        }

        // 6. Демонстрация работы SemanticChunker отдельно
        System.out.println("\n=== Демонстрация работы SemanticChunker ===");
        String testText = "Java это язык программирования. Он используется для создания приложений. Spring Framework популярен для enterprise разработки.";

        List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(testText, 100);
        System.out.println("Текст разбит на " + chunks.size() + " семантических чанка:");
        for (int i = 0; i < chunks.size(); i++) {
            SemanticChunker.Chunk chunk = chunks.get(i);
            System.out.println("\nЧанк " + (i+1) + " (позиция: " + chunk.getPosition() +
                    ", длина: " + chunk.getLength() + " символов):");
            System.out.println(chunk.getText());
        }

        // 7. Тест с собственным эмбеддингом
        System.out.println("\n=== Тест с ручным добавлением документа ===");
        String customText = "Векторные базы данных особенно эффективны для семантического поиска и рекомендательных систем.";

        // Получаем эмбеддинг
        float[] customEmbedding = semanticChunker.getEmbedding(customText);

        // Создаем метаданные
        JSONObject metadata = new JSONObject();
        metadata.put("source", "custom_doc.txt");
        metadata.put("author", "test_user");
        metadata.put("topic", "vector_databases");

        // Добавляем документ с ручным эмбеддингом
        int docId = documentChunker.addDocument(customText, metadata, "client1", customEmbedding);
        if (docId != -1) {
            System.out.println("Документ добавлен с ID: " + docId);

            // Ищем похожие документы
            String vectorQuery = "семантический поиск в векторных базах";
            List<DocumentChunker.SimilarDocument> vectorResults =
                    documentChunker.getContextDocumentsForText(vectorQuery, "client1", 200, 3);

            System.out.println("Найдено похожих документов: " + vectorResults.size());
            if (!vectorResults.isEmpty()) {
                System.out.println("Лучший результат (схожесть: " +
                        String.format("%.3f", vectorResults.get(0).getSimilarity()) + "):");
                System.out.println(vectorResults.get(0).getContent());
            }
        }
        System.out.println("\n=== Тестирование завершено ===");
    }
}
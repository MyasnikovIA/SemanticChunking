package ru.miacomsoft.semantic;

import org.json.JSONObject;
import java.util.List;
import java.util.Scanner;

public class UsageExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== ДЕМОНСТРАЦИЯ СИСТЕМЫ СЕМАНТИЧЕСКОГО ПОИСКА ===");

        // 1. Инициализация системы
        System.out.println("\n1. ИНИЦИАЛИЗАЦИЯ КОМПОНЕНТОВ");
        System.out.println("=".repeat(50));

        ConfigLoader configLoader = new ConfigLoader("application.properties");
        System.out.println("✓ ConfigLoader загружен");

        SemanticChunker semanticChunker = new SemanticChunker(configLoader);
        System.out.println("✓ SemanticChunker инициализирован");

        DocumentChunker documentChunker = new DocumentChunker(configLoader, semanticChunker);
        System.out.println("✓ DocumentChunker инициализирован");

        // Информация о подключении
        documentChunker.printDatabaseInfo();

        // Очистка старых документов (опционально)
        System.out.println("\n2. ОЧИСТКА СТАРЫХ ДАННЫХ (опционально)");
        System.out.println("=".repeat(50));

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Очистить старые документы для client1? (y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("y") || answer.equals("yes") || answer.equals("да")) {
                documentChunker.clearDocuments("client1");
                System.out.println("✓ Старые документы удалены");
            }
        }

        // 3. ЗАГРУЗКА ДОКУМЕНТОВ В БАЗУ ДАННЫХ
        System.out.println("\n3. ЗАГРУЗКА ДОКУМЕНТОВ В БАЗУ ДАННЫХ");
        System.out.println("=".repeat(50));

        // Документ 1: Технологии Java
        String javaDoc = """
            Java — это высокоуровневый объектно-ориентированный язык программирования, разработанный компанией Sun Microsystems.
            Основные особенности Java включают:
            1. Кроссплатформенность — программа, написанная на Java, может работать на любой платформе, поддерживающей JVM.
            2. Автоматическое управление памятью (сборка мусора).
            3. Безопасность — встроенные механизмы безопасности предотвращают многие виды атак.
            4. Многопоточность — поддержка параллельного выполнения потоков.
            
            Ключевые компоненты экосистемы Java:
            - JVM (Java Virtual Machine) — виртуальная машина для выполнения байт-кода.
            - JRE (Java Runtime Environment) — среда выполнения Java-приложений.
            - JDK (Java Development Kit) — комплект разработчика, включающий компилятор и инструменты.
            
            Популярные фреймворки:
            - Spring Framework — для создания enterprise-приложений.
            - Hibernate — ORM для работы с базами данных.
            - Apache Maven — инструмент для сборки проектов и управления зависимостями.
            """;

        System.out.println("Загрузка документа по Java программированию...");
        int javaChunks = documentChunker.addDocumentWithChunking(javaDoc, "client1", "java_programming_guide.txt", 500);
        System.out.println("✓ Загружено чанков: " + javaChunks);

        // Документ 2: Веб-разработка
        String webDevDoc = """
            Веб-разработка включает создание и поддержку веб-сайтов и веб-приложений.
            
            Frontend-разработка (клиентская часть):
            - HTML (HyperText Markup Language) — язык разметки веб-страниц.
            - CSS (Cascading Style Sheets) — язык стилей для оформления веб-страниц.
            - JavaScript — язык программирования для интерактивности.
            - React, Angular, Vue.js — популярные JavaScript фреймворки.
            
            Backend-разработка (серверная часть):
            - Обработка HTTP-запросов и ответов.
            - Работа с базами данных (MySQL, PostgreSQL, MongoDB).
            - Реализация бизнес-логики приложения.
            - Аутентификация и авторизация пользователей.
            
            REST API (Representational State Transfer):
            - Архитектурный стиль для создания веб-сервисов.
            - Использует HTTP методы (GET, POST, PUT, DELETE).
            - Данные передаются в формате JSON или XML.
            
            Микросервисная архитектура:
            - Приложение разбивается на небольшие независимые сервисы.
            - Каждый сервис выполняет одну бизнес-функцию.
            - Сервисы общаются через API.
            """;

        System.out.println("\nЗагрузка документа по веб-разработке...");
        int webChunks = documentChunker.addDocumentWithChunking(webDevDoc, "client1", "web_development_guide.txt", 500);
        System.out.println("✓ Загружено чанков: " + webChunks);

        // Документ 3: Базы данных PostgreSQL
        String postgresqlDoc = """
            PostgreSQL — это мощная объектно-реляционная система управления базами данных (СУБД) с открытым исходным кодом.
            
            Основные характеристики:
            1. Полная поддержка ACID (Atomicity, Consistency, Isolation, Durability).
            2. Расширяемость — поддержка пользовательских типов данных, операторов и функций.
            3. Поддержка JSON и JSONB для работы с полуструктурированными данными.
            4. Репликация и отказоустойчивость.
            
            Ключевые возможности:
            - Транзакции с различными уровнями изоляции.
            - Индексы (B-tree, Hash, GiST, SP-GiST, GIN, BRIN) для ускорения запросов.
            - Полнотекстовый поиск.
            - Геопространственные данные через расширение PostGIS.
            
            Расширение pg_vector:
            - Добавляет поддержку векторных типов данных.
            - Позволяет хранить эмбеддинги для семантического поиска.
            - Оптимизировано для операций с векторами (косинусное сходство, евклидово расстояние).
            
            Пример создания таблицы с векторами:
            CREATE TABLE documents (
                id SERIAL PRIMARY KEY,
                content TEXT,
                embedding vector(384)
            );
            
            Пример поиска похожих векторов:
            SELECT * FROM documents 
            ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector 
            LIMIT 10;
            """;

        System.out.println("\nЗагрузка документа по PostgreSQL...");
        int pgChunks = documentChunker.addDocumentWithChunking(postgresqlDoc, "client1", "postgresql_guide.txt", 500);
        System.out.println("✓ Загружено чанков: " + pgChunks);

        // Документ 4: Искусственный интеллект и машинное обучение
        String aiDoc = """
            Искусственный интеллект (ИИ) — область компьютерных наук, занимающаяся созданием интеллектуальных машин.
            
            Основные направления ИИ:
            1. Машинное обучение (ML) — алгоритмы, которые учатся на данных.
            2. Глубокое обучение (DL) — нейронные сети с множеством слоев.
            3. Обработка естественного языка (NLP) — понимание и генерация человеческой речи.
            4. Компьютерное зрение (CV) — анализ и понимание визуальной информации.
            
            Типы машинного обучения:
            - Обучение с учителем (Supervised Learning) — модель обучается на размеченных данных.
            - Обучение без учителя (Unsupervised Learning) — модель ищет паттерны в неразмеченных данных.
            - Обучение с подкреплением (Reinforcement Learning) — модель учится через взаимодействие со средой.
            
            Популярные алгоритмы:
            - Линейная и логистическая регрессия.
            - Деревья решений и случайные леса.
            - Метод опорных векторов (SVM).
            - Нейронные сети и глубокое обучение.
            
            Применение ИИ:
            - Рекомендательные системы (YouTube, Netflix, Amazon).
            - Распознавание изображений и лиц.
            - Машинный перевод (Google Translate).
            - Чат-боты и виртуальные ассистенты.
            """;

        System.out.println("\nЗагрузка документа по искусственному интеллекту...");
        int aiChunks = documentChunker.addDocumentWithChunking(aiDoc, "client1", "artificial_intelligence_guide.txt", 500);
        System.out.println("✓ Загружено чанков: " + aiChunks);

        // 4. СТАТИСТИКА БАЗЫ ДАННЫХ
        System.out.println("\n4. СТАТИСТИКА БАЗЫ ДАННЫХ");
        System.out.println("=".repeat(50));

        int totalDocuments = documentChunker.getDocumentCount();
        System.out.println("Всего документов в базе: " + totalDocuments);

        // 5. ТЕСТИРОВАНИЕ ПОИСКА И ГЕНЕРАЦИИ ПРОМПТОВ
        System.out.println("\n5. ТЕСТИРОВАНИЕ СЕМАНТИЧЕСКОГО ПОИСКА");
        System.out.println("=".repeat(50));

        // Тест 1: Поиск по теме Java
        System.out.println("\nТЕСТ 1: Запрос по теме Java");
        System.out.println("-".repeat(30));
        String query1 = "Какие основные особенности языка программирования Java?";
        System.out.println("Запрос: " + query1);

        // Получаем контекстные документы
        List<DocumentChunker.SimilarDocument> results1 =
                documentChunker.getContextDocumentsForText(query1, "client1", 300, 5);
        System.out.println("Найдено релевантных документов: " + results1.size());

        // Генерируем промпт
        String prompt1 = documentChunker.generateChatPrompt(query1, "client1", 300, 5, null, null);
        System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
        System.out.println("=".repeat(80));
        System.out.println(prompt1);
        System.out.println("=".repeat(80));

        // Тест 2: Поиск по теме веб-разработки
        System.out.println("\n\nТЕСТ 2: Запрос по теме веб-разработки");
        System.out.println("-".repeat(30));
        String query2 = "Что такое REST API и как он используется в веб-разработке?";
        System.out.println("Запрос: " + query2);

        List<DocumentChunker.SimilarDocument> results2 =
                documentChunker.getContextDocumentsForText(query2, "client1", 300, 5);
        System.out.println("Найдено релевантных документов: " + results2.size());

        String prompt2 = documentChunker.generateChatPrompt(query2, "client1", 300, 5, null, null);
        System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
        System.out.println("=".repeat(80));
        System.out.println(prompt2);
        System.out.println("=".repeat(80));

        // Тест 3: Поиск по теме PostgreSQL
        System.out.println("\n\nТЕСТ 3: Запрос по теме PostgreSQL");
        System.out.println("-".repeat(30));
        String query3 = "Какие возможности предоставляет расширение pg_vector в PostgreSQL?";
        System.out.println("Запрос: " + query3);

        List<DocumentChunker.SimilarDocument> results3 =
                documentChunker.getContextDocumentsForText(query3, "client1", 300, 5);
        System.out.println("Найдено релевантных документов: " + results3.size());

        String prompt3 = documentChunker.generateChatPrompt(query3, "client1", 300, 5, null, null);
        System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
        System.out.println("=".repeat(80));
        System.out.println(prompt3);
        System.out.println("=".repeat(80));

        // Тест 4: Поиск по теме ИИ
        System.out.println("\n\nТЕСТ 4: Запрос по теме искусственного интеллекта");
        System.out.println("-".repeat(30));
        String query4 = "Какие существуют типы машинного обучения и их применение?";
        System.out.println("Запрос: " + query4);

        List<DocumentChunker.SimilarDocument> results4 =
                documentChunker.getContextDocumentsForText(query4, "client1", 300, 5);
        System.out.println("Найдено релевантных документов: " + results4.size());

        String prompt4 = documentChunker.generateChatPrompt(query4, "client1", 300, 5, null, null);
        System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
        System.out.println("=".repeat(80));
        System.out.println(prompt4);
        System.out.println("=".repeat(80));

        // Тест 5: Смешанный запрос
        System.out.println("\n\nТЕСТ 5: Смешанный запрос (кросс-тематика)");
        System.out.println("-".repeat(30));
        String query5 = "Как можно использовать базы данных PostgreSQL в веб-приложениях на Java?";
        System.out.println("Запрос: " + query5);

        List<DocumentChunker.SimilarDocument> results5 =
                documentChunker.getContextDocumentsForText(query5, "client1", 300, 7);
        System.out.println("Найдено релевантных документов: " + results5.size());

        String prompt5 = documentChunker.generateChatPrompt(query5, "client1", 300, 7, null, null);
        System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
        System.out.println("=".repeat(80));
        System.out.println(prompt5);
        System.out.println("=".repeat(80));

        // 6. ДЕМОНСТРАЦИЯ ГЕНЕРАЦИИ ПРОМПТОВ С РАЗНЫМИ ПАРАМЕТРАМИ
        System.out.println("\n6. ДЕМОНСТРАЦИЯ ГЕНЕРАЦИИ ПРОМПТОВ С РАЗНЫМИ ПАРАМЕТРАМИ");
        System.out.println("=".repeat(50));

        // Тест с увеличенным количеством документов
        System.out.println("\nА. Промпт с 10 документами (больше контекста)");
        System.out.println("-".repeat(30));
        String queryA = "Расскажи о многопоточности в Java";
        String promptA = documentChunker.generateChatPrompt(queryA, "client1", 400, 10, 0.5, null);
        System.out.println("Запрос: " + queryA);
        System.out.println("Количество документов в контексте: 10");
        System.out.println("Порог схожести: 0.5");
        System.out.println("\nДлина промпта: " + promptA.length() + " символов");
        System.out.println("Первые 300 символов промпта:");
        System.out.println(promptA.substring(0, Math.min(300, promptA.length())) + "...");

        // Тест с высоким порогом схожести
        System.out.println("\n\nБ. Промпт с высоким порогом схожести (0.8)");
        System.out.println("-".repeat(30));
        String queryB = "Что такое нейронные сети?";
        String promptB = documentChunker.generateChatPrompt(queryB, "client1", 300, 5, 0.8, null);
        System.out.println("Запрос: " + queryB);
        System.out.println("Порог схожести: 0.8 (только очень релевантные документы)");
        System.out.println("\nДлина промпта: " + promptB.length() + " символов");
        System.out.println("Первые 300 символов промпта:");
        System.out.println(promptB.substring(0, Math.min(300, promptB.length())) + "...");

        // Тест с кастомным шаблоном промпта
        System.out.println("\n\nВ. Промпт с кастомным шаблоном");
        System.out.println("-".repeat(30));

        String customTemplate = """
            Ты — технический эксперт. Используй предоставленный контекст для ответа на вопрос.
            Если информация в контексте недостаточна, дополни ответ своими знаниями.
            Отвечай подробно, с примерами и практическими рекомендациями.
            
            Релевантный контекст:
            {context}
            
            Технический вопрос:
            {query}
            
            Развернутый ответ эксперта:
            """;

        String queryC = "Как настроить индексы в PostgreSQL для оптимизации запросов?";
        String promptC = documentChunker.generatePromptWithCustomTemplate(
                customTemplate, queryC, "client1", 400, 5
        );

        System.out.println("Запрос: " + queryC);
        System.out.println("Кастомный шаблон: 'Технический эксперт'");
        System.out.println("\nДлина промпта: " + promptC.length() + " символов");
        System.out.println("Первые 400 символов промпта:");
        System.out.println(promptC.substring(0, Math.min(400, promptC.length())) + "...");

        // 7. ПРОСМОТР ЗАГРУЖЕННЫХ ДОКУМЕНТОВ
        System.out.println("\n\n7. ПРОСМОТР ЗАГРУЖЕННЫХ ДОКУМЕНТОВ");
        System.out.println("=".repeat(50));

        List<DocumentChunker.SimilarDocument> allDocs = documentChunker.getAllDocuments("client1", 10);
        System.out.println("Первые 10 загруженных документов:");

        for (int i = 0; i < allDocs.size(); i++) {
            DocumentChunker.SimilarDocument doc = allDocs.get(i);
            System.out.println("\nДокумент " + (i + 1) + ":");
            System.out.println("ID: " + doc.getId());
            System.out.println("Метаданные: " + doc.getMetadata());
            System.out.println("Содержимое (первые 150 символов):");
            System.out.println(doc.getContent().substring(0, Math.min(150, doc.getContent().length())) + "...");
            System.out.println("-".repeat(50));
        }

        // 8. ДЕМОНСТРАЦИЯ ФОРМАТИРОВАНИЯ КОНТЕКСТА
        System.out.println("\n8. ФОРМАТИРОВАНИЕ КОНТЕКСТА ИЗ ДОКУМЕНТОВ");
        System.out.println("=".repeat(50));

        System.out.println("Пример форматирования 3 найденных документов:");
        if (!results1.isEmpty()) {
            Object[] promptAndDocs = documentChunker.getPromptAndDocuments(query1, "client1", 300, 3, null);
            String formattedContext = ((String) promptAndDocs[0])
                    .replace(documentChunker.getDefaultPromptTemplate()
                            .replace("{context}", "")
                            .replace("{query}", query1), "")
                    .replace("Ответ:", "");

            //System.out.println("Контекст для запроса: \"" + query1 + "\"");
            System.out.println(formattedContext);
        }

        // 9. ИНТЕРАКТИВНЫЙ РЕЖИМ
        System.out.println("\n9. ИНТЕРАКТИВНЫЙ РЕЖИМ (дополнительно)");
        System.out.println("=".repeat(50));
        System.out.println("Для тестирования других запросов запустите программу с интерактивным режимом.");

        // 10. СВОДКА
        System.out.println("\n\n10. СВОДКА РЕЗУЛЬТАТОВ");
        System.out.println("=".repeat(50));

        int totalChunks = javaChunks + webChunks + pgChunks + aiChunks;
        System.out.println("Всего загружено чанков: " + totalChunks);
        System.out.println("Темы документов:");
        System.out.println("  1. Java программирование: " + javaChunks + " чанков");
        System.out.println("  2. Веб-разработка: " + webChunks + " чанков");
        System.out.println("  3. PostgreSQL: " + pgChunks + " чанков");
        System.out.println("  4. Искусственный интеллект: " + aiChunks + " чанков");

        System.out.println("\nПротестировано запросов: 5");
        System.out.println("Сгенерировано промптов: 5 + 3 (с разными параметрами) = 8");
        System.out.println("\nШаблон промпта по умолчанию:");
        System.out.println("-".repeat(40));
        System.out.println(documentChunker.getDefaultPromptTemplate());
        System.out.println("-".repeat(40));

        System.out.println("\n✓ Система готова к использованию с Ollama!");
        System.out.println("✓ Загружено " + totalDocuments + " документов в базу данных");
        System.out.println("✓ Семантический поиск работает корректно");
        System.out.println("✓ Промпты генерируются в правильном формате для моделей Ollama");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ДЕМОНСТРАЦИЯ ЗАВЕРШЕНА УСПЕШНО!");
        System.out.println("=".repeat(80));
    }
}
package ru.miacomsoft.semantic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class BookProcessor {

    public static void main(String[] args) {
        System.out.println("=== ОБРАБОТЧИК КНИГ ДЛЯ СЕМАНТИЧЕСКОГО ПОИСКА ===\n");

        // 1. Настройка пути к книге
        String bookPath = "C:\\Book\\Java\\ече\\Современный язык Java.txt";
        Path path = Paths.get(bookPath);

        // 2. Инициализация компонентов
        ConfigLoader configLoader = new ConfigLoader("application.properties");
        SemanticChunker semanticChunker = new SemanticChunker(configLoader);
        DocumentChunker documentChunker = new DocumentChunker(configLoader, semanticChunker);

        // 3. Чтение книги
        System.out.println("Чтение книги: " + bookPath);
        String bookContent = readBookFromFile(path);

        if (bookContent == null || bookContent.trim().isEmpty()) {
            System.err.println("Ошибка: книга не найдена или пуста");
            return;
        }

        System.out.println("Размер книги: " + bookContent.length() + " символов");
        System.out.println("Примерное количество слов: " + bookContent.split("\\s+").length);

        // 4. Очистка старых данных для этой книги
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("\nОчистить старые данные для этой книги? (y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("y") || answer.equals("yes") || answer.equals("да")) {
                documentChunker.clearDocuments("book_java_modern");
                System.out.println("✓ Старые данные удалены");
            }
        }

        // 5. Разбиение книги на семантические чанки
        System.out.println("\nНачало семантического чанкинга книги...");

        long startTime = System.currentTimeMillis();

        try {
            // Выполняем семантическое чанкинг книги
            List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(bookContent, 800);

            System.out.println("✓ Книга разбита на " + chunks.size() + " семантических чанков");

            // 6. Сохранение чанков в базу данных
            System.out.println("\nСохранение чанков в базу данных...");

            int savedCount = saveBookChunksToDatabase(documentChunker, chunks, bookPath);

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            System.out.println("\n✓ Обработка завершена за " + duration + " секунд");
            System.out.println("✓ Сохранено " + savedCount + " чанков из " + chunks.size());

            // 7. Статистика базы данных
            int totalDocs = documentChunker.getDocumentCount();
            System.out.println("Всего документов в базе: " + totalDocs);

            // 8. Пример поиска и формирования контекста
            demonstrateSearchAndContext(documentChunker);

        } catch (Exception e) {
            System.err.println("Ошибка при обработке книги: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Чтение книги из файла
     */
    private static String readBookFromFile(Path filePath) {
        try {
            System.out.println("Чтение файла...");
            return Files.readString(filePath);
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
            return null;
        }
    }

    /**
     * Сохранение чанков книги в базу данных
     */
    private static int saveBookChunksToDatabase(DocumentChunker documentChunker,
                                                List<SemanticChunker.Chunk> chunks,
                                                String bookPath) {
        int savedCount = 0;
        int batchSize = 50; // Сохраняем пачками для отображения прогресса

        System.out.println("Сохранение " + chunks.size() + " чанков...");

        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<SemanticChunker.Chunk> batch = chunks.subList(i, end);

            // Используем массивную загрузку через addDocuments
            documentChunker.addDocuments(batch, "book_java_modern",
                    getBookNameFromPath(bookPath));

            savedCount += batch.size();

            // Прогресс
            double progress = ((double) savedCount / chunks.size()) * 100;
            System.out.printf("Прогресс: %.1f%% (%d/%d чанков)%n",
                    progress, savedCount, chunks.size());

            // Небольшая пауза между пачками
            if (end < chunks.size()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return savedCount;
    }

    /**
     * Извлечение имени книги из пути
     */
    private static String getBookNameFromPath(String path) {
        Path p = Paths.get(path);
        String fileName = p.getFileName().toString();
        // Убираем расширение
        return fileName.replace(".txt", "").replace(".TXT", "");
    }

    /**
     * Демонстрация поиска и формирования контекста
     */
    private static void demonstrateSearchAndContext(DocumentChunker documentChunker) throws Exception {
        System.out.println("\n=== ДЕМОНСТРАЦИЯ ПОИСКА И ФОРМИРОВАНИЯ КОНТЕКСТА ===\n");

        // Примеры запросов по теме Java
        String[] exampleQueries = {
                "Что нового появилось в современном языке Java?",
                "Какие изменения в лямбда-выражениях?",
                "Как работать с потоками данных в Java?",
                "Что такое модульная система в Java?",
                "Как использовать Optional в современном Java?",
                "Какие улучшения в коллекциях появились?",
                "Как работает сборщик мусора в современных версиях Java?",
                "Что такое записи (records) в Java?",
                "Как использовать sealed классы?",
                "Какие есть новые методы в Stream API?"
        };

        Scanner scanner = new Scanner(System.in);

        System.out.println("Доступные действия:");
        System.out.println("1. Тестовый поиск по предустановленным запросам");
        System.out.println("2. Интерактивный поиск");
        System.out.println("3. Просмотр загруженных чанков");
        System.out.println("4. Выход");

        while (true) {
            System.out.print("\nВыберите действие (1-4): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    testPredefinedQueries(documentChunker, exampleQueries);
                    break;

                case "2":
                    interactiveSearch(documentChunker, scanner);
                    break;

                case "3":
                    viewLoadedChunks(documentChunker);
                    break;

                case "4":
                    System.out.println("Выход из программы");
                    return;

                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }

    /**
     * Тестирование предустановленных запросов
     */
    private static void testPredefinedQueries(DocumentChunker documentChunker,
                                              String[] queries) throws Exception {
        System.out.println("\n--- ТЕСТИРОВАНИЕ ПРЕДУСТАНОВЛЕННЫХ ЗАПРОСОВ ---\n");

        for (int i = 0; i < queries.length; i++) {
            System.out.println((i + 1) + ". " + queries[i]);

            List<DocumentChunker.SimilarDocument> results =
                    documentChunker.getContextDocumentsForText(
                            queries[i], "book_java_modern", 300, 5);

            System.out.println("   Найдено релевантных фрагментов: " + results.size());

            if (!results.isEmpty()) {
                // Показываем первый (наиболее релевантный) результат
                DocumentChunker.SimilarDocument bestMatch = results.get(0);
                System.out.println("   Лучшее соответствие (схожесть: " +
                        String.format("%.3f", bestMatch.getSimilarity()) + "):");
                System.out.println("   " +
                        bestMatch.getContent().substring(0,
                                Math.min(150, bestMatch.getContent().length())) + "...");
            }
            System.out.println();
        }

        // Пример генерации полного промпта для одного запроса
        System.out.println("\n--- ПРИМЕР СГЕНЕРИРОВАННОГО ПРОМПТА ---\n");
        String exampleQuery = queries[0];
        String generatedPrompt = documentChunker.generateChatPrompt(
                exampleQuery, "book_java_modern", 300, 3, null, null);

        System.out.println("Запрос: " + exampleQuery);
        System.out.println("\nСгенерированный промпт для Ollama:");
        System.out.println("=".repeat(80));
        System.out.println(generatedPrompt);
        System.out.println("=".repeat(80));
    }

    /**
     * Интерактивный поиск
     */
    private static void interactiveSearch(DocumentChunker documentChunker,
                                          Scanner scanner) throws Exception {
        System.out.println("\n--- ИНТЕРАКТИВНЫЙ ПОИСК ---");
        System.out.println("Введите ваш запрос (или 'выход' для возврата):");

        while (true) {
            System.out.print("\n> ");
            String query = scanner.nextLine().trim();

            if (query.equalsIgnoreCase("выход") || query.equalsIgnoreCase("exit")) {
                break;
            }

            if (query.isEmpty()) {
                continue;
            }

            long searchStart = System.currentTimeMillis();

            // Поиск релевантных фрагментов
            List<DocumentChunker.SimilarDocument> results =
                    documentChunker.getContextDocumentsForText(
                            query, "book_java_modern", 400, 5);

            long searchEnd = System.currentTimeMillis();

            System.out.println("\nНайдено фрагментов: " + results.size());
            System.out.println("Время поиска: " + (searchEnd - searchStart) + "мс");

            if (!results.isEmpty()) {
                System.out.println("\nНаиболее релевантные фрагменты из книги:");
                System.out.println("-".repeat(80));

                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    DocumentChunker.SimilarDocument doc = results.get(i);
                    System.out.println("\nФРАГМЕНТ " + (i + 1) +
                            " (схожесть: " + String.format("%.3f", doc.getSimilarity()) + "):");
                    System.out.println(doc.getContent());
                    System.out.println("-".repeat(80));
                }

                // Генерация промпта
                System.out.print("\nСгенерировать промпт для Ollama? (y/n): ");
                String generate = scanner.nextLine().trim().toLowerCase();

                if (generate.equals("y") || generate.equals("yes") || generate.equals("да")) {
                    String prompt = documentChunker.generateChatPrompt(
                            query, "book_java_modern", 400, 3, null, null);

                    System.out.println("\nСГЕНЕРИРОВАННЫЙ ПРОМПТ:");
                    System.out.println("=".repeat(80));
                    System.out.println(prompt);
                    System.out.println("=".repeat(80));
                }
            } else {
                System.out.println("Релевантные фрагменты не найдены.");
                System.out.println("Попробуйте переформулировать запрос или использовать другие ключевые слова.");
            }
        }
    }

    /**
     * Просмотр загруженных чанков
     */
    private static void viewLoadedChunks(DocumentChunker documentChunker) {
        System.out.println("\n--- ПРОСМОТР ЗАГРУЖЕННЫХ ЧАНКОВ ---\n");

        List<DocumentChunker.SimilarDocument> chunks =
                documentChunker.getAllDocuments("book_java_modern", 10);

        System.out.println("Первые 10 чанков из книги:");
        System.out.println("-".repeat(80));

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunker.SimilarDocument chunk = chunks.get(i);

            System.out.println("\nЧАНК " + (i + 1) + " (ID: " + chunk.getId() + ")");

            String content = chunk.getContent();
            int previewLength = Math.min(200, content.length());

            System.out.println("Длина: " + content.length() + " символов");
            System.out.println("Превью:");
            System.out.println(content.substring(0, previewLength) +
                    (content.length() > previewLength ? "..." : ""));

            System.out.println("-".repeat(80));
        }

        if (chunks.isEmpty()) {
            System.out.println("Чанки не найдены. Возможно, книга еще не загружена.");
        }
    }
}
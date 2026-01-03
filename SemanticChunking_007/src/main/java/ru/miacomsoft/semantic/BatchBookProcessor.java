package ru.miacomsoft.semantic;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BatchBookProcessor {

    public static void main(String[] args) {
        System.out.println("=== ПАКЕТНЫЙ ОБРАБОТЧИК КНИГ ===\n");

        // Папка с книгами
        String booksDirectory = "C:\\Book\\Java\\ече\\";

        // Расширения текстовых файлов
        List<String> textExtensions = List.of(".txt", ".md", ".text", ".java", ".py");

        // Инициализация компонентов
        ConfigLoader configLoader = new ConfigLoader("application.properties");
        SemanticChunker semanticChunker = new SemanticChunker(configLoader);
        DocumentChunker documentChunker = new DocumentChunker(configLoader, semanticChunker);

        // Поиск книг в директории
        List<Path> bookFiles = findBookFiles(Paths.get(booksDirectory), textExtensions);

        if (bookFiles.isEmpty()) {
            System.out.println("В указанной директории не найдено текстовых файлов.");
            return;
        }

        System.out.println("Найдено " + bookFiles.size() + " книг:");
        for (int i = 0; i < bookFiles.size(); i++) {
            System.out.println((i + 1) + ". " + bookFiles.get(i).getFileName());
        }

        // Обработка каждой книги
        int totalChunks = 0;

        for (Path bookFile : bookFiles) {
            try {
                System.out.println("\n" + "=".repeat(80));
                System.out.println("ОБРАБОТКА: " + bookFile.getFileName());
                System.out.println("=".repeat(80));

                // Чтение книги
                String content = Files.readString(bookFile);
                System.out.println("Размер: " + content.length() + " символов");

                // Семантическое чанкинг
                List<SemanticChunker.Chunk> chunks =
                        semanticChunker.semanticChunking(content, 800);

                System.out.println("Создано чанков: " + chunks.size());

                // Генерация уникального clientId для книги
                String clientId = "book_" +
                        bookFile.getFileName().toString()
                                .replaceAll("[^a-zA-Z0-9]", "_")
                                .toLowerCase();

                // Очистка старых данных для этой книги
                documentChunker.clearDocuments(clientId);

                // Сохранение в базу данных
                documentChunker.addDocuments(chunks, clientId, bookFile.getFileName().toString());

                totalChunks += chunks.size();

                System.out.println("✓ Книга успешно загружена в базу данных");

            } catch (Exception e) {
                System.err.println("Ошибка при обработке книги " + bookFile + ": " + e.getMessage());
            }
        }

        // Итоговая статистика
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ОБРАБОТКА ЗАВЕРШЕНА");
        System.out.println("=".repeat(80));
        System.out.println("Обработано книг: " + bookFiles.size());
        System.out.println("Всего чанков в базе: " + totalChunks);
        System.out.println("Общее количество документов в базе: " +
                documentChunker.getDocumentCount());

        // Тестовый поиск по всем книгам
        testCrossBookSearch(documentChunker);
    }

    /**
     * Поиск текстовых файлов в директории
     */
    private static List<Path> findBookFiles(Path directory, List<String> extensions) {
        List<Path> bookFiles = new ArrayList<>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            System.err.println("Директория не существует: " + directory);
            return bookFiles;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString().toLowerCase();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            bookFiles.add(path);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при поиске файлов: " + e.getMessage());
        }

        return bookFiles;
    }

    /**
     * Тестовый поиск по всем книгам
     */
    private static void testCrossBookSearch(DocumentChunker documentChunker) {
        System.out.println("\n--- ТЕСТОВЫЙ ПОИСК ПО ВСЕМ КНИГАМ ---\n");

        // Общий clientId для поиска по всем книгам
        String allBooksClientId = "all_books";

        String[] testQueries = {
                "Объектно-ориентированное программирование",
                "Многопоточность",
                "Коллекции данных",
                "Исключения и обработка ошибок",
                "Шаблоны проектирования"
        };

        try {
            for (String query : testQueries) {
                System.out.println("Запрос: " + query);

                // Поиск по всем книгам (используем специальный clientId)
                List<DocumentChunker.SimilarDocument> results =
                        documentChunker.getContextDocumentsForText(query, allBooksClientId, 400, 5);

                System.out.println("Найдено релевантных фрагментов: " + results.size());

                if (!results.isEmpty()) {
                    // Генерация промпта с контекстом из разных книг
                    String prompt = documentChunker.generateChatPrompt(
                            query, allBooksClientId, 400, 3, null, null);

                    System.out.println("Длина промпта: " + prompt.length() + " символов");
                    System.out.println("Источник лучшего результата: " +
                            extractSourceFromMetadata(results.get(0).getMetadata()));
                }

                System.out.println("-".repeat(60));
            }
        } catch (Exception e) {
            System.err.println("Ошибка при тестовом поиске: " + e.getMessage());
        }
    }

    /**
     * Извлечение источника из метаданных
     */
    private static String extractSourceFromMetadata(String metadata) {
        try {
            org.json.JSONObject meta = new org.json.JSONObject(metadata);
            return meta.optString("source", "неизвестно");
        } catch (Exception e) {
            return "неизвестно";
        }
    }
}
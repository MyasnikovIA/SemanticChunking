package ru.miacomsoft.semantic;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Создаем ConfigLoader
        ConfigLoader configLoader = new ConfigLoader("application.properties");

        // 2. Создаем SemanticChunker с ConfigLoader
        SemanticChunker semanticChunker = new SemanticChunker(configLoader);

        // 3. Создаем DocumentChunker с ConfigLoader и SemanticChunker
        DocumentChunker documentChunker = new DocumentChunker(configLoader, semanticChunker);

        // 4. Добавляем документ с автоматическим чанкингом
        String text = "Это пример текста для чанкинга. Он будет разбит на семантические чанки...";
        documentChunker.addDocumentWithChunking(text, "client1", "example.txt", 1000);

        // 5. Ищем контекстные документы
        String query = "пример текста";
        List<DocumentChunker.SimilarDocument> contextDocs = documentChunker.getContextDocumentsForText(query, "client1", 500, 10);
        System.out.println("Найдено контекстных документов: " + contextDocs.size());

        // 6. Пример использования новых методов генерации контекста
        System.out.println("\n=== Пример генерации контекста для Ollama ===");

        // Генерация контекста для чата
        String chatQuery = "Как работает семантический чанкинг?";
        String chatContext = documentChunker.generateChatContext(chatQuery, "client1");
        System.out.println("Контекст для чата:\n" + chatContext.substring(0, Math.min(500, chatContext.length())) + "...\n");

        // Генерация контекста для генерации текста
        String generationPrompt = "Напиши объяснение о том, как работает семантический поиск";
        String generationContext = documentChunker.generateGenerationContext(generationPrompt, "client1");
        System.out.println("Контекст для генерации:\n" + generationContext.substring(0, Math.min(500, generationContext.length())) + "...\n");

        // Комбинированный метод
        Object[] contextAndDocs = documentChunker.getContextAndDocuments("семантический анализ", "client1", 300, 3, true);
        System.out.println("Форматированный контекст (первые 600 символов):\n" +
                ((String)contextAndDocs[0]).substring(0, Math.min(600, ((String)contextAndDocs[0]).length())) + "...");
        System.out.println("Найдено документов: " + ((List<?>)contextAndDocs[1]).size());
    }
}
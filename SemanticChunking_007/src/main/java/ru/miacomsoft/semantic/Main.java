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

        // 6. Пример использования новых методов генерации промпта
        System.out.println("\n=== Пример генерации промпта для Ollama ===");

        // Генерация промпта для чата
        String chatQuery = "Как работает семантический чанкинг?";
        String chatPrompt = documentChunker.generateChatPrompt(chatQuery, "client1");
        System.out.println("Промпт для чата:\n" + chatPrompt.substring(0, Math.min(500, chatPrompt.length())) + "...\n");

        // Генерация промпта для генерации текста
        String generationQuery = "Напиши объяснение о том, как работает семантический поиск";
        String generationPrompt = documentChunker.generateGenerationPrompt(generationQuery, "client1");
        System.out.println("Промпт для генерации:\n" + generationPrompt.substring(0, Math.min(500, generationPrompt.length())) + "...\n");

        // Показать шаблон по умолчанию
        System.out.println("Шаблон промпта по умолчанию:");
        System.out.println(documentChunker.getDefaultPromptTemplate());

        // Комбинированный метод
        Object[] promptAndDocs = documentChunker.getPromptAndDocuments("семантический анализ", "client1", 300, 3, null);
        System.out.println("\nСгенерированный промпт (первые 600 символов):\n" +
                ((String)promptAndDocs[0]).substring(0, Math.min(600, ((String)promptAndDocs[0]).length())) + "...");
        System.out.println("Найдено документов: " + ((List<?>)promptAndDocs[1]).size());
    }
}
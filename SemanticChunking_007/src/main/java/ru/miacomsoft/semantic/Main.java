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
    }
}


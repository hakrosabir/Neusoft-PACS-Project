package com.neusoft.neusoft_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorRagService {

    private static final Logger log = LoggerFactory.getLogger(DoctorRagService.class);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public DoctorRagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        // Build the ChatClient
        this.chatClient = builder.build();
    }

    /**
     * 1. Ingest PDF
     * Reads a PDF (Research Paper, Clinical Report, Patent), splits it,
     * and stores it in the Vector Database.
     */
    public void ingestPDF(Resource pdfResource) {
        try {
            // 1. Read the PDF
            PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource);
            List<Document> documents = reader.get();

            // 2. Split the text into smaller chunks
            // CRITICAL: Keep size at 400 to avoid "Context Window" crashes with Ollama.
            TokenTextSplitter splitter = new TokenTextSplitter(400, 20, 5, 10000, true);
            List<Document> chunks = splitter.apply(documents);

            // 3. Store in Vector Store
            vectorStore.add(chunks);

            log.info("✅ Successfully ingested PDF. Total chunks stored: {}", chunks.size());

        } catch (Exception e) {
            log.error("❌ Error ingesting PDF: ", e);
            throw new RuntimeException("Failed to process PDF file.");
        }
    }

    /**
     * 2. Chat with PDF
     * Intelligent RAG capable of handling Medical Records, Research Papers, and Patents.
     */
    public String askDocument(String question) {
        // Search Logic:
        // - TopK(4): Fetch slightly more context (4 chunks) to capture abstract/titles better.
        // - SimilarityThreshold(0.5): Lowered slightly to ensure we catch titles and introductions.
        SearchRequest smartSearch = SearchRequest.builder()
                .topK(4)
                .similarityThreshold(0.5)
                .build();

        // SYSTEM PROMPT: Designed for Doctors & Researchers
        String systemPrompt = """
                You are an advanced Medical Research and Clinical Assistant designed to assist doctors.
                You are answering questions based ONLY on the provided document context (which may be a Research Paper, Clinical Trial, Patent, or Patient Record).

                Guidelines:
                1. **Professional Tone:** Use precise medical and scientific terminology suitable for a doctor.
                2. **Research Papers/Patents:** If analyzing a paper, summarize methodologies, findings, and novelties clearly.
                3. **Clinical Data:** If analyzing patient records, be exact with values, dates, and diagnoses.
                4. **No Hallucinations:** If the answer is not in the document, explicitly state: "The provided document does not contain this specific information."
                5. **Directness:** Do not preach; provide the data directly.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .advisors(new QuestionAnswerAdvisor(vectorStore, smartSearch))
                .call()
                .content();
    }
}
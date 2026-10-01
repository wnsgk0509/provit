package com.provit.service.document.generator;

import java.util.List;
import com.provit.dto.document.DocumentReviewRequestDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.document.DocumentReviewResultDTO.Document;

public interface DocumentReviewGenerator {
    DocumentReviewResultDTO generate(DocumentReviewRequestDTO request, List<Document> documents);
}

package com.provit.dao.document;

import java.util.List;
import com.provit.dto.document.DocumentReviewDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.document.DocumentReviewResultDTO.*;

public interface DocumentReviewDAO {
    int insertReview(DocumentReviewDTO review);
    int insertDocument(Document document);
    int insertStrength(Strength strength);
    int insertImprovement(Improvement improvement);
    int insertConsistency(Consistency consistency);
    int insertSource(Source source);
    int completeReview(DocumentReviewDTO review);
    DocumentReviewResultDTO selectReview(int userNum, long reviewNum);
    List<DocumentReviewDTO> selectReviews(int userNum, int offset, int pageSize);
    List<Document> selectDocuments(long reviewNum);
    List<Strength> selectStrengths(long reviewNum);
    List<Improvement> selectImprovements(long reviewNum);
    List<Consistency> selectConsistencies(long reviewNum);
    List<Source> selectSources(long reviewNum);
}

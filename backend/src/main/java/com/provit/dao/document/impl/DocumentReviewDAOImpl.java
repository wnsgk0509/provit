package com.provit.dao.document.impl;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;
import com.provit.dao.document.DocumentReviewDAO;
import com.provit.dto.document.DocumentReviewDTO;
import com.provit.dto.document.DocumentReviewResultDTO;
import com.provit.dto.document.DocumentReviewResultDTO.*;

@Repository
public class DocumentReviewDAOImpl implements DocumentReviewDAO {
    private static final String NS = "com.provit.mapper.document.DocumentReviewMapper.";
    private final SqlSession sqlSession;

    public DocumentReviewDAOImpl(SqlSession sqlSession) { this.sqlSession = sqlSession; }

    @Override public int insertReview(DocumentReviewDTO review) { return sqlSession.insert(NS + "insertReview", review); }
    @Override public int insertDocument(Document document) { return sqlSession.insert(NS + "insertDocument", document); }
    @Override public int insertStrength(Strength strength) { return sqlSession.insert(NS + "insertStrength", strength); }
    @Override public int insertImprovement(Improvement improvement) { return sqlSession.insert(NS + "insertImprovement", improvement); }
    @Override public int insertConsistency(Consistency consistency) { return sqlSession.insert(NS + "insertConsistency", consistency); }
    @Override public int insertSource(Source source) { return sqlSession.insert(NS + "insertSource", source); }
    @Override public int completeReview(DocumentReviewDTO review) { return sqlSession.update(NS + "completeReview", review); }
    @Override public int failReview(DocumentReviewDTO review) { return sqlSession.update(NS + "failReview", review); }
    @Override public int updateDocumentSummary(Document document) { return sqlSession.update(NS + "updateDocumentSummary", document); }
    @Override public String lockReview(int userNum, long reviewNum) {
        return sqlSession.selectOne(NS + "lockReview", Map.of("userNum", userNum, "reviewNum", reviewNum));
    }
    @Override public DocumentReviewResultDTO selectReview(int userNum, long reviewNum) {
        return sqlSession.selectOne(NS + "selectReview", Map.of("userNum", userNum, "reviewNum", reviewNum));
    }
    @Override public List<DocumentReviewDTO> selectReviews(int userNum, int offset, int pageSize) {
        return sqlSession.selectList(NS + "selectReviews", Map.of("userNum", userNum, "offset", offset, "pageSize", pageSize));
    }
    @Override public List<Document> selectDocuments(long reviewNum) { return sqlSession.selectList(NS + "selectDocuments", reviewNum); }
    @Override public List<Strength> selectStrengths(long reviewNum) { return sqlSession.selectList(NS + "selectStrengths", reviewNum); }
    @Override public List<Improvement> selectImprovements(long reviewNum) { return sqlSession.selectList(NS + "selectImprovements", reviewNum); }
    @Override public List<Consistency> selectConsistencies(long reviewNum) { return sqlSession.selectList(NS + "selectConsistencies", reviewNum); }
    @Override public List<Source> selectSources(long reviewNum) { return sqlSession.selectList(NS + "selectSources", reviewNum); }
}

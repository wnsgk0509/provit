package com.provit.service.document.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.document.CareerDTO;
import com.provit.dto.document.CertificationDTO;
import com.provit.dto.document.CoverLetterDTO;
import com.provit.dto.document.DocumentSummaryDTO;
import com.provit.dto.document.EducationDTO;
import com.provit.dto.document.PortfolioCreateRequestDTO;
import com.provit.dto.document.PortfolioDTO;
import com.provit.dto.document.ResumeDTO;
import com.provit.dto.document.ResumeDetailDTO;
import com.provit.service.document.DocumentService;
import com.provit.service.document.storage.PortfolioFileStorage;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_PORTFOLIO_FILE_SIZE = 20_000_000L;
    private static final byte[] PDF_SIGNATURE = { '%', 'P', 'D', 'F', '-' };
    private static final ZoneId RESUME_DATE_ZONE = ZoneId.of("Asia/Seoul");

    private final DocumentDAO documentDAO;
    private final PortfolioFileStorage portfolioFileStorage;

    @Autowired
    public DocumentServiceImpl(
            DocumentDAO documentDAO,
            PortfolioFileStorage portfolioFileStorage) {
        this.documentDAO = documentDAO;
        this.portfolioFileStorage = portfolioFileStorage;
    }

    @Override
    @Transactional
    public ResumeDetailDTO createResume(int userNum, ResumeDetailDTO resumeDetail) {
        validateResumeDetail(resumeDetail);

        ResumeDTO resume = resumeDetail.getResume();
        resume.setResumeNum(0);
        resume.setUserNum(userNum);
        resume.setCreatedAt(null);
        resume.setUpdatedAt(null);
        trimResume(resume);
        resolveResumeJob(resume);

        requireSingleInsert(documentDAO.insertResume(resume), "이력서");

        List<EducationDTO> educationList = safeList(resumeDetail.getEducationList());
        List<CareerDTO> careerList = safeList(resumeDetail.getCareerList());
        List<CertificationDTO> certificationList = safeList(resumeDetail.getCertificationList());

        for (EducationDTO education : educationList) {
            education.setEduNum(0);
            education.setResumeNum(resume.getResumeNum());
            trimEducation(education);
            requireSingleInsert(documentDAO.insertEducation(education), "학력");
        }
        for (CareerDTO career : careerList) {
            career.setCareerNum(0);
            career.setResumeNum(resume.getResumeNum());
            trimCareer(career);
            requireSingleInsert(documentDAO.insertCareer(career), "경력");
        }
        for (CertificationDTO certification : certificationList) {
            certification.setCertNum(0);
            certification.setResumeNum(resume.getResumeNum());
            trimCertification(certification);
            requireSingleInsert(documentDAO.insertCertification(certification), "자격증");
        }

        resumeDetail.setEducationList(educationList);
        resumeDetail.setCareerList(careerList);
        resumeDetail.setCertificationList(certificationList);
        return resumeDetail;
    }

    @Override
    @Transactional
    public PortfolioDTO createPortfolio(int userNum, PortfolioCreateRequestDTO portfolioRequest) {
        validatePortfolioRequest(portfolioRequest);

        int portfolioNum = documentDAO.selectNextPortfolioNum();
        String fileUrl = portfolioFileStorage.store(portfolioNum, portfolioRequest.getFile());
        registerFileRollback(fileUrl);

        PortfolioDTO portfolio = new PortfolioDTO();
        portfolio.setPortfolioNum(portfolioNum);
        portfolio.setUserNum(userNum);
        portfolio.setPortfolioTitle(portfolioRequest.getPortfolioTitle().trim());
        portfolio.setFileUrl(fileUrl);
        portfolio.setOriginalFileName(originalFileName(portfolioRequest.getFile()));
        portfolio.setSavedFileName(Path.of(fileUrl).getFileName().toString());
        requireSingleInsert(documentDAO.insertPortfolio(portfolio), "포트폴리오");
        return portfolio;
    }

    @Override
    @Transactional
    public CoverLetterDTO createCoverLetter(int userNum, CoverLetterDTO coverLetter) {
        validateCoverLetter(coverLetter);

        coverLetter.setLetterNum(0);
        coverLetter.setUserNum(userNum);
        coverLetter.setCoverLetterTitle(trimRequired(coverLetter.getCoverLetterTitle()));
        coverLetter.setGrowthProcess(trimOptional(coverLetter.getGrowthProcess()));
        coverLetter.setPersonalityStrengthsWeaknesses(
                trimOptional(coverLetter.getPersonalityStrengthsWeaknesses()));
        coverLetter.setProblemSolvingExperience(
                trimOptional(coverLetter.getProblemSolvingExperience()));
        coverLetter.setPostJoiningAspiration(
                trimOptional(coverLetter.getPostJoiningAspiration()));
        coverLetter.setCreatedAt(null);
        coverLetter.setUpdatedAt(null);

        requireSingleInsert(documentDAO.insertCoverLetter(coverLetter), "자기소개서");
        return coverLetter;
    }

    @Override
    @Transactional
    public ResumeDetailDTO updateResume(
            int userNum, int resumeNum, ResumeDetailDTO resumeDetail) {
        validateResumeDetail(resumeDetail);
        if (documentDAO.selectResume(userNum, resumeNum) == null) {
            throw new NoSuchElementException("수정할 수 있는 이력서가 없습니다.");
        }

        ResumeDTO resume = resumeDetail.getResume();
        resume.setResumeNum(resumeNum);
        resume.setUserNum(userNum);
        resume.setCreatedAt(null);
        resume.setUpdatedAt(null);
        trimResume(resume);
        resolveResumeJob(resume);

        requireSingleUpdate(documentDAO.updateResume(resume), "이력서");

        documentDAO.deleteEducationList(resumeNum);
        documentDAO.deleteCareerList(resumeNum);
        documentDAO.deleteCertificationList(resumeNum);

        for (EducationDTO education : safeList(resumeDetail.getEducationList())) {
            education.setEduNum(0);
            education.setResumeNum(resumeNum);
            trimEducation(education);
            requireSingleInsert(documentDAO.insertEducation(education), "학력");
        }
        for (CareerDTO career : safeList(resumeDetail.getCareerList())) {
            career.setCareerNum(0);
            career.setResumeNum(resumeNum);
            trimCareer(career);
            requireSingleInsert(documentDAO.insertCareer(career), "경력");
        }
        for (CertificationDTO certification : safeList(resumeDetail.getCertificationList())) {
            certification.setCertNum(0);
            certification.setResumeNum(resumeNum);
            trimCertification(certification);
            requireSingleInsert(documentDAO.insertCertification(certification), "자격증");
        }

        return getResume(userNum, resumeNum);
    }

    @Override
    @Transactional
    public CoverLetterDTO updateCoverLetter(
            int userNum, int letterNum, CoverLetterDTO coverLetter) {
        validateCoverLetter(coverLetter);
        if (documentDAO.selectCoverLetter(userNum, letterNum) == null) {
            throw new NoSuchElementException("수정할 수 있는 자기소개서가 없습니다.");
        }

        coverLetter.setLetterNum(letterNum);
        coverLetter.setUserNum(userNum);
        coverLetter.setCoverLetterTitle(trimRequired(coverLetter.getCoverLetterTitle()));
        coverLetter.setGrowthProcess(trimOptional(coverLetter.getGrowthProcess()));
        coverLetter.setPersonalityStrengthsWeaknesses(
                trimOptional(coverLetter.getPersonalityStrengthsWeaknesses()));
        coverLetter.setProblemSolvingExperience(
                trimOptional(coverLetter.getProblemSolvingExperience()));
        coverLetter.setPostJoiningAspiration(
                trimOptional(coverLetter.getPostJoiningAspiration()));
        coverLetter.setCreatedAt(null);
        coverLetter.setUpdatedAt(null);

        requireSingleUpdate(documentDAO.updateCoverLetter(coverLetter), "자기소개서");
        return getCoverLetter(userNum, letterNum);
    }

    @Override
    @Transactional
    public void deleteResume(int userNum, int resumeNum) {
        if (documentDAO.deleteResume(userNum, resumeNum) != 1) {
            throw new NoSuchElementException("삭제할 수 있는 이력서가 없습니다.");
        }
    }

    @Override
    @Transactional
    public void deleteCoverLetter(int userNum, int letterNum) {
        if (documentDAO.deleteCoverLetter(userNum, letterNum) != 1) {
            throw new NoSuchElementException("삭제할 수 있는 자기소개서가 없습니다.");
        }
    }

    @Override
    @Transactional
    public void deletePortfolio(int userNum, int portfolioNum) {
        PortfolioDTO portfolio = getPortfolio(userNum, portfolioNum);
        if (documentDAO.deletePortfolio(userNum, portfolioNum) != 1) {
            throw new NoSuchElementException("삭제할 수 있는 포트폴리오가 없습니다.");
        }
        registerFileDeleteAfterCommit(portfolio.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDetailDTO getResume(int userNum, int resumeNum) {
        ResumeDTO resume = documentDAO.selectResume(userNum, resumeNum);
        if (resume == null) {
            throw new NoSuchElementException("조회할 수 있는 이력서가 없습니다.");
        }

        ResumeDetailDTO resumeDetail = new ResumeDetailDTO();
        resumeDetail.setResume(resume);
        resumeDetail.setEducationList(documentDAO.selectEducationList(resumeNum));
        resumeDetail.setCareerList(documentDAO.selectCareerList(resumeNum));
        resumeDetail.setCertificationList(documentDAO.selectCertificationList(resumeNum));
        return resumeDetail;
    }

    @Override
    @Transactional(readOnly = true)
    public CoverLetterDTO getCoverLetter(int userNum, int letterNum) {
        CoverLetterDTO coverLetter = documentDAO.selectCoverLetter(userNum, letterNum);
        if (coverLetter == null) {
            throw new NoSuchElementException("조회할 수 있는 자기소개서가 없습니다.");
        }
        return coverLetter;
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioDTO getPortfolio(int userNum, int portfolioNum) {
        PortfolioDTO portfolio = documentDAO.selectPortfolioByPortfolioNum(portfolioNum);
        if (portfolio == null) {
            throw new NoSuchElementException("조회할 수 있는 포트폴리오가 없습니다.");
        }
        if (portfolio.getUserNum() != userNum) {
            throw new SecurityException("본인이 등록한 포트폴리오만 조회하거나 삭제할 수 있습니다.");
        }
        return portfolio;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getPortfolioFile(int userNum, int portfolioNum) {
        PortfolioDTO portfolio = getPortfolio(userNum, portfolioNum);
        return portfolioFileStorage.loadAsResource(portfolio.getFileUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> getResumeSummaryList(int userNum) {
        return documentDAO.selectResumeSummaryList(userNum);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> getCoverLetterSummaryList(int userNum) {
        return documentDAO.selectCoverLetterSummaryList(userNum);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentSummaryDTO> getPortfolioSummaryList(int userNum) {
        return documentDAO.selectPortfolioSummaryList(userNum);
    }

    private void validateCoverLetter(CoverLetterDTO coverLetter) {
        if (coverLetter == null) {
            throw new IllegalArgumentException("자기소개서 정보를 입력해 주세요.");
        }

        validateRequiredText(coverLetter.getCoverLetterTitle(), 200, "자기소개서 제목");
        validateOptionalText(coverLetter.getGrowthProcess(), 1000, "성장 과정");
        validateOptionalText(
                coverLetter.getPersonalityStrengthsWeaknesses(), 1000, "성격의 장단점");
        validateOptionalText(
                coverLetter.getProblemSolvingExperience(), 1000, "문제 해결 경험");
        validateOptionalText(
                coverLetter.getPostJoiningAspiration(), 1000, "입사 후 포부");
    }

    private void validatePortfolioRequest(PortfolioCreateRequestDTO portfolioRequest) {
        if (portfolioRequest == null) {
            throw new IllegalArgumentException("포트폴리오 정보를 입력해 주세요.");
        }

        validateRequiredText(portfolioRequest.getPortfolioTitle(), 200, "포트폴리오 제목");

        MultipartFile file = portfolioRequest.getFile();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("포트폴리오 PDF 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_PORTFOLIO_FILE_SIZE) {
            throw new IllegalArgumentException("포트폴리오 파일은 20MB 이하여야 합니다.");
        }

        String originalFilename = originalFileName(file);
        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF 형식의 파일만 등록할 수 있습니다.");
        }
        if (originalFilename.length() > 255 || originalFilename.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("포트폴리오 원본 파일명이 올바르지 않습니다.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);
            if (!java.util.Arrays.equals(signature, PDF_SIGNATURE)) {
                throw new IllegalArgumentException("올바른 PDF 파일이 아닙니다.");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("포트폴리오 파일을 확인하지 못했습니다.", exception);
        }
    }

    private String originalFileName(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("포트폴리오 원본 파일명이 없습니다.");
        }
        filename = filename.replace('\\', '/');
        return filename.substring(filename.lastIndexOf('/') + 1);
    }

    private void registerFileRollback(String fileUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    portfolioFileStorage.deleteIfExists(fileUrl);
                }
            }
        });
    }

    private void registerFileDeleteAfterCommit(String fileUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                portfolioFileStorage.deleteIfExists(fileUrl);
            }
        });
    }

    private void validateResumeDetail(ResumeDetailDTO resumeDetail) {
        if (resumeDetail == null || resumeDetail.getResume() == null) {
            throw new IllegalArgumentException("이력서 정보를 입력해 주세요.");
        }

        ResumeDTO resume = resumeDetail.getResume();
        validateRequiredText(resume.getResumeTitle(), 200, "이력서 제목");
        validateRequiredText(resume.getOccupationCode(), 20, "지원 직군");
        validateRequiredText(resume.getJobCode(), 20, "지원 직무");
        validateRequiredText(resume.getHighestLevel(), 20, "최종 학력");
        validateOptionalText(resume.getDesiredLocation(), 200, "희망 근무 지역");
        validateOptionalText(resume.getDesiredWorkType(), 100, "희망 근무 형태");
        LocalDate today = LocalDate.now(RESUME_DATE_ZONE);

        for (EducationDTO education : safeList(resumeDetail.getEducationList())) {
            if (education == null) {
                throw new IllegalArgumentException("학력 정보가 올바르지 않습니다.");
            }
            validateRequiredText(education.getSchoolName(), 200, "학교명");
            validateRequiredText(education.getEducationStatus(), 20, "졸업 상태");
            validateOptionalText(education.getMajor(), 200, "전공");
            validateNotFutureDate(education.getAdmissionDate(), today, "입학일");
            validateDateRange(
                    education.getAdmissionDate(), education.getGraduationDate(),
                    "졸업일은 입학일보다 빠를 수 없습니다.");
        }

        for (CareerDTO career : safeList(resumeDetail.getCareerList())) {
            if (career == null) {
                throw new IllegalArgumentException("경력 정보가 올바르지 않습니다.");
            }
            validateRequiredText(career.getCompanyName(), 200, "회사명");
            validateOptionalText(career.getMainDuty(), 2000, "주요 업무");
            validateNotFutureDate(career.getJoinDate(), today, "입사일");
            validateDateRange(
                    career.getJoinDate(), career.getResignDate(),
                    "퇴사(예정)일은 입사일보다 빠를 수 없습니다.");
        }

        for (CertificationDTO certification : safeList(resumeDetail.getCertificationList())) {
            if (certification == null) {
                throw new IllegalArgumentException("자격증 정보가 올바르지 않습니다.");
            }
            validateRequiredText(certification.getCertName(), 200, "자격증명");
            validateOptionalText(certification.getCertGrade(), 100, "자격증 등급/점수");
            validateNotFutureDate(certification.getIssueDate(), today, "취득일");
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private void resolveResumeJob(ResumeDTO resume) {
        resume.setOccupationCode(resume.getOccupationCode().trim());
        resume.setJobCode(resume.getJobCode().trim());
        ResumeDTO selectedJob = documentDAO.selectResumeJob(resume.getOccupationCode(), resume.getJobCode());
        if (selectedJob == null) {
            throw new IllegalArgumentException("선택한 직군에 속하는 유효한 직무를 선택해 주세요.");
        }
        resume.setOccupationName(selectedJob.getOccupationName());
        resume.setJobName(selectedJob.getJobName());
    }

    private void validateRequiredText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "을(를) 입력해 주세요.");
        }
        validateOptionalText(value, maxLength, fieldName);
    }

    private void validateOptionalText(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "은(는) " + maxLength + "자 이하로 입력해 주세요.");
        }
    }

    private void validateNotFutureDate(Date date, LocalDate today, String fieldName) {
        if (date != null && Instant.ofEpochMilli(date.getTime()).atZone(RESUME_DATE_ZONE)
                .toLocalDate().isAfter(today)) {
            throw new IllegalArgumentException(fieldName + "은 오늘 이후 날짜를 선택할 수 없습니다.");
        }
    }

    private void validateDateRange(Date startDate, Date endDate, String message) {
        if (startDate != null && endDate != null && endDate.before(startDate)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void trimResume(ResumeDTO resume) {
        resume.setResumeTitle(trimRequired(resume.getResumeTitle()));
        resume.setHighestLevel(trimRequired(resume.getHighestLevel()));
        resume.setMotivation(trimOptional(resume.getMotivation()));
        resume.setDesiredLocation(trimOptional(resume.getDesiredLocation()));
        resume.setDesiredWorkType(trimOptional(resume.getDesiredWorkType()));
    }

    private void trimEducation(EducationDTO education) {
        education.setSchoolName(trimRequired(education.getSchoolName()));
        education.setMajor(trimOptional(education.getMajor()));
        education.setEducationStatus(trimRequired(education.getEducationStatus()));
    }

    private void trimCareer(CareerDTO career) {
        career.setCompanyName(trimRequired(career.getCompanyName()));
        career.setMainDuty(trimOptional(career.getMainDuty()));
    }

    private void trimCertification(CertificationDTO certification) {
        certification.setCertName(trimRequired(certification.getCertName()));
        certification.setCertGrade(trimOptional(certification.getCertGrade()));
    }

    private String trimRequired(String value) {
        return value.trim();
    }

    private String trimOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void requireSingleInsert(int insertCount, String documentName) {
        if (insertCount != 1) {
            throw new IllegalStateException(documentName + " 정보를 저장하지 못했습니다.");
        }
    }

    private void requireSingleUpdate(int updateCount, String documentName) {
        if (updateCount != 1) {
            throw new IllegalStateException(documentName + " 정보를 수정하지 못했습니다.");
        }
    }
}

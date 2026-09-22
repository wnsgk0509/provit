package com.provit.service.document.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.document.DocumentDAO;
import com.provit.dto.user.CareerDTO;
import com.provit.dto.user.CertificationDTO;
import com.provit.dto.user.EducationDTO;
import com.provit.dto.user.ResumeDTO;
import com.provit.dto.user.ResumeDetailDTO;
import com.provit.service.document.DocumentService;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentDAO documentDAO;

    @Autowired
    public DocumentServiceImpl(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
    }

    @Override
    @Transactional
    public ResumeDetailDTO createResume(int userNum, ResumeDetailDTO resumeDetail) {
        validateResumeDetail(resumeDetail);

        ResumeDTO resume = resumeDetail.getResume();
        resume.setResumeNum(0);
        resume.setUserNum(userNum);
        resume.setEducationName(null);
        resume.setCreatedAt(null);
        resume.setUpdatedAt(null);
        trimResume(resume);

        if (documentDAO.countEducationCode(resume.getEducationCode()) != 1) {
            throw new IllegalArgumentException("유효하지 않은 학력 구분입니다.");
        }
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

    private void validateResumeDetail(ResumeDetailDTO resumeDetail) {
        if (resumeDetail == null || resumeDetail.getResume() == null) {
            throw new IllegalArgumentException("이력서 정보를 입력해 주세요.");
        }

        ResumeDTO resume = resumeDetail.getResume();
        validateRequiredText(resume.getResumeTitle(), 200, "이력서 제목");
        validateRequiredText(resume.getHighestLevel(), 20, "최종 학력");
        validateOptionalText(resume.getDesiredLocation(), 200, "희망 근무 지역");
        validateOptionalText(resume.getDesiredWorkType(), 100, "희망 근무 형태");

        for (EducationDTO education : safeList(resumeDetail.getEducationList())) {
            if (education == null) {
                throw new IllegalArgumentException("학력 정보가 올바르지 않습니다.");
            }
            validateRequiredText(education.getSchoolName(), 200, "학교명");
            validateRequiredText(education.getEducationStatus(), 20, "졸업 상태");
            validateOptionalText(education.getMajor(), 200, "전공");
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
            validateDateRange(
                    career.getJoinDate(), career.getResignDate(),
                    "퇴사일은 입사일보다 빠를 수 없습니다.");
        }

        for (CertificationDTO certification : safeList(resumeDetail.getCertificationList())) {
            if (certification == null) {
                throw new IllegalArgumentException("자격증 정보가 올바르지 않습니다.");
            }
            validateRequiredText(certification.getCertName(), 200, "자격증명");
            validateOptionalText(certification.getCertGrade(), 100, "자격증 등급/점수");
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
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
}

package com.provit.service.document.impl;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.provit.dao.document.MainResumeDAO;
import com.provit.dto.document.MainResumeJobInfoDTO;
import com.provit.service.document.MainResumeService;

@Service
public class MainResumeServiceImpl implements MainResumeService {
    private final MainResumeDAO mainResumeDAO;

    public MainResumeServiceImpl(MainResumeDAO mainResumeDAO) {
        this.mainResumeDAO = mainResumeDAO;
    }

    @Override
    @Transactional
    public MainResumeJobInfoDTO setMainResume(long userNum, long resumeNum) {
        validateUserNum(userNum);
        if (resumeNum <= 0 || resumeNum > 999_999_999_999_999_999L) {
            throw new IllegalArgumentException("올바른 이력서 번호를 입력해 주세요.");
        }
        try {
            if (mainResumeDAO.updateMainResume(userNum, resumeNum) != 1) {
                throw new NoSuchElementException("대표로 지정할 수 있는 이력서가 없습니다.");
            }
        } catch (DataIntegrityViolationException exception) {
            // 지정 도중 이력서가 삭제되어 Oracle 외래키 검증에 실패한 경우.
            if (exception.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 2291) {
                throw new NoSuchElementException("대표로 지정할 수 있는 이력서가 없습니다.");
            }
            throw exception;
        }
        MainResumeJobInfoDTO result = mainResumeDAO.selectMainResumeJobInfo(userNum);
        if (result == null) {
            throw new NoSuchElementException("대표로 지정할 수 있는 이력서가 없습니다.");
        }
        return result;
    }

    @Override
    @Transactional
    public void clearMainResume(long userNum) {
        validateUserNum(userNum);
        if (mainResumeDAO.clearMainResume(userNum) != 1) {
            throw new NoSuchElementException("조회할 수 있는 회원이 없습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MainResumeJobInfoDTO getMainResumeJobInfo(long userNum) {
        validateUserNum(userNum);
        return mainResumeDAO.selectMainResumeJobInfo(userNum);
    }

    private void validateUserNum(long userNum) {
        if (userNum <= 0) {
            throw new IllegalArgumentException("올바른 회원 번호를 입력해 주세요.");
        }
    }
}

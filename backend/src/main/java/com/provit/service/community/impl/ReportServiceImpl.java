package com.provit.service.community.impl;

import com.provit.dao.community.ReportDAO;
import com.provit.dto.community.ReportDTO;
import com.provit.service.community.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {
    
    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private com.provit.dao.auth.UserDAO userDAO;

    @Autowired
    private com.provit.service.auth.MailService mailService;

    @Override
    @Transactional
    public void submitReport(ReportDTO reportDTO) throws Exception {
        // 중복 신고 여부 확인
        int count = reportDAO.checkDuplicateReport(reportDTO);
        if (count > 0) {
            throw new IllegalArgumentException("이미 해당 항목에 대해 신고를 접수하셨습니다.");
        }
        
        // 신고 정보 등록
        reportDAO.insertReport(reportDTO);

        // 신고자(본인)에게 접수 완료 자동 메일 발송
        com.provit.dto.auth.UserDTO reporter = userDAO.selectByUserNum(reportDTO.getReporterNum());
        if (reporter != null && reporter.getUserEmail() != null) {
            mailService.sendReportReceiptMail(reporter.getUserEmail());
        }
    }
}

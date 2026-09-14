package com.provit.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.provit.dao.TestDao;
import com.provit.service.TestService;

/**
 * 팀원 참고용 샘플 서비스 구현체 (생성자 주입 방식)
 */
@Service
public class TestServiceImpl implements TestService {

    private final TestDao testDao;

    // 생성자 주입 (Constructor Injection)
    @Autowired
    public TestServiceImpl(@Autowired(required = false) TestDao testDao) {
        this.testDao = testDao;
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serviceName", "Provit Backend API");
        status.put("framework", "Spring MVC 5.3.39");
        status.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (testDao != null) {
            status.put("dbTime", testDao.selectCurrentTime());
        }

        return status;
    }
}

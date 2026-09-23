package com.provit.service.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface PortfolioFileStorage {

    String store(int portfolioNum, MultipartFile file);

    void deleteIfExists(String fileUrl);

    Resource loadAsResource(String fileUrl);
}

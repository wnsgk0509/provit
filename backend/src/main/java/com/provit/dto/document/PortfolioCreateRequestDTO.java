package com.provit.dto.document;

import org.springframework.web.multipart.MultipartFile;

public class PortfolioCreateRequestDTO {

    private String portfolioTitle;
    private MultipartFile file;

    public String getPortfolioTitle() {
        return portfolioTitle;
    }

    public void setPortfolioTitle(String portfolioTitle) {
        this.portfolioTitle = portfolioTitle;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}

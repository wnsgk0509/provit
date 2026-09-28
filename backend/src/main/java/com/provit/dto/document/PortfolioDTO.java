package com.provit.dto.document;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class PortfolioDTO {

    private int portfolioNum;
    private int userNum;
    private String portfolioTitle;
    private String originalFileName;
    @JsonIgnore
    private String savedFileName;
    @JsonIgnore
    private String fileUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date updatedAt;
}

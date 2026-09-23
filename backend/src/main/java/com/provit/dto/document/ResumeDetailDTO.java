package com.provit.dto.document;

import java.util.List;

import lombok.Data;

@Data
public class ResumeDetailDTO {

    private ResumeDTO resume;
    private List<EducationDTO> educationList;
    private List<CareerDTO> careerList;
    private List<CertificationDTO> certificationList;
}

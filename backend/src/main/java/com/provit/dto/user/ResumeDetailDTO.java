package com.provit.dto.user;

import java.util.List;

import lombok.Data;

@Data
public class ResumeDetailDTO {

    private ResumeDTO resume;
    private List<EducationDTO> educationList;
    private List<CareerDTO> careerList;
    private List<CertificationDTO> certificationList;
}

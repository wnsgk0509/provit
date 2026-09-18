package com.provit.dao.recruitment;

import java.util.List;
import com.provit.dto.recruitment.OccupationDTO;

public interface RecruitmentDAO {

    public List<OccupationDTO> selectOccupationList();
}

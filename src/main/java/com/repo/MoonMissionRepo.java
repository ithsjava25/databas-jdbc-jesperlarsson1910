package com.repo;


import com.repo.DTO.MissionDTO;

import java.util.List;
import java.util.Optional;

public interface MoonMissionRepo {

    List<MissionDTO>  getAllMissions();

    Optional<MissionDTO> getMissionById(int missionid);

    int missionCount(int year);
}

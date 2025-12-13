package com.repo.DTO;

import java.time.LocalDate;

public record MissionDTO (
        int missionid,
        String spacecraft,
        LocalDate launchDate,
        String carrierRocket,
        String operator,
        String missionType,
        String outcome){
}

package com.repo;

import com.repo.DTO.MissionDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoonMissionRepository implements MoonMissionRepo{
    private final DataSource dataSource;

    public MoonMissionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<MissionDTO> getAllMissions() {
        List<MissionDTO> missions = new ArrayList<>();

        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from moon_mission")){

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                missions.add(mapMission(rs));
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
        return missions;
    }

    @Override
    public Optional<MissionDTO> getMissionById(int missionid) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from moon_mission where missionid = ?")){
            ps.setInt(1, missionid);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return Optional.of(mapMission(rs));
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public int missionCount(int year) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select count(*) from moon_mission where year(launch_date) = ?")) {
            ps.setInt(1, year);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count(*)");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private MissionDTO mapMission(ResultSet rs) throws SQLException {
        return new MissionDTO(
                rs.getInt("mission_id"),
                rs.getString("spacecraft"),
                rs.getDate("launch_date").toLocalDate(),
                rs.getString("carrier_rocket"),
                rs.getString("operator"),
                rs.getString("mission_type"),
                rs.getString("outcome")
        );
    }
}

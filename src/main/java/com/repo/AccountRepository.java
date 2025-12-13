package com.repo;

import com.repo.DTO.AccountDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountRepository implements AccountRepo{
    private final DataSource dataSource;

    public AccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<AccountDTO> getAccountByName(String name) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from account where name = ?")){
            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return Optional.of(mapAccount(rs));
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AccountDTO> getAccountByID(int userid) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from account where user_id = ?")){
            ps.setInt(1, userid);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return Optional.of(mapAccount(rs));
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public boolean accountExists(String name) {
        return getAccountByName(name).isPresent();
    }

    @Override
    public boolean accountExists(int userid) {
        return getAccountByID(userid).isPresent();
    }

    @Override
    public boolean matchCredentials(String name, String password) {
        Optional<AccountDTO> account = getAccountByName(name);

        return account.isPresent() && account.get().password().equals(password);
    }

    @Override
    public void createAccount(AccountDTO account) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("insert into account (name, password, first_name, last_name, ssn) values (?, ?, ?, ?, ?)")){
            ps.setString(1, account.name());
            ps.setString(2, account.password());
            ps.setString(3, account.firstName());
            ps.setString(4, account.lastName());
            ps.setString(5, account.ssn());

            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updatePassword(int userid, String password) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("update account set password = ? where user_id = ?")){
            ps.setString(1, password);
            ps.setInt(2, userid);

            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAccount(int userid) {
        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("delete from account where user_id = ?")){
            ps.setInt(1, userid);

            ps.executeUpdate();
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AccountDTO> getAllAccounts() {
        List<AccountDTO> accounts = new ArrayList<>();

        try(Connection c = dataSource.getConnection();
            PreparedStatement ps = c.prepareStatement("select * from account")){

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                accounts.add(mapAccount(rs));
            }
        }
        catch(SQLException e){
            throw new RuntimeException(e);
        }
        return accounts;
    }

    private AccountDTO mapAccount(ResultSet rs) throws SQLException {
        return new AccountDTO(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("password"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("ssn")
        );
    }

}

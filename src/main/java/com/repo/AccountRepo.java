package com.repo;


import com.repo.DTO.AccountDTO;

import java.util.List;
import java.util.Optional;

public interface AccountRepo {

    Optional<AccountDTO> getAccountByName(String name);

    Optional<AccountDTO> getAccountByID(int userid);

    boolean accountExists(String name);

    boolean accountExists(int userid);

    boolean matchCredentials(String name, String password);

    void createAccount(AccountDTO account);

    void updatePassword(int userid, String password);

    void deleteAccount(int userid);

    List<AccountDTO> getAllAccounts();
}

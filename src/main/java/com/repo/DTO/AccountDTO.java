package com.repo.DTO;

public record AccountDTO (
        int userid,
        String name,
        String password, //DB has pw in plaintext so we do too
        String firstName,
        String lastName,
        String ssn){
}

package com.example.Online_job_portal.dao;

import java.util.List;
import java.util.Optional;

public interface DAO<T> {

    void save(T item);

    Optional<T> findById(int id);

    List<T> findAll();

    void deleteById(int id);
}


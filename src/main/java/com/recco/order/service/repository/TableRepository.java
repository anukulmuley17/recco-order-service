package com.recco.order.service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.recco.order.service.entity.TableEntity;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, String> {

    @Query(value = "SELECT DISTINCT table_id FROM tables", nativeQuery = true)
    List<String> findAllTableIds();


}

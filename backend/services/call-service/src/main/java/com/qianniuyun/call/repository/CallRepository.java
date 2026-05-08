package com.qianniuyun.call.repository;

import com.qianniuyun.call.entity.Call;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<Call, String> {
}

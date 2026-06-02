package com.example.companysearch.repository;

import com.example.companysearch.domain.SearchCacheEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCacheRepository extends JpaRepository<SearchCacheEntity, Long> {

    Optional<SearchCacheEntity> findByNormalizedQuery(String normalizedQuery);
}

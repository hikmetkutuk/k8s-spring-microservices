package com.k8sspringmicroservices.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, String> {}

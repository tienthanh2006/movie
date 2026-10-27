package com.cinema.booking.repository;

import com.cinema.booking.model.SeatClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatClaimRepository extends JpaRepository<SeatClaim, String> {}

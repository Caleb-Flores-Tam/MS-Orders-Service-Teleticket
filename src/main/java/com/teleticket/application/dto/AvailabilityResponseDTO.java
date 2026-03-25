package com.teleticket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponseDTO {
    private Long performanceId;               //
    private List<SeatAvailabilityDTO> seats;  //
    private Integer totalSeats;               //
    private Integer availableSeats;           //
    private Integer heldSeats;                //
    private Integer soldSeats;                //
}

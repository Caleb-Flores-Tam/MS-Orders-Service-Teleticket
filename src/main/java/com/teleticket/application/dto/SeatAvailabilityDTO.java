package com.teleticket.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityDTO {
    private Long seatId;             //
    private Long zoneId;             //
    private String row;              //
    private Integer number;          //
    private String seatType;         // - Puedes usar String si no tienes el Enum
    private String status;           // - (AVAILABLE, HELD, SOLD)
    private Long lockedByUserId;     //
}
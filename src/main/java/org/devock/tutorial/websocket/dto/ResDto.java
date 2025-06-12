package org.devock.tutorial.websocket.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResDto {
	
	private String message;
	private LocalDateTime localDateTime;

}

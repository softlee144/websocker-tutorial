package org.devock.tutorial.websocket.controller;

import java.time.LocalDateTime;

import org.devock.tutorial.websocket.dto.ReqDto;
import org.devock.tutorial.websocket.dto.ResDto;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StompController {
	
	@MessageMapping("/hello")    // /app/hello
	@SendTo("/topic/hello")
	public ResDto basic(ReqDto reqDto) {
		log.info("reqDto: {}", reqDto);
		
		return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
	}
	

}

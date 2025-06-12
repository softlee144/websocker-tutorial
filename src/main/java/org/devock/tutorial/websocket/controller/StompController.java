package org.devock.tutorial.websocket.controller;

import java.time.LocalDateTime;
import java.util.Set;

import org.devock.tutorial.websocket.dto.ReqDto;
import org.devock.tutorial.websocket.dto.ResDto;
import org.devock.tutorial.websocket.dto.ResSessionsDto;
import org.devock.tutorial.websocket.listener.StompEventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StompController {
	
	private final StompEventListener eventListener;
	
	public StompController(StompEventListener eventListener) {
		this.eventListener = eventListener;
	}

	@MessageMapping("/hello")    // /app/hello
	@SendTo("/topic/hello")
	public ResDto basic(ReqDto reqDto, Message<ReqDto> message, MessageHeaders headers) {
		log.info("reqDto: {}", reqDto);
		log.info("message: {}", message);
		log.info("headers: {}", headers);
		
		return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
	}
	
	@MessageMapping("/hello/{detail}")    // /app/hello/xxx
	@SendTo({"/topic/hello", "/topic/hello2"})
	public ResDto detail(ReqDto reqDto, @DestinationVariable("detail") String detail) { // @PathVariable과 유사
		log.info("reqDto: {}", reqDto);
		log.info("detail: {}", detail);
		
		return new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
	}
	
	@MessageMapping({"/sessions"})	// /app/sessions
	@SendToUser("/queue/sessions")	// 특정 유저(세션)한테만 보낼 때 SendToUser 사용
	public ResSessionsDto sessions(ReqDto reqDto, MessageHeaders headers) {
		log.info("reqDto: {}", reqDto);
		String sessionId = headers.get("simpSessionId").toString();
		log.info("sessionId: {}", sessionId);
		
		Set<String> sessions = eventListener.getSessions();
		
		
		return new ResSessionsDto(sessions.size(), sessions.stream().toList(), sessionId, LocalDateTime.now());
	}
	

}

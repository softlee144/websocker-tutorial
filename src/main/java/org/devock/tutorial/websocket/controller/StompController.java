package org.devock.tutorial.websocket.controller;

import java.time.LocalDateTime;
import java.util.Set;

import org.devock.tutorial.websocket.dto.ReqDto;
import org.devock.tutorial.websocket.dto.ResDto;
import org.devock.tutorial.websocket.dto.ResSessionsDto;
import org.devock.tutorial.websocket.listener.StompEventListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StompController {
	
	private final StompEventListener eventListener;
	
	private final SimpMessagingTemplate messagingTemplate;
	
	public StompController(StompEventListener eventListener, SimpMessagingTemplate messagingTemplate) {
		this.eventListener = eventListener;
		this.messagingTemplate = messagingTemplate;
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
	

	// 프로그래밍 방식으로 메세지 전달하기
	
	@MessageMapping("/code1")    // /app/code1
	public void code1(ReqDto reqDto, Message<ReqDto> message, MessageHeaders headers) {
		log.info("reqDto: {}", reqDto);
		log.info("message: {}", message);
		log.info("headers: {}", headers);
		
		ResDto resDto = new ResDto(reqDto.getMessage().toUpperCase(), LocalDateTime.now());
		messagingTemplate.convertAndSend("/topic/hello", resDto);
	}
	
	@MessageMapping({"/code2"})	// /app/code2
	public void code2(ReqDto reqDto, MessageHeaders headers) {
		log.info("reqDto: {}", reqDto);
		String sessionId = headers.get("simpSessionId").toString();
		log.info("sessionId: {}", sessionId);
		
		Set<String> sessions = eventListener.getSessions();
		
		ResSessionsDto resSessionsDto = new ResSessionsDto(sessions.size(), sessions.stream().toList(), sessionId, LocalDateTime.now());
		messagingTemplate.convertAndSendToUser(sessionId, "/queue/sessions", resSessionsDto, createHeaders(sessionId));
		
		
	}
	
	private MessageHeaders createHeaders(@Nullable String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

        if (sessionId != null) {
            headerAccessor.setSessionId(sessionId);
        }
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }

}

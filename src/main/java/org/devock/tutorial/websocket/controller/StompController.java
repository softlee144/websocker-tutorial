package org.devock.tutorial.websocket.controller;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class StompController {
	
	private final StompEventListener eventListener;
	
	private final SimpMessagingTemplate messagingTemplate;
	
	private final TaskScheduler taskScheduler;
	
	private final ConcurrentHashMap<String, ScheduledFuture<?>> sessionMap = new ConcurrentHashMap<>();
	
	public StompController(StompEventListener eventListener, SimpMessagingTemplate messagingTemplate, TaskScheduler taskScheduler) {
		this.eventListener = eventListener;
		this.messagingTemplate = messagingTemplate;
		this.taskScheduler = taskScheduler;
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
	
	@MessageMapping({"/start"})	// /app/start
	public void start(ReqDto reqDto, MessageHeaders headers) {
		log.info("start =================================");
		log.info("headers: {}", headers);
		String sessionId = headers.get("simpSessionId").toString();
		log.info("sessionId: {}", sessionId);
		
		// 3초마다 실행
		ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
			Random random = new Random();
			int currentPrice = random.nextInt(100);
			messagingTemplate.convertAndSendToUser(sessionId, "/queue/trade", currentPrice, createHeaders(sessionId));
		}, Duration.ofSeconds(3));
		
		// sessionMap에 scheduledFuture 정보 보관
		sessionMap.put(sessionId, scheduledFuture);
	}
	
	@MessageMapping({"/stop"})	// /app/stop
	public void stop(ReqDto reqDto, MessageHeaders headers) {
		log.info("stop =================================");
		log.info("headers: {}", headers);
		String sessionId = headers.get("simpSessionId").toString();
		log.info("sessionId: {}", sessionId);
		
		// start 메소드에서 실행되던 데이터를 cancel시키기 위함
		ScheduledFuture<?> remove = sessionMap.remove(sessionId);
		remove.cancel(true);

	}
	
	@MessageMapping("/exception")
    @SendTo("/topic/hello")
    public void exception(ReqDto request, MessageHeaders headers) throws Exception {
        log.info("request: {}", request);
        String message = request.getMessage();
        switch(message) {
            case "runtime":
                throw new RuntimeException();
            case "nullPointer":
                throw new NullPointerException();
            case "io":
                throw new IOException();
            case "exception":
                throw new Exception();
            default:
                throw new InvalidParameterException();
        }
    }
	
	

}

# netty-message-sender

`key=value\nkey=value\nkey=value\n` 형태의 메시지를 주고 받는 Netty 기반 Message 전송기

### 예제 코드
```java
	NioMessageSender messageSender = new NioMessageSender("127.0.0.1", 9001); // (1)
	messageSender.setWorkerGroup(new NioEventLoopGroup()); // (2)
	messageSender.setMessageTimeoutMilliseconds(5000); // (3)
    messageSender.init(); // (4)
	messageSender.addCallback(inMap -> { // (5)
		log.info(inMap.entrySet()
			.stream()
			.map(entry -> entry.getKey() + "=" + entry.getValue())
			.collect(Collectors.joining(", ", "[", "]")));
	}, ex -> {
		log.error("{}", ex.getMessage());
	});

	for (int i = 0; i < WORKER_COUNT; i++) {
		Map<String, String> outMap = new HashMap<>();
		outMap.put("KEY1", "VALUE1-" + i);
		outMap.put("KEY2", "VALUE2-" + i);
		outMap.put("KEY3", "VALUE3-" + i);
		messageSender.send(outMap); // (6)
	}
```

### 데이터 송신
1. 접속할 서버 아이피와 포트를 파라메터에 전달한다. (필수)
2. 송신에 사용할 NioEventLoopGroup() 를 전달한다. <br>기본값은 1 개의 EventLoop 만을 사용한다.
3. 수신할 데이터를 기다리는 시간을 설정한다. 기본값은 3000 ms 이다.
4. MessageSender 를 초기화 한다. (필수)
5. 성공, 실패시 호출될 Callback Interface 를 전달한다.
6. Map<String, String> 형태로 데이터 가공 후 send() 메소드를 호출한다.

Map에 저장된 데이터를 다음과 같이 변형되어 전달된다.
```sh
KEY1=VALUE1-0
KEY2=VALUE2-0
KEY3=VALUE3-0
```

### 데이터 수신
서버로 부터 수신된 메시지가 KEY1=xxx\nKEY2=yyy\nKEY3=zzz 이면, Map<String, String> 형태로 저장되어 callback 메소드가 호출된다.

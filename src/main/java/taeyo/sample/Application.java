package taeyo.sample;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import taeyo.sample.netty.CustomService;
import taeyo.sample.netty.NioMessageSender;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Taeyoung, Kim
 */
@Slf4j
@SpringBootApplication
public class Application {

    private static final int WORKER_COUNT = 10;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    private NioMessageSender messageSender;

    @Autowired
    private CustomService customService;

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NioMessageSender nioMessageSender() {
        NioMessageSender messageSender = new NioMessageSender("127.0.0.1", 9001);
        messageSender.setWorkerGroup(new NioEventLoopGroup(4));
        messageSender.setMessageTimeoutMilliseconds(3000);
        messageSender.init();
        messageSender.setSuccessCallback(inMap -> customService.doIt(inMap));
        messageSender.setFailureCallback(ex -> log.error("{}", ex.toString()));
        return messageSender;
    }

    @Bean
    public CommandLineRunner commandLineRunner() {

        return args -> {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                for (int i = 0; i < WORKER_COUNT; i++) {
                    Map<String, String> outMap = new HashMap<>();
                    outMap.put("KEY1", "VALUE1-" + i);
                    outMap.put("KEY2", "VALUE2-" + i);
                    outMap.put("KEY3", "VALUE3-" + i);
                    messageSender.send(outMap);
                }
            }, 0, 10, TimeUnit.SECONDS);
        };
    }
}

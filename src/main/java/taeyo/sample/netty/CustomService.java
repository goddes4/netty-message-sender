package taeyo.sample.netty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 *
 * @author Taeyoung, Kim
 */
@Slf4j
@Transactional
@Service
public class CustomService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void doIt(Map<String, String> inMap) {
        log.info(inMap.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(joining(", ", "[", "]")));

        namedParameterJdbcTemplate.update("INSERT INTO test (KEY1, KEY2, KEY3) VALUES (:KEY1, :KEY2, :KEY3)", inMap);

        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM TEST");
        log.info("db : {}", results.stream().map(map -> map.toString()).collect(joining("\n", "\n===================\n", "")));
    }
}

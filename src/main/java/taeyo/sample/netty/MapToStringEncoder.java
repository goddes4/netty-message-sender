package taeyo.sample.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Taeyoung, Kim
 */
public class MapToStringEncoder extends MessageToMessageEncoder<Map<String, String>> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Map<String, String> outMap, List<Object> out) throws Exception {
        for (Map.Entry<String, String> entry : outMap.entrySet()) {
            out.add(entry.getKey() + "=" + entry.getValue() + "\n");
        }
    }
}

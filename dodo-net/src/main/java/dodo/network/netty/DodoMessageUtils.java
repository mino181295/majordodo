/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package dodo.network.netty;

import dodo.network.Message;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author enrico.olivelli
 */
public class DodoMessageUtils {

    private static final byte VERSION = 'a';

    private static final byte OPCODE_REPLYMESSAGEID = 1;
    private static final byte OPCODE_WORKERPROCESSID = 2;
    private static final byte OPCODE_STRING_PARAMETER = 3;
    private static final byte OPCODE_INT_PARAMETER = 4;
    private static final byte OPCODE_LONG_PARAMETER = 5;

    private static void writeUTF8String(ByteBuf buf, String s) {
        byte[] asarray = s.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(asarray.length);
        buf.writeBytes(asarray);
    }

    private static String readUTF8String(ByteBuf buf) {
        int len = buf.readInt();
        byte[] s = new byte[len];
        buf.readBytes(s);
        return new String(s, StandardCharsets.UTF_8);
    }

    public static void encodeMessage(ByteBuf encoded, Message m) {
        encoded.writeByte(VERSION);
        encoded.writeInt(m.type);
        writeUTF8String(encoded, m.messageId);
        if (m.replyMessageId != null) {
            encoded.writeByte(OPCODE_REPLYMESSAGEID);
            writeUTF8String(encoded, m.replyMessageId);
        }
        if (m.workerProcessId != null) {
            encoded.writeByte(OPCODE_WORKERPROCESSID);
            writeUTF8String(encoded, m.workerProcessId);
        }
        if (m.parameters != null) {
            for (Map.Entry<String, Object> p : m.parameters.entrySet()) {
                if (p.getKey() != null && p.getValue() != null) {
                    Object value = p.getValue();
                    if (value instanceof String) {
                        encoded.writeByte(OPCODE_STRING_PARAMETER);
                        writeUTF8String(encoded, p.getKey());
                        writeUTF8String(encoded, (String) value);
                    } else if (value instanceof Long) {
                        encoded.writeByte(OPCODE_LONG_PARAMETER);
                        writeUTF8String(encoded, p.getKey());
                        encoded.writeLong((Long) value);
                    } else if (value instanceof Integer) {
                        encoded.writeByte(OPCODE_INT_PARAMETER);
                        writeUTF8String(encoded, p.getKey());
                        encoded.writeInt((Integer) value);
                    } else {
                        throw new RuntimeException("bad parameter type key= " + p.getKey() + ", class =" + p.getClass());
                    }
                }
            }
        }

    }

    public static Message decodeMessage(ByteBuf encoded) {
        byte version = encoded.readByte();
        if (version != VERSION) {
            throw new RuntimeException("bad protocol version " + version);
        }
        int type = encoded.readInt();
        String messageId = readUTF8String(encoded);
        String replyMessageId = null;
        String workerProcessId = null;
        Map<String, Object> params = new HashMap<>();
        while (encoded.isReadable()) {
            byte opcode = encoded.readByte();
            switch (opcode) {
                case OPCODE_REPLYMESSAGEID:
                    replyMessageId = readUTF8String(encoded);
                    break;
                case OPCODE_WORKERPROCESSID:
                    workerProcessId = readUTF8String(encoded);
                    break;
                case OPCODE_INT_PARAMETER: {
                    String key = readUTF8String(encoded);
                    int p = encoded.readInt();
                    params.put(key, p);
                }
                break;
                case OPCODE_LONG_PARAMETER: {
                    String key = readUTF8String(encoded);
                    long p = encoded.readLong();
                    params.put(key, p);
                }
                break;
                case OPCODE_STRING_PARAMETER: {
                    String key = readUTF8String(encoded);
                    String p = readUTF8String(encoded);
                    params.put(key, p);
                }
                break;
            }
        }
        Message m = new Message(workerProcessId, type, params);
        if (replyMessageId != null) {
            m.replyMessageId = replyMessageId;
        }
        m.messageId = messageId;
        return m;

    }
}

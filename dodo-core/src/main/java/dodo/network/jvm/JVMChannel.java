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
package dodo.network.jvm;

import dodo.network.Channel;
import dodo.network.Message;
import dodo.network.ReplyCallback;
import dodo.network.SendResultCallback;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-JVM comunications
 *
 * @author enrico.olivelli
 */
public class JVMChannel extends Channel {

    private volatile boolean active = false;
    private final Map<String, ReplyCallback> pendingReplyMessages = new ConcurrentHashMap<>();
    private final Map<String, Message> pendingReplyMessagesSource = new ConcurrentHashMap<>();
    private JVMChannel otherSide;
    private final ExecutorService callbackexecutor = Executors.newCachedThreadPool();
    private final ExecutorService executionserializer = Executors.newFixedThreadPool(1);

    public JVMChannel() {
    }

    private void receiveMessageFromPeer(Message message) {
//        System.out.println("receiveMessageFromPeer:" + message);
        if (message.getReplyMessageId() != null) {
            handleReply(message);
        } else {
            try {
                messagesReceiver.messageReceived(message);
            } catch (Throwable t) {
                t.printStackTrace();
                close();
            }
        }
    }

    public void setOtherSide(JVMChannel brokerSide) {
        this.otherSide = brokerSide;
        this.active = true;
    }

    @Override
    public void sendOneWayMessage(Message message, SendResultCallback callback) {
//        System.out.println("[JVM] sendOneWayMessage " + message);
        if (!active) {
            return;
        }
        executionserializer.submit(() -> {
            otherSide.receiveMessageFromPeer(message);
            callbackexecutor.submit(() -> {
                callback.messageSent(message, null);
            });
        });

    }

    private void handleReply(Message anwermessage) {

        final ReplyCallback callback = pendingReplyMessages.get(anwermessage.getReplyMessageId());
//        System.out.println("[JVM] handleReply " + anwermessage + " callback=" + callback + ", pendingReplyMessages=" + pendingReplyMessages);
        if (callback != null) {
            pendingReplyMessages.remove(anwermessage.getReplyMessageId());
            Message original = pendingReplyMessagesSource.remove(anwermessage.getReplyMessageId());
            if (original != null) {
                callbackexecutor.submit(() -> {
                    callback.replyReceived(original, anwermessage, null);
                });
            }
        }
    }

    @Override
    public void sendReplyMessage(Message inAnswerTo, Message message) {
        executionserializer.submit(() -> {
//        System.out.println("[JVM] sendReplyMessage inAnswerTo=" + inAnswerTo.getMessageId() + " newmessage=" + message);
            if (!active) {
                System.out.println("[JVM] channel not active, discarding reply message " + message);
                return;
            }
            message.setMessageId(UUID.randomUUID().toString());
            message.setReplyMessageId(inAnswerTo.messageId);
            otherSide.receiveMessageFromPeer(message);
        });
    }

    @Override
    public void sendMessageWithAsyncReply(Message message, ReplyCallback callback) {
        executionserializer.submit(() -> {
//        System.out.println("[JVM] sendMessageWithAsyncReply " + message);
            if (!active) {
                callbackexecutor.submit(() -> {
                    callback.replyReceived(message, null, new Exception("connection is not active"));
                });
                return;
            }
            message.setMessageId(UUID.randomUUID().toString());
            pendingReplyMessages.put(message.getMessageId(), callback);
            pendingReplyMessagesSource.put(message.getMessageId(), message);
            otherSide.receiveMessageFromPeer(message);
        });
    }

    @Override
    public void close() {
        active = false;
        pendingReplyMessages.forEach((key, callback) -> {
            callbackexecutor.submit(() -> {
                Message original = pendingReplyMessagesSource.remove(key);
                if (original != null) {
                    callback.replyReceived(original, null, new Exception("comunication channel closed"));
                }
            });
        });
        pendingReplyMessages.clear();

        if (otherSide.active) {
            otherSide.close();
        }
        executionserializer.shutdown();
        callbackexecutor.shutdown();
    }

}

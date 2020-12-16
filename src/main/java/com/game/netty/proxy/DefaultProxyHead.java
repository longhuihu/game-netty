/*
 * Copyright 2020 Long Huihu
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


package com.game.netty.proxy;

/**
 * A default proxy header type, contain three field：
 * <p>
 * 1. sessionId, a global identify to the channel between client and proxy
 * 2. ip, the client ip address
 * 3. userId, the user id if client has login, if the type is not applicable for you , I'm sorry
 */
public class DefaultProxyHead {

    private final String sessionId;
    private final String ip;
    private final long userId;


    public DefaultProxyHead() {
        this.sessionId = null;
        this.ip = null;
        this.userId = 0;
    }

    public DefaultProxyHead(String sessionId) {
        this.sessionId = sessionId;
        this.ip = null;
        this.userId = 0;
    }

    public DefaultProxyHead(String sessionId, String ip, long userId) {
        this.sessionId = sessionId;
        this.ip = ip;
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getIp() {
        return ip;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "DefaultProxyHead{" +
                "sessionId='" + sessionId + '\'' +
                ", ip='" + ip + '\'' +
                ", userId=" + userId +
                '}';
    }
}

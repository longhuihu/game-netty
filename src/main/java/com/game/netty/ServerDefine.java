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
package com.game.netty;

/**
 * a server is a target for tcp connection
 */
public final class ServerDefine {
    private int serverId;
    private String ip;
    private int port;

    public ServerDefine(int serverId, String ip, int port) {
        this.serverId = serverId;
        this.ip = ip;
        this.port = port;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServerDefine) {
            return (((ServerDefine) obj).port == port)
                    && (((ServerDefine) obj).ip.equals(ip))
                    && (((ServerDefine) obj).serverId == serverId);
        }
        return false;
    }

    public String identifier() {
        return serverId + ":" + ip + ":" + port;
    }

    @Override
    public String toString() {
        return identifier();
    }

    @Override
    public int hashCode() {
        int result = serverId;
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }
}

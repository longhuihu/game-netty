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

package com.game.netty.netty;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

/**
 * GameNettyEnv is abstraction for netty environment configuration
 */
public interface GameNettyEnv {

    GameNettyEnv DEFAULT = GameNettyUtil.ENV;

    /**
     * @return Netty ServerChannel class
     */
    Class<? extends ServerChannel> serverChannelClass();

    /**
     * @return Netty tcp channel class
     */
    Class<? extends Channel> clientChannelClass();

    /**
     * @return boss event loop group
     */
    EventLoopGroup createBossEventLoopGroup();

    /**
     * @param nThreads, 0 for default
     * @return work event loop group
     */
    EventLoopGroup createWorkerEventLoopGroup(int nThreads);
}




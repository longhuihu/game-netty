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

package com.game.netty.client.acceptor.session;

import com.game.netty.client.acceptor.ClientAcceptedChannel;

/**
 * session is a info bundle attached to ClientAcceptedChannel, user can implement this interface to append more information.
 * <p>
 * The user should make sure, session id is unique globally among all the established channels
 */
public interface SessionInterface {
    String sessionId();

    ClientAcceptedChannel channel();
}

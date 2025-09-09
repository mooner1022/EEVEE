/*
 * Events.kt created by Minki Moon(mooner1022) on 22. 2. 2. 오후 6:08
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.eevee.event

import dev.mooner.eevee.UserMode
import dev.mooner.eevee.logger.LogData
import kotlinx.coroutines.CoroutineScope

sealed class Events {

    sealed class Log {

        class Create(
            val log: LogData,
            val coroutineScope: CoroutineScope = eventHandlerScope()
        ): Log(), Event, CoroutineScope by coroutineScope
    }

    sealed class Config {

        class GlobalConfigUpdate(
            val key: String,
            val value: Any,
            val coroutineScope: CoroutineScope = eventHandlerScope()
        ): Config(), Event, CoroutineScope by coroutineScope
    }

    sealed class Camera {

        class OverlayRelease(
            val coroutineScope: CoroutineScope = eventHandlerScope()
        ): Camera(), Event, CoroutineScope by coroutineScope
    }

    sealed class MDM {

        class UserModeUpdate(
            val userMode: UserMode,
            val coroutineScope: CoroutineScope = eventHandlerScope()
        ): MDM(), Event, CoroutineScope by coroutineScope

        class LockStateUpdate(
            val locked: Boolean,
            val coroutineScope: CoroutineScope = eventHandlerScope()
        ): MDM(), Event, CoroutineScope by coroutineScope
    }
}
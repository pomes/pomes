package com.github.pomes.cli.utility

import ch.qos.logback.classic.Level

enum LogLevel {
    error(Level.ERROR.levelStr, Level.ERROR),
    warn(Level.WARN.levelStr, Level.WARN),
    info(Level.INFO.levelStr, Level.INFO),
    debug(Level.DEBUG.levelStr, Level.DEBUG),
    trace(Level.TRACE.levelStr, Level.TRACE),
    all(Level.ALL.levelStr, Level.ALL),
    off(Level.OFF.levelStr, Level.OFF)

    String value
    Level level

    LogLevel(String val, Level lvl) {
        this.value = val
        this.level = lvl
    }
}

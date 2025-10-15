package com.infosecconference.desktop.util;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SQLRunnable {
    void run(Connection connection) throws SQLException;
}

package com.wizm.wizmcrates.data;

import com.wizm.wizmcrates.WizmCrates;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final WizmCrates plugin;
    private HikariDataSource ds;
    private String prefix;

    public DatabaseManager(WizmCrates plugin) { this.plugin = plugin; }

    public void connect() {
        if (!plugin.cfg().mysqlEnabled()) return;
        prefix = plugin.cfg().tablePrefix();
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:mysql://" + plugin.cfg().mysqlHost() + ":" + plugin.cfg().mysqlPort() + "/"
            + plugin.cfg().mysqlDb() + "?useSSL=" + plugin.cfg().mysqlSsl() + "&characterEncoding=utf8&autoReconnect=true");
        hc.setUsername(plugin.cfg().mysqlUser());
        hc.setPassword(plugin.cfg().mysqlPass());
        hc.setMaximumPoolSize(6);
        hc.setPoolName("WizmCrates");
        try { ds = new HikariDataSource(hc); createTables(); plugin.getLogger().info("MySQL connesso."); }
        catch (Exception e) { plugin.getLogger().severe("MySQL: " + e.getMessage()); }
    }

    private void createTables() throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "keys (player VARCHAR(36), crate VARCHAR(64), amount INT DEFAULT 0, PRIMARY KEY(player,crate))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "stats (player VARCHAR(36), crate VARCHAR(64), opened INT DEFAULT 0, PRIMARY KEY(player,crate))");
        }
    }

    public void disconnect() { if (ds != null && !ds.isClosed()) ds.close(); }
    public boolean isReady() { return ds != null && !ds.isClosed(); }

    public int getKeys(UUID u, String crate) {
        if (!isReady()) return 0;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT amount FROM " + prefix + "keys WHERE player=? AND crate=?")) {
            ps.setString(1, u.toString()); ps.setString(2, crate);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void addKeys(UUID u, String crate, int delta) {
        if (!isReady()) return;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO " + prefix + "keys (player,crate,amount) VALUES (?,?,?) ON DUPLICATE KEY UPDATE amount = amount + ?")) {
            ps.setString(1, u.toString()); ps.setString(2, crate); ps.setInt(3, delta); ps.setInt(4, delta);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getOpened(UUID u, String crate) {
        if (!isReady()) return 0;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT opened FROM " + prefix + "stats WHERE player=? AND crate=?")) {
            ps.setString(1, u.toString()); ps.setString(2, crate);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int incrementOpened(UUID u, String crate) {
        if (!isReady()) return 0;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO " + prefix + "stats (player,crate,opened) VALUES (?,?,1) ON DUPLICATE KEY UPDATE opened = opened + 1")) {
            ps.setString(1, u.toString()); ps.setString(2, crate);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
        return getOpened(u, crate);
    }

    public Map<UUID, Integer> topOpened(String crate, int limit) {
        Map<UUID, Integer> map = new LinkedHashMap<>();
        if (!isReady()) return map;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT player, opened FROM " + prefix + "stats WHERE crate=? ORDER BY opened DESC LIMIT ?")) {
            ps.setString(1, crate); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { try { map.put(UUID.fromString(rs.getString(1)), rs.getInt(2)); } catch (Exception ignored) {} }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }
}

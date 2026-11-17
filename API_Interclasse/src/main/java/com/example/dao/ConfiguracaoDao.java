package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.example.util.Conexao;

public class ConfiguracaoDao {

    public Map<String, String> listarTodas() {
        Map<String, String> configs = new HashMap<>();
        String sql = "SELECT chave, valor FROM configuracao";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                configs.put(rs.getString("chave"), rs.getString("valor"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return configs;
    }

    public String obterValor(String chave, String valorPadrao) {
        String sql = "SELECT valor FROM configuracao WHERE chave = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chave);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("valor");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return valorPadrao;
    }

    public void atualizar(String chave, String valor) {
        String sql = "UPDATE configuracao SET valor = ? WHERE chave = ?";
        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, valor);
            stmt.setString(2, chave);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
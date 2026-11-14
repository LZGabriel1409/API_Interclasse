package com.example.dao;

import com.example.model.Partida;
import com.example.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela partida. */
public class PartidaDao {
    public List<Partida> listar() throws SQLException {
        List<Partida> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM partida ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Partida buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM partida WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Partida inserir(Partida item) throws SQLException {
        String sql = "INSERT INTO partida (modalidade_id, time_a_id, time_b_id, placar_a, placar_b, data, hora, local, fase, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, item.getModalidadeId());
            ps.setObject(2, item.getTimeAId());
            ps.setObject(3, item.getTimeBId());
            ps.setObject(4, item.getPlacarA());
            ps.setObject(5, item.getPlacarB());
            ps.setString(6, item.getData());
            ps.setString(7, item.getHora());
            ps.setString(8, item.getLocal());
            ps.setString(9, item.getFase());
            ps.setString(10, item.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        return item;
    }

    public boolean atualizar(Partida item) throws SQLException {
        String sql = "UPDATE partida SET modalidade_id = ?, time_a_id = ?, time_b_id = ?, placar_a = ?, placar_b = ?, data = ?, hora = ?, local = ?, fase = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
            ps.setObject(1, item.getModalidadeId());
            ps.setObject(2, item.getTimeAId());
            ps.setObject(3, item.getTimeBId());
            ps.setObject(4, item.getPlacarA());
            ps.setObject(5, item.getPlacarB());
            ps.setString(6, item.getData());
            ps.setString(7, item.getHora());
            ps.setString(8, item.getLocal());
            ps.setString(9, item.getFase());
            ps.setString(10, item.getStatus());
            ps.setObject(10, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("DELETE FROM partida WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Partida mapear(ResultSet rs) throws SQLException {
        Partida item = new Partida();
        int id = rs.getInt("id");
        item.setId(rs.wasNull() ? null : id);
        int modalidadeId = rs.getInt("modalidade_id");
        item.setModalidadeId(rs.wasNull() ? null : modalidadeId);
        int timeAId = rs.getInt("time_a_id");
        item.setTimeAId(rs.wasNull() ? null : timeAId);
        int timeBId = rs.getInt("time_b_id");
        item.setTimeBId(rs.wasNull() ? null : timeBId);
        int placarA = rs.getInt("placar_a");
        item.setPlacarA(rs.wasNull() ? null : placarA);
        int placarB = rs.getInt("placar_b");
        item.setPlacarB(rs.wasNull() ? null : placarB);
        item.setData(rs.getString("data"));
        item.setHora(rs.getString("hora"));
        item.setLocal(rs.getString("local"));
        item.setFase(rs.getString("fase"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}

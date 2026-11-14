package com.example.dao;

import com.example.model.Inscricao;
import com.example.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela inscricao. */
public class InscricaoDao {
    public List<Inscricao> listar() throws SQLException {
        List<Inscricao> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM inscricao ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Inscricao buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM inscricao WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Inscricao inserir(Inscricao item) throws SQLException {
        String sql = "INSERT INTO inscricao (time_id, modalidade_id, data_inscricao, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, item.getTimeId());
            ps.setObject(2, item.getModalidadeId());
            ps.setString(3, item.getDataInscricao());
            ps.setString(4, item.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        return item;
    }

    public boolean atualizar(Inscricao item) throws SQLException {
        String sql = "UPDATE inscricao SET time_id = ?, modalidade_id = ?, data_inscricao = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
            ps.setObject(1, item.getTimeId());
            ps.setObject(2, item.getModalidadeId());
            ps.setString(3, item.getDataInscricao());
            ps.setString(4, item.getStatus());
            ps.setObject(4, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("DELETE FROM inscricao WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Inscricao mapear(ResultSet rs) throws SQLException {
        Inscricao item = new Inscricao();
        int id = rs.getInt("id");
        item.setId(rs.wasNull() ? null : id);
        int timeId = rs.getInt("time_id");
        item.setTimeId(rs.wasNull() ? null : timeId);
        int modalidadeId = rs.getInt("modalidade_id");
        item.setModalidadeId(rs.wasNull() ? null : modalidadeId);
        item.setDataInscricao(rs.getString("data_inscricao"));
        item.setStatus(rs.getString("status"));
        return item;
    }
}

package com.example.dao;

import com.example.model.Modalidade;
import com.example.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela modalidade. */
public class ModalidadeDao {
    public List<Modalidade> listar() throws SQLException {
        List<Modalidade> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM modalidade ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Modalidade buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM modalidade WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Modalidade inserir(Modalidade item) throws SQLException {
        String sql = "INSERT INTO modalidade (nome, ativo) VALUES (?, ?)";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getNome());
            ps.setObject(2, item.getAtivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        return item;
    }

    public boolean atualizar(Modalidade item) throws SQLException {
        String sql = "UPDATE modalidade SET nome = ?, ativo = ? WHERE id = ?";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
            ps.setString(1, item.getNome());
            ps.setObject(2, item.getAtivo());
            ps.setObject(2, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("DELETE FROM modalidade WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Modalidade mapear(ResultSet rs) throws SQLException {
        Modalidade item = new Modalidade();
        int id = rs.getInt("id");
        item.setId(rs.wasNull() ? null : id);
        item.setNome(rs.getString("nome"));
        int ativo = rs.getInt("ativo");
        item.setAtivo(rs.wasNull() ? null : ativo);
        return item;
    }
}

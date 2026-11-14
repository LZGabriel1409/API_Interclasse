package com.example.dao;

import com.example.model.Turma;
import com.example.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela turma. */
public class TurmaDao {
    public List<Turma> listar() throws SQLException {
        List<Turma> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM turma ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Turma buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM turma WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Turma inserir(Turma item) throws SQLException {
        String sql = "INSERT INTO turma (nome, curso, ano) VALUES (?, ?, ?)";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getNome());
            ps.setString(2, item.getCurso());
            ps.setObject(3, item.getAno());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
        return item;
    }

    public boolean atualizar(Turma item) throws SQLException {
        String sql = "UPDATE turma SET nome = ?, curso = ?, ano = ? WHERE id = ?";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
            ps.setString(1, item.getNome());
            ps.setString(2, item.getCurso());
            ps.setObject(3, item.getAno());
            ps.setObject(3, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("DELETE FROM turma WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Turma mapear(ResultSet rs) throws SQLException {
        Turma item = new Turma();
        int id = rs.getInt("id");
        item.setId(rs.wasNull() ? null : id);
        item.setNome(rs.getString("nome"));
        item.setCurso(rs.getString("curso"));
        int ano = rs.getInt("ano");
        item.setAno(rs.wasNull() ? null : ano);
        return item;
    }
}

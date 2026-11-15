package com.example.dao;

import com.example.model.Turma;
import com.example.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela turma. */
public class TurmaDao {

    private static final String SQL_LISTAR = "SELECT * FROM turma ORDER BY id";
    private static final String SQL_BUSCAR = "SELECT * FROM turma WHERE id = ?";
    private static final String SQL_INSERIR = "INSERT INTO turma (nome, curso, ano) VALUES (?, ?, ?)";
    private static final String SQL_ATUALIZAR = "UPDATE turma SET nome = ?, curso = ?, ano = ? WHERE id = ?";
    private static final String SQL_DELETAR = "DELETE FROM turma WHERE id = ?";

    public List<Turma> listar() throws SQLException {
        List<Turma> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Turma buscarPorId(int id) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Turma inserir(Turma item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getNome());
            ps.setString(2, item.getCurso());
            ps.setObject(3, item.getAno());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    public boolean atualizar(Turma item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
            ps.setString(1, item.getNome());
            ps.setString(2, item.getCurso());
            ps.setObject(3, item.getAno());
            ps.setObject(4, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETAR)) {
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

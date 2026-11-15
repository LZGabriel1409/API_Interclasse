package com.example.dao;

import com.example.model.Modalidade;
import com.example.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela modalidade. */
public class ModalidadeDao {

    private static final String SQL_LISTAR = "SELECT * FROM modalidade ORDER BY id";
    private static final String SQL_BUSCAR = "SELECT * FROM modalidade WHERE id = ?";
    private static final String SQL_BUSCAR_NOME = "SELECT * FROM modalidade WHERE nome = ?";
    private static final String SQL_INSERIR = "INSERT INTO modalidade (nome, ativo) VALUES (?, ?)";
    private static final String SQL_ATUALIZAR = "UPDATE modalidade SET nome = ?, ativo = ? WHERE id = ?";
    private static final String SQL_ALTERAR_ATIVO = "UPDATE modalidade SET ativo = ? WHERE id = ?";
    private static final String SQL_DELETAR = "DELETE FROM modalidade WHERE id = ?";

    public List<Modalidade> listar() throws SQLException {
        List<Modalidade> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Modalidade buscarPorId(int id) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Modalidade buscarPorNome(String nome) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_NOME)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Modalidade inserir(Modalidade item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getNome());
            ps.setObject(2, item.getAtivo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    public boolean atualizar(Modalidade item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
            ps.setString(1, item.getNome());
            ps.setObject(2, item.getAtivo());
            ps.setObject(3, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Ativa (1) ou desativa (0) a modalidade. */
    public boolean alterarAtivo(int id, int ativo) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ALTERAR_ATIVO)) {
            ps.setInt(1, ativo);
            ps.setInt(2, id);
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

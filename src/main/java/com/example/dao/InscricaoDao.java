package com.example.dao;

import com.example.model.Inscricao;
import com.example.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela inscricao. */
public class InscricaoDao {

    private static final String SQL_LISTAR = "SELECT * FROM inscricao ORDER BY id";
    private static final String SQL_LISTAR_MODALIDADE = "SELECT * FROM inscricao WHERE modalidade_id = ? ORDER BY id";
    private static final String SQL_BUSCAR = "SELECT * FROM inscricao WHERE id = ?";
    private static final String SQL_BUSCAR_TIME = "SELECT * FROM inscricao WHERE time_id = ?";
    private static final String SQL_CONFIRMADA =
            "SELECT COUNT(*) FROM inscricao WHERE time_id = ? AND modalidade_id = ? AND status = 'CONFIRMADA'";
    private static final String SQL_INSERIR =
            "INSERT INTO inscricao (time_id, modalidade_id, data_inscricao, status) VALUES (?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE inscricao SET time_id = ?, modalidade_id = ?, data_inscricao = ?, status = ? WHERE id = ?";
    private static final String SQL_ATUALIZAR_STATUS = "UPDATE inscricao SET status = ? WHERE id = ?";
    private static final String SQL_DELETAR = "DELETE FROM inscricao WHERE id = ?";

    public List<Inscricao> listar() throws SQLException {
        List<Inscricao> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Inscricao> listarPorModalidade(int modalidadeId) throws SQLException {
        List<Inscricao> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_MODALIDADE)) {
            ps.setInt(1, modalidadeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Inscricao buscarPorId(int id) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Inscricao do time (a tabela permite apenas uma por time). */
    public Inscricao buscarPorTime(int timeId) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_TIME)) {
            ps.setInt(1, timeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** True quando o time tem inscricao CONFIRMADA na modalidade informada. */
    public boolean temInscricaoConfirmada(int timeId, int modalidadeId) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_CONFIRMADA)) {
            ps.setInt(1, timeId);
            ps.setInt(2, modalidadeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Inscricao inserir(Inscricao item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, item.getTimeId());
            ps.setObject(2, item.getModalidadeId());
            ps.setString(3, item.getDataInscricao());
            ps.setString(4, item.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    public boolean atualizar(Inscricao item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
            ps.setObject(1, item.getTimeId());
            ps.setObject(2, item.getModalidadeId());
            ps.setString(3, item.getDataInscricao());
            ps.setString(4, item.getStatus());
            ps.setObject(5, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean atualizarStatus(int id, String status) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR_STATUS)) {
            ps.setString(1, status);
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

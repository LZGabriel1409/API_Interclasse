package com.example.dao;

import com.example.model.Partida;
import com.example.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela partida. */
public class PartidaDao {

    private static final String SQL_LISTAR = "SELECT * FROM partida ORDER BY data, hora, id";
    private static final String SQL_BUSCAR = "SELECT * FROM partida WHERE id = ?";
    private static final String SQL_INSERIR =
            "INSERT INTO partida (modalidade_id, time_a_id, time_b_id, placar_a, placar_b, data, hora, local, fase, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR =
            "UPDATE partida SET modalidade_id = ?, time_a_id = ?, time_b_id = ?, placar_a = ?, placar_b = ?, "
                    + "data = ?, hora = ?, local = ?, fase = ?, status = ? WHERE id = ?";
    private static final String SQL_ATUALIZAR_PLACAR =
            "UPDATE partida SET placar_a = ?, placar_b = ? WHERE id = ?";
    private static final String SQL_ATUALIZAR_STATUS = "UPDATE partida SET status = ? WHERE id = ?";
    private static final String SQL_DELETAR = "DELETE FROM partida WHERE id = ?";

    public List<Partida> listar() throws SQLException {
        List<Partida> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Lista com filtros opcionais (modalidade, status e data). */
    public List<Partida> listarComFiltros(Integer modalidadeId, String status, String data) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM partida");
        List<String> condicoes = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        if (modalidadeId != null) {
            condicoes.add("modalidade_id = ?");
            parametros.add(modalidadeId);
        }
        if (status != null && !status.isBlank()) {
            condicoes.add("status = ?");
            parametros.add(status);
        }
        if (data != null && !data.isBlank()) {
            condicoes.add("data = ?");
            parametros.add(data);
        }
        if (!condicoes.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", condicoes));
        }
        sql.append(" ORDER BY data, hora, id");

        List<Partida> lista = new ArrayList<>();
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public Partida buscarPorId(int id) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Partida inserir(Partida item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
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
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        }
        return item;
    }

    public boolean atualizar(Partida item) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
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
            ps.setObject(11, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean atualizarPlacar(int id, int placarA, int placarB) throws SQLException {
        Connection conn = Conexao.getConexao();
        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR_PLACAR)) {
            ps.setInt(1, placarA);
            ps.setInt(2, placarB);
            ps.setInt(3, id);
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

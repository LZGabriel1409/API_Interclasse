package com.example.dao;

import com.example.model.Configuracao;
import com.example.util.Conexao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Operacoes CRUD da tabela configuracao. */
public class ConfiguracaoDao {
    public List<Configuracao> listar() throws SQLException {
        List<Configuracao> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM configuracao ORDER BY chave");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Configuracao buscarPorChave(String chave) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT * FROM configuracao WHERE chave = ?")) {
            ps.setString(1, chave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Configuracao inserir(Configuracao item) throws SQLException {
        String sql = "INSERT INTO configuracao (chave, valor) VALUES (?, ?)";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getChave());
            ps.setString(2, item.getValor());
            ps.executeUpdate();
        }
        return item;
    }

    public boolean atualizar(Configuracao item) throws SQLException {
        String sql = "UPDATE configuracao SET valor = ? WHERE chave = ?";
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
            ps.setString(1, item.getValor());
            ps.setString(2, item.getChave());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(String chave) throws SQLException {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("DELETE FROM configuracao WHERE chave = ?")) {
            ps.setString(1, chave);
            return ps.executeUpdate() > 0;
        }
    }

    private Configuracao mapear(ResultSet rs) throws SQLException {
        Configuracao item = new Configuracao();
        item.setChave(rs.getString("chave"));
        item.setValor(rs.getString("valor"));
        return item;
    }
}

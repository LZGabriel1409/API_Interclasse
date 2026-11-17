package com.example.controller;

import com.example.util.Conexao;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/** Cadastro de atletas e ranking individual de gols/assistencias. */
@WebServlet("/jogadores/*")
public class JogadorController extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!SessaoUtil.estaLogado(req)) { JsonUtil.enviarErro(resp, 401, "faca login para consultar jogadores"); return; }
        String path = req.getPathInfo();
        if (path != null && !path.equals("/") && !path.isBlank() && !path.equals("/ranking")) { JsonUtil.enviarErro(resp,404,"rota nao encontrada"); return; }
        String sql = "SELECT j.id,j.nome,j.time_id,t.nome AS nomeTime,j.gols,j.assistencias FROM jogador j JOIN time t ON t.id=j.time_id ORDER BY j.gols DESC,j.assistencias DESC,j.nome COLLATE NOCASE";
        try (Connection c=Conexao.getConexao(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()) {
            List<Map<String,Object>> lista=new ArrayList<>(); int pos=0;
            while(rs.next()) { Map<String,Object> item=new LinkedHashMap<>(); item.put("posicao",++pos); item.put("id",rs.getInt("id")); item.put("nome",rs.getString("nome")); item.put("timeId",rs.getInt("time_id")); item.put("nomeTime",rs.getString("nomeTime")); item.put("gols",rs.getInt("gols")); item.put("assistencias",rs.getInt("assistencias")); lista.add(item); }
            JsonUtil.enviar(resp,lista);
        } catch(SQLException e){ JsonUtil.enviarErro(resp,500,"erro ao consultar ranking de jogadores"); }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        if (!SessaoUtil.estaLogado(req)) { JsonUtil.enviarErro(resp,401,"faca login para continuar"); return; }
        if (!SessaoUtil.ehOrganizador(req)) { JsonUtil.enviarErro(resp,403,"acesso restrito a organizadores"); return; }
        String path=req.getPathInfo();
        if(path!=null && !path.equals("/") && !path.isBlank()){JsonUtil.enviarErro(resp,404,"rota nao encontrada");return;}
        Jogador j=JsonUtil.ler(req,Jogador.class);
        if(j==null || !JsonUtil.preenchido(j.nome) || j.timeId==null || j.timeId<1){JsonUtil.enviarErro(resp,400,"nome e timeId sao obrigatorios");return;}
        if (j.gols == null) j.gols = 0;
        if (j.assistencias == null) j.assistencias = 0;
        if(j.gols<0 || j.assistencias<0){JsonUtil.enviarErro(resp,400,"gols e assistencias nao podem ser negativos");return;}
        try(Connection c=Conexao.getConexao()){
            try(PreparedStatement check=c.prepareStatement("SELECT id FROM time WHERE id=?")){check.setInt(1,j.timeId);try(ResultSet rs=check.executeQuery()){if(!rs.next()){JsonUtil.enviarErro(resp,404,"time nao encontrado");return;}}}
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO jogador(nome,time_id,gols,assistencias) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                ps.setString(1,j.nome.trim());ps.setInt(2,j.timeId);ps.setInt(3,j.gols);ps.setInt(4,j.assistencias);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(rs.next())j.id=rs.getInt(1);}}
            JsonUtil.enviar(resp,j);
        }catch(SQLException e){JsonUtil.enviarErro(resp,500,"nao foi possivel cadastrar o jogador");}
    }
    @Override protected void doPut(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        if (!SessaoUtil.estaLogado(req)) { JsonUtil.enviarErro(resp,401,"faca login para continuar"); return; }
        if (!SessaoUtil.ehOrganizador(req)) { JsonUtil.enviarErro(resp,403,"acesso restrito a organizadores"); return; }
        String path=req.getPathInfo();
        if(path!=null && !path.equals("/") && !path.isBlank()){JsonUtil.enviarErro(resp,404,"rota nao encontrada");return;}
        String id=req.getParameter("id"); Jogador j=JsonUtil.ler(req,Jogador.class);
        if(id==null || j==null || j.gols==null || j.assistencias==null || j.gols<0 || j.assistencias<0){JsonUtil.enviarErro(resp,400,"informe id, gols e assistencias validos; os dois campos sao obrigatorios e nao negativos");return;}
        try{int jogadorId=Integer.parseInt(id);try(Connection c=Conexao.getConexao();PreparedStatement ps=c.prepareStatement("UPDATE jogador SET gols=?,assistencias=? WHERE id=?")){ps.setInt(1,j.gols);ps.setInt(2,j.assistencias);ps.setInt(3,jogadorId);if(ps.executeUpdate()==0){JsonUtil.enviarErro(resp,404,"jogador nao encontrado");return;}JsonUtil.enviarMensagem(resp,200,"estatisticas atualizadas");}}
        catch(NumberFormatException e){JsonUtil.enviarErro(resp,400,"id invalido");}catch(SQLException e){JsonUtil.enviarErro(resp,500,"nao foi possivel atualizar as estatisticas");}
    }
    public static class Jogador { public Integer id,timeId; public String nome; public Integer gols=0,assistencias=0; }
}

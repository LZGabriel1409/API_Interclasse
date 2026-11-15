package com.example.controller;

import com.example.dao.PartidaDao;
import com.example.dao.ModalidadeDao;
import com.example.model.Partida;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/** Retorna partidas organizadas por fase do torneio. */
@WebServlet("/chaveamento")
public class ChaveamentoController extends HttpServlet {
    private final PartidaDao dao=new PartidaDao();
    private final ModalidadeDao modalidadeDao=new ModalidadeDao();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        if(!SessaoUtil.estaLogado(req)){JsonUtil.enviarErro(resp,401,"faca login para consultar o chaveamento");return;}
        String modalidade=req.getParameter("modalidadeId");
        if(modalidade==null || modalidade.isBlank()){JsonUtil.enviarErro(resp,400,"modalidadeId e obrigatorio");return;}
        try{
            int id=Integer.parseInt(modalidade);
            if(modalidadeDao.buscarPorId(id)==null){JsonUtil.enviarErro(resp,404,"modalidade nao encontrada");return;}
            List<Partida> partidas=dao.listarComFiltros(id,null,null);
            Map<String,List<Partida>> fases=new LinkedHashMap<>();
            for(String f:Arrays.asList("GRUPOS","QUARTAS","SEMI","FINAL"))fases.put(f,new ArrayList<>());
            for(Partida p:partidas){String fase=p.getFase()==null?"AVULSO":p.getFase().toUpperCase();if(fases.containsKey(fase))fases.get(fase).add(p);}
            Map<String,Object> resultado=new LinkedHashMap<>();resultado.put("modalidadeId",id);resultado.put("fases",fases);resultado.put("observacao","As partidas do mata-mata devem ser cadastradas pelo organizador com a fase correspondente. A API lista o chaveamento e seus resultados.");
            JsonUtil.enviar(resp,resultado);
        }catch(NumberFormatException e){JsonUtil.enviarErro(resp,400,"modalidadeId invalido");}catch(SQLException e){JsonUtil.enviarErro(resp,500,"erro ao consultar chaveamento");}
    }
}

package com.example.controller;

import com.example.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raiz da API. Apenas um resumo das rotas disponiveis, util para testar
 * se o servidor subiu (GET /api).
 */
@WebServlet("/")
public class ApiController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("nome", "API Interclasse ETEC");
        resumo.put("versao", "0.2.0");
        resumo.put("descricao", "Sistema de organizacao do interclasse de final de ano");

        Map<String, Object> rotas = new LinkedHashMap<>();
        rotas.put("GET /api", "Resumo da API");
        rotas.put("POST /api/auth/registrar", "Cadastro de participante");
        rotas.put("POST /api/auth/registrar-organizador", "Cadastro de organizador (chave mestra)");
        rotas.put("POST /api/auth/login", "Login");
        rotas.put("POST /api/auth/logout", "Logout");
        rotas.put("GET /api/auth/me", "Usuario logado");

        rotas.put("GET /api/times", "Lista de times");
        rotas.put("GET /api/times?id=1", "Detalhe de um time");
        rotas.put("POST /api/times", "Cria um time");
        rotas.put("PUT /api/times?id=1", "Atualiza um time");
        rotas.put("DELETE /api/times?id=1", "Remove um time");

        rotas.put("GET /api/turmas", "Lista de turmas");
        rotas.put("GET /api/turmas?id=1", "Detalhe de uma turma");
        rotas.put("POST /api/turmas", "Cadastra uma turma");
        rotas.put("PUT /api/turmas?id=1", "Atualiza uma turma");
        rotas.put("DELETE /api/turmas?id=1", "Remove uma turma");

        rotas.put("GET /api/modalidades", "Lista de modalidades");
        rotas.put("GET /api/modalidades?id=1", "Detalhe de uma modalidade");
        rotas.put("POST /api/modalidades", "Cadastra uma modalidade");
        rotas.put("PUT /api/modalidades?id=1", "Atualiza nome e/ou ativo");
        rotas.put("PUT /api/modalidades?id=1&ativo=0", "Ativa (1) ou desativa (0) a modalidade");
        rotas.put("DELETE /api/modalidades?id=1", "Remove uma modalidade");

        rotas.put("GET /api/inscricoes", "Lista de inscricoes");
        rotas.put("GET /api/inscricoes?modalidadeId=1", "Inscricoes de uma modalidade");
        rotas.put("POST /api/inscricoes", "Inscreve um time (nasce PENDENTE)");
        rotas.put("PUT /api/inscricoes?id=1", "Organizador confirma ou nega");

        rotas.put("GET /api/partidas", "Lista de partidas");
        rotas.put("GET /api/chaveamento?modalidadeId=1", "Partidas separadas por GRUPOS, QUARTAS, SEMI e FINAL");
        rotas.put("GET /api/jogadores/ranking", "Ranking individual de gols e assistencias");
        rotas.put("POST /api/jogadores", "Organizador cadastra jogador");
        rotas.put("PUT /api/jogadores?id=1", "Organizador atualiza gols e assistencias");
        rotas.put("GET /api/classificacao?modalidadeId=1&formato=csv", "Exporta classificacao em CSV");
        rotas.put("GET /api/partidas?modalidadeId=1&status=AGENDADA&data=2026-11-20", "Partidas filtradas");
        rotas.put("POST /api/partidas", "Agenda uma partida");
        rotas.put("PATCH /api/partidas?id=1", "Atualiza o placar (organizador)");
        rotas.put("POST /api/partidas/finalizar?id=1", "Finaliza a partida (organizador)");

        resumo.put("rotas", rotas);
        JsonUtil.enviar(response, resumo);
    }
}

package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados do usuário logado (Perfil). */
@Service
public class UsuarioService {
  private final UsuarioRepository usuarios;

  public UsuarioService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

  /** Monta a resposta JSON do usuário (sem senha, sem token). */
  public UsuarioRes dados(Usuario u) {
    return new UsuarioRes(u.getId(), u.getNome(), u.getIdade(), u.getEmail());
  }

  @Transactional
  public UsuarioRes atualizar(Usuario u, PerfilReq r) {
    u.setNome(r.nome().trim());
    u.setIdade(r.idade());
    return dados(usuarios.save(u));
  }
}

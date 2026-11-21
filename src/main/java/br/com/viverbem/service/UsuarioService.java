package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Usuario;
import br.com.viverbem.repository.SupervisorRepository;
import br.com.viverbem.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados do usuário logado (Perfil). */
@Service
public class UsuarioService {
  private final UsuarioRepository usuarios;
  private final SupervisorRepository supervisores;

  public UsuarioService(UsuarioRepository usuarios, SupervisorRepository supervisores) {
    this.usuarios = usuarios; this.supervisores = supervisores;
  }

  /** Monta a resposta JSON do usuário (sem senha, sem token). */
  @Transactional(readOnly = true)
  public UsuarioRes dados(Usuario u) {
    SupervisorRes sup = supervisores.findFirstByUsuarioId(u.getId())
        .map(s -> new SupervisorRes(s.getNome(), s.getParentesco(), s.isOnline())).orElse(null);
    return new UsuarioRes(u.getId(), u.getNome(), u.getIdade(), u.getEmail(), sup);
  }

  @Transactional
  public UsuarioRes atualizar(Usuario u, PerfilReq r) {
    u.setNome(r.nome().trim());
    u.setIdade(r.idade());
    return dados(usuarios.save(u));
  }
}

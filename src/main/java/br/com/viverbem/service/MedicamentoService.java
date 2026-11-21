package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;
import static java.time.DayOfWeek.*;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.*;
import br.com.viverbem.repository.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class MedicamentoService {
  /** Siglas usadas no JSON ↔ dias da semana do Java. */
  private static final Map<String, DayOfWeek> DIAS = Map.of(
      "SEG", MONDAY, "TER", TUESDAY, "QUA", WEDNESDAY, "QUI", THURSDAY,
      "SEX", FRIDAY, "SAB", SATURDAY, "DOM", SUNDAY);
  private static final Map<DayOfWeek, String> SIGLAS = DIAS.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  /** Minutos depois do horário em que o remédio ainda conta como "no horário". */
  public static final int TOLERANCIA_MIN = 30;
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

  private final MedicamentoRepository meds;
  private final TomadaRepository tomadas;

  public MedicamentoService(MedicamentoRepository meds, TomadaRepository tomadas) {
    this.meds = meds; this.tomadas = tomadas;
  }

  /** Só os remédios agendados para aquela data, em ordem de horário, com o status daquela data. */
  @Transactional(readOnly = true)
  public List<MedicamentoRes> listar(Usuario u, LocalDate data) {
    Map<Long, Tomada> tomadasDoDia = tomadas.findByMedicamentoUsuarioIdAndData(u.getId(), data).stream()
        .collect(Collectors.toMap(t -> t.getMedicamento().getId(), Function.identity()));
    LocalDateTime agora = Fuso.agora();
    return meds.findByUsuarioIdAndAtivoTrueOrderByHorario(u.getId()).stream()
        .filter(m -> m.agendadoEm(data))
        .map(m -> res(m, data, tomadasDoDia.get(m.getId()), agora)).toList();
  }

  /**
   * Regra única do status de um remédio numa data (o front só exibe). Limite = horário + TOLERANCIA_MIN.
   * Tomou até o limite (ou tomada antiga sem hora) = TOMADO; tomou depois = TOMADO_COM_ATRASO;
   * não tomou e agora já passou do limite = ATRASADO (vale também para dia passado); senão PENDENTE.
   */
  public static String calcularStatus(Medicamento m, LocalDate data, Tomada tomada, LocalDateTime agora) {
    LocalDateTime limite = data.atTime(m.getHorario()).plusMinutes(TOLERANCIA_MIN);
    if (tomada != null) {
      boolean comAtraso = tomada.getTomadoEm() != null && tomada.getTomadoEm().isAfter(limite);
      return comAtraso ? "TOMADO_COM_ATRASO" : "TOMADO";
    }
    return agora.isAfter(limite) ? "ATRASADO" : "PENDENTE";
  }

  public MedicamentoRes criar(Usuario u, MedicamentoReq r) {
    Medicamento m = new Medicamento();
    m.setUsuario(u); m.setNome(r.nome().trim()); m.setDose(r.dose().trim());
    m.setHorario(LocalTime.parse(r.horario())); m.setOrientacao(r.orientacao());
    m.setDataInicio(Fuso.hoje());
    m.setDiasSemana(converterDias(r.diasSemana()));
    return res(meds.save(m), Fuso.hoje(), null, Fuso.agora());
  }

  /**
   * Edita nome, dose, horário, orientação e dias. A dataInicio não muda.
   * Limitação do MVP: os dias novos também valem para o cálculo dos dias passados no histórico.
   */
  public MedicamentoRes atualizar(Usuario u, Long id, MedicamentoReq r) {
    Medicamento m = buscarDoUsuario(u, id);
    m.setNome(r.nome().trim()); m.setDose(r.dose().trim());
    m.setHorario(LocalTime.parse(r.horario())); m.setOrientacao(r.orientacao());
    m.getDiasSemana().clear();
    m.getDiasSemana().addAll(converterDias(r.diasSemana()));
    LocalDate hoje = Fuso.hoje();
    return res(meds.save(m), hoje, tomadas.findByMedicamentoIdAndData(id, hoje).orElse(null), Fuso.agora());
  }

  /**
   * Exclusão que preserva o histórico: sem nenhuma tomada, apaga de vez;
   * com tomadas, encerra ontem (some de hoje em diante, mas os dias passados continuam contando).
   */
  public void excluir(Usuario u, Long id) {
    Medicamento m = buscarDoUsuario(u, id);
    if (tomadas.existsByMedicamentoId(id)) {
      m.setDataFim(Fuso.hoje().minusDays(1));
      meds.save(m);
    } else {
      meds.delete(m); // o Hibernate apaga junto as linhas de medicamento_dia
    }
  }

  public MedicamentoRes tomar(Usuario u, Long id, LocalDate data) {
    if (!data.equals(Fuso.hoje()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só é possível marcar o remédio de hoje.");
    Medicamento m = buscarDoUsuario(u, id);
    if (!m.agendadoEm(data))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este remédio não está agendado para hoje.");
    // A hora da tomada é sempre a do servidor (Tomada.tomadoEm); nunca vem do cliente.
    Tomada t = tomadas.findByMedicamentoIdAndData(id, data).orElseGet(() -> {
      Tomada nova = new Tomada(); nova.setMedicamento(m); nova.setData(data); return tomadas.save(nova);
    });
    return res(m, data, t, Fuso.agora());
  }

  /** Remédio do usuário logado e ainda não encerrado; senão 404 (não revela remédio de outra pessoa). */
  private Medicamento buscarDoUsuario(Usuario u, Long id) {
    return meds.findByIdAndUsuarioId(id, u.getId())
        .filter(m -> m.getDataFim() == null || !m.getDataFim().isBefore(Fuso.hoje()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Remédio não encontrado."));
  }

  /** ["SEG","QUA"] -> {MONDAY, WEDNESDAY} */
  private Set<DayOfWeek> converterDias(List<String> siglas) {
    return siglas.stream().map(DIAS::get).collect(Collectors.toCollection(HashSet::new));
  }

  /** tomada = registro daquela data, ou null se não tomou. */
  private MedicamentoRes res(Medicamento m, LocalDate data, Tomada tomada, LocalDateTime agora) {
    List<String> dias = Arrays.stream(DayOfWeek.values()) // MONDAY ... SUNDAY = SEG ... DOM
        .filter(m.getDiasSemana()::contains).map(SIGLAS::get).toList();
    String tomadoEm = tomada == null || tomada.getTomadoEm() == null ? null : tomada.getTomadoEm().format(HORA);
    return new MedicamentoRes(m.getId(), m.getNome(), m.getDose(), m.getHorario().toString(), m.getOrientacao(),
        tomada != null, dias, m.getDataInicio(), calcularStatus(m, data, tomada, agora), tomadoEm);
  }
}

package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;
import static java.time.DayOfWeek.*;

import br.com.viverbem.config.Fuso;
import br.com.viverbem.model.*;
import br.com.viverbem.repository.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private final MedicamentoRepository meds;
  private final TomadaRepository tomadas;

  public MedicamentoService(MedicamentoRepository meds, TomadaRepository tomadas) {
    this.meds = meds; this.tomadas = tomadas;
  }

  /** Só os remédios agendados para aquela data, em ordem de horário, com "tomado" daquela data. */
  @Transactional(readOnly = true)
  public List<MedicamentoRes> listar(Usuario u, LocalDate data) {
    Set<Long> tomados = tomadas.findByMedicamentoUsuarioIdAndData(u.getId(), data).stream()
        .map(t -> t.getMedicamento().getId()).collect(Collectors.toSet());
    return meds.findByUsuarioIdAndAtivoTrueOrderByHorario(u.getId()).stream()
        .filter(m -> m.agendadoEm(data))
        .map(m -> res(m, tomados.contains(m.getId()))).toList();
  }

  public MedicamentoRes criar(Usuario u, MedicamentoReq r) {
    Medicamento m = new Medicamento();
    m.setUsuario(u); m.setNome(r.nome().trim()); m.setDose(r.dose().trim());
    m.setHorario(LocalTime.parse(r.horario())); m.setOrientacao(r.orientacao());
    m.setDataInicio(Fuso.hoje());
    m.setDiasSemana(r.diasSemana().stream().map(DIAS::get).collect(Collectors.toCollection(HashSet::new)));
    return res(meds.save(m), false);
  }

  public MedicamentoRes tomar(Usuario u, Long id, LocalDate data) {
    if (!data.equals(Fuso.hoje()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Só é possível marcar o remédio de hoje.");
    Medicamento m = meds.findByIdAndUsuarioId(id, u.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Remédio não encontrado."));
    if (!m.agendadoEm(data))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este remédio não está agendado para hoje.");
    if (!tomadas.existsByMedicamentoIdAndData(id, data)) {
      Tomada t = new Tomada(); t.setMedicamento(m); t.setData(data); tomadas.save(t);
    }
    return res(m, true);
  }

  private MedicamentoRes res(Medicamento m, boolean tomado) {
    List<String> dias = Arrays.stream(DayOfWeek.values()) // MONDAY ... SUNDAY = SEG ... DOM
        .filter(m.getDiasSemana()::contains).map(SIGLAS::get).toList();
    return new MedicamentoRes(m.getId(), m.getNome(), m.getDose(), m.getHorario().toString(), m.getOrientacao(),
        tomado, dias, m.getDataInicio());
  }
}

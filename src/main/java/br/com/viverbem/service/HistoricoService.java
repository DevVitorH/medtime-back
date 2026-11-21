package br.com.viverbem.service;

import static br.com.viverbem.dto.Dtos.*;

import br.com.viverbem.model.Medicamento;
import br.com.viverbem.model.Tomada;
import br.com.viverbem.repository.MedicamentoRepository;
import br.com.viverbem.repository.TomadaRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class HistoricoService {
  private final MedicamentoRepository meds;
  private final TomadaRepository tomadas;

  public HistoricoService(MedicamentoRepository meds, TomadaRepository tomadas) {
    this.meds = meds; this.tomadas = tomadas;
  }

  /**
   * Dia "TOMOU" = todos os remédios agendados naquele dia foram tomados. Só conta dias anteriores a "hoje".
   * Dia sem nenhum remédio agendado não aparece e não quebra a sequência.
   */
  public HistoricoRes calcular(Long usuarioId, YearMonth mes, LocalDate hoje) {
    List<Medicamento> ativos = meds.findByUsuarioIdAndAtivoTrueOrderByHorario(usuarioId);
    LocalDate ini = mes.atDay(1);
    LocalDate ontem = hoje.minusDays(1);
    LocalDate fim = mes.atEndOfMonth().isBefore(ontem) ? mes.atEndOfMonth() : ontem;
    List<DiaRes> dias = new ArrayList<>();
    int previstas = 0, tomadasTotal = 0;
    if (!fim.isBefore(ini)) {
      // Para cada dia, os ids dos remédios tomados.
      Map<LocalDate, Set<Long>> porDia = tomadas.findByMedicamentoUsuarioIdAndDataBetween(usuarioId, ini, fim).stream()
          .collect(Collectors.groupingBy(Tomada::getData,
              Collectors.mapping(t -> t.getMedicamento().getId(), Collectors.toSet())));
      for (LocalDate d = ini; !d.isAfter(fim); d = d.plusDays(1)) {
        LocalDate dia = d;
        List<Medicamento> agendados = ativos.stream().filter(m -> m.agendadoEm(dia)).toList();
        if (agendados.isEmpty()) continue;
        Set<Long> feitos = porDia.getOrDefault(d, Set.of());
        int t = (int) agendados.stream().filter(m -> feitos.contains(m.getId())).count();
        previstas += agendados.size(); tomadasTotal += t;
        dias.add(new DiaRes(d.getDayOfMonth(), t == agendados.size() ? "TOMOU" : "FALHOU"));
      }
    }
    int sequencia = 0;
    for (int i = dias.size() - 1; i >= 0 && "TOMOU".equals(dias.get(i).status()); i--) sequencia++;
    int adesao = previstas == 0 ? 0 : Math.round(100f * tomadasTotal / previstas);
    return new HistoricoRes(sequencia, adesao, dias);
  }
}

package com.prettypetty.dto;

import com.prettypetty.model.PlannerBoard;
import com.prettypetty.model.PlannerNote;
import java.util.List;

public record BoardResponse(PlannerBoard board, List<PlannerNote> notes) {
}
